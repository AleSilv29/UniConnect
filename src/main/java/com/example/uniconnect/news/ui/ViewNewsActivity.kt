package com.example.uniconnect.news.ui

import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import androidx.activity.ComponentActivity
import com.example.uniconnect.R


class ViewNewsActivity : ComponentActivity() {

    lateinit var backButton: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.view_news_layout)

        val title = intent.getStringExtra("EXTRA_TITLE")
        val content = intent.getStringExtra("EXTRA_CONTENT")

        val textViewTitleDetails = findViewById<TextView>(R.id.textViewTitle)
        val textViewContentDetails = findViewById<TextView>(R.id.textViewContent)

        textViewTitleDetails.text = title
        textViewContentDetails.text = content

        backButton = findViewById(R.id.buttonBack)

        backButton.setOnClickListener {
            finish()
        }
    }
}