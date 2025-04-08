package com.example.uniconnect.socialFeed.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.EditText
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.uniconnect.R
import com.example.uniconnect.socialFeed.adapter.SearchUserRecyclerAdapter
import com.example.uniconnect.socialFeed.model.UserModel
import com.example.uniconnect.utils.FirebaseUtil
import com.firebase.ui.firestore.FirestoreRecyclerOptions


class SearchUserActivity : AppCompatActivity() {

    var searchInput: EditText? = null
    var searchButton: ImageButton? = null
    var backButton: ImageButton? = null
    var recyclerView: RecyclerView? = null

    var adapter: SearchUserRecyclerAdapter? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_search_user)

        searchInput = findViewById(R.id.search_username_input)
        searchButton = findViewById(R.id.search_user_btn)
        backButton = findViewById(R.id.back_btn)
        recyclerView = findViewById(R.id.search_user_recycler_view)

        backButton?.setOnClickListener {
            val intent = Intent(this, ChatPageActivity::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
            finish()
        }

        searchButton?.setOnClickListener(View.OnClickListener { v: View? ->
            val searchTerm = searchInput?.getText().toString()
            if (searchTerm.isEmpty() || searchTerm.length < 3) {
                Toast.makeText(this, "Invalid Username", Toast.LENGTH_SHORT).show()
            }
            setupSearchRecyclerView(searchTerm)
        })
    }

    fun setupSearchRecyclerView(searchTerm: String) {
        val query: com.google.firebase.firestore.Query = FirebaseUtil().allUserCollectionReference()
            .whereGreaterThanOrEqualTo("Email", searchTerm)
            .whereLessThanOrEqualTo("Email", searchTerm + '\uf8ff')

        val options: FirestoreRecyclerOptions<UserModel?> =
            FirestoreRecyclerOptions.Builder<UserModel>()
                .setQuery(query, UserModel::class.java).build()

        adapter = SearchUserRecyclerAdapter(options, applicationContext)
        recyclerView!!.layoutManager = LinearLayoutManager(this)
        recyclerView!!.adapter = adapter
        adapter!!.startListening()

        query.get().addOnSuccessListener { documents ->
            for (document in documents) {
                Log.d("User", "${document.id} => ${document.data}")
            }
        }.addOnFailureListener { exception ->
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

    override fun onBackPressed() {
        super.onBackPressed()
        val intent = Intent(this, ChatPageActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(intent)
        finish()
    }

}