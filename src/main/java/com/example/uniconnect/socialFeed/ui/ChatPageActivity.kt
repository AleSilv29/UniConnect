package com.example.uniconnect.socialFeed.ui

import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.ImageButton
import androidx.appcompat.app.AppCompatActivity
import com.example.uniconnect.R
import com.example.uniconnect.utils.FirebaseUtil
import com.google.android.gms.tasks.Task
import com.google.android.material.bottomnavigation.BottomNavigationView
import com.google.android.material.navigation.NavigationBarView
import com.google.firebase.messaging.FirebaseMessaging


class ChatPageActivity : AppCompatActivity() {

    var bottomNavigationView: BottomNavigationView? = null
    var searchButton: ImageButton? = null
    var backBtn: ImageButton? = null

    var chatFragment: ChatFragment? = null
    var profileFragment: ProfileFragment? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatFragment = ChatFragment()
        profileFragment = ProfileFragment()

        bottomNavigationView = findViewById(R.id.bottom_navigation);
        searchButton = findViewById(R.id.chat_search_btn)
        backBtn = findViewById(R.id.back_btn)
        backBtn?.setOnClickListener(View.OnClickListener { v: View? -> onBackPressed() })

        searchButton?.setOnClickListener(View.OnClickListener { v: View? ->
            startActivity(
                Intent(
                    this@ChatPageActivity,
                    SearchUserActivity::class.java
                )
            )
        })

        bottomNavigationView?.setOnItemSelectedListener(NavigationBarView.OnItemSelectedListener { item ->
        if (item.itemId == R.id.menu_chat){
            getSupportFragmentManager().beginTransaction().replace(
                R.id.chat_frame_layout,
                chatFragment!!
            ).commit()
        }
            if (item.itemId == R.id.menu_profile){
                getSupportFragmentManager().beginTransaction().replace(
                    R.id.chat_frame_layout,
                    profileFragment!!
                ).commit()
            }
            true
        })
        bottomNavigationView?.setSelectedItemId(R.id.menu_chat)

        getFCMToken()

    }

    fun getFCMToken() {
        FirebaseMessaging.getInstance().token.addOnCompleteListener { task: Task<String?> ->
            if (task.isSuccessful) {
                val token = task.result
                FirebaseUtil().currentUserDetails().update("fcmToken",token)
                    .addOnSuccessListener {
                        Log.d("FCMTokenUpdate", "FCM token updated successfully")
                    }
                    .addOnFailureListener { e ->
                        Log.e("FCMTokenUpdate", "Error updating FCM token: $e")
                    }
            } else {
                Log.e("FCMToken", "Fetching FCM token failed")
            }
        }
    }
}