package com.example.uniconnect.utils

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import com.bumptech.glide.Glide
import com.bumptech.glide.request.RequestOptions
import com.example.uniconnect.socialFeed.model.UserModel


class AndroidUtil {
    fun showToast(context: Context?, message: String?) {
        Toast.makeText(context, message, Toast.LENGTH_LONG).show()
    }

    fun passUserModelAsIntent(intent: Intent, model: UserModel) {
        intent.putExtra("Email", model.Email)
        intent.putExtra("Group", model.Group)
        intent.putExtra("Year", model.Year)
        intent.putExtra("userId", model.userId)
        intent.putExtra("fcmToken", model.fcmToken)
    }


    fun getUserModelFromIntent(intent: Intent): UserModel {
        val userModel = UserModel()
        userModel.Email = intent.getStringExtra("Email")
        userModel.Group = intent.getStringExtra("Group")
        userModel.Year = intent.getStringExtra("Year")
        userModel.userId = intent.getStringExtra("userId")
        userModel.fcmToken = intent.getStringExtra("fcmToken")
        return userModel
    }

    fun setProfilePic(context: Context?, imageUri: Uri?, imageView: ImageView?) {
        Glide.with(context!!).load(imageUri).apply(RequestOptions.circleCropTransform())
            .into(imageView!!)
    }

    fun validateTimeFormat(timeString: String): Boolean {
        val regex = Regex("""^\d{1,2}:\d{2}-\d{1,2}:\d{2}$""")
        return regex.matches(timeString)
    }

    fun formatHours(hours: String): String {
        val parts = hours.split("-")
        val startHourParts = parts[0].split(":")
        val endHourParts = parts[1].split(":")

        val startHour = startHourParts[0].toIntOrNull()?.toString() ?: startHourParts[0]
        val endHour = endHourParts[0].toIntOrNull()?.toString() ?: endHourParts[0]

        return "$startHour:${startHourParts[1]}-$endHour:${endHourParts[1]}"
    }

    fun extractFullNameFromEmail(email: String?): String {
        return email?.substringBefore('@')?.split('.')?.joinToString(" ") { it.capitalize() } ?: ""
    }

    fun sortContainersByHours(containers: List<LinearLayout>) {
        containers.forEach { container ->
            val childViews = mutableListOf<View>()
            for (i in 0 until container.childCount) {
                childViews.add(container.getChildAt(i))
            }
            childViews.sortedWith(compareBy(
                { view -> // Sort by start hour
                    if (view is TextView) {
                        val text = view.text.toString()
                        val startTime = text.substringBefore("-").trim().split(":").firstOrNull()?.toIntOrNull()
                        startTime ?: Int.MAX_VALUE
                    } else {
                        Int.MAX_VALUE
                    }
                },
                { view -> // Then sort by end hour
                    if (view is TextView) {
                        val text = view.text.toString()
                        val endTime = text.substringAfter("-").trim().split(":").firstOrNull()?.toIntOrNull()
                        endTime ?: Int.MAX_VALUE
                    } else {
                        Int.MAX_VALUE
                    }
                }
            )).forEach { sortedView ->
                container.removeView(sortedView)
                container.addView(sortedView)
            }
        }
    }

    fun updateContainerHeight(container: LinearLayout) {
        val params = container.layoutParams as ViewGroup.LayoutParams
        params.height = ViewGroup.LayoutParams.WRAP_CONTENT // Set to WRAP_CONTENT for dynamic height
        container.layoutParams = params
    }
}