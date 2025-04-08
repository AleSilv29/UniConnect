package com.example.uniconnect

import android.content.Intent
import android.view.View
import android.widget.RadioButton
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.IdlingRegistry
import androidx.test.espresso.UiController
import androidx.test.espresso.ViewAction
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.RootMatchers
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.example.uniconnect.news.ui.ImpNewsPageActivity
import org.hamcrest.Matchers.allOf
import org.hamcrest.Matchers.not
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import java.util.regex.Matcher

@RunWith(AndroidJUnit4::class)
@LargeTest
class ImportantNewsInstrumentedTest {
    private lateinit var scenario: ActivityScenario<ImpNewsPageActivity>

    @Before
    fun setUp() {
        val intent = Intent(ApplicationProvider.getApplicationContext(), ImpNewsPageActivity::class.java)
        intent.putExtra("isTeacher", true)
        scenario = ActivityScenario.launch(intent)
    }

    @Test
    fun testOpenPopupDialog() {
        onView(withId(R.id.addImpNewsButton)).perform(click())

        onView(withId(R.id.editTextTitle)).perform(typeText("Room change"), closeSoftKeyboard())
        onView(withId(R.id.editTextContent)).perform(typeText("The room \"Saturn\" will be unavailable today. The courses will take place in \"Journey\"."), closeSoftKeyboard())

        onView(withId(R.id.year1RadioButton)).perform(click())

        onView(withId(R.id.group1RadioButton)).perform(click())

        onView(withId(R.id.semigroup1RadioButton)).perform(click())

        onView(withText("Add")).inRoot(RootMatchers.isDialog()).perform(click())

        val idlingResource = ToastIdlingResource(withText("News added successfully!"), 10000)
        IdlingRegistry.getInstance().register(idlingResource)

        // Wait for the toast message
        // Unregister the IdlingResource
        IdlingRegistry.getInstance().unregister(idlingResource)

    }

    // Extension function to perform a click on a radio button with specific text
    fun clickOnRadioButtonWithText(text: String): ViewAction {
        return object : ViewAction {
            override fun getConstraints(): org.hamcrest.Matcher<View>? {
                return allOf(isDisplayed(), isAssignableFrom(RadioButton::class.java))
            }

            override fun getDescription(): String {
                return "Click on RadioButton with text $text"
            }

            override fun perform(uiController: UiController?, view: View) {
                (view as RadioButton).let {
                    if (it.text.toString() == text) {
                        it.performClick()
                    }
                }
            }
        }
    }
}