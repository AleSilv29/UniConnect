package com.example.uniconnect.auth.ui

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging
import android.provider.Settings
import android.net.Uri
import com.example.uniconnect.common.ui.HomePageActivity
import com.example.uniconnect.common.ui.MainActivity
import com.example.uniconnect.R

class CheckEmailActivity : ComponentActivity() {

    private lateinit var buttonCheckVerification: Button
    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore

    private lateinit var email: String
    private var isTeacher: Boolean = false
    private var year: String = ""
    private var group: String = ""
    private var semigroup: String = ""
    lateinit var backButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_check_email)

        buttonCheckVerification = findViewById(R.id.buttonCheckVerification)

        email = intent.getStringExtra("email") ?: ""
        isTeacher = intent.getBooleanExtra("isTeacher", false)
        year = intent.getStringExtra("year") ?: ""
        group = intent.getStringExtra("group") ?: ""
        semigroup = intent.getStringExtra("semigroup") ?: ""

        buttonCheckVerification.setOnClickListener {
            val user = firebaseAuth.currentUser
            user?.reload()?.addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    if (user.isEmailVerified) {
                        Toast.makeText(this@CheckEmailActivity, "Email verified. You can now use the app.", Toast.LENGTH_SHORT).show()
                        saveUserData(user)
                    } else {
                        Toast.makeText(this@CheckEmailActivity, "Email not verified yet. Please check your email.", Toast.LENGTH_SHORT).show()
                    }
                }
            }
        }
        auth = FirebaseAuth.getInstance()

        backButton = findViewById(R.id.buttonBack)

        backButton.setOnClickListener {
            auth.signOut()
            val mainIntent = Intent(this@CheckEmailActivity, MainActivity::class.java)
            mainIntent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            startActivity(mainIntent)
            finish()
        }
    }

    private fun saveUserData(user: FirebaseUser) {
        // Retrieve FCM token
        FirebaseMessaging.getInstance().token.addOnCompleteListener { fcmTask ->
            if (fcmTask.isSuccessful) {
                val token = fcmTask.result
                // Update user document with FCM token
                val hashUser = hashMap(
                                user,
                                isTeacher,
                                year,
                                group,
                                semigroup,
                                token
                            )
                firestore.collection("Users").add(hashUser)
                    .addOnSuccessListener { documentReference ->
                        Log.d(TAG, "DocumentSnapshot added with ID: ${documentReference.id}")
                        showNotificationPermissionPopup { granted ->
                            val homeIntent = Intent(this@CheckEmailActivity, HomePageActivity::class.java).apply {
                                putExtra("isTeacher", isTeacher)
                            }
                            startActivity(homeIntent)
                            finish()
                        }
                    }.addOnFailureListener { e ->
                        Log.w(TAG, "Error adding document", e)
                    }
            } else {
                Log.e("FCMToken", "Fetching FCM token failed")
            }
        }
    }

    private fun showNotificationPermissionPopup(callback: (Boolean) -> Unit) {
        val alertDialogBuilder = AlertDialog.Builder(this)
        alertDialogBuilder.setTitle("Notification Permission")
        alertDialogBuilder.setMessage("Allow notifications to receive important news and messages?")
        alertDialogBuilder.setPositiveButton("Allow") { dialog, which ->
            callback(true) // Notify that permission is granted
            // Open app settings
            openAppSettings()
        }
        alertDialogBuilder.setNegativeButton("Not Now") { dialog, which ->
            callback(false) // Notify that permission is not granted
        }
        alertDialogBuilder.setCancelable(false)
        alertDialogBuilder.show()
    }

    private fun openAppSettings() {
        val intent = Intent()
        intent.action = Settings.ACTION_APPLICATION_DETAILS_SETTINGS
        val uri: Uri = Uri.fromParts("package", packageName, null)
        intent.data = uri
        startActivity(intent)
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun hashMap(
        user: FirebaseUser, isTeacher: Boolean, year: String, group: String, semigroup: String, fcmToken: String
    ) = hashMapOf(
        "Email" to user.email,
        "isTeacher" to isTeacher,
        "Year" to year,
        "Group" to group,
        "Semigroup" to semigroup,
        "fcmToken" to fcmToken

    )


}
