package com.example.uniconnect.socialFeed.adapter

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.uniconnect.socialFeed.ui.ChatMessagesActivity
import com.example.uniconnect.socialFeed.model.ChatroomModel
import com.example.uniconnect.socialFeed.model.UserModel
import com.example.uniconnect.utils.AndroidUtil
import com.example.uniconnect.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.android.gms.tasks.Task
import com.google.firebase.auth.FirebaseAuth


class RecentChatRecyclerAdapter(
    private val context: Context
) : FirestoreRecyclerAdapter<ChatroomModel, RecentChatRecyclerAdapter.ChatroomModelViewHolder>(
    FirestoreRecyclerOptions.Builder<ChatroomModel>()
        .setQuery(FirebaseUtil().getChatroomsForCurrentUser(), ChatroomModel::class.java)
        .build()
) {
    private val glide = Glide.with(context)
    private val firebaseUtil = FirebaseUtil()

    override fun onBindViewHolder(
        holder: ChatroomModelViewHolder,
        position: Int,
        model: ChatroomModel
    ) {
        val userEmail = FirebaseAuth.getInstance().currentUser?.email

        model.userIds?.let { userIds ->
            val otherUserId = if (userEmail == userIds[0]) userIds[1] else userIds[0]
            holder.usernameText.text = otherUserId // Assign usernameText here

            firebaseUtil.getOtherUserFromChatroom(userIds).get().addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val lastMessageSentByMe = model.lastMessageSenderId == firebaseUtil.currentUserId()

                    firebaseUtil.getOtherProfilePicStorageRef(otherUserId).getDownloadUrl()
                        .addOnCompleteListener { t: Task<Uri?> ->
                            if (t.isSuccessful) {
                                val uri = t.result
                                if (!(context as Activity).isFinishing) {
                                    glide.load(uri)
                                        .apply(RequestOptions.circleCropTransform())
                                        .into(holder.profilePic)
                                    AndroidUtil().setProfilePic(context, uri, holder.profilePic)
                                }
                            }
                        }

                    holder.lastMessageText.text = if (lastMessageSentByMe) "You: ${model.lastMessage}" else model.lastMessage
                    holder.lastMessageTime.text = model.lastMessageTimestamp?.let { firebaseUtil.timestampToString(it) }

                    holder.itemView.setOnClickListener {
                        firebaseUtil.allUserCollectionReference()
                            .whereEqualTo("Email", otherUserId)
                            .get()
                            .addOnSuccessListener { documents ->
                                if (!documents.isEmpty) {
                                    val otherUserModel = documents.first().toObject(UserModel::class.java)
                                    val intent = Intent(context, ChatMessagesActivity::class.java)
                                    AndroidUtil().passUserModelAsIntent(intent, otherUserModel)
                                    intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
                                    context.startActivity(intent)
                                } else {
                                    Log.e("Chat", "User not found with email: $otherUserId")
                                }
                            }
                            .addOnFailureListener { exception ->
                                Log.e("Chat", "Error getting user with email $otherUserId: $exception")
                            }
                    }
                }
            }
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChatroomModelViewHolder {
        val view = LayoutInflater.from(parent.context).inflate(com.example.uniconnect.R.layout.recent_chat_recycler_row, parent, false)
        return ChatroomModelViewHolder(view)
    }

    inner class ChatroomModelViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        var usernameText: TextView = itemView.findViewById(com.example.uniconnect.R.id.user_name_text)
        var lastMessageText: TextView = itemView.findViewById(com.example.uniconnect.R.id.last_message_text)
        var lastMessageTime: TextView = itemView.findViewById(com.example.uniconnect.R.id.last_message_time_text)
        var profilePic: ImageView = itemView.findViewById(com.example.uniconnect.R.id.profile_pic_image_view)
    }
}





