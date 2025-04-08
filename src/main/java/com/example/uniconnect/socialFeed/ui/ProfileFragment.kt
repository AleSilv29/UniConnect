package com.example.uniconnect.socialFeed.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment
import com.example.uniconnect.R
import com.example.uniconnect.socialFeed.model.UserModel
import com.example.uniconnect.utils.AndroidUtil
import com.example.uniconnect.utils.FirebaseUtil
import com.github.dhaval2404.imagepicker.ImagePicker
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.storage.UploadTask


class ProfileFragment : Fragment() {

    var profilePic: ImageView? = null
    var usernameInput: TextView? = null
    var yearInput: EditText? = null
    var groupInput: EditText? = null
    var semigroupInput: EditText? = null
    var updateProfileBtn: Button? = null
    var progressBar: ProgressBar? = null

    var currentUserModel: UserModel? = null
    var imagePickLauncher: ActivityResultLauncher<Intent>? = null
    var selectedImageUri: Uri? = null

    private val REQUEST_CODE = 100

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        imagePickLauncher = registerForActivityResult<Intent, ActivityResult>(
            ActivityResultContracts.StartActivityForResult()
        ) { result: ActivityResult ->
            if (result.resultCode == Activity.RESULT_OK) {
                val data = result.data
                if (data != null && data.data != null) {
                    selectedImageUri = data.data
                    AndroidUtil().setProfilePic(getContext(),selectedImageUri,profilePic);
                }
            }
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_profile, container, false)
        profilePic = view?.findViewById(R.id.profile_image_view)
        usernameInput = view?.findViewById(R.id.profile_username)
        yearInput = view?.findViewById(R.id.profile_year)
        groupInput = view?.findViewById(R.id.profile_group)
        semigroupInput = view?.findViewById(R.id.profile_semigroup)
        updateProfileBtn = view?.findViewById(R.id.profle_update_btn)
        progressBar = view?.findViewById(R.id.profile_progress_bar)
        requestStoragePermission()

//        updateProfileBtn!!.setOnClickListener { v: View? -> updateBtnClick() }

        profilePic!!.setOnClickListener { v: View? ->
            ImagePicker.with(this).cropSquare().compress(512).maxResultSize(512, 512)
                .createIntent { intent: Intent? ->
                    imagePickLauncher!!.launch(intent)
                    null
                }
        }

        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Request the permission
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
        }

        getUserData()

        return view
    }

    // Requesting permission method
    private fun requestStoragePermission() {
        if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
            // Permission is not granted
            // Request the permission
            ActivityCompat.requestPermissions(requireActivity(), arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), REQUEST_CODE)
        } else {
            // Permission already granted
            // Perform necessary actions here, such as loading an image from storage
            updateBtnClick()
            //updateToFirestore()
        }
    }


    // Handling permission result

    @Deprecated("Deprecated in Java")
    override fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                // Permission granted
                // You can perform necessary actions here, such as loading an image from storage
                updateBtnClick()
            } else {
                // Permission denied
                // You may inform the user about the necessity of the permission and prompt them to grant it
                requestStoragePermission()
            }
        }
    }

    fun updateBtnClick(){
        if (selectedImageUri != null) {
            uploadProfilePicture(selectedImageUri!!)
        } else {
            updateToFirestore()
        }
    }

    fun uploadProfilePicture(imageUri: Uri) {
        val storageRef = FirebaseUtil().getCurrentProfilePicStorageRef()
        storageRef.putFile(imageUri)
            .addOnCompleteListener(OnCompleteListener<UploadTask.TaskSnapshot?> { task: Task<UploadTask.TaskSnapshot?>? ->
                if (task != null) {
                    if (task.isSuccessful) {
                        // After successful upload, update profile data
                        updateToFirestore()
                        Toast.makeText(context, "Profile picture uploaded successfully", Toast.LENGTH_SHORT).show()
                    } else {
                        Toast.makeText(context, "Failed to upload profile picture", Toast.LENGTH_SHORT).show()
                    }
                }
            })
    }

    fun updateToFirestore() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            val userEmail = user.email
            val db = FirebaseFirestore.getInstance()
            db.collection("Users")
                .whereEqualTo("Email", userEmail)
                .get()
                .addOnSuccessListener { documents ->
                    if (!documents.isEmpty) {
                        val document = documents.documents[0]
                        val isTeacher = document.getBoolean("isTeacher") ?: false
                        if (!isTeacher) {
                            // User is not a teacher, proceed with the update
                            document.reference.update(
                                mapOf(
                                    "Year" to yearInput?.text.toString(),
                                    "Group" to groupInput?.text.toString(),
                                    "Semigroup" to semigroupInput?.text.toString()
                                )
                            ).addOnSuccessListener {
                                // Update successful
                                AndroidUtil().showToast(context, "Profile updated successfully")
                            }.addOnFailureListener { e ->
                                // Update failed
                                Log.e("ProfileFragment", "Error updating profile", e)
                                AndroidUtil().showToast(context, "Failed to update profile")
                            }
                        } else {
                            // User is a teacher, cannot update profile
                            AndroidUtil().showToast(context, "Teachers cannot add year/group/semigroup")
                        }
                    }
                }
                .addOnFailureListener { e ->
                    // Error fetching user data
                    Log.e("ProfileFragment", "Error getting user data", e)
                    AndroidUtil().showToast(context, "Failed to get user data")
                }
        }
    }


    fun getUserData() {
        setInProgress(true)

        val user = FirebaseAuth.getInstance().currentUser
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

                            val group = document.getString("Group")
                            val year = document.getString("Year")
                            val semigroup = document.getString("Semigroup")

                            currentUserModel = UserModel(userEmail, group, year, null, null, null)

                            usernameInput?.setText(userEmail)
                            yearInput?.setText(year)
                            groupInput?.setText(group)
                            semigroupInput?.setText(semigroup)

                            // Set up update button click listener
                            updateProfileBtn!!.setOnClickListener { v: View? -> updateBtnClick() }

                            // Load profile picture
                            FirebaseUtil().getCurrentProfilePicStorageRef().getDownloadUrl()
                                .addOnCompleteListener { task ->
                                    if (task.isSuccessful) {
                                        val uri = task.result
                                        AndroidUtil().setProfilePic(context, uri, profilePic)
                                    }
                                    setInProgress(false)
                                }
                        } else {
                            // User does not exist in the database
                            setInProgress(false)
                        }
                    } else {
                        Log.d("TAG", "Error getting documents: ", task.exception)
                        setInProgress(false)
                    }
                }
        }
    }


    fun setInProgress(inProgress: Boolean) {
        if (progressBar != null && updateProfileBtn != null) { // Check if progressBar and updateProfileBtn are not null
            if (inProgress) {
                progressBar!!.visibility = View.VISIBLE
                updateProfileBtn!!.visibility = View.GONE
            } else {
                progressBar!!.visibility = View.GONE
                updateProfileBtn!!.visibility = View.VISIBLE
            }
        } else {
            Log.e("ProfileFragment", "ProgressBar or updateProfileBtn is null")
        }
    }

}