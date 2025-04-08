package com.example.uniconnect.auth.ui

import android.app.AlertDialog
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.example.uniconnect.common.ui.HomePageActivity
import com.example.uniconnect.R
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.google.firebase.messaging.FirebaseMessaging

class SignInActivity : ComponentActivity() {

    private var editTextEmail: EditText? = null
    private var editTextPassword: EditText? = null
    private lateinit var buttonLoginAccount: Button
    private lateinit var buttonResetPsswd: Button

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()
    private val firestore = Firebase.firestore

    private lateinit var personIconPasswordImageView: ImageView
    private lateinit var personIconImageView: ImageView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_in)


        editTextEmail = findViewById(R.id.username)
        editTextPassword = findViewById(R.id.password)
        buttonLoginAccount = findViewById(R.id.loginAccountButton)
        buttonResetPsswd = findViewById(R.id.resetPsswdButton)
        personIconPasswordImageView = findViewById(R.id.person_icon_password)
        personIconImageView = findViewById(R.id.person_icon)
        // Set onFocusChangeListener to password EditText
        editTextPassword?.let { editText ->
            editText.onFocusChangeListener = View.OnFocusChangeListener { _, hasFocus ->
                if (hasFocus) {
                    // Change the icon's visibility to VISIBLE when EditText gains focus
                    personIconPasswordImageView.visibility = View.VISIBLE
                } else {
                    // Change the icon's visibility back to GONE when EditText loses focus
                    personIconPasswordImageView.visibility = View.GONE
                }
            }
        }


        buttonLoginAccount.setOnClickListener(View.OnClickListener {
            val email = editTextEmail?.text.toString()
            val password = editTextPassword?.text.toString()
            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this@SignInActivity, "Enter email", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this@SignInActivity, "Enter password", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }

            checkIfEmailIsBlocked(email) { isBlocked ->
                if (isBlocked) {
                    Toast.makeText(this@SignInActivity, "This account was banned", Toast.LENGTH_SHORT).show()
                } else {
                    loginUser(email, password)
                    buttonResetPsswd.setOnClickListener {
                        resetPsswd()
                    }
                }}
        })

    }

    private fun resetPsswd() {
        val dialogView =
            LayoutInflater.from(this@SignInActivity).inflate(R.layout.reset_psswd_dialog, null)

        val email = dialogView.findViewById<EditText>(R.id.email)

        val alertDialogBuilder = AlertDialog.Builder(this@SignInActivity)
        alertDialogBuilder.setView(dialogView)
        alertDialogBuilder.setTitle("Reset Password")

        alertDialogBuilder.setPositiveButton(
            "Recover"
        ) { _, _ ->
            val emailAddr = email.text.toString().trim { it <= ' ' }
            beginRecovery(emailAddr)
        }
        alertDialogBuilder.setNegativeButton(
            "Cancel"
        ) { dialog, _ -> dialog.dismiss() }
        alertDialogBuilder.create().show()
    }

    private fun beginRecovery(email: String) {

        firebaseAuth.sendPasswordResetEmail(email).addOnCompleteListener { task ->
            if (task.isSuccessful) {

                Toast.makeText(this@SignInActivity, "Done sent", Toast.LENGTH_LONG).show()
            } else {
                Toast.makeText(this@SignInActivity, "Error Occurred", Toast.LENGTH_LONG).show()
            }
        }
    }

    fun loginUser(email: String, password: String) {
        firebaseAuth.signInWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                Toast.makeText(this@SignInActivity, "Login successful", Toast.LENGTH_SHORT).show()

                val user = firebaseAuth.currentUser
                var isTeacher = false

                if (user != null) {
                    val userEmail = user.email

                    // Check if FCM token exists for the user
                    firestore.collection("Users")
                        .whereEqualTo("Email", userEmail)
                        .get()
                        .addOnSuccessListener { documents ->
                            for (document in documents) {
                                val fcmToken = document.getString("fcmToken")
                                if (fcmToken == null) {
                                    // FCM token doesn't exist, retrieve and update the document
                                    FirebaseMessaging.getInstance().token.addOnCompleteListener { fcmTask ->
                                        if (fcmTask.isSuccessful) {
                                            val token = fcmTask.result
                                            firestore.collection("Users").document(document.id)
                                                .update("fcmToken", token)
                                                .addOnSuccessListener {
                                                    Log.d("fcmTokenLogIn", "FCM token added for user: $userEmail")
                                                }
                                                .addOnFailureListener { e ->
                                                    Log.w("fcmTokenLogIn", "Error adding FCM token for user: $userEmail", e)
                                                }
                                        } else {
                                            Log.e("fcmTokenLogIn", "Fetching FCM token failed")
                                        }
                                    }
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.w("fcmTokenLogIn", "Error getting user document for email: $userEmail", exception)
                        }

                    firestore.collection("Users").get().addOnSuccessListener { users ->
                        for (u in users) {
                            if (u.data["Email"].toString() == user.email) {
                                isTeacher = u.data["isTeacher"] as? Boolean ?: false
                            }

                            val homeIntent =
                                Intent(this@SignInActivity, HomePageActivity::class.java).apply {
                                    putExtra("isTeacher", isTeacher)
                                }
                            startActivity(homeIntent)
                        }
                    }
                }
            } else {
                Toast.makeText(
                    this@SignInActivity,
                    "Authentication failed",
                    Toast.LENGTH_SHORT
                )
                    .show()
            }
        }
    }

    private fun checkIfEmailIsBlocked(email: String, callback: (Boolean) -> Unit) {
        val db = Firebase.firestore
        val blockedUsersCollectionRef = db.collection("BlockedUsers")

        blockedUsersCollectionRef.document(email).get()
            .addOnSuccessListener { documentSnapshot ->
                callback(documentSnapshot.exists())
            }
            .addOnFailureListener { exception ->
                callback(false) // Assume email is not blocked in case of error
            }
    }
}
