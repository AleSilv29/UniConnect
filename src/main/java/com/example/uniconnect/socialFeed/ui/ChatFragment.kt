package com.example.uniconnect.socialFeed.ui

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uniconnect.R
import com.example.uniconnect.socialFeed.adapter.RecentChatRecyclerAdapter
import com.example.uniconnect.socialFeed.model.ChatroomModel
import com.example.uniconnect.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.Query

class ChatFragment : Fragment() {
    var recyclerView: RecyclerView? = null
    var adapter: RecentChatRecyclerAdapter? = null
    var userEmail: String? = null

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.fragment_chat, container, false)
        recyclerView = view.findViewById<RecyclerView>(R.id.recyler_view)
        setupRecyclerView()

        return view
    }

    fun setupRecyclerView() {
        val user = FirebaseAuth.getInstance().currentUser
        if (user != null) {
            userEmail = user.email
        }
        val query: Query? = userEmail?.let {
            FirebaseUtil().allChatroomCollectionReference()
                .whereArrayContains("userIds", it)
                .orderBy("lastMessageTimestamp", Query.Direction.DESCENDING)
        }

        query?.let {
            FirestoreRecyclerOptions.Builder<ChatroomModel>()
                .setQuery(it, ChatroomModel::class.java).build()
        }


        context?.let {
            adapter = RecentChatRecyclerAdapter(it)
            recyclerView?.adapter = adapter
        }
        recyclerView!!.layoutManager = LinearLayoutManager(context)
        recyclerView!!.adapter = adapter
        adapter!!.startListening()

        query?.get()?.addOnSuccessListener { documents ->
            for (document in documents) {
                Log.d("User", "${document.id} => ${document.data}")
            }
        }?.addOnFailureListener { exception ->
            Log.e("User", "Error fetching data: $exception")
        }
    }
    override fun onStart() {
        super.onStart()
        if (adapter != null) adapter!!.startListening()
    }

    override fun onStop() {
        super.onStop()
        if (adapter != null) adapter!!.stopListening()
    }

    override fun onResume() {
        super.onResume()
        if (adapter != null) adapter!!.startListening()
    }


}
