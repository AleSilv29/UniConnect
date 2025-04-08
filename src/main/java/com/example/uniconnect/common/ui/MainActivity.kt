package com.example.uniconnect.common.ui


import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.widget.Button
import androidx.activity.ComponentActivity
import com.example.uniconnect.R
import com.example.uniconnect.auth.ui.CheckEmailActivity
import com.example.uniconnect.auth.ui.SignInActivity
import com.example.uniconnect.auth.ui.SignUpActivity
import com.google.firebase.FirebaseApp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase


class MainActivity : ComponentActivity() {

    private lateinit var buttonLoginAccount: Button
    private lateinit var buttonSignUp: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.main_activity_layout)

        auth = FirebaseAuth.getInstance()

        FirebaseApp.initializeApp(this)

        val currentUser: FirebaseUser? = auth.currentUser
        Log.d("Auth123", "$currentUser")
        if (currentUser != null) {
            checkIfUserBanned { isBanned ->
                if (isBanned) {
                    FirebaseAuth.getInstance().signOut()
                    finish()
                } else {
                    if (currentUser.isEmailVerified) {
                        val homeIntent = Intent(this@MainActivity, HomePageActivity::class.java)
                        startActivity(homeIntent)
                        finish()
                    } else {
                        checkIfUserExistsInFirestore(currentUser.email) { exists ->
                            if (exists) {
                                val homeIntent = Intent(this@MainActivity, HomePageActivity::class.java)
                                startActivity(homeIntent)
                                finish()
                            } else {
                                val checkEmailIntent = Intent(this@MainActivity, CheckEmailActivity::class.java)
                                startActivity(checkEmailIntent)
                                finish()
                            }
                        }
                    }
                }
            }
        }


        buttonLoginAccount = findViewById(R.id.loginButton)
        buttonSignUp = findViewById(R.id.signUpButton)

        buttonSignUp.setOnClickListener {
            val signUpIntent = Intent(this@MainActivity, SignUpActivity::class.java)
            startActivity(signUpIntent)
        }

        buttonLoginAccount.setOnClickListener {
            val signInIntent = Intent(this@MainActivity, SignInActivity::class.java)
            startActivity(signInIntent)
        }
    }

    private fun checkIfUserExistsInFirestore(email: String?, callback: (Boolean) -> Unit) {
        if (email == null) {
            callback(false)
            return
        }
        val db = Firebase.firestore
        val usersRef = db.collection("Users")
        usersRef.whereEqualTo("Email", email).get()
            .addOnSuccessListener { documents ->
                callback(!documents.isEmpty)
            }
            .addOnFailureListener { exception ->
                Log.w("Firestore", "Error checking user existence: ", exception)
                callback(false)
            }
    }


    private fun checkIfUserBanned(callback: (Boolean) -> Unit) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        currentUserEmail?.let { email ->
            val db = Firebase.firestore
            val blockedUsersRef = db.collection("BlockedUsers").document(email)

            blockedUsersRef.get()
                .addOnSuccessListener { documentSnapshot ->
                    if (documentSnapshot.exists() && documentSnapshot.getString("status") == "Banned") {
                        // User is banned
                        callback(true)
                    } else {
                        // User is not banned
                        callback(false)
                    }
                }
                .addOnFailureListener { exception ->
                    callback(false) // Assume not banned in case of error
                }
        } ?: run {
            // Current user not logged in
            callback(false) // Assume not banned if not logged in
        }
    }


}


