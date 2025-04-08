package com.example.uniconnect.common.ui


import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.uniconnect.R
import com.example.uniconnect.classbook.ui.CoursesListActivity
import com.example.uniconnect.news.ui.ImpNewsPageActivity
import com.example.uniconnect.news.ui.NewsPageActivity
import com.example.uniconnect.socialFeed.ui.ChatPageActivity
import com.example.uniconnect.timetable.ui.TimetableActivity
import com.example.uniconnect.utils.AndroidUtil
import com.example.uniconnect.utils.FirebaseUtil
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class HomePageActivity : ComponentActivity() {

    private lateinit var buttonImpNews: Button
    private lateinit var buttonTimeTable: Button
    private lateinit var buttonNews: Button
    private lateinit var buttonCatalog: Button
    private lateinit var buttonLogout: Button
    private lateinit var buttonSocialFeed: Button
    private lateinit var userText: TextView
    private val firestore = Firebase.firestore
    var profilePic: ImageView? = null

    private var isTeacher: Boolean? = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_home_page)
        profilePic = findViewById(R.id.profile_image_view)

        buttonImpNews = findViewById(R.id.buttonImpNews)
        buttonTimeTable = findViewById(R.id.buttonTimeTable)
        buttonNews = findViewById(R.id.buttonNews)
        buttonCatalog = findViewById(R.id.buttonCatalog)
        buttonLogout = findViewById(R.id.logoutButton)
        buttonSocialFeed = findViewById(R.id.buttonSocialFeed)



        userText = findViewById(R.id.username_text)

        FirebaseUtil().getCurrentProfilePicStorageRef()?.getDownloadUrl()
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uri = task.result
                    AndroidUtil().setProfilePic(this, uri, profilePic)
                }
            }


        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userEmail = user.email
            val fullName = AndroidUtil().extractFullNameFromEmail(userEmail)
            val helloUser = "Hello, $fullName"
            userText.setText(helloUser)
            userText.setVisibility(View.VISIBLE);
        }

        if (user != null) {
            val userEmail = user.email

            // Check if user already exists in the database
            val db = FirebaseFirestore.getInstance()
            db.collection("Users")
                .whereEqualTo("Email", userEmail)
                .get()
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        val documents = task.result
                        if (documents != null && !documents.isEmpty) {
                            // User already exists in the database
                            val document = documents.documents[0]

                            isTeacher = document.getBoolean("isTeacher")
                            Log.d("isTeacher2", "$isTeacher")
                            if(isTeacher == true){
                                Log.d("isTeacher2", "yes true")
                                checkIfBlockedUsersExist { isBlocked ->
                                    if (isBlocked) {
                                        Log.d("CheckBlock", "isBlocked")
                                        checkBlockedUsersAndShowPopup()
                                    }
                                }

                            }
                        }

                    } else {
                        Log.d("TAG", "Error getting documents: ", task.exception)
                    }
                }
        }

        if (isTeacher!=null) {
            buttonSocialFeed.setOnClickListener(View.OnClickListener { v: View? ->
                checkIfUserReported { isReported ->
                    if (!isReported) {
                        // User has not been reported, start the chat activity
                        startActivity(Intent(this@HomePageActivity, ChatPageActivity::class.java))
                    } else {
                        // User has been reported, show toast and disable chat
                        Toast.makeText(
                            this@HomePageActivity,
                            "You have been reported by other users. Chat is disabled for now",
                            Toast.LENGTH_SHORT
                        ).show()
                    }
                }
            })


            buttonNews.setOnClickListener {
                goToNewsPage()
            }
            

            buttonImpNews.setOnClickListener {
                goToImpNewsPage()
            }
            buttonTimeTable.setOnClickListener({
                goToTimetable()
            })
            buttonLogout.setOnClickListener {
                logoutUser()
            }
            buttonCatalog.setOnClickListener {
                goToCatalog()
            }
        }


    }

    override fun onResume() {
        super.onResume()

        // Update profile picture
        FirebaseUtil().getCurrentProfilePicStorageRef()?.downloadUrl
            ?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val uri = task.result
                    AndroidUtil().setProfilePic(this, uri, profilePic)
                }
            }
    }

    private fun checkIfUserReported(callback: (Boolean) -> Unit) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        if (currentUserEmail != null) {
            val blockedUserDocumentRef = Firebase.firestore.collection("BlockedUsers").document(currentUserEmail)

            blockedUserDocumentRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    // Invoke the callback with the result
                    callback(documentSnapshot.exists())
                }
                .addOnFailureListener { exception ->
                    // Error handling
                    callback(false) // Assume not reported in case of error
                }
        } else {
            // Current user not logged in, cannot determine if reported
            callback(false) // Assume not reported if not logged in
        }
    }

    private fun checkIfBlockedUsersExist(callback: (Boolean) -> Unit) {
        val db = Firebase.firestore

        val blockedUsersCollectionRef = db.collection("BlockedUsers")

        // Check if there are any documents in the BlockedUsers collection
        blockedUsersCollectionRef.get()
            .addOnSuccessListener { querySnapshot ->
                // Invoke the callback with the result
                callback(!querySnapshot.isEmpty)
            }
            .addOnFailureListener { exception ->
                // Error handling
                callback(false) // Assume no blocked users in case of error
            }
    }

    private fun checkBlockedUsersAndShowPopup() {
        val db = Firebase.firestore
        val blockedUsersCollectionRef = db.collection("BlockedUsers")

        blockedUsersCollectionRef.get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot.documents) {
                    val blockedUserEmail = document.id
                    val blockedByUsersRef = document.reference.collection("blockedByUsers")
                    val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email

                    // Check if the current user has voted on this blocked user
                    currentUserEmail?.let { email ->
                        blockedByUsersRef.document(email).get()
                            .addOnSuccessListener { documentSnapshot ->
                                if (!documentSnapshot.exists()) {
                                    // Current user has not voted to block, show the popup
                                    showBlockedUserPopup(blockedUserEmail, currentUserEmail)
                                }
                            }
                            .addOnFailureListener { exception ->
                                showBlockedUserPopup(blockedUserEmail, currentUserEmail) // Show the popup anyway if there's an error
                            }
                    } ?: run {
                        Toast.makeText(this, "You are not logged in", Toast.LENGTH_SHORT).show()
                    }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to fetch blocked users", exception)
            }
    }

    @SuppressLint("SetTextI18n")
    private fun showBlockedUserPopup(blockedUserName: String, currentUserEmail: String) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.popup_blocked_user, null)

        // Modify the blocked user's name
        dialogView.findViewById<TextView>(R.id.text_blocked_user_name).text = blockedUserName

        // Reference to the BlockedUsers document in Firestore
        val db = Firebase.firestore
        val blockedUsersRef = db.collection("BlockedUsers").document(blockedUserName)
        val blockedByUsersRef = blockedUsersRef.collection("blockedByUsers")
        val keepByUsersRef = blockedUsersRef.collection("keepByUsers")

        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setView(dialogView)

        val alertDialog = alertDialogBuilder.create()

        // Fetch the list of blocked users and update the vote count TextView
        blockedByUsersRef.get().addOnSuccessListener { blockedByUsersList ->
            // Update the vote count TextView
            dialogView.findViewById<TextView>(R.id.text_vote_count).text =
                "${blockedByUsersList.size()} Teachers voted to block this user"
        }

        // Button to block the user
        dialogView.findViewById<Button>(R.id.button_block).setOnClickListener {
            // Add the current user to the list of users who clicked the block button
            blockedByUsersRef.document(currentUserEmail).set(mapOf("blockedByUser" to true))

            // Check if there are at least 5 teachers who clicked the block button
            blockedByUsersRef.get().addOnSuccessListener { blockedByUsersList ->
                if (blockedByUsersList.size() >= 6) {
                    // Update the status of the blocked user to "Banned"
                    blockedUsersRef.update("status", "Banned")
                }

                // Update the vote count TextView
                dialogView.findViewById<TextView>(R.id.text_vote_count).text =
                    "${blockedByUsersList.size()} Teachers voted to block this user"
            }
            alertDialog.dismiss()
        }

        // Button to keep the user
        dialogView.findViewById<Button>(R.id.button_keep).setOnClickListener {
            // Add the current user to the list of users who clicked the keep button
            keepByUsersRef.document(currentUserEmail).set(mapOf("keepByUser" to true))

            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun logoutUser() {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        Log.d("fcmTokenDelete", "$userEmail")

        if (userEmail != null) {
            val usersCollection = firestore.collection("Users")
            usersCollection.whereEqualTo("Email", userEmail)
                .get()
                .addOnSuccessListener { documents ->
                    for (document in documents) {
                        val userId = document.id
                        val userRef = usersCollection.document(userId)
                        userRef.update("fcmToken", FieldValue.delete())
                            .addOnSuccessListener {
                                Log.d(TAG, "FCM token removed for user: $userId")
                            }
                            .addOnFailureListener { e ->
                                Log.w(TAG, "Error removing FCM token for user: $userId", e)
                            }
                    }
                }
                .addOnFailureListener { exception ->
                    Log.w(TAG, "Error getting user document for email: $userEmail", exception)
                }
        }
        FirebaseAuth.getInstance().signOut()
        goToMainPage()
    }

    private fun goToMainPage() {
        val mainIntent = Intent(this@HomePageActivity, MainActivity::class.java)
        startActivity(mainIntent)
    }

    private fun goToImpNewsPage() {
        val impNewsIntent = Intent(this@HomePageActivity, ImpNewsPageActivity::class.java).apply {
            putExtra("isTeacher", isTeacher)
        }
        startActivity(impNewsIntent)
    }

    private fun goToNewsPage() {
        val newsIntent = Intent(this@HomePageActivity, NewsPageActivity::class.java).apply {
            putExtra("isTeacher", isTeacher)
        }
        startActivity(newsIntent)
    }

    private fun goToTimetable() {
        val newsIntent = Intent(this@HomePageActivity, TimetableActivity::class.java).apply {
            putExtra("isTeacher", isTeacher)
        }
        startActivity(newsIntent)
    }

    private fun goToCatalog() {
        val catalogIntent = Intent(this@HomePageActivity, CoursesListActivity::class.java).apply {
            putExtra("isTeacher", isTeacher)
        }
        startActivity(catalogIntent)
    }


}