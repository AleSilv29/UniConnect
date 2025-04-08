package com.example.uniconnect.utils

import android.annotation.SuppressLint
import android.util.Log
import com.example.uniconnect.socialFeed.model.UserModel
import com.google.firebase.Timestamp
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.CollectionReference
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import com.google.firebase.storage.FirebaseStorage
import com.google.firebase.storage.StorageReference
import java.text.SimpleDateFormat

class FirebaseUtil {
    fun currentUserId(): String? {
        return FirebaseAuth.getInstance().uid
    }


    fun currentUserDetails(): DocumentReference {
        Log.d("OtherUser123", "${currentUserId()}!!")
        return FirebaseFirestore.getInstance().collection("Users").document(currentUserId()!!)
    }

    fun allUserCollectionReference(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("Users")
    }


    fun getChatroomReference(chatroomId: String?): DocumentReference {
        return FirebaseFirestore.getInstance().collection("Chatrooms").document(chatroomId!!)
    }

    fun getChatroomMessageReference(chatroomId: String?): CollectionReference {
        return getChatroomReference(chatroomId).collection("Chats")
    }

    fun getChatroomId(userId1: String, userId2: String): String {
        return if (userId1.hashCode() < userId2.hashCode()) {
            userId1 + "_" + userId2
        } else {
            userId2 + "_" + userId1
        }
    }

    fun allChatroomCollectionReference(): CollectionReference {
        return FirebaseFirestore.getInstance().collection("Chatrooms")
    }

    fun getOtherUserFromChatroom(userIds: List<String>): DocumentReference {
        return if (userIds[0] == this.currentUserId()) {
            Log.d("RecentChat12", "$userIds[1]")
            allUserCollectionReference().document(userIds[1])
        } else {
            Log.d("RecentChat12", "$userIds[0]")
            allUserCollectionReference().document(userIds[0])
        }
    }

    @SuppressLint("SimpleDateFormat")
    fun timestampToString(timestamp: Timestamp): String? {
        return SimpleDateFormat("HH:MM").format(timestamp.toDate())
    }


    fun getCurrentProfilePicStorageRef(): StorageReference {
        Log.d("Profile1234", "${currentUserId()}")
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        Log.d("Profile12345", "$currentUserEmail")
        return FirebaseStorage.getInstance().reference.child("profile_pic")
            .child(currentUserEmail!!)
    }

    fun getOtherProfilePicStorageRef(otherUserEmail: String?): StorageReference {
        return FirebaseStorage.getInstance().reference.child("profile_pic")
            .child(otherUserEmail!!)
    }

    fun getUserByUserEmail(userEmail: String?, callback: (UserModel?) -> Unit) {
        // Query the Users collection to find the document with the matching email
        val query = allUserCollectionReference().whereEqualTo("Email", userEmail).limit(1)

        query.get().addOnCompleteListener { task ->
            if (task.isSuccessful) {
                val document = task.result?.documents?.firstOrNull()
                val userModel = document?.toObject(UserModel::class.java)
                callback(userModel)
            } else {
                // case where the query fails
                Log.e("FirebaseUtil", "Error getting user by email: ${task.exception}")
                callback(null)
            }
        }
    }

    fun getChatroomsForCurrentUser(): Query {
        val currentUserEmail = FirebaseAuth.getInstance().currentUser?.email
        return FirebaseFirestore.getInstance()
            .collection("Chatrooms")
            .whereArrayContains("userIds", currentUserEmail!!)
            .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
    }


}