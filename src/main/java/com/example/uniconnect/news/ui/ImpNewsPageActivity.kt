package com.example.uniconnect.news.ui

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.icu.text.DateFormat
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.uniconnect.R
import com.example.uniconnect.news.model.ImpNewsItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import okhttp3.Call
import okhttp3.Callback
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.util.Date


class ImpNewsPageActivity : ComponentActivity() {


    private lateinit var addButton: Button
    private lateinit var backButton: Button

    private var isTeacher: Boolean = false

    // List to store imp news items
    private val newsList: MutableList<ImpNewsItem> = mutableListOf()
    private val firestore = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_imp_news_page)

        updateUI()

        addButton = findViewById(R.id.addImpNewsButton)
        backButton = findViewById(R.id.buttonBack)

        isTeacher = intent.getBooleanExtra("isTeacher", false)

        if (isTeacher) {
            addButton.visibility = View.VISIBLE
        } else {
            addButton.visibility = View.GONE
        }


        addButton.setOnClickListener {
            addImpNews()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    private fun addImpNews() {
        val dialogView = LayoutInflater.from(this@ImpNewsPageActivity)
            .inflate(R.layout.add_imp_news_dialog, null)


        val title = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val description = dialogView.findViewById<EditText>(R.id.editTextContent)

        val yearRG = dialogView.findViewById<RadioGroup>(R.id.yearRadioGroup)
        val groupRG = dialogView.findViewById<RadioGroup>(R.id.groupRadioGroup)
        val semigroupRG = dialogView.findViewById<RadioGroup>(R.id.semigroupRadioGroup)

        val alertDialogBuilder = AlertDialog.Builder(this@ImpNewsPageActivity)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setTitle("Add Important News")

        // If add operation is confirmed imp news item is added to firestore
        alertDialogBuilder.setPositiveButton(
            "Add"
        ) { _, _ ->
            val titleText = title.text.toString().trim()
            val contentText = description.text.toString().trim()
            val currentTime = DateFormat.getDateInstance().format(Date())

            val selectedYearId: Int = yearRG.checkedRadioButtonId
            val selectedGroupId: Int = groupRG.checkedRadioButtonId
            val selectedSemigroupId: Int = semigroupRG.checkedRadioButtonId

            val selectedYear = dialogView.findViewById<RadioButton>(selectedYearId)
            val selectedYearText = selectedYear.text.toString()

            val selectedGroup = dialogView.findViewById<RadioButton>(selectedGroupId)
            val selectedGroupText = selectedGroup.text.toString()

            val selectedSemigroup = dialogView.findViewById<RadioButton>(selectedSemigroupId)
            val selectedSemigroupText = selectedSemigroup.text.toString()

            val newsItem = ImpNewsItem(
                titleText,
                contentText,
                currentTime,
                selectedYearText,
                selectedGroupText,
                selectedSemigroupText
            )
            newsList.add(newsItem)
            saveToDB(newsItem)
            updateUI()
            Toast.makeText(this, "News added successfully!", Toast.LENGTH_SHORT).show()
        }
        alertDialogBuilder.setNegativeButton(
            "Cancel"
        ) { dialog, _ -> dialog.dismiss() }
        alertDialogBuilder.create().show()
    }

    // Inflate layout for imp news items and add that layout in the imp news page layout
    private fun display(newsItem: ImpNewsItem) {

        val mainNewsPageLayout = findViewById<LinearLayout>(R.id.impNewsPageLayout)

        val inflater = LayoutInflater.from(this)
        val impNewsItemLayout =
            inflater.inflate(R.layout.imp_news_item_layout, mainNewsPageLayout, false)

        val textViewDate = impNewsItemLayout.findViewById<TextView>(R.id.textViewDate)
        val textViewTitle = impNewsItemLayout.findViewById<TextView>(R.id.textViewTitle)
        val textViewContent = impNewsItemLayout.findViewById<TextView>(R.id.textViewContent)
        val textViewYear = impNewsItemLayout.findViewById<TextView>(R.id.textViewYear)
        val textViewGroup = impNewsItemLayout.findViewById<TextView>(R.id.textViewGroup)
        val textViewSemigroup = impNewsItemLayout.findViewById<TextView>(R.id.textViewSemigroup)
        textViewDate.text = newsItem.currentDate
        textViewTitle.text = newsItem.title
        textViewContent.text = newsItem.description
        textViewYear.text = newsItem.year
        textViewGroup.text = newsItem.group
        textViewSemigroup.text = newsItem.semigroup

        textViewDate.setTextColor(ContextCompat.getColor(this, R.color.white))
        textViewTitle.setTextColor(ContextCompat.getColor(this, R.color.white))
        textViewContent.setTextColor(ContextCompat.getColor(this, R.color.white))
        textViewYear.setTextColor(ContextCompat.getColor(this, R.color.white))
        textViewGroup.setTextColor(ContextCompat.getColor(this, R.color.white))
        textViewSemigroup.setTextColor(ContextCompat.getColor(this, R.color.white))

        // Adding the news item layout to main news page layout
        mainNewsPageLayout.addView(impNewsItemLayout)

        // Showing full importanr news item when clicked on
        impNewsItemLayout.setOnClickListener {
            showFullContentDialog(newsItem.title, newsItem.description)
        }

        val deleteButton = impNewsItemLayout.findViewById<Button>(R.id.deleteButton)
        val editButton = impNewsItemLayout.findViewById<Button>(R.id.editButton)

        if (isTeacher) {
            deleteButton.visibility = View.VISIBLE
            editButton.visibility = View.VISIBLE
        } else {
            deleteButton.visibility = View.GONE
            editButton.visibility = View.GONE
        }

        deleteButton.setOnClickListener {
            deleteNews(newsItem)
        }

        editButton.setOnClickListener {
            editNews(newsItem)
        }
    }

    private fun showFullContentDialog(title: String, content: String) {
        val intent = Intent(this@ImpNewsPageActivity, ViewNewsActivity::class.java).apply {
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_CONTENT", content)
        }
        startActivity(intent)
    }

    private fun deleteNews(newsItem: ImpNewsItem) {
        deleteFromDB(newsItem)
    }

    private fun editNews(newsItem: ImpNewsItem) {
        val dialogView = LayoutInflater.from(
            this@ImpNewsPageActivity
        ).inflate(R.layout.add_news_dialog, null)

        val titleEditText = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val contentEditText = dialogView.findViewById<EditText>(R.id.editTextContent)

        titleEditText.setText(newsItem.title)
        contentEditText.setText(newsItem.description)

        val alertDialogBuilder = AlertDialog.Builder(this@ImpNewsPageActivity)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setTitle("Edit News")

        alertDialogBuilder.setPositiveButton("Edit") { _, _ ->
            val editedTitle = titleEditText.text.toString().trim()
            val editedContent = contentEditText.text.toString().trim()

            val oldNewsItem = ImpNewsItem(
                newsItem.title,
                newsItem.description,
                newsItem.currentDate,
                newsItem.year,
                newsItem.group,
                newsItem.semigroup
            )

            // Updating the news item in the list
            newsItem.title = editedTitle
            newsItem.description = editedContent

            // Updating the UI
            updateDB(oldNewsItem, newsItem)
        }
        alertDialogBuilder.setNegativeButton(
            "Cancel"
        ) { dialog, _ -> dialog.dismiss() }
        alertDialogBuilder.create().show()
    }


    private suspend fun filterNewsForUser(): Triple<String, String, String> {
        val auth: FirebaseAuth = Firebase.auth
        val currUser = auth.currentUser
        var userInfo = Triple("", "", "")

        try {
            val users = firestore.collection("Users").get().await()

            for (user in users) {
                if (user.data["Email"].toString() == currUser?.email) {
                    userInfo = Triple(
                        user.data["Year"].toString(),
                        user.data["Group"].toString(),
                        user.data["Semigroup"].toString()
                    )
                }
            }
        } catch (exception: Exception) {
            Log.d(TAG, "Error getting documents: ", exception)
        }

        return userInfo
    }


    private fun updateUI() {
        // Updating the UI with the latest news list
        val mainNewsPageLayout = findViewById<LinearLayout>(R.id.impNewsPageLayout)
        mainNewsPageLayout.removeAllViews()

        lifecycleScope.launch {
            val userInfo = filterNewsForUser()

            firestore.collection("ImpNews").get().addOnSuccessListener { result ->
                val sortedResult = result.sortedByDescending { it.data["CurrentDate"].toString() }
                for (document in sortedResult) {
                    // The user is a teacher, they can view all news
                    if (userInfo.first == "" && userInfo.second == "" && userInfo.third == "") {
                        display(
                            ImpNewsItem(
                                document.data["Title"].toString(),
                                document.data["Content"].toString(),
                                document.data["CurrentDate"].toString(),
                                document.data["Year"].toString(),
                                document.data["Group"].toString(),
                                document.data["Semigroup"].toString()
                            )
                        )
                    }
                    // The user is a student, show only relevant news
                    else if (document.data["Year"].toString() == userInfo.first && (document.data["Group"].toString() == userInfo.second || document.data["Group"].toString() == "all") && (document.data["Semigroup"].toString() == userInfo.third || document.data["Semigroup"].toString() == "all")) {
                        display(
                            ImpNewsItem(
                                document.data["Title"].toString(),
                                document.data["Content"].toString(),
                                document.data["CurrentDate"].toString(),
                                document.data["Year"].toString(),
                                document.data["Group"].toString(),
                                document.data["Semigroup"].toString()
                            )
                        )
                        Log.d(
                            TAG,
                            "${document.id}, ${document.data["Title"]}, ${document.data["Content"]}"
                        )
                    }

                }
            }.addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
        }

    }

    private fun saveToDB(newsItem: ImpNewsItem) {
        val hashNewsItem = hashMap(newsItem)

        firestore.collection("ImpNews").add(hashNewsItem)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                // Fetch FCM tokens of users in the target group and send notifications
                fetchGroupMembersAndSendNotification(newsItem)
            }.addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
    }

    private fun fetchGroupMembersAndSendNotification(newsItem: ImpNewsItem) {

        if(newsItem.group!="all" && newsItem.semigroup!="all") {
            firestore.collection("Users")
                .whereEqualTo("Year", newsItem.year)
                .whereEqualTo("Group", newsItem.group)
                .whereEqualTo("Semigroup", newsItem.semigroup)
                .get()
                .addOnSuccessListener { documents ->
                    val fcmTokens = mutableListOf<String>()

                    for (document in documents) {
                        val fcmToken = document.getString("fcmToken")
                        fcmToken?.let { fcmTokens.add(it) }
                    }

                    // Send notification to users in the target group
                    sendImpNewsNotificationToTokens(newsItem.title, newsItem.description, fcmTokens)
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents: ", exception)
                }
        }
        if (newsItem.group=="all" && newsItem.semigroup!="all")
        {
            firestore.collection("Users")
                .whereEqualTo("Year", newsItem.year)
                .whereEqualTo("Semigroup", newsItem.semigroup)
                .get()
                .addOnSuccessListener { documents ->
                    val fcmTokens = mutableListOf<String>()

                    for (document in documents) {
                        val fcmToken = document.getString("fcmToken")
                        fcmToken?.let { fcmTokens.add(it) }
                    }

                    // Send notification to users in the target group
                    sendImpNewsNotificationToTokens(newsItem.title, newsItem.description, fcmTokens)
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents: ", exception)
                }
        }
        if(newsItem.group!="all" && newsItem.semigroup=="all")
        {
            firestore.collection("Users")
                .whereEqualTo("Year", newsItem.year)
                .whereEqualTo("Group", newsItem.group)
                .get()
                .addOnSuccessListener { documents ->
                    val fcmTokens = mutableListOf<String>()

                    for (document in documents) {
                        val fcmToken = document.getString("fcmToken")
                        fcmToken?.let { fcmTokens.add(it) }
                    }

                    // Send notification to users in the target group
                    sendImpNewsNotificationToTokens(newsItem.title, newsItem.description, fcmTokens)
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents: ", exception)
                }
        }
        if (newsItem.group=="all" && newsItem.semigroup=="all")
        {
            firestore.collection("Users")
                .whereEqualTo("Year", newsItem.year)
                .get()
                .addOnSuccessListener { documents ->
                    val fcmTokens = mutableListOf<String>()

                    for (document in documents) {
                        val fcmToken = document.getString("fcmToken")
                        fcmToken?.let { fcmTokens.add(it) }
                    }

                    // Send notification to users in the target group
                    sendImpNewsNotificationToTokens(newsItem.title, newsItem.description, fcmTokens)
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting documents: ", exception)
                }
        }
    }


    private fun sendImpNewsNotificationToTokens(title: String, message: String, fcmTokens: List<String>) {
        // Construct the notification payload as a JSON object
        val notification = JSONObject().apply {
            put("title", title)
            put("body", message)
        }

        // Construct the data payload as a JSON object
        val data = JSONObject().apply {
            put("title", title)
            put("message", message)
        }

        // Construct the JSON payload
        val payload = JSONObject().apply {
            put("notification", notification)
            put("data", data)
            put("registration_ids", JSONArray(fcmTokens))
        }


        val JSON = "application/json; charset=utf-8".toMediaType()
        val client = OkHttpClient()
        val url = "https://fcm.googleapis.com/fcm/send"
        val body = RequestBody.create(JSON, payload.toString())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer PRIVATE")
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.d(TAG, "Failed to send notification: $e")
            }

            override fun onResponse(call: Call, response: Response) {
                val responseBody = response.body?.string()

                if (response.isSuccessful) {
                    Log.d(TAG, "Notification sent successfully")
                } else {
                    Log.d(TAG, "Failed to send notification: ${response.code}")
                    // Log the response body for further analysis
                    Log.d(TAG, "Response Body: $responseBody")
                }
            }
        })
    }



    private fun deleteFromDB(newsItem: ImpNewsItem) {
        firestore.collection("ImpNews").whereEqualTo("Title", newsItem.title)
            .whereEqualTo("Content", newsItem.description)
            .whereEqualTo("CurrentDate", newsItem.currentDate).whereEqualTo("Year", newsItem.year)
            .whereEqualTo("Group", newsItem.group).whereEqualTo("Semigroup", newsItem.semigroup)
            .get().addOnSuccessListener { documents ->
                for (document in documents) {
                    firestore.collection("ImpNews").document(document.id).delete()
                }
                updateUI()
                Log.d(TAG, "DocumentSnapshot successfully deleted!")
            }.addOnFailureListener { exception ->
                Log.w(TAG, "Error deleting document", exception)
            }
    }

    private fun updateDB(oldNewsItem: ImpNewsItem, newsItem: ImpNewsItem) {
        firestore.collection("ImpNews").whereEqualTo("Title", oldNewsItem.title)
            .whereEqualTo("Content", oldNewsItem.description)
            .whereEqualTo("CurrentDate", oldNewsItem.currentDate)
            .whereEqualTo("Year", oldNewsItem.year).whereEqualTo("Group", oldNewsItem.group)
            .whereEqualTo("Semigroup", oldNewsItem.semigroup).get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    firestore.collection("ImpNews").document(document.id).update(
                        mapOf(
                            "Title" to newsItem.title,
                            "Content" to newsItem.description,
                            "CurrentDate" to newsItem.currentDate,
                            "Year" to oldNewsItem.year,
                            "Group" to oldNewsItem.group,
                            "Semigroup" to oldNewsItem.semigroup
                        )
                    )
                    updateUI()
                }
                Log.d(TAG, "DocumentSnapshot successfully updated!")
            }.addOnFailureListener { exception ->
                Log.w(TAG, "Error updating document", exception)
            }
    }

    private fun hashMap(newsItem: ImpNewsItem) = hashMapOf(
        "Title" to newsItem.title,
        "Content" to newsItem.description,
        "CurrentDate" to newsItem.currentDate,
        "Year" to newsItem.year,
        "Group" to newsItem.group,
        "Semigroup" to newsItem.semigroup
    )

}

