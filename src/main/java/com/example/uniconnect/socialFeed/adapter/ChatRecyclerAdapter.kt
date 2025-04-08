package com.example.uniconnect.socialFeed.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.example.uniconnect.socialFeed.model.ChatMessageModel
import com.example.uniconnect.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerAdapter
import com.firebase.ui.firestore.FirestoreRecyclerOptions


class ChatRecyclerAdapter(
    options: FirestoreRecyclerOptions<ChatMessageModel?>?,
    var context: Context
) :
    FirestoreRecyclerAdapter<ChatMessageModel, ChatRecyclerAdapter.ChatModelViewHolder>(options!!) {
    override fun onBindViewHolder(
        holder: ChatModelViewHolder,
        position: Int,
        model: ChatMessageModel
    ) {
        Log.i("ChatRecyclerAdapter123", "asjd")
        if (model.senderId == FirebaseUtil().currentUserId()) {
            holder.leftChatLayout.visibility = View.GONE
            holder.rightChatLayout.visibility = View.VISIBLE
            holder.rightChatTextview.text = model.message
        } else {
            holder.rightChatLayout.visibility = View.GONE
            holder.leftChatLayout.visibility = View.VISIBLE
            holder.leftChatTextview.text = model.message
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): ChatModelViewHolder {
        val view: View =
            LayoutInflater.from(context).inflate(com.example.uniconnect.R.layout.chat_message_recycler_row, parent, false)
        return ChatModelViewHolder(view)
    }

    inner class ChatModelViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var leftChatLayout: LinearLayout
        var rightChatLayout: LinearLayout
        var leftChatTextview: TextView
        var rightChatTextview: TextView

        init {
            leftChatLayout = itemView.findViewById<LinearLayout>(com.example.uniconnect.R.id.left_chat_layout)
            rightChatLayout = itemView.findViewById<LinearLayout>(com.example.uniconnect.R.id.right_chat_layout)
            leftChatTextview = itemView.findViewById<TextView>(com.example.uniconnect.R.id.left_chat_textview)
            rightChatTextview = itemView.findViewById<TextView>(com.example.uniconnect.R.id.right_chat_textview)
        }
    }
}