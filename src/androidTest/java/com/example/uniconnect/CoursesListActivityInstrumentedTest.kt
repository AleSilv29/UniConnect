package com.example.uniconnect

import android.content.Intent
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.uniconnect.classbook.ui.CoursesListActivity

@RunWith(AndroidJUnit4::class)
@LargeTest
class CoursesListActivityInstrumentedTest {
    private lateinit var scenario: ActivityScenario<CoursesListActivity>

    @Before
    fun setUp() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), CoursesListActivity::class.java)
        intent.putExtra("isTeacher", true)
        scenario = ActivityScenario.launch(intent)
    }
    @Test
    fun testOpenPopupDialog() {
        onView(withId(R.id.addCourseButton)).perform(click())

        onView(withId(R.id.courseNameEditText)).perform(typeText("Test Course"), closeSoftKeyboard())
        onView(withId(R.id.hoursEditText)).perform(typeText("8:0-9:00"), closeSoftKeyboard())
        onView(withId(R.id.groupEditText)).perform(typeText("711"), closeSoftKeyboard())
        onView(withId(R.id.roomEditText)).perform(typeText("Socrate"), closeSoftKeyboard())

        onView(withId(R.id.daySpinner)).perform(click())
        onView(withText("Tuesday")).inRoot(RootMatchers.isPlatformPopup()).perform(click())

        onView(withId(R.id.subgroupSpinner)).perform(click())
        onView(withText("1")).inRoot(RootMatchers.isPlatformPopup()).perform(click())

        onView(withId(R.id.typeSpinner)).perform(click())
        onView(withText("Seminar")).inRoot(RootMatchers.isPlatformPopup()).perform(click())

        onView(withId(R.id.weekSpinner)).perform(click())
        onView(withText("Week 1")).inRoot(RootMatchers.isPlatformPopup()).perform(click())

        onView(withId(R.id.teacherSpinner)).perform(click())
        onView(withText("Teacher")).inRoot(RootMatchers.isPlatformPopup()).perform(click())

        onView(withId(R.id.addButton)).perform(click())

        val idlingResource = ToastIdlingResource(withText("Please enter the time in the format e.g., 7:00-8:00"), 10000)
        IdlingRegistry.getInstance().register(idlingResource)

        // Wait for the toast message
        // Unregister the IdlingResource
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}