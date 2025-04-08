package com.example.uniconnect.news.ui

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.icu.text.DateFormat.getDateInstance
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.core.content.ContextCompat
import com.example.uniconnect.R
import com.example.uniconnect.news.model.NewsItem
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
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


class NewsPageActivity : ComponentActivity() {

    private lateinit var addButton: Button
    private lateinit var backButton: Button


    private var isTeacher: Boolean = false

    // List to store news items in
    private val newsList: MutableList<NewsItem> = mutableListOf()


    var newsDatabase = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_news_page)


        subscribeToNewsTopic()

        updateUI()

        addButton = findViewById(R.id.addNewsButton)
        backButton = findViewById(R.id.buttonBack)

        isTeacher = intent.getBooleanExtra("isTeacher", false)

        if (isTeacher) {
            addButton.visibility = View.VISIBLE
        } else {
            addButton.visibility = View.GONE
        }

        addButton.setOnClickListener {
            addNews()
        }

        backButton.setOnClickListener {
            finish()
        }
    }

    //display news dinamically
    //inflate layout for news items and add that layout in the news page layout
    private fun display(newsItemId: String, newsItem: NewsItem) {


        val mainNewsPageLayout = findViewById<LinearLayout>(R.id.newsPageLayout)

        val inflater = LayoutInflater.from(this)
        val newsItemLayout = inflater.inflate(R.layout.news_item, mainNewsPageLayout, false)

        val textViewDate = newsItemLayout.findViewById<TextView>(R.id.textViewDate)
        val textViewTitle = newsItemLayout.findViewById<TextView>(R.id.textViewTitle)
        val textViewContent = newsItemLayout.findViewById<TextView>(R.id.textViewContent)
        textViewDate.text = newsItem.currentDate
        textViewTitle.text = newsItem.title
        textViewContent.text = newsItem.content

        textViewDate.setTextColor(ContextCompat.getColor(this, R.color.white))
        textViewTitle.setTextColor(ContextCompat.getColor(this, R.color.black))
        textViewContent.setTextColor(ContextCompat.getColor(this, R.color.black))

        val likeCountTextView = newsItemLayout.findViewById<TextView>(R.id.likeCount)
        val dislikeCountTextView = newsItemLayout.findViewById<TextView>(R.id.dislikeCount)
        likeCountTextView.text = newsItem.likes.size.toString()

        val likeCount = newsItem.likes.size
        likeCountTextView.text = likeCount.toString()
        Log.d("NewsLikes", "Likes count: $likeCount")

        dislikeCountTextView.text = newsItem.dislikes.size.toString()

        var userEmail: String? = null
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            userEmail = user.email.toString()
        }

        // Handle like and dislike clicks
        val likeIcon = newsItemLayout.findViewById<ImageView>(R.id.likeIcon)
        val dislikeIcon = newsItemLayout.findViewById<ImageView>(R.id.dislikeIcon)


        likeIcon.setOnClickListener {
            if (userEmail != null) {
                newsItem.likeNewsItem(newsItemId, userEmail)
                // Update the UI
                updateLikeDislikeCounts(newsItem, likeCountTextView, dislikeCountTextView)
            }
        }

        dislikeIcon.setOnClickListener {
            if (userEmail != null) {
                newsItem.dislikeNewsItem(newsItemId, userEmail)
                // Update the UI
                updateLikeDislikeCounts(newsItem, likeCountTextView, dislikeCountTextView)
            }
        }


        // Adding the news item layout to your main news page layout
        mainNewsPageLayout.addView(newsItemLayout)

        // Show full news item when clicked on
        newsItemLayout.setOnClickListener {
            showFullContentDialog(newsItem.title, newsItem.content)
        }

        val deleteButton = newsItemLayout.findViewById<Button>(R.id.deleteButton)
        val editButton = newsItemLayout.findViewById<Button>(R.id.editButton)

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

    private fun updateLikeDislikeCounts(
        newsItem: NewsItem,
        likeCountTextView: TextView,
        dislikeCountTextView: TextView
    ) {
        // Update like and dislike counts after Firestore operation
        likeCountTextView.text = newsItem.likes.size.toString()
        dislikeCountTextView.text = newsItem.dislikes.size.toString()
    }

    private fun addNews() {
        val dialogView =
            LayoutInflater.from(this@NewsPageActivity).inflate(R.layout.add_news_dialog, null)

        val title = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val description = dialogView.findViewById<EditText>(R.id.editTextContent)

        val alertDialogBuilder = AlertDialog.Builder(this@NewsPageActivity)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setTitle("Add News")

        // If add operation confirmed by user add to firestore and update news page
        alertDialogBuilder.setPositiveButton(
            "Add"
        ) { _, _ ->
            val titleText = title.text.toString().trim()
            val contentText = description.text.toString().trim()
            val currentTime = getDateInstance().format(Date())
            val newsItem = NewsItem(titleText, contentText, currentTime, mutableListOf(), mutableListOf())
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

    // show whole content of news item after it gets clicked on
    private fun showFullContentDialog(title: String, content: String) {
        val intent = Intent(this@NewsPageActivity, ViewNewsActivity::class.java).apply {
            putExtra("EXTRA_TITLE", title)
            putExtra("EXTRA_CONTENT", content)
        }
        startActivity(intent)
    }

    private fun deleteNews(newsItem: NewsItem) {
        deleteFromDB(newsItem)
        Toast.makeText(this, "News deleted successfully!", Toast.LENGTH_SHORT).show()
    }

    private fun editNews(newsItem: NewsItem) {
        val dialogView =
            LayoutInflater.from(
                this@NewsPageActivity
            ).inflate(R.layout.add_news_dialog, null)

        val titleEditText = dialogView.findViewById<EditText>(R.id.editTextTitle)
        val contentEditText = dialogView.findViewById<EditText>(R.id.editTextContent)

        titleEditText.setText(newsItem.title)
        contentEditText.setText(newsItem.content)

        val alertDialogBuilder = AlertDialog.Builder(this@NewsPageActivity)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setTitle("Edit News")

        alertDialogBuilder.setPositiveButton("Edit") { _, _ ->
            val editedTitle = titleEditText.text.toString().trim()
            val editedContent = contentEditText.text.toString().trim()

            val oldNewsItem = NewsItem(newsItem.title, newsItem.content, newsItem.currentDate)

            newsItem.title = editedTitle
            newsItem.content = editedContent

            updateDB(oldNewsItem, newsItem)
        }
        alertDialogBuilder.setNegativeButton(
            "Cancel"
        ) { dialog, _ -> dialog.dismiss() }
        alertDialogBuilder.create().show()
    }

    fun updateUI() {
        // Update the UI with the latest news list
        val mainNewsPageLayout = findViewById<LinearLayout>(R.id.newsPageLayout)
        mainNewsPageLayout.removeAllViews()

        newsDatabase.collection("News").get().addOnSuccessListener { result ->
            val sortedResult = result.sortedByDescending { it.data["CurrentDate"].toString() }
            for (document in sortedResult) {
                // Retrieve the document ID
                val documentId = document.id
                val likes = document.data["Likes"] as? MutableList<String> ?: mutableListOf()
                val dislikes = document.data["Dislikes"] as? MutableList<String> ?: mutableListOf()

                display(documentId, NewsItem(
                    document.data["Title"].toString(),
                    document.data["Content"].toString(),
                    document.data["CurrentDate"].toString(), likes, dislikes)
                )
                Log.d(
                    TAG,
                    "$documentId, ${document.data["Title"]}, ${document.data["Content"]}"
                )
            }
        }
            .addOnFailureListener { exception ->
                Log.d(TAG, "Error getting documents: ", exception)
            }
    }


    fun saveToDB(newsItem: NewsItem) {
        val hashNewsItem = hashMap(newsItem)

        newsDatabase.collection("News")
            .add(hashNewsItem)
            .addOnSuccessListener { documentReference ->
                Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")

                // Send notification to all users
                fetchAllFCMTokensAndSendNotification(newsItem.title, newsItem.content)

            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error adding document", e)
            }
    }

    private fun fetchAllFCMTokensAndSendNotification(title: String, message: String) {
        // Fetch all FCM tokens from your database
        newsDatabase.collection("Users")
            .get()
            .addOnSuccessListener { documents ->
                val fcmTokens = mutableListOf<String>()

                for (document in documents) {
                    val fcmToken = document.getString("fcmToken")
                    fcmToken?.let { fcmTokens.add(it) }
                }

                // Send notification to all FCM tokens after fetching them
                sendNewsNotificationToTokens(title, message, fcmTokens)
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error getting documents: ", exception)
            }
    }

    private fun sendNewsNotificationToTokens(title: String, message: String, fcmTokens: List<String>) {
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
            put("registration_ids", JSONArray(fcmTokens)) // Convert List<String> to JSONArray
        }

        // Send the notification using OkHttp
        val JSON = "application/json; charset=utf-8".toMediaType()
        val client = OkHttpClient()
        val url = "https://fcm.googleapis.com/fcm/send"
        val body = RequestBody.create(JSON, payload.toString())
        val request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer AAAAiataEiI:APA91bFL-mTbqhfv8hnKWlY2EqGiFoEWVbC6-TuYfJtC56sr4m4XusLPoGF8V2PdGK3MgN8SynqPtdsq-m2AgmsMgtFm2ck19V71kLIIJDx_iU0866zTBrTRYY3qDL3L4WHcaFUNu3ci")
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
                    // Logging for analysis
                    Log.d(TAG, "Response Body: $responseBody")
                }
            }
        })
    }


    private fun deleteFromDB(newsItem: NewsItem) {
        newsDatabase.collection("News")
            .whereEqualTo("Title", newsItem.title)
            .whereEqualTo("Content", newsItem.content)
            .whereEqualTo("CurrentDate", newsItem.currentDate)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    newsDatabase.collection("News").document(document.id).delete()
                }
                updateUI()
                Log.d(TAG, "DocumentSnapshot successfully deleted!")
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error deleting document", exception)
            }
    }

    fun updateDB(oldNewsItem: NewsItem, newsItem: NewsItem) {
        newsDatabase.collection("News")
            .whereEqualTo("Title", oldNewsItem.title)
            .whereEqualTo("Content", oldNewsItem.content)
            .whereEqualTo("CurrentDate", oldNewsItem.currentDate)
            .get()
            .addOnSuccessListener { documents ->
                for (document in documents) {
                    newsDatabase.collection("News").document(document.id)
                        .update(
                            mapOf(
                                "Title" to newsItem.title,
                                "Content" to newsItem.content,
                                "CurrentDate" to newsItem.currentDate
                            )
                        )
                    updateUI()
                }
                Log.d(TAG, "DocumentSnapshot successfully updated!")
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error updating document", exception)
            }
    }

    fun hashMap(newsItem: NewsItem) = hashMapOf(
        "Title" to newsItem.title,
        "Content" to newsItem.content,
        "CurrentDate" to newsItem.currentDate,
        "Likes" to newsItem.likes,
        "Dislikes" to newsItem.dislikes
    )


    private fun subscribeToNewsTopic() {
        FirebaseMessaging.getInstance().subscribeToTopic("news")
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    Log.d(TAG, "Successfully subscribed to news topic")
                } else {
                    Log.w(TAG, "Subscription to news topic failed", task.exception)
                }
            }
    }
}
