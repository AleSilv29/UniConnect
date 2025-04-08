package com.example.uniconnect

import androidx.test.core.app.ActivityScenario
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.uniconnect.timetable.ui.TimetableActivity

@RunWith(AndroidJUnit4::class)
@LargeTest
class TimetableActivityInstrumentedTest {

    private lateinit var scenario: ActivityScenario<TimetableActivity>

    @Before
    fun setUp() {
        scenario = ActivityScenario.launch(TimetableActivity::class.java)
    }

    @Test
    fun testOpenPopupDialog() {
        onView(withId(R.id.buttonOpenPopup)).perform(click())

        onView(withId(R.id.editTextPopup)).perform(typeText("Test Schedule"), closeSoftKeyboard())
        onView(withId(R.id.secondEditTextPopup)).perform(typeText("8:00-9:00"), closeSoftKeyboard())

        onView(withText("Add")).perform(click())

        // Register the IdlingResource
        val idlingResource = ToastIdlingResource(withText("Schedule entry added successfully"), 10000)
        IdlingRegistry.getInstance().register(idlingResource)

        // Wait for the toast message
        // Unregister the IdlingResource
        IdlingRegistry.getInstance().unregister(idlingResource)
    }
}
