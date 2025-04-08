package com.example.uniconnect.news.model

import android.content.ContentValues.TAG
import android.util.Log
import com.google.firebase.Firebase
import com.google.firebase.firestore.firestore

class NewsItem(var title: String, var content: String, var currentDate: String, var likes: MutableList<String> = mutableListOf(),
               var dislikes: MutableList<String> = mutableListOf()) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false

        other as NewsItem

        if (title != other.title) return false
        if (content != other.content) return false
        if (currentDate != other.currentDate) return false

        return true
    }

    override fun hashCode(): Int {
        var result = title.hashCode()
        result = 31 * result + content.hashCode()
        result = 31 * result + currentDate.hashCode()
        return result
    }

    fun likeNewsItem(newsItemId: String, userEmail: String) {
        if (dislikes.contains(userEmail)) {
            dislikes.remove(userEmail)
        }

        if (likes.contains(userEmail)) {
            likes.remove(userEmail)
        } else {
            likes.add(userEmail)
        }
        updateLikesInFirestore(newsItemId, userEmail)
    }

    fun dislikeNewsItem(newsItemId: String, userEmail: String) {
        if (likes.contains(userEmail)) {
            likes.remove(userEmail)
        }
        if (dislikes.contains(userEmail)) {
            dislikes.remove(userEmail)
        } else {
            dislikes.add(userEmail)
        }
        updateDislikesInFirestore(newsItemId, userEmail)
    }

    private fun updateLikesInFirestore(newsItemId: String, userEmail: String) {
        val newsDocument = Firebase.firestore.collection("News").document(newsItemId)
        newsDocument.get().addOnSuccessListener { documentSnapshot ->
            val currentLikes = documentSnapshot["Likes"] as? MutableList<String> ?: mutableListOf()
            if (!currentLikes.contains(userEmail)) {
                currentLikes.add(userEmail)
                newsDocument.update("Likes", currentLikes)
                    .addOnSuccessListener {
                        Log.d("NewsItem1", "Likes updated successfully in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.w("NewsItem1", "Error updating likes in Firestore", e)
                    }
            }
        }
    }

    private fun updateDislikesInFirestore(newsItemId: String, userEmail: String) {
        val newsDocument = Firebase.firestore.collection("News").document(newsItemId)
        newsDocument.get().addOnSuccessListener { documentSnapshot ->
            val currentDislikes = documentSnapshot["Dislikes"] as? MutableList<String> ?: mutableListOf()
            if (!currentDislikes.contains(userEmail)) {
                currentDislikes.add(userEmail)
                newsDocument.update("Dislikes", currentDislikes)
                    .addOnSuccessListener {
                        Log.d(TAG, "Dislikes updated successfully in Firestore")
                    }
                    .addOnFailureListener { e ->
                        Log.w(TAG, "Error updating dislikes in Firestore", e)
                    }
            }
        }
    }

}