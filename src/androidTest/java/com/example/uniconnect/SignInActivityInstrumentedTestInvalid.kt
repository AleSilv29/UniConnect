package com.example.uniconnect

import androidx.test.core.app.ActivityScenario
import com.example.uniconnect.auth.ui.SignInActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.matcher.ViewMatchers.withText

@RunWith(AndroidJUnit4::class)
@LargeTest
class SignInActivityInstrumentedTestInvalid {

    @Before
    fun setUp() {
        ActivityScenario.launch(SignInActivity::class.java)
    }


    @Test
    fun testSignInWithEmailAndPassword_Failure() {
        ActivityScenario.launch(SignInActivity::class.java)

        onView(withId(R.id.username)).perform(typeText("invalid@stud.ubbcluj.com"), closeSoftKeyboard())
        onView(withId(R.id.password)).perform(typeText("invalidpassword"), closeSoftKeyboard())

        onView(withId(R.id.loginAccountButton)).perform(click())

        val idlingResource = ToastIdlingResource(withText("Authentication failed"), 10000)
        IdlingRegistry.getInstance().register(idlingResource)

        // Wait for the toast message
        // Unregister the IdlingResource
        IdlingRegistry.getInstance().unregister(idlingResource)
    }

}