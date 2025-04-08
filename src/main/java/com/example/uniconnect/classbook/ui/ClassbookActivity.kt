package com.example.uniconnect.classbook.ui
import android.annotation.SuppressLint
import android.app.DatePickerDialog
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Button
import android.widget.EditText
import android.widget.HorizontalScrollView
import android.widget.TableLayout
import android.widget.TableRow
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Spinner
import androidx.annotation.RequiresApi
import com.example.uniconnect.R
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import com.example.uniconnect.utils.AndroidUtil


data class Student(val fullName: String)

@RequiresApi(Build.VERSION_CODES.O)
private val dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

class ClassbookActivity : AppCompatActivity() {

    private val db = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()
    private lateinit var attendanceTableLayout: TableLayout
    lateinit var backButton: Button
    private var isTeacher: Boolean = false
    private lateinit var selectDateButton: Button
    private var selectedAttendanceDate: String? = null

    @SuppressLint("NewApi")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_classbook)

        isTeacher = intent.getBooleanExtra("isTeacher", false)

        attendanceTableLayout = findViewById(R.id.attendanceTableLayout)
        backButton = findViewById(R.id.buttonBack)

        selectDateButton = findViewById<Button>(R.id.selectDateButton)

        // Initialize selectedAttendanceDate to the current date if not already set
        if (selectedAttendanceDate == null) {
            selectedAttendanceDate = LocalDate.now().format(dateFormatter)
        }

        val selectedGroup = intent.getStringExtra("selectedGroup").orEmpty()
        val courseId = intent.getStringExtra("courseName").orEmpty()
        val type = intent.getStringExtra("type").orEmpty()
        val week = intent.getStringExtra("week").orEmpty()
        val courseName = "$courseId-$selectedGroup-$type-$week"
        val dateSpinner = findViewById<Spinner>(R.id.dateSpinner)

        if (isTeacher) {
            findViewById<Button>(R.id.saveButton).visibility = View.VISIBLE
            findViewById<HorizontalScrollView>(R.id.horizontal_scroll_view).visibility = View.VISIBLE
        } else {
            findViewById<Button>(R.id.saveButton).visibility = View.GONE
            findViewById<HorizontalScrollView>(R.id.horizontal_scroll_view).visibility = View.GONE
        }

        selectDateButton.setOnClickListener {
            showDatePickerDialog()
        }


        backButton.setOnClickListener {
            finish()
        }

        getStudentListFromFirebase(selectedGroup) { students ->
            addColumns(attendanceTableLayout, students, courseName)
        }

        findViewById<Button>(R.id.showAttendanceTableButton).setOnClickListener {
            val dateSpinner = findViewById<Spinner>(R.id.dateSpinner)
            val selectedDate = dateSpinner.selectedItem.toString()

            showFirebaseAttendanceTable(courseName, selectedDate)
        }

        getFirebaseDates(courseName) { dates ->
            val adapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, dates)
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
            dateSpinner.adapter = adapter

            // Set a listener to handle date selection
            dateSpinner.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                override fun onItemSelected(parentView: AdapterView<*>?, selectedItemView: View?, position: Int, id: Long) {
                    val selectedDate = dates[position]
                    showFirebaseAttendanceTable(courseName, selectedDate)
                }

                override fun onNothingSelected(parentView: AdapterView<*>?) {
                    // Do nothing
                }
            }
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun showDatePickerDialog() {
        val datePicker = DatePickerDialog(
            this,
            { _, year, month, dayOfMonth ->
                selectedAttendanceDate = LocalDate.of(year, month + 1, dayOfMonth).format(
                    dateFormatter
                )
                // Add the selected date to the spinner
                val dateSpinner = findViewById<Spinner>(R.id.dateSpinner)
                (dateSpinner.adapter as ArrayAdapter<String>).add(selectedAttendanceDate)
                // Set the selected date in the spinner
                dateSpinner.setSelection((dateSpinner.adapter as ArrayAdapter<String>).count - 1)
                val selectedDateTextView = findViewById<TextView>(R.id.selectedDateTextView)
                selectedDateTextView.text = selectedAttendanceDate
            },
            LocalDate.now().year,
            LocalDate.now().monthValue - 1,
            LocalDate.now().dayOfMonth
        )
        datePicker.show()
    }


    @SuppressLint("NewApi")
    private fun getFirebaseDates(courseName: String, callback: (List<String>) -> Unit) {
        val datesCollectionRef = db.collection("AttendanceList")
            .document("AttendanceTable")
            .collection("Courses")
            .document(courseName)
            .collection("Dates")
        // Fetch dates data from Firebase
        datesCollectionRef.get()
            .addOnSuccessListener { querySnapshot ->
                val dates = mutableListOf<String>()

                for (document in querySnapshot.documents) {
                    dates.add(document.id)
                }

                // Add the current date to the list as a string
                dates.add(LocalDate.now().format(dateFormatter))

                // Callback with the fetched dates
                callback(dates)
            }
            .addOnFailureListener { exception ->
                Log.e("ClassbookActivity", "Error fetching dates from Firebase", exception)
                callback(emptyList())
            }
    }



    @RequiresApi(Build.VERSION_CODES.O)
    private fun handleTextInputChange(
        studentName: String,
        courseId: String,
        attendanceStatus: String
    ) {
        auth.currentUser?.uid.orEmpty()
        // Update the attendance data when text input is changed
        updateAttendance(selectedAttendanceDate!!, courseId, studentName, attendanceStatus)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun addColumns(tableLayout: TableLayout, students: List<Student>, courseId: String) {
        LocalDate.now().format(dateFormatter)

        for (student in students) {
            val tableRow = TableRow(this)

            // Student name (first column)
            val studentNameTextView = TextView(this)
            studentNameTextView.text = student.fullName
            tableRow.addView(studentNameTextView)

            // Attendance text input
            val attendanceInput = EditText(this)
            attendanceInput.hint = "Enter attendance status"
            attendanceInput.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    // Handle text input change when focus is lost
                    handleTextInputChange(
                        student.fullName,
                        courseId,
                        attendanceInput.text.toString()
                    )
                }
            }
            tableRow.addView(attendanceInput)

            tableLayout.addView(tableRow)
        }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun updateAttendance(
        date: String,
        courseId: String,
        studentName: String,
        attendanceStatus: String
    ) {
        val attendanceRef = db.collection("AttendanceList").document("AttendanceTable")
            .collection("Courses").document(courseId)
            .collection("Dates").document(date)
            .collection("Students").document(studentName)

        val attendanceData = mapOf("attendanceStatus" to attendanceStatus)

        // Save the updated data back to Firestore
        attendanceRef.set(attendanceData)
            .addOnSuccessListener {
                Log.d("ClassbookActivity", "Attendance data updated successfully")
            }
            .addOnFailureListener { exception ->
                Log.e("ClassbookActivity", "Error updating attendance data", exception)
            }
    }

    @RequiresApi(Build.VERSION_CODES.O)
    fun onSaveButtonClicked(view: View) {
        val selectedGroup = intent.getStringExtra("selectedGroup").orEmpty()
        val courseId = intent.getStringExtra("courseName").orEmpty()
        val type = intent.getStringExtra("type").orEmpty()
        val week = intent.getStringExtra("week").orEmpty()
        val courseName = "$courseId-$selectedGroup-$type-$week"

        // Fetching and update student list
        getStudentListFromFirebase(selectedGroup) { students ->
            for (student in students) {
                // Extract attendance status from the corresponding EditText
                val attendanceInput = findAttendanceInput(student.fullName)
                val attendanceStatus = attendanceInput?.text.toString()

                // Update attendance only if the attendance status is not empty
                if (attendanceStatus.isNotEmpty()) {
                    updateAttendance(
                        selectedAttendanceDate!!,
                        courseName,
                        student.fullName,
                        attendanceStatus
                    )
                }
            }
        }
    }


    private fun findAttendanceInput(studentName: String): EditText? {
        val tableLayout = findViewById<TableLayout>(R.id.attendanceTableLayout)
        for (i in 0 until tableLayout.childCount) {
            val tableRow = tableLayout.getChildAt(i) as? TableRow
            if (tableRow != null) {
                val studentNameTextView = tableRow.getChildAt(0) as? TextView
                if (studentNameTextView?.text?.toString() == studentName) {
                    return tableRow.getChildAt(1) as? EditText
                }
            }
        }
        return null
    }



    private fun getStudentListFromFirebase(selectedGroup: String, callback: (List<Student>) -> Unit) {
        val students = mutableListOf<Student>()

        db.collection("Users")
            .whereEqualTo("isTeacher", false)
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    val email = document.getString("Email")
                    document.getString("Group")
                    val year = document.getString("Year")
                    val semigroup = document.getString("Semigroup")

                    // Decompose the selectedGroup to find corresponding Year and Semigroup
                    val selectedYear = selectedGroup.getOrNull(1)?.toString()
                    val selectedSemigroup = selectedGroup.lastOrNull()?.toString()

                    // Filter users based on Year and Semigroup
                    if (year == selectedYear && semigroup == selectedSemigroup) {
                        val fullName = AndroidUtil().extractFullNameFromEmail(email)
                        val student = Student(fullName)
                        students.add(student)
                    }
                }

                // After fetching the student list, the callback is invoked
                callback(students)
            }
            .addOnFailureListener { exception ->
                Log.e("ClassbookActivity", "getStudentListFromFirebase: Error fetching students", exception)
            }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun showFirebaseAttendanceTable(courseName: String, selectedDate: String) {
        // Clear existing rows from the firebaseAttendanceTableLayout
        findViewById<TableLayout>(R.id.firebaseAttendanceTableLayout).removeAllViews()

        // Fetch attendance data from Firebase based on selectedGroup, courseName, and selectedDate
        getFirebaseAttendanceData(courseName, selectedDate) { firebaseAttendanceData ->
            // Adding columns dynamically for each attendance status
            addFirebaseAttendanceColumns(findViewById(R.id.firebaseAttendanceTableLayout), firebaseAttendanceData)

            // Adding rows with attendance data for each student
            for (student in firebaseAttendanceData) {
                addFirebaseAttendanceRow(findViewById(R.id.firebaseAttendanceTableLayout), student, selectedDate)
            }
            findViewById<TableLayout>(R.id.firebaseAttendanceTableLayout).visibility = View.VISIBLE
        }
    }


    @SuppressLint("SetTextI18n")
    private fun addFirebaseAttendanceColumns(tableLayout: TableLayout, students: List<Student>) {
        // Adding the first column for student names
        val headerRow = TableRow(this)
        val studentNameHeader = TextView(this)
        studentNameHeader.text = "Student Name"
        headerRow.addView(studentNameHeader)

        // Adding columns for each student's attendance status
        for (student in students) {
            val attendanceStatusHeader = TextView(this)
            headerRow.addView(attendanceStatusHeader)
        }

        // Adding the header row to the table
        tableLayout.addView(headerRow)
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun getFirebaseAttendanceData(
        courseName: String,
        selectedDate: String,
        callback: (List<Student>) -> Unit
    ) {
        val attendanceCollectionRef = db.collection("AttendanceList")
            .document("AttendanceTable")
            .collection("Courses")
            .document(courseName)
            .collection("Dates")
            .document(selectedDate)
            .collection("Students")

        attendanceCollectionRef.get()
            .addOnSuccessListener { querySnapshot ->
                val students = mutableListOf<Student>()

                for (document in querySnapshot.documents) {
                    val studentName = document.id
                    val student = Student(studentName)
                    students.add(student)
                }

                // Callback with the fetched attendance data
                callback(students)
            }
            .addOnFailureListener { exception ->
                // Callback with an empty list in case of an error
                callback(emptyList())
            }
    }


    @RequiresApi(Build.VERSION_CODES.O)
    private fun addFirebaseAttendanceRow(tableLayout: TableLayout, student: Student, selectedDate: String) {
        // Adding a row for each student's attendance data
        val row = TableRow(this)

        // Adding the student name in the first column
        val studentNameTextView = TextView(this)
        studentNameTextView.text = student.fullName
        row.addView(studentNameTextView)
        val courseId = intent.getStringExtra("courseName").orEmpty()
        val type = intent.getStringExtra("type").orEmpty()
        val selectedGroup = intent.getStringExtra("selectedGroup").orEmpty()
        val week = intent.getStringExtra("week").orEmpty()
        val courseName = "$courseId-$selectedGroup-$type-$week"

        val attendanceCollectionRef = db.collection("AttendanceList")
            .document("AttendanceTable")
            .collection("Courses")
            .document(courseName)
            .collection("Dates")
            .document(selectedDate)
            .collection("Students")
            .document(student.fullName)

        // Fetch attendance data
        attendanceCollectionRef.get()
            .addOnSuccessListener { documentSnapshot ->
                val attendanceData = documentSnapshot.data

                if (attendanceData != null) {
                    val attendanceStatus = attendanceData["attendanceStatus"] as? String

                    if (attendanceStatus != null) {
                        // Display the attendance status
                        val attendanceTextView = TextView(this)
                        attendanceTextView.text = attendanceStatus
                        row.addView(attendanceTextView)
                    } else {
                        Log.e("ClassbookActivity", "Unexpected format for attendanceStatus")
                    }
                } else {
                    Log.e("ClassbookActivity", "No data found for attendance")
                }

                // Add the row to the table
                tableLayout.addView(row)
            }
            .addOnFailureListener { exception ->
                Log.e("ClassbookActivity", "Error fetching attendance data from Firebase", exception)
            }
    }

}