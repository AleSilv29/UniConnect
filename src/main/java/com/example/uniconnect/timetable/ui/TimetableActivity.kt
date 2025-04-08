package com.example.uniconnect.timetable.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.text.Spannable
import android.text.SpannableString
import android.text.style.AbsoluteSizeSpan
import android.util.Log
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.firebase.firestore.FirebaseFirestore
import android.view.Gravity
import android.view.MotionEvent
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.Toast
import com.google.firebase.auth.FirebaseAuth
import androidx.core.content.ContextCompat
import androidx.core.view.setPadding
import com.example.uniconnect.R
import com.example.uniconnect.classbook.ui.CoursesListActivity
import com.google.firebase.firestore.DocumentSnapshot
import kotlinx.coroutines.*
import kotlinx.coroutines.tasks.await
import com.example.uniconnect.utils.AndroidUtil


class TimetableActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var resultTextView: TextView
    private lateinit var resultTextViewTuesday: TextView
    private lateinit var resultTextViewWednesday: TextView
    private lateinit var resultTextViewThursday: TextView
    private lateinit var resultTextViewFriday: TextView
    lateinit var backButton: Button

    private lateinit var mondayContainer: LinearLayout
    private lateinit var tuesdayContainer: LinearLayout
    private lateinit var wednesdayContainer: LinearLayout
    private lateinit var thursdayContainer: LinearLayout
    private lateinit var fridayContainer: LinearLayout

    private var isTeacher: Boolean = false

    private var isButtonActivated = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_timetable)

        mondayContainer = findViewById(R.id.mondayContainer)
        tuesdayContainer = findViewById(R.id.tuesdayContainer)
        wednesdayContainer = findViewById(R.id.wednesdayContainer)
        thursdayContainer = findViewById(R.id.thursdayContainer)
        fridayContainer = findViewById(R.id.fridayContainer)

        resultTextView = findViewById(R.id.resultTextView)
        resultTextViewTuesday = findViewById(R.id.resultTextViewTuesday)
        resultTextViewWednesday = findViewById(R.id.resultTextViewWednesday)
        resultTextViewThursday = findViewById(R.id.resultTextViewThursday)
        resultTextViewFriday = findViewById(R.id.resultTextViewFriday)



        findViewById<Button>(R.id.buttonOpenPopup).setOnClickListener{
            openPopupDialog()
        }


        findViewById<Button>(R.id.btnFiltreaza).setOnClickListener{
            val catalogIntent = Intent(this@TimetableActivity, CoursesListActivity::class.java).apply {
                putExtra("isTeacher", isTeacher)
            }
            startActivity(catalogIntent)
        }


        isTeacher = intent.getBooleanExtra("isTeacher", false)
        if (isTeacher){
            val user = FirebaseAuth.getInstance().currentUser
            if (user != null) {
                loadTimetableData()
            }
        }
        else {
            loadTimetableData()
        }

        backButton = findViewById(R.id.buttonBack)

        backButton.setOnClickListener {
            finish()
        }

    }


    private fun openPopupDialog() {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.popup_layout, null)
        dialogBuilder.setView(dialogView)

        val spinner = dialogView.findViewById<Spinner>(R.id.daySpinnerPopup)
        val editTextPopup = dialogView.findViewById<EditText>(R.id.editTextPopup)
        val secondEditTextPopup = dialogView.findViewById<EditText>(R.id.secondEditTextPopup)

        ArrayAdapter.createFromResource(
            this,
            R.array.days_of_week,
            android.R.layout.simple_spinner_item
        ).also { adapter ->
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            spinner.adapter = adapter
        }

        val currentUser = FirebaseAuth.getInstance().currentUser
        val studentEmail = currentUser?.email


        dialogBuilder.setTitle("Enter Text")

        dialogBuilder.setPositiveButton(
            "Add"
        ) { dialog, whichButton ->
            val enteredText = editTextPopup.text.toString().trim { it <= ' ' }
            val secondEnteredText = secondEditTextPopup.text.toString().trim { it <= ' ' }
            val dayEditText = spinner.selectedItem.toString()


            if (AndroidUtil().validateTimeFormat(secondEnteredText)) {
                val formattedHours = AndroidUtil().formatHours(secondEnteredText)

                if (studentEmail != null) {
                    addDocumentToFirestore(studentEmail, enteredText, formattedHours, dayEditText)
                    loadTimetableData()
                }
            } else {
                // Show error message if the format is incorrect
                Toast.makeText(this, "Please enter the time in the format e.g., 7:00-8:00", Toast.LENGTH_SHORT).show()
            }
        }

        dialogBuilder.setNegativeButton(
            "Cancel"
        ) { dialog, whichButton ->
            // Canceled, do nothing
        }

        val alertDialog = dialogBuilder.create()
        alertDialog.show()
    }

    private fun clearContainers() {
        val containers = listOf(
            findViewById<LinearLayout>(R.id.mondayContainer),
            findViewById<LinearLayout>(R.id.tuesdayContainer),
            findViewById<LinearLayout>(R.id.wednesdayContainer),
            findViewById<LinearLayout>(R.id.thursdayContainer),
            findViewById<LinearLayout>(R.id.fridayContainer)
        )

        for (container in containers) {
            container.removeAllViews() // Remove all child views from the container
        }
    }


    private fun addDocumentToFirestore(
        studentEmail: String,
        enteredText: String,
        secondEnteredText: String,
        dayEditTextPopup: String
    ) {
        studentEmail.let { email ->
            val data = hashMapOf(
                "Schedule" to enteredText,
                "Hours" to secondEnteredText,
                "day" to dayEditTextPopup
            )

            val userDocumentRef = db.collection("PTUsers").document(email)
            val scheduleCollectionRef = userDocumentRef.collection("schedule")

            // Check if the user's document already exists
            userDocumentRef.get().addOnSuccessListener { documentSnapshot ->
                if (documentSnapshot.exists()) {
                    // Document already exists, append the schedule entry with a name
                    val scheduleName = generateScheduleName()
                    val scheduleDocumentRef = scheduleCollectionRef.document(scheduleName)

                    scheduleDocumentRef.set(data)
                        .addOnCompleteListener { task ->
                            if (task.isSuccessful) {
                                Toast.makeText(this, "Schedule entry added successfully", Toast.LENGTH_SHORT).show()
                            } else {
                                Log.e(TAG, "Failed to add schedule entry: ${task.exception?.message}")
                                Toast.makeText(this, "Failed to add schedule entry", Toast.LENGTH_SHORT).show()
                            }
                        }
                } else {
                    // Document doesn't exist, create it and add the schedule entry
                    userDocumentRef.set(data)
                        .addOnCompleteListener { createUserTask ->
                            if (createUserTask.isSuccessful) {
                                // Document created successfully, now adding the schedule entry
                                val scheduleName = generateScheduleName()
                                val scheduleDocumentRef = scheduleCollectionRef.document(scheduleName)

                                scheduleDocumentRef.set(data)
                                    .addOnCompleteListener { addScheduleTask ->
                                        if (addScheduleTask.isSuccessful) {
                                            Toast.makeText(this, "Schedule entry added successfully", Toast.LENGTH_SHORT).show()
                                        } else {
                                            Log.e(TAG, "Failed to add schedule entry: ${addScheduleTask.exception?.message}")
                                            Toast.makeText(this, "Failed to add schedule entry", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                            } else {
                                Log.e(TAG, "Failed to create user document: ${createUserTask.exception?.message}")
                                Toast.makeText(this, "Failed to create user document", Toast.LENGTH_SHORT).show()
                            }
                        }

                }
            }.addOnFailureListener { exception ->
                Log.e(TAG, "Failed to get user document: ${exception.message}")
                Toast.makeText(this, "Failed to get user document", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun generateScheduleName(): String {
        return "schedule_${System.currentTimeMillis()}"
    }


    private fun updateEditButtonAppearance() {
        val editButton = findViewById<ImageButton>(R.id.editButton)
        if (isButtonActivated) {
            // Changing the tint color to indicate activation
            editButton.setColorFilter(ContextCompat.getColor(this, R.color.darkerPurple))
            Toast.makeText(this, "Please click on the personal schedule you want to delete", Toast.LENGTH_SHORT).show()
        } else {
            // Reseting the tint color to the default color
            editButton.clearColorFilter()
        }
    }

    private fun loadTimetableData() {
        val MAX_RETRIES = 3 // Maximum number of retry attempts
        val retryCount = 0 // Counter to track the number of retries

        CoroutineScope(Dispatchers.Main).launch {
            clearContainers()
            loadDataWithRetry(MAX_RETRIES, retryCount)
        }
    }

    private suspend fun loadDataWithRetry(MAX_RETRIES: Int, retryCount: Int) {
        try {
            val currentUser = FirebaseAuth.getInstance().currentUser
            val userEmail = currentUser?.email

            val timetableData = if (isTeacher) {
                fetchTeacherTimetableData(userEmail)
            } else {
                fetchUserTimetableData()
            }

            val personalScheduleData = fetchPersonalScheduleData()

            // Now updating the UI with fetched data
            updateUI(timetableData, personalScheduleData)
        } catch (e: Exception) {
            if (retryCount < MAX_RETRIES) {
                Log.w("TimetableActivity", "Retrying... attempt ${retryCount + 1}")
                delay(1000L * (retryCount + 1)) // Exponential backoff
                loadDataWithRetry(MAX_RETRIES, retryCount + 1)
            } else {
                Log.e("TimetableActivity", "Error loading timetable data", e)
            }
        }
    }

    private suspend fun fetchUserTimetableData(): List<DocumentSnapshot> = withContext(Dispatchers.IO) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email
        val querySnapshot = db.collection("Users")
            .whereEqualTo("isTeacher", false)
            .whereEqualTo("Email", userEmail)
            .get()
            .await()

        val selectedGroups = mutableSetOf<String>()
        for (document in querySnapshot) {
            val year = document.getString("Year")
            val group = document.getString("Group")
            val semigroup = document.getString("Semigroup")
            val selectedGroup = "7${year.orEmpty()}${group.orEmpty()} ${semigroup.orEmpty()}"
            selectedGroups.add(selectedGroup)
        }

        val allTimetableData = mutableListOf<DocumentSnapshot>()
        for (selectedGroup in selectedGroups) {
            val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
            for (day in daysOfWeek) {
                val dayTimetable = db.collection("Timetable").document(day)
                    .collection(selectedGroup)
                    .get()
                    .await()
                allTimetableData.addAll(dayTimetable.documents)
            }
        }
        allTimetableData
    }

    private suspend fun fetchTeacherTimetableData(userEmail: String?): List<DocumentSnapshot> = withContext(Dispatchers.IO) {
        val fullName = AndroidUtil().extractFullNameFromEmail(userEmail)

        val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")
        val allTimetableData = mutableListOf<DocumentSnapshot>()

        for (day in daysOfWeek) {
            try {
                val timetableRef = db.collection("Teachers").document(fullName).collection(day)
                val dayTimetable = timetableRef.get().await()
                allTimetableData.addAll(dayTimetable.documents)
            } catch (e: Exception) {
                Log.e("TimetableActivityVar2", "Error fetching documents for $fullName on $day", e)
            }
        }
        allTimetableData
    }



    private suspend fun fetchPersonalScheduleData(): List<DocumentSnapshot> = withContext(Dispatchers.IO) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email ?: throw IllegalStateException("User not logged in")
        val ptUsersRef = db.collection("PTUsers").document(userEmail).collection("schedule")
        val personalScheduleSnapshot = ptUsersRef.get().await()
        personalScheduleSnapshot.documents
    }

    @SuppressLint("WrongViewCast")
    private fun updateUI(userTimetableData: List<DocumentSnapshot>, personalScheduleData: List<DocumentSnapshot>) {
        val addedCourses = mutableSetOf<String>()
        val containers = mapOf(
            "Monday" to findViewById<LinearLayout>(R.id.mondayContainer),
            "Tuesday" to findViewById<LinearLayout>(R.id.tuesdayContainer),
            "Wednesday" to findViewById<LinearLayout>(R.id.wednesdayContainer),
            "Thursday" to findViewById<LinearLayout>(R.id.thursdayContainer),
            "Friday" to findViewById<LinearLayout>(R.id.fridayContainer)
        )

        // Process timetable documents
        userTimetableData.forEach { document ->
            // Check if the document is from the "Teachers" collection
            val isTeacherDocument = document.reference.path.contains("Teachers")
            val day = if (isTeacherDocument) {
                document.reference.parent.id // Direct parent is the day
            } else {
                document.reference.parent.parent?.id // Parent's parent is the day for "Timetable" collection
            }

            if (day != null) {
                val container = containers[day]
                if (container != null) {
                    Log.d("TimetableActivity", "Processing timetable document for $day: ${document.data}")
                    if (!addedCourses.contains(document.id)) {
                        addCourseToContainer(container, document)
                        addedCourses.add(document.id)
                    } else {
                        Log.d("TimetableActivity", "Document already added: ${document.id}")
                    }
                } else {
                    Log.d("TimetableActivity", "No container found for day: $day")
                }
            } else {
                Log.d("TimetableActivity", "Document has no day: ${document.data}")
            }
        }

        // Process personal schedule documents
        personalScheduleData.forEach { document ->
            val day = document.getString("day")
            if (day != null) {
                val container = containers[day]
                if (container != null) {
                    Log.d("TimetableActivity", "Processing personal schedule document for $day: ${document.data}")
                    addPersonalScheduleToContainer(container, document)
                } else {
                    Log.d("TimetableActivity", "No container found for day: $day")
                }
            } else {
                Log.d("TimetableActivity", "Personal schedule document has no day: ${document.data}")
            }
        }

        // Sort the containers by hours
        AndroidUtil().sortContainersByHours(containers.values.toList())

        // Update container height
        containers.values.forEach { AndroidUtil().updateContainerHeight(it) }

        // Set up the edit button functionality
        findViewById<ImageButton>(R.id.editButton).setOnClickListener {
            // Toggle the activation state of the editButton
            isButtonActivated = !isButtonActivated
            updateEditButtonAppearance()
        }
    }


    @SuppressLint("UseCompatLoadingForDrawables")
    private fun addCourseToContainer(container: LinearLayout, document: DocumentSnapshot) {
        // Ensure the fields are accessed correctly
        val courseName = document.getString("CourseName") ?: document.id
        val hours = document.getString("Hours")
        val room = document.getString("Room")
        val teacher = document.getString("Teacher")
        val type = document.getString("Type")
        val week = document.getString("Week")
        val group = document.getString("Group")
        val semigroup = document.getString("Semigroup")

        val teacherInfo = if (isTeacher) "$group, $semigroup" else teacher

        val fullText = "$hours, $week\n$courseName, $type\n$teacherInfo\n$room"
        val spannableString = SpannableString(fullText).apply {
            setSpan(AbsoluteSizeSpan(20, true), 0, hours?.length ?: 0, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(AbsoluteSizeSpan(20, true), hours?.length ?: 0, fullText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val courseTextView = TextView(this).apply {
            text = spannableString
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            background = getDrawable(R.drawable.rounded_rectangle)?.apply {
                setTint(Color.parseColor(arrayOf("#88cbb2", "#9ec3e1", "#ac80df", "#4da6ec").random()))
            }
            setPadding(resources.getDimensionPixelSize(R.dimen.course_padding))
            layoutParams = LinearLayout.LayoutParams(850, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
                bottomMargin = resources.getDimensionPixelSize(R.dimen.course_spacing)
            }
        }
        container.addView(courseTextView)
    }



    @SuppressLint("ClickableViewAccessibility", "UseCompatLoadingForDrawables")
    private fun addPersonalScheduleToContainer(container: LinearLayout, document: DocumentSnapshot) {
        val scheduleName = document.getString("Schedule")
        val hours = document.getString("Hours")

        val fullText = "$hours\n$scheduleName"
        val spannableString = SpannableString(fullText).apply {
            setSpan(AbsoluteSizeSpan(20, true), 0, hours?.length ?: 0, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
            setSpan(AbsoluteSizeSpan(20, true), hours?.length ?: 0, fullText.length, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE)
        }

        val personalScheduleTextView = TextView(this).apply {
            text = spannableString
            textSize = 24f
            gravity = Gravity.CENTER
            setTextColor(Color.BLACK)
            background = getDrawable(R.drawable.rounded_rectangle)?.apply {
                setTint(Color.parseColor(arrayOf("#88cbb2", "#9ec3e1", "#ac80df", "#4da6ec").random()))
            }
            setPadding(resources.getDimensionPixelSize(R.dimen.course_padding))
            layoutParams = LinearLayout.LayoutParams(850, LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                gravity = Gravity.CENTER
                bottomMargin = resources.getDimensionPixelSize(R.dimen.course_spacing)
            }
        }

        // Set the onTouchListener for editing functionality
        personalScheduleTextView.setOnTouchListener { v, event ->
            if (isButtonActivated) {
                when (event.action) {
                    MotionEvent.ACTION_DOWN -> {
                        personalScheduleTextView.setBackgroundColor(ContextCompat.getColor(this,
                            R.color.purple
                        ))
                        showScheduleSettings(document)
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        personalScheduleTextView.background = getDrawable(R.drawable.rounded_rectangle)?.apply {
                            setTint(Color.parseColor(arrayOf("#88cbb2", "#9ec3e1", "#ac80df", "#4da6ec").random()))
                        }
                    }
                }
                true
            } else {
                false
            }
        }
        container.addView(personalScheduleTextView)
    }

    private fun showScheduleSettings(document: DocumentSnapshot) {
        val dialogBuilder = AlertDialog.Builder(this)
        val inflater = this.layoutInflater
        val dialogView: View = inflater.inflate(R.layout.popup_edit_schedule, null)
        dialogBuilder.setView(dialogView)

        val deleteButton = dialogView.findViewById<Button>(R.id.delete_button)
        val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)

        val alertDialog = dialogBuilder.create()

        deleteButton.setOnClickListener {
            deleteFromPTUsers(document)
            alertDialog.dismiss()
        }

        cancelButton.setOnClickListener {
            alertDialog.dismiss()
        }

        alertDialog.show()
    }

    private fun deleteFromPTUsers(documentSnapshot: DocumentSnapshot) {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        currentUserEmail?.let { email ->
            // Reference to the user's document
            val userDocumentRef = db.collection("PTUsers").document(email)
            // Reference to the schedule subcollection
            val scheduleCollectionRef = userDocumentRef.collection("schedule")

            // Query the schedule subcollection to find the schedule item
            scheduleCollectionRef
                .whereEqualTo("Schedule", documentSnapshot.getString("Schedule"))
                .whereEqualTo("Hours", documentSnapshot.getString("Hours"))
                .get()
                .addOnSuccessListener { documents ->
                    // Iterate through the documents and delete each one
                    for (document in documents) {
                        scheduleCollectionRef.document(document.id).delete()
                            .addOnSuccessListener {
                                Log.d("Schedule", "DocumentSnapshot successfully deleted!")
                            }
                            .addOnFailureListener { exception ->
                                Log.w("Schedule", "Error deleting document", exception)
                            }
                    }
                    loadTimetableData()
                }
                .addOnFailureListener { exception ->
                    Log.w("Schedule", "Error getting documents", exception)
                }
        }
    }

}
