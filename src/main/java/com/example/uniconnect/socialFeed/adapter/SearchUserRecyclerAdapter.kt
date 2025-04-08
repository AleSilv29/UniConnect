package com.example.uniconnect.socialFeed.adapter

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.uniconnect.socialFeed.ui.ChatMessagesActivity
import com.example.uniconnect.socialFeed.model.UserModel
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.example.uniconnect.utils.FirebaseUtil
import com.example.uniconnect.utils.AndroidUtil
import com.google.android.gms.tasks.OnCompleteListener
import com.google.android.gms.tasks.Task


class SearchUserRecyclerAdapter(
    options: FirestoreRecyclerOptions<UserModel?>?,
    var context: Context
) :
    FirestoreRecyclerAdapter<UserModel, SearchUserRecyclerAdapter.UserModelViewHolder>(options!!) {
    @SuppressLint("SetTextI18n")
    override fun onBindViewHolder(
        holder: UserModelViewHolder,
        position: Int,
        model: UserModel
    ) {
        var yearGroupText = " "
        if (model.Group!=""){
            yearGroupText = "7${model.Year}${model.Group}"
        }
        holder.emailText.text = model.Email
        holder.groupText.text = yearGroupText
        val firebaseUtil = FirebaseUtil()
        val currentUserId = firebaseUtil.currentUserId()
        if (model.userId == currentUserId) {
            holder.emailText.text = model.Email + " (Me)"
        }

        FirebaseUtil().getOtherProfilePicStorageRef(model.Email).getDownloadUrl()
            .addOnCompleteListener(OnCompleteListener<Uri?> { t: Task<Uri?> ->
                if (t.isSuccessful) {
                    val uri = t.result
                    AndroidUtil().setProfilePic(context, uri, holder.profilePic)
                }
            })


        holder.itemView.setOnClickListener { v: View? ->
            // Navigate to chat activity
            val intent = Intent(context, ChatMessagesActivity::class.java)
            AndroidUtil().passUserModelAsIntent(intent, model)
            intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK
            context.startActivity(intent)
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): UserModelViewHolder {
        val view: View =
            LayoutInflater.from(context).inflate(com.example.uniconnect.R.layout.search_user_recycler_row, parent, false)
        return UserModelViewHolder(view)
    }

    inner class UserModelViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var emailText: TextView
        var groupText: TextView
        var profilePic: ImageView

        init {
            emailText = itemView.findViewById(com.example.uniconnect.R.id.user_name_text)
            groupText = itemView.findViewById(com.example.uniconnect.R.id.group_text)
            profilePic = itemView.findViewById(com.example.uniconnect.R.id.profile_pic_image_view)
        }
    }
}