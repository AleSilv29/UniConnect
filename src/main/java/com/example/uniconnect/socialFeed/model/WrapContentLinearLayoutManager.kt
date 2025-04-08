package com.example.uniconnect.socialFeed.model
import android.content.ContentValues.TAG
import android.content.Context
import android.util.Log
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.RecyclerView.Recycler

class WrapContentLinearLayoutManager(context: Context) : LinearLayoutManager(context) {

    override fun onLayoutChildren(recycler: Recycler?, state: RecyclerView.State?) {
        try {
            super.onLayoutChildren(recycler, state)
        } catch (e: IndexOutOfBoundsException) {
            Log.e(TAG, "Inconsistency detected")
        }
    }
}