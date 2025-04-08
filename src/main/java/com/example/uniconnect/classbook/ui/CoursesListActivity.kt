package com.example.uniconnect.classbook.ui

import android.annotation.SuppressLint
import android.app.AlertDialog
import android.content.ContentValues.TAG
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.FirebaseFirestore
import android.view.Gravity
import android.view.LayoutInflater
import android.widget.ArrayAdapter
import android.widget.Spinner
import android.widget.Toast
import androidx.core.content.ContextCompat
import com.example.uniconnect.R
import com.example.uniconnect.utils.AndroidUtil

class CoursesListActivity : ComponentActivity() {

    private val db = FirebaseFirestore.getInstance()
    private lateinit var editTextGroup: EditText
    private lateinit var resultTextView: TextView
    private lateinit var resultTextViewTuesday: TextView
    private lateinit var resultTextViewWednesday: TextView
    private lateinit var resultTextViewThursday: TextView
    private lateinit var resultTextViewFriday: TextView
    private lateinit var mondayTextView: TextView
    private lateinit var tuesdayTextView: TextView
    private lateinit var wednesdayTextView: TextView
    private lateinit var thursdayTextView: TextView
    private lateinit var fridayTextView: TextView
    lateinit var backButton: Button
    lateinit var addCourseButton: Button
    lateinit var deleteCourseButton: Button
    private var isButtonActivated = false

    private lateinit var mondayContainer: LinearLayout
    private lateinit var tuesdayContainer: LinearLayout
    private lateinit var wednesdayContainer: LinearLayout
    private lateinit var thursdayContainer: LinearLayout
    private lateinit var fridayContainer: LinearLayout

    private var isTeacher: Boolean = false


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_courses_list)

        isTeacher = intent.getBooleanExtra("isTeacher", false)


        mondayContainer = findViewById(R.id.mondayContainer)
        tuesdayContainer = findViewById(R.id.tuesdayContainer)
        wednesdayContainer = findViewById(R.id.wednesdayContainer)
        thursdayContainer = findViewById(R.id.thursdayContainer)
        fridayContainer = findViewById(R.id.fridayContainer)

        editTextGroup = findViewById(R.id.editTextGroup)
        resultTextView = findViewById(R.id.resultTextView)
        resultTextViewTuesday = findViewById(R.id.resultTextViewTuesday)
        resultTextViewWednesday = findViewById(R.id.resultTextViewWednesday)
        resultTextViewThursday = findViewById(R.id.resultTextViewThursday)
        resultTextViewFriday = findViewById(R.id.resultTextViewFriday)

        mondayTextView = findViewById(R.id.mondayTextView)
        tuesdayTextView = findViewById(R.id.tuesdayTextView)
        wednesdayTextView = findViewById(R.id.wednesdayTextView)
        thursdayTextView = findViewById(R.id.thursdayTextView)
        fridayTextView = findViewById(R.id.fridayTextView)

        addCourseButton = findViewById(R.id.addCourseButton)
        deleteCourseButton = findViewById(R.id.deleteCourseButton)


        if (isTeacher)
        {
            addCourseButton.visibility = View.VISIBLE
            deleteCourseButton.visibility = View.VISIBLE
            deleteCourseButton.setOnClickListener {
                // Toggle the activation state of the delete button
                isButtonActivated = !isButtonActivated
                updateDeleteButtonAppearance()
            }
            addCourseButton.setOnClickListener {
                isButtonActivated = !isButtonActivated
                updateAddButtonAppearance()
            }
        }

        findViewById<Button>(R.id.btnFiltreaza).setOnClickListener {
            val selectedGroup = editTextGroup.text.toString()
            if (selectedGroup.isEmpty()) {
                Toast.makeText(this, "Please enter group semigroup", Toast.LENGTH_SHORT).show()
                mondayTextView.visibility = View.GONE
                tuesdayTextView.visibility = View.GONE
                wednesdayTextView.visibility = View.GONE
                thursdayTextView.visibility = View.GONE
                fridayTextView.visibility = View.GONE
            } else {
                Toast.makeText(this, "Please click on any schedule to see attendance", Toast.LENGTH_SHORT).show()
                mondayTextView.visibility = View.VISIBLE
                tuesdayTextView.visibility = View.VISIBLE
                wednesdayTextView.visibility = View.VISIBLE
                thursdayTextView.visibility = View.VISIBLE
                fridayTextView.visibility = View.VISIBLE

                loadTimetableData()
            }
        }


        backButton = findViewById(R.id.buttonBack)

        backButton.setOnClickListener {
            finish()
        }

    }

    private fun updateDeleteButtonAppearance() {
        if (isButtonActivated) {
            deleteCourseButton.setBackgroundColor(ContextCompat.getColor(this, R.color.darkerPurple))
            Toast.makeText(this, "Please click on the schedule you want to delete", Toast.LENGTH_SHORT).show()
        } else {
            deleteCourseButton.setBackgroundColor(ContextCompat.getColor(this, R.color.lightPurple))
        }
    }

    private fun updateAddButtonAppearance() {
        if (isButtonActivated) {
            addCourseButton.setBackgroundColor(ContextCompat.getColor(this, R.color.darkerPurple))
            openAddPopupDialog()

        } else {
            addCourseButton.setBackgroundColor(ContextCompat.getColor(this, R.color.lightPurple))
        }
    }

private fun showPopup(uniqueDocumentName: String, dayOfWeek: String) {
    val dialogBuilder = AlertDialog.Builder(this)
    val inflater = this.layoutInflater
    val dialogView: View = inflater.inflate(R.layout.popup_edit_schedule, null)
    dialogBuilder.setView(dialogView)

    val deleteButton = dialogView.findViewById<Button>(R.id.delete_button)
    val cancelButton = dialogView.findViewById<Button>(R.id.cancel_button)

    val alertDialog = dialogBuilder.create()

    deleteButton.setOnClickListener {
        deleteData(uniqueDocumentName, dayOfWeek)
        alertDialog.dismiss()
    }

    cancelButton.setOnClickListener {
        alertDialog.dismiss()
    }

    alertDialog.show()
}



    private fun openAddPopupDialog() {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.popup_add_course, null)

        val builder = AlertDialog.Builder(this)
            .setView(dialogView)
            .setTitle("Add Course")

        val alertDialog = builder.show()

        val daySpinner = dialogView.findViewById<Spinner>(R.id.daySpinner)
        val groupEditText = dialogView.findViewById<EditText>(R.id.groupEditText)
        val subgroupSpinner = dialogView.findViewById<Spinner>(R.id.subgroupSpinner)
        val courseNameEditText = dialogView.findViewById<EditText>(R.id.courseNameEditText)
        val hoursEditText = dialogView.findViewById<EditText>(R.id.hoursEditText)
        val roomEditText = dialogView.findViewById<EditText>(R.id.roomEditText)
        val typeEditText = dialogView.findViewById<Spinner>(R.id.typeSpinner)
        val weekEditText = dialogView.findViewById<Spinner>(R.id.weekSpinner)
        val teacherSpinner = dialogView.findViewById<Spinner>(R.id.teacherSpinner)
        val addButton = dialogView.findViewById<Button>(R.id.addButton)

        // Fetch and populate spinner with teacher data
        val teachers = mutableListOf<String>() // List to store teachers' names

        // Fetch teachers' names from the database
        db.collection("Users")
            .whereEqualTo("isTeacher", true) // Only fetch users who are teachers
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val email = document.getString("Email")
                    val fullName = AndroidUtil().extractFullNameFromEmail(email)
                    teachers.add(fullName)
                }

                // After fetching the teacher names, populate the spinner
                val teacherAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, teachers)
                teacherAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                teacherSpinner.adapter = teacherAdapter
            }
            .addOnFailureListener { exception ->
                Log.e("YourActivity", "Error fetching teachers", exception)
            }


        // Set up button click listener to add the course
        addButton.setOnClickListener {
            // Retrieve values from the dialog fields
            val selectedDay = daySpinner.selectedItem.toString()
            val group = groupEditText.text.toString()
            val subgroup = subgroupSpinner.selectedItem.toString()
            val courseName = courseNameEditText.text.toString()
            val hours = hoursEditText.text.toString()
            val room = roomEditText.text.toString()
            val type = typeEditText.selectedItem.toString()
            val week = weekEditText.selectedItem.toString()
            val selectedTeacher = teacherSpinner.selectedItem.toString()

            if (!AndroidUtil().validateTimeFormat(hours)) {
                Toast.makeText(this, "Please enter the time in the format e.g., 7:00-8:00", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (subgroup == "All") {
                for (i in 1..3) {
                    val semigroup = i.toString()
                    addOrUpdateCourseToTimetable(selectedDay, group, semigroup, courseName, hours, room, selectedTeacher, type, week)
                }
                addOrUpdateCourseToTeacherTimetable(selectedTeacher, selectedDay, courseName, group, hours, room, subgroup, type, week)
            } else {
                addOrUpdateCourseToTimetable(selectedDay, group, subgroup, courseName, hours, room, selectedTeacher, type, week)
                addOrUpdateCourseToTeacherTimetable(selectedTeacher, selectedDay, courseName, group, hours, room, subgroup, type, week)
            }

            alertDialog.dismiss()
        }
    }


    private fun addOrUpdateCourseToTimetable(
        day: String,
        group: String,
        subgroup: String,
        courseName: String,
        hours: String,
        room: String,
        teacher: String,
        type: String,
        week: String
    ) {
        val uniqueDocumentName = "$courseName-$group $subgroup-$type-$week"

        val dayDocumentRef = db.collection("Timetable").document(day)

        // Check if the day document exists
        dayDocumentRef.get()
            .addOnSuccessListener { dayDocumentSnapshot ->
                if (dayDocumentSnapshot.exists()) {
                    // Day document exists
                    // Reference to the group/subgroup collection within the day document
                    val groupSubgroupCollectionRef = dayDocumentRef.collection("$group $subgroup")

                    // Add or update the course document
                    groupSubgroupCollectionRef.document(uniqueDocumentName)
                        .set(createCourseData(courseName, hours, room, teacher, type, week))
                        .addOnSuccessListener {
                            // Course added or updated successfully
                            Log.d(TAG, "Course added successfully")
                        }
                        .addOnFailureListener { exception ->
                            Log.w(TAG, "Error adding course", exception)
                        }
                } else {
                    // Day document doesn't exist
                    // Create the day document and group/subgroup collection
                    // Create the group/subgroup collection
                    val groupSubgroupCollectionRef = dayDocumentRef.collection("$group $subgroup")

                    // Add the course document
                    groupSubgroupCollectionRef.document(uniqueDocumentName)
                        .set(createCourseData(courseName, hours, room, teacher, type, week))
                        .addOnSuccessListener {
                            // Course added successfully
                            Log.d(TAG, "Course added successfully")
                        }
                        .addOnFailureListener { exception ->
                            Log.w(TAG, "Error adding course", exception)
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error adding course", exception)
            }
    }


    private fun addOrUpdateCourseToTeacherTimetable(
        teacherName: String,
        day: String,
        courseName: String,
        group: String,
        hours: String,
        room: String,
        semigroup: String,
        type: String,
        week: String
    ) {
        val uniqueDocumentName = "$courseName-$group $semigroup-$type-$week"

        val teacherDocumentRef = db.collection("Teachers").document(teacherName)

        val dayCollectionRef = teacherDocumentRef.collection(day)

        val courseDocumentRef = dayCollectionRef.document(uniqueDocumentName)

        // Data to be added or updated
        val courseData = hashMapOf(
            "CourseName" to courseName,
            "Group" to group,
            "Hours" to hours,
            "Room" to room,
            "Semigroup" to semigroup,
            "Type" to type,
            "Week" to week
        )

        // Add or update the course document
        courseDocumentRef.set(courseData)
            .addOnSuccessListener {
                // Course added successfully
                Toast.makeText(this, "Course Added Successfully", Toast.LENGTH_SHORT).show()
                loadTimetableData()
            }
            .addOnFailureListener { exception ->
                Log.w(TAG, "Error adding course", exception)
            }
    }

    private fun createCourseData(
        courseName: String,
        hours: String,
        room: String,
        teacher: String,
        type: String,
        week: String
    ): Map<String, Any> {
        val courseData = HashMap<String, Any>()
        courseData["CourseName"] = courseName
        courseData["Hours"] = hours
        courseData["Room"] = room
        courseData["Teacher"] = teacher
        courseData["Type"] = type
        courseData["Week"] = week
        return courseData
    }


    @SuppressLint("SetTextI18n")
    private fun loadTimetableData() {
        val selectedGroup = editTextGroup.text.toString()

        if (selectedGroup.isEmpty()) {
            Toast.makeText(this, "Please enter group semigroup", Toast.LENGTH_SHORT).show()
            mondayTextView.visibility = View.GONE
            tuesdayTextView.visibility = View.GONE
            wednesdayTextView.visibility = View.GONE
            thursdayTextView.visibility = View.GONE
            fridayTextView.visibility = View.GONE
            return
        }

        val daysOfWeek = listOf("Monday", "Tuesday", "Wednesday", "Thursday", "Friday")

        for (day in daysOfWeek) {
            val timetableRef = db.collection("Timetable").document(day)
                .collection(selectedGroup)

            timetableRef.get()
                .addOnSuccessListener { result ->
                    val stringBuilder = StringBuilder()

                    for (document in result) {
                        val courseName = document.getString("CourseName")
                        val hours = document.getString("Hours")
                        val room = document.getString("Room")
                        val teacher = document.getString("Teacher")
                        val type = document.getString("Type")
                        val week = document.getString("Week")

                        // Build a string with the retrieved data
                        stringBuilder.append("Course: $courseName\n")
                        stringBuilder.append("Hours: $hours\n")
                        stringBuilder.append("Room: $room\n")
                        stringBuilder.append("Teacher: $teacher\n")
                        stringBuilder.append("Type: $type\n")
                        stringBuilder.append("Week: $week\n\n")
                    }
                    // Display the data in your UI (TextView) based on the day of the week
                    when (day) {
                        "Monday" -> resultTextView.text = stringBuilder.toString()
                        "Tuesday" -> resultTextViewTuesday.text = stringBuilder.toString()
                        "Wednesday" -> resultTextViewWednesday.text = stringBuilder.toString()
                        "Thursday" -> resultTextViewThursday.text = stringBuilder.toString()
                        "Friday" -> resultTextViewFriday.text = stringBuilder.toString()
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e("TimetableActivity", "Error loading timetable data", exception)
                    when (day) {
                        "Monday" -> resultTextView.text = "Error loading timetable data"
                        "Tuesday" -> resultTextViewTuesday.text = "Error loading timetable data"
                        "Wednesday" -> resultTextViewWednesday.text = "Error loading timetable data"
                        "Thursday" -> resultTextViewThursday.text = "Error loading timetable data"
                        "Friday" -> resultTextViewFriday.text = "Error loading timetable data"
                    }
                }

            // Call clearAndPopulateButtons function for each day
            val container = when (day) {
                "Monday" -> mondayContainer
                "Tuesday" -> tuesdayContainer
                "Wednesday" -> wednesdayContainer
                "Thursday" -> thursdayContainer
                "Friday" -> fridayContainer
                else -> null
            }

            container?.let { clearAndPopulateButtons(it, timetableRef, day) }
            container?.let{ AndroidUtil().sortContainersByHours(listOf(it)) }
            container?.let { AndroidUtil().updateContainerHeight(it) }
        }
    }

@SuppressLint("SetTextI18n", "UseCompatLoadingForDrawables")
private fun clearAndPopulateButtons(container: LinearLayout, timetableRef: CollectionReference, dayOfWeek: String) {
    val selectedGroup = editTextGroup.text.toString()
    container.removeAllViews()

    val dayTextView = TextView(this)
    container.addView(dayTextView)

    timetableRef.get()
        .addOnSuccessListener { result ->
            for (document in result) {
                val courseName = document.getString("CourseName")
                val hours = document.getString("Hours")
                val room = document.getString("Room")
                val teacher = document.getString("Teacher")
                val type = document.getString("Type")
                val week = document.getString("Week")

                val uniqueDocumentName = "$courseName-$selectedGroup-$type-$week"

                // Create a new Button for each document
                val courseButton = Button(this)
                courseButton.text = "$courseName, $type\n$hours, $week\n$teacher\n$room"
                courseButton.textSize = 14f // Adjust the text size as needed
                courseButton.gravity = Gravity.CENTER // Center the text horizontally

                // Set a custom background with rounded corners and different colors for each course
                val colorArray = arrayOf("#88cbb2", "#9ec3e1", "#ac80df", "#4da6ec")
                val randomColor = colorArray.random()
                val drawable = resources.getDrawable(R.drawable.rounded_rectangle, theme)
                drawable.setTint(android.graphics.Color.parseColor(randomColor))

                // Set layout parameters for width and gravity
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.WRAP_CONTENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.width = 850  // Adjust the width as needed
                params.gravity = Gravity.CENTER

                // Adjust the size of the rectangle
                val padding = resources.getDimensionPixelSize(R.dimen.course_padding)
                courseButton.background = drawable
                courseButton.setPadding(padding, padding, padding, padding)

                // Apply layout parameters to the courseButton
                courseButton.layoutParams = params

                // Add bottom margin to create space between buttons
                params.bottomMargin = resources.getDimensionPixelSize(R.dimen.course_spacing)

                courseButton.setOnClickListener {
                    if (isButtonActivated) {
                        // If the delete button is activated, show the popup_edit_schedule popup
                        showPopup(uniqueDocumentName, dayOfWeek)  // Pass the full document name to the popup
                    } else {
                        // If the delete button is not activated, handle regular button click
                        if (type != null && courseName != null && week!=null) {
                            navigateToAttendanceList(courseName, selectedGroup, type, week)
                        }
                    }
                }
                container.addView(courseButton)
                container.let{ AndroidUtil().sortContainersByHours(listOf(it)) }
            }
        }
}


private fun deleteData(uniqueDocumentName: String, dayOfWeek: String) {
    val selectedGroup = editTextGroup.text.toString()
    val documentRef = db.collection("Timetable").document(dayOfWeek).collection(selectedGroup).document(uniqueDocumentName)

    // First, fetch the document to get the teacher's name
    documentRef.get()
        .addOnSuccessListener { document ->
            if (document.exists()) {
                val teacherName = document.getString("Teacher")

                // Delete the document from the Timetable collection
                documentRef.delete()
                    .addOnSuccessListener {
                        // Document successfully deleted from Timetable
                        Log.d(TAG, "DocumentSnapshot successfully deleted!")
                        loadTimetableData()

                        // Now, delete the document from the Teachers collection
                        if (teacherName != null) {
                            deleteFromTeacherCollection(uniqueDocumentName, dayOfWeek, teacherName)
                        }
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error deleting document", e)
                    }
            } else {
                Log.d(TAG, "No such document")
            }
        }
        .addOnFailureListener { e ->
            Log.w(TAG, "Error getting document", e)
        }
}

    private fun deleteFromTeacherCollection(uniqueDocumentName: String, dayOfWeek: String, teacherName: String) {
        val teacherDocumentRef = db.collection("Teachers").document(teacherName).collection(dayOfWeek).document(uniqueDocumentName)

        teacherDocumentRef.delete()
            .addOnSuccessListener {
                Log.d(TAG, "Document successfully deleted from Teachers collection!")
            }
            .addOnFailureListener { e ->
                Log.w(TAG, "Error deleting document from Teachers collection", e)
            }
    }



    private fun navigateToAttendanceList(courseName: String, selectedGroup: String, type: String, week: String) {
        val intent: Intent
        intent = Intent(this, ClassbookActivity::class.java)
        intent.putExtra("isTeacher", isTeacher)
        intent.putExtra("courseName", courseName)
        intent.putExtra("selectedGroup", selectedGroup)
        intent.putExtra("type", type)
        intent.putExtra("week", week)
        startActivity(intent)
    }


}
