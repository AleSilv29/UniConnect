package com.example.uniconnect

import androidx.test.core.app.ActivityScenario
import com.example.uniconnect.auth.ui.SignInActivity
import androidx.test.espresso.Espresso.onView
import androidx.test.espresso.action.ViewActions.*
import androidx.test.espresso.matcher.ViewMatchers.*
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import com.google.firebase.auth.FirebaseAuth
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import androidx.test.espresso.IdlingRegistry
import com.google.android.gms.tasks.Tasks
import com.google.firebase.auth.AuthResult
import org.mockito.Mock
import org.mockito.Mockito.`when`
import androidx.test.espresso.matcher.ViewMatchers.withText
import org.mockito.MockitoAnnotations

@RunWith(AndroidJUnit4::class)
@LargeTest
class SignInActivityInstrumentedTest {

    private lateinit var firebaseAuth: FirebaseAuth

    @Mock
    private lateinit var mockedAuth: FirebaseAuth

    @Before
    fun setUp() {
        MockitoAnnotations.initMocks(this)
        firebaseAuth = FirebaseAuth.getInstance()
        ActivityScenario.launch(SignInActivity::class.java)
    }

    @Test
    fun testSignInWithValidCredentials() {
        val email = "student711@stud.ubbcluj.ro"
        val password = "123456"

        onView(withId(R.id.username)).perform(typeText(email), closeSoftKeyboard())
        onView(withId(R.id.password)).perform(typeText(password), closeSoftKeyboard())

        val expectedResult = Tasks.forResult<AuthResult>(null)

        `when`(mockedAuth.signInWithEmailAndPassword(email, password)).thenReturn(expectedResult)

        onView(withId(R.id.loginAccountButton)).perform(click())


        // Register an IdlingResource to wait for the "Login successful" text to appear
        val idlingResource = ToastIdlingResource(withText("Login successful"), 10000)
        IdlingRegistry.getInstance().register(idlingResource)

        // Unregister the IdlingResource
        IdlingRegistry.getInstance().unregister(idlingResource)
    }

}

