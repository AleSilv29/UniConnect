package com.example.uniconnect.auth.ui

import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.RadioButton
import android.widget.RadioGroup
import android.widget.Toast
import androidx.activity.ComponentActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase
import com.example.uniconnect.R


class SignUpActivity : ComponentActivity() {

    private var editTextEmail: EditText? = null
    private var editTextPassword: EditText? = null

    private lateinit var buttonSignUp: Button

    private val firebaseAuth: FirebaseAuth = FirebaseAuth.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_sign_up)


        editTextEmail = findViewById(R.id.username)
        editTextPassword = findViewById(R.id.password)
        buttonSignUp = findViewById(R.id.createAccountButton)


        buttonSignUp.setOnClickListener(View.OnClickListener {
            val email = editTextEmail?.text.toString()
            val password = editTextPassword?.text.toString()


            if (TextUtils.isEmpty(email)) {
                Toast.makeText(this@SignUpActivity, "Enter email", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }
            if (TextUtils.isEmpty(password)) {
                Toast.makeText(this@SignUpActivity, "Enter password", Toast.LENGTH_SHORT).show()
                return@OnClickListener
            }

            checkIfEmailIsBlocked(email) { isBlocked ->
                if (isBlocked) {
                    Toast.makeText(this@SignUpActivity, "This account was banned", Toast.LENGTH_SHORT).show()
                } else {

            val domain = email.split('@')[1]
            if (domain == "ubbcluj.ro") {
                signUpUser(email, password, true)

            } else if (domain == "stud.ubbcluj.ro") {

                val dialogView = LayoutInflater.from(this@SignUpActivity)
                    .inflate(R.layout.add_student_info, null)

                val builder = AlertDialog.Builder(this@SignUpActivity)
                builder.setView(dialogView)


                val yearRG = dialogView.findViewById<RadioGroup>(R.id.yearRadioGroup)
                val groupRG = dialogView.findViewById<RadioGroup>(R.id.groupRadioGroup)
                val semigroupRG = dialogView.findViewById<RadioGroup>(R.id.semigroupRadioGroup)

                builder.setPositiveButton("Save") { _, _ ->
                    val selectedYearId: Int = yearRG.checkedRadioButtonId
                    val selectedGroupId: Int = groupRG.checkedRadioButtonId
                    val selectedSemigroupId: Int = semigroupRG.checkedRadioButtonId

                    val selectedYear = dialogView.findViewById<RadioButton>(selectedYearId)
                    val selectedYearText = selectedYear.text.toString()

                    val selectedGroup = dialogView.findViewById<RadioButton>(selectedGroupId)
                    val selectedGroupText = selectedGroup.text.toString()

                    val selectedSemigroup =
                        dialogView.findViewById<RadioButton>(selectedSemigroupId)
                    val selectedSemigroupText = selectedSemigroup.text.toString()

                    signUpUser(
                        email,
                        password,
                        false,
                        selectedYearText,
                        selectedGroupText,
                        selectedSemigroupText
                    )
                }
                builder.setNegativeButton(
                    "Cancel"
                ) { dialog, _ -> dialog.dismiss() }
                builder.create().show()
            } else {
                Toast.makeText(this@SignUpActivity, "Invalid email domain", Toast.LENGTH_SHORT)
                    .show()
            }
                }
            }

        })

    }


    private fun signUpUser(
        email: String,
        password: String,
        isTeacher: Boolean,
        year: String = "",
        group: String = "",
        semigroup: String = ""
    ) {

        firebaseAuth.createUserWithEmailAndPassword(email, password).addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val user = firebaseAuth.currentUser
                user?.sendEmailVerification()?.addOnCompleteListener { verificationTask ->
                    if (verificationTask.isSuccessful) {
                        Toast.makeText(this@SignUpActivity, "Verification email sent. Please verify your email.", Toast.LENGTH_SHORT).show()
                        Log.d(TAG, "Verification email sent to ${user.email}")

                        // Pass necessary data to CheckEmailActivity
                        val intent = Intent(this@SignUpActivity, CheckEmailActivity::class.java).apply {
                            putExtra("isTeacher", isTeacher)
                            putExtra("year", year)
                            putExtra("group", group)
                            putExtra("semigroup", semigroup)
                        }
                        startActivity(intent)
                    } else {
                        Toast.makeText(this@SignUpActivity, "Failed to send verification email.", Toast.LENGTH_SHORT).show()
                        Log.e(TAG, "Failed to send verification email", verificationTask.exception)
                    }
                }
            } else {
                Toast.makeText(this@SignUpActivity, "Sign-up failed", Toast.LENGTH_SHORT).show()
                Log.e(TAG, "Sign-up failed", task.exception)
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