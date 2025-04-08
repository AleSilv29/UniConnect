package com.example.uniconnect

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.assertion.ViewAssertions.doesNotExist
import androidx.test.espresso.assertion.ViewAssertions.matches
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.uniconnect.news.ui.NewsPageActivity
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith


@RunWith(AndroidJUnit4::class)
@LargeTest
class NewsActivityInstrumentedTest {
    private lateinit var scenario: ActivityScenario<NewsPageActivity>

    @Before
    fun setUp() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), NewsPageActivity::class.java)
        intent.putExtra("isTeacher", true)
        scenario = ActivityScenario.launch(intent)
    }
    @Test
    fun testOpenPopupDialog() {
        onView(withId(R.id.addNewsButton)).perform(click())

        onView(withId(R.id.editTextTitle)).perform(typeText("Course room change"), closeSoftKeyboard())
        onView(withId(R.id.editTextContent)).perform(typeText("The room \"Saturn\" will be unavailable for the whole month of June. The courses will take place in \"Journey\"."), closeSoftKeyboard())

        onView(withText("Add")).inRoot(RootMatchers.isDialog()).perform(click())

        val idlingResource = ToastIdlingResource(withText("News added successfully!"), 10000)
        IdlingRegistry.getInstance().register(idlingResource)

        // Wait for the toast message
        // Unregister the IdlingResource
        IdlingRegistry.getInstance().unregister(idlingResource)

    }

}