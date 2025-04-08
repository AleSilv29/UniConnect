package com.example.uniconnect.socialFeed.ui

import android.content.ContentValues.TAG
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.AdapterDataObserver
import com.example.uniconnect.socialFeed.adapter.ChatRecyclerAdapter
import com.example.uniconnect.socialFeed.model.ChatMessageModel
import com.example.uniconnect.socialFeed.model.ChatroomModel
import com.example.uniconnect.socialFeed.model.UserModel
import com.example.uniconnect.socialFeed.model.WrapContentLinearLayoutManager
import com.example.uniconnect.utils.AndroidUtil
import com.example.uniconnect.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.DocumentSnapshot
import com.google.firebase.firestore.Query
import okhttp3.*
import okhttp3.MediaType.Companion.toMediaType
import org.json.JSONObject
import org.tensorflow.lite.Interpreter
import java.io.IOException
import java.util.Arrays
import com.example.uniconnect.socialFeed.model.Tokenizer
import com.google.firebase.ml.modeldownloader.CustomModel
import com.google.firebase.ml.modeldownloader.CustomModelDownloadConditions
import com.google.firebase.ml.modeldownloader.DownloadType
import com.google.firebase.ml.modeldownloader.FirebaseModelDownloader
import android.content.Context
import com.example.uniconnect.R
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore
import com.google.firebase.firestore.ktx.firestore


class ChatMessagesActivity : AppCompatActivity() {
    var otherUser: UserModel? = null
    var userId: String? = null
    var userEmail: String? = null
    var chatroomId: String? = null
    var sendMessageBtn: ImageButton? = null
    var backBtn: ImageButton? = null
    var optionsBtn: ImageButton? = null
    var otherUsername: TextView? = null
    var messageInput: EditText? = null
    var recyclerView: RecyclerView? = null
    var chatroomModel: ChatroomModel? = null
    var adapter: ChatRecyclerAdapter? = null
    var imageView: ImageView?= null
    private lateinit var interpreter: Interpreter
    private val tokenizer = Tokenizer()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat_messages)

        otherUser = AndroidUtil().getUserModelFromIntent(getIntent())
        userId = FirebaseUtil().currentUserId()

        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            userEmail = user.email
        }

        chatroomId = userEmail?.let {
            FirebaseUtil().getChatroomId(
                it,
                otherUser?.Email.toString()
            )
        }
        if (userEmail != null && otherUser?.Email != null) {
            // Concatenate emails in a consistent order to generate chatroomId
            if (userEmail!! < otherUser?.Email!!) {
                chatroomId = userEmail?.let {
                    FirebaseUtil().getChatroomId(
                        it,
                        otherUser?.Email.toString()
                    )
                }
            } else {
                chatroomId = userEmail?.let {
                    FirebaseUtil().getChatroomId(
                        otherUser?.Email.toString(),
                        it
                    )
                }
            }
        }

        messageInput = findViewById(R.id.chat_message_input)
        sendMessageBtn = findViewById(R.id.message_send_btn)
        backBtn = findViewById(R.id.back_btn)
        otherUsername = findViewById(R.id.other_username)
        recyclerView = findViewById(R.id.chat_recycler_view)
        imageView = findViewById(R.id.profile_pic_image_view)
        optionsBtn = findViewById(R.id.options_btn)


        FirebaseUtil().getOtherProfilePicStorageRef(otherUser?.Email)?.getDownloadUrl()
            ?.addOnCompleteListener(OnCompleteListener<Uri?> { t: Task<Uri?> ->
                if (t.isSuccessful) {
                    val uri = t.result
                    AndroidUtil().setProfilePic(this, uri, imageView)
                }
            })

        backBtn?.setOnClickListener {
            val intent = Intent(this, ChatPageActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        optionsBtn?.setOnClickListener {
            downloadModel()
            showPopupMenu(it)
        }

        otherUsername?.setText(otherUser?.Email)

        sendMessageBtn?.setOnClickListener(View.OnClickListener { v: View? ->
            val message = messageInput?.getText().toString().trim { it <= ' ' }
            if (message.isEmpty()) return@OnClickListener
            sendMessageToUser(message)
        })

        getOrCreateChatroomModel()
        setupChatRecyclerView()

    }

    private fun showPopupMenu(view: View) {
        val popupMenu = PopupMenu(this, view)
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            userEmail = user.email
        }
        popupMenu.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.menu_report -> {
                    // Handle report action
                    val lastMessage = chatroomModel?.lastMessage
                    val texts = listOf(lastMessage)
                    tokenizer.fitOnTexts(texts)
                    Toast.makeText(this, "User Reported", Toast.LENGTH_SHORT).show()
                    if (lastMessage != null) {
                        val isSpam = checkIfMessageIsSpam(lastMessage)
                        if (isSpam) {
                            Toast.makeText(this, "Spam detected", Toast.LENGTH_SHORT).show()
                            addOrUpdateReportList(otherUser!!.Email!!, userEmail!!, true)
                        } else {
                            Toast.makeText(this, "Spam not detected", Toast.LENGTH_SHORT).show()
                        }
                    }
                    true
                }
                R.id.menu_block -> {
                    // Handle block action
                    Toast.makeText(this, "User Blocked", Toast.LENGTH_SHORT).show()
                    true
                }
                R.id.menu_report_user -> {
                    addOrUpdateReportList(otherUser!!.Email!!, userEmail!!, false)
                    true
                }
                else -> false
            }
        }
        popupMenu.inflate(R.menu.popup_chat_options)
        popupMenu.show()
    }


    fun setupChatRecyclerView(){
        val query = FirebaseUtil().getChatroomMessageReference(chatroomId)
            ?.orderBy("timestamp", Query.Direction.DESCENDING)

        if (query != null) {
            query.get().addOnSuccessListener { documents ->
                for (document in documents) {
                    Log.d("Chat", "${document.id} => ${document.data}")
                }
            }.addOnFailureListener { exception ->
                Log.e("Chat", "Error fetching data: $exception")
            }
        }

        val options = query?.let {
            FirestoreRecyclerOptions.Builder<ChatMessageModel>()
                .setQuery(it, ChatMessageModel::class.java).build()
        }

        adapter = ChatRecyclerAdapter(options, applicationContext)
        val manager = WrapContentLinearLayoutManager(this)
        manager.reverseLayout = true
        recyclerView!!.layoutManager = manager
        recyclerView!!.adapter = adapter
        adapter!!.startListening()
        adapter!!.registerAdapterDataObserver(object : AdapterDataObserver() {
            override fun onItemRangeInserted(positionStart: Int, itemCount: Int) {
                super.onItemRangeInserted(positionStart, itemCount)
                recyclerView!!.smoothScrollToPosition(0)
            }
        })
    }

    fun getOrCreateChatroomModel() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            userEmail = user.email
        }
        FirebaseUtil().getChatroomReference(chatroomId).get()
            .addOnCompleteListener(OnCompleteListener<DocumentSnapshot> { task: Task<DocumentSnapshot> ->
                if (task.isSuccessful) {
                    chatroomModel = task.result.toObject(ChatroomModel::class.java)
                    if (chatroomModel == null) {
                        //first time chat
                        chatroomModel = ChatroomModel(
                            chatroomId,
                            Arrays.asList<String?>(
                                //FirebaseUtil().currentUserId(),
                                userEmail,
                                otherUser!!.Email
                            ),
                            Timestamp.now(),
                            ""
                        )
                        FirebaseUtil().getChatroomReference(chatroomId).set(chatroomModel!!)
                    }
                }
            })
    }

    fun sendMessageToUser(message: String){
        chatroomModel?.lastMessageTimestamp = Timestamp.now()
        chatroomModel?.lastMessageSenderId = FirebaseUtil().currentUserId()
        chatroomModel?.lastMessage = message
        FirebaseUtil().getChatroomReference(chatroomId).set(chatroomModel!!)
        val chatMessageModel = ChatMessageModel(message, FirebaseUtil().currentUserId(), Timestamp.now())
        FirebaseUtil().getChatroomMessageReference(chatroomId).add(chatMessageModel)
            ?.addOnCompleteListener(OnCompleteListener<DocumentReference?> { task ->
                if (task.isSuccessful) {
                    messageInput!!.setText("")
                    sendNotification(message)
                }
            })
    }

    fun sendNotification(message: String?) {
        otherUser = AndroidUtil().getUserModelFromIntent(getIntent())
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        // Fetch the FCM token from Firestore based on the current user's email
        FirebaseUtil().getUserByUserEmail(currentUserEmail) { userModel ->
            userModel?.let { user ->
                try {
                    val jsonObject = JSONObject()
                    val notificationObj = JSONObject()
                    notificationObj.put("title", user.Email)
                    notificationObj.put("body", message)
                    val dataObj = JSONObject()
                    dataObj.put("userId", user.Email)
                    jsonObject.put("notification", notificationObj)
                    jsonObject.put("data", dataObj)
                    jsonObject.put("to", otherUser!!.fcmToken)
                    callApi(jsonObject)
                } catch (e: Exception) {
                    Log.e("MessageNotif", "Error sending notification: $e")
                }
            }
        }

    }

    fun callApi(jsonObject: JSONObject) {
        val JSON: MediaType = "application/json; charset=utf-8".toMediaType()
        val client = OkHttpClient()
        val url = "https://fcm.googleapis.com/fcm/send"
        val body: RequestBody = RequestBody.create(JSON, jsonObject.toString())
        val request: Request = Request.Builder()
            .url(url)
            .post(body)
            .header("Authorization", "Bearer PRIVATE")
            .build()
        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "API call failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (!response.isSuccessful) {
                    Log.e(TAG, "API call unsuccessful: ${response.message}")
                }
            }


        })
    }

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, ChatPageActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

    private fun downloadModel() {
        val conditions = CustomModelDownloadConditions.Builder()
            .requireWifi()  // Also possible: .requireCharging() and .requireDeviceIdle()
            .build()
        FirebaseModelDownloader.getInstance()
            .getModel("Spam-Detector", DownloadType.LOCAL_MODEL_UPDATE_IN_BACKGROUND,
                conditions)
            .addOnSuccessListener { model: CustomModel? ->
                // Download complete.
                // The CustomModel object contains the local path of the model file,
                // which can instantiate a TensorFlow Lite interpreter.
                val modelFile = model?.file
                if (modelFile != null) {
                    interpreter = Interpreter(modelFile)
                }
            }
    }

    private fun checkIfMessageIsSpam(message: String): Boolean {
        // Preprocess the message
        val preprocessedMessage = preprocessMessage(message)

        // Create an array to hold the output
        val outputs = Array(1) { FloatArray(1) }

        // Run the model
        interpreter.run(preprocessedMessage, outputs)

        // Interpret the output
        val isSpam = interpretOutput(outputs[0][0])
        return isSpam
    }


    private fun preprocessMessage(message: String): Array<FloatArray> {
        // Remove punctuations
        val messageWithoutPunctuations = removePunctuations(message)
        Log.d("Preprocessing", "Message without punctuations: $messageWithoutPunctuations")

        // Remove stopwords
        val messageWithoutStopwords = removeStopwords(this, messageWithoutPunctuations)
        Log.d("Preprocessing", "Message without stopwords: $messageWithoutStopwords")

        // Tokenize the text
        val tokenizedMessage = tokenizeText(messageWithoutStopwords)
        Log.d("Preprocessing", "Tokenized message: $tokenizedMessage")

        // Pad the sequence
        val paddedSequence = padSequence(tokenizedMessage)
        Log.d("Preprocessing", "Padded sequence: $paddedSequence")

        // Convert to Array<FloatArray>
        val preprocessedMessage = convertToFloatArray(paddedSequence)
        return preprocessedMessage
    }


    private fun removePunctuations(message: String): String {
        // Remove punctuations
        return message.replace("\\p{Punct}".toRegex(), "")
    }

    fun loadStopwords(context: Context): Set<String> {
        val inputStream = context.assets.open("stopwords.txt")
        val stopwordsString = inputStream.bufferedReader().use { it.readText() }
        return stopwordsString.split("\n").toSet()
    }

    private fun removeStopwords(context: Context, message: String): String {
        // Define a list of stopwords
        val stopwords = loadStopwords(context)

        // Remove stopwords
        val words = message.split(" ")
        val importantWords = words.filter { it !in stopwords }
        return importantWords.joinToString(" ")
    }


    private fun tokenizeText(message: String): List<Int> {
        // Tokenize the text
        return tokenizer.textsToSequences(message)
    }

    private fun padSequence(tokenizedMessage: List<Int>): List<Int> {
        // Pad the sequence
        val maxLen = 100
        val paddedSequence = tokenizedMessage.take(maxLen) + List(maxLen - tokenizedMessage.size) { 0 }
        return paddedSequence
    }

    private fun convertToFloatArray(paddedSequence: List<Int>): Array<FloatArray> {
        // Convert to Array<FloatArray>
        return arrayOf(paddedSequence.map { it.toFloat() }.toFloatArray())
    }

    private fun interpretOutput(output: Float): Boolean {
        // Choose a threshold
        val threshold = 0.5f
        // Return whether the output is greater than the threshold
        return output > threshold
    }

    private fun addOrUpdateReportList(
        userEmail: String,
        reportedUserEmail: String,
        isSpam: Boolean
    ) {
        val db = Firebase.firestore
        val userReportDocumentRef = if (isSpam) {
            db.collection("ReportListSpam").document(userEmail)
        } else {
            db.collection("ReportList").document(userEmail)
        }

        // Check if the user's report document exists
        userReportDocumentRef.get()
            .addOnSuccessListener { userReportDocumentSnapshot ->
                if (userReportDocumentSnapshot.exists()) {
                    // User's report document exists
                    val reportedUsersCollectionRef = userReportDocumentRef.collection("reportedUsers")

                    // Check if the reported users collection exists
                    reportedUsersCollectionRef.get()
                        .addOnSuccessListener { reportedUsersCollectionSnapshot ->
                            if (reportedUsersCollectionSnapshot.isEmpty) {
                                // Reported users collection doesn't exist
                                // Create the reported users collection and add the reported user
                                reportedUsersCollectionRef.document(userEmail)
                                    .set(mapOf("reported" to true))
                                    .addOnSuccessListener {
                                        checkIfUserShouldBeBlocked(userEmail)
                                        // Report added successfully
                                    }
                                    .addOnFailureListener { exception ->
                                        Log.e(TAG, "Failed to add report: ${exception.message}")
                                    }
                            } else {
                                // Reported users collection exists
                                // Check if the reported user already exists
                                val existingReportedUserDocument = reportedUsersCollectionSnapshot.documents
                                    .find { it.id == reportedUserEmail }

                                if (existingReportedUserDocument == null) {
                                    Toast.makeText(this, "User reported", Toast.LENGTH_SHORT).show()
                                    // Reported user doesn't exist
                                    // Add the reported user to the collection
                                    reportedUsersCollectionRef.document(reportedUserEmail)
                                        .set(mapOf("reported" to true))
                                        .addOnSuccessListener {
                                            checkIfUserShouldBeBlocked(userEmail)
                                            // Report added successfully
                                            // Display success message or perform any other action
                                        }
                                        .addOnFailureListener { exception ->
                                            Log.e(TAG, "Failed to add report: ${exception.message}")
                                        }
                                } else {
                                    Toast.makeText(this, "User already reported", Toast.LENGTH_SHORT).show()
                                }
                            }
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Failed to add report: ${exception.message}")
                        }
                } else {
                    // User's report document doesn't exist
                    // Create the user's report document and reported users collection

                    // Create the reported users collection
                    val reportedUsersCollectionRef = userReportDocumentRef.collection("reportedUsers")

                    // Add the reported user to the collection
                    reportedUsersCollectionRef.document(reportedUserEmail)
                        .set(mapOf("reported" to true))
                        .addOnSuccessListener {
                            checkIfUserShouldBeBlocked(userEmail)
                            // Report added successfully
                        }
                        .addOnFailureListener { exception ->
                            Log.e(TAG, "Failed to add report: ${exception.message}")
                        }
                }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to add report: ${exception.message}")
            }
    }

    private fun checkIfUserShouldBeBlocked(reportedUserEmail: String){
        val reportListRef = com.google.firebase.ktx.Firebase.firestore.collection("ReportList").document(reportedUserEmail)
        val reportListSpamRef = com.google.firebase.ktx.Firebase.firestore.collection("ReportListSpam").document(reportedUserEmail)

        // Get the reported users count from "ReportList"
        reportListRef.collection("reportedUsers").get()
            .addOnSuccessListener { reportedUsersList ->
                val reportListCount = reportedUsersList.size()

                // Get the reported users count from "ReportListSpam"
                reportListSpamRef.collection("reportedUsers").get()
                    .addOnSuccessListener { reportedUsersSpamList ->
                        val reportListSpamCount = reportedUsersSpamList.size()

                        // Check if the total reports count is greater than or equal to 10
                        // and if the spam reports count is greater than or equal to 5
                        val isReported = reportListCount + reportListSpamCount >= 10 || reportListSpamCount >= 5

                        // Invoke the callback with the result
                        if (isReported){
                            addUserToBlockedUsers(reportedUserEmail, "Waiting")
                        }
                    }
                    .addOnFailureListener {
                        Log.e(TAG, "Failed to get reported users from ReportListSpam}")
                        // Assume not reported in case of error
                    }
            }
            .addOnFailureListener {
                Log.e(TAG, "Failed to get reported users from ReportListSpam}")
                // Assume not reported in case of error
            }
    }

    private fun addUserToBlockedUsers(reportedUserEmail: String, status: String) {
        val db = Firebase.firestore

        val blockedUserDocumentRef = db.collection("BlockedUsers").document(reportedUserEmail)

        val userData = hashMapOf(
            "email" to reportedUserEmail,
            "status" to status
        )

        // Setting the user's data in the document
        blockedUserDocumentRef.set(userData)
            .addOnSuccessListener {
                // User added to BlockedUsers collection successfully
                // Now adding the collections for blockedByUsers and keepByUsers
                blockedUserDocumentRef.collection("blockedByUsers").document().set(hashMapOf("students" to true))
                    .addOnSuccessListener {
                        Log.e(TAG, "Added to blockedByUsers collection")
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to add blockedByUsers collection: ${exception.message}")
                    }

                blockedUserDocumentRef.collection("keepByUsers").document().set(hashMapOf("students" to true))
                    .addOnSuccessListener {
                        Log.e(TAG, "Added to keepByUsers collection")
                    }
                    .addOnFailureListener { exception ->
                        Log.e(TAG, "Failed to add keepByUsers collection: ${exception.message}")
                    }
            }
            .addOnFailureListener { exception ->
                Log.e(TAG, "Failed to add user to BlockedUsers collection: ${exception.message}")
            }
    }



}