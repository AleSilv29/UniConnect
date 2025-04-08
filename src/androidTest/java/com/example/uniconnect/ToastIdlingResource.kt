package com.example.uniconnect

import android.view.View
import androidx.test.espresso.IdlingResource
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class ToastIdlingResource(private val textMatcher: org.hamcrest.Matcher<View>, private val timeoutMillis: Long) : IdlingResource {

    private var resourceCallback: IdlingResource.ResourceCallback? = null
    private var isIdle = false

    override fun getName(): String {
        return ToastIdlingResource::class.java.simpleName
    }

    override fun isIdleNow(): Boolean {
        if (isIdle) {
            return true
        }

        val latch = CountDownLatch(1)
        val toastListener = object : ToastListener() {
            override fun onToastDisplayed() {
                latch.countDown()
            }
        }

        registerToastListener(toastListener)

        try {
            isIdle = latch.await(timeoutMillis, TimeUnit.MILLISECONDS)
        } finally {
            unregisterToastListener(toastListener)
        }

        if (isIdle) {
            resourceCallback?.onTransitionToIdle()
        }

        return isIdle
    }

    override fun registerIdleTransitionCallback(callback: IdlingResource.ResourceCallback?) {
        resourceCallback = callback
    }

    private fun registerToastListener(listener: ToastListener) {
        ToastMonitor.registerToastListener(listener)
    }

    private fun unregisterToastListener(listener: ToastListener) {
        ToastMonitor.unregisterToastListener(listener)
    }

    companion object {
        private object ToastMonitor : ToastListener() {
            private val listeners = mutableListOf<ToastListener>()

            fun registerToastListener(listener: ToastListener) {
                listeners.add(listener)
            }

            fun unregisterToastListener(listener: ToastListener) {
                listeners.remove(listener)
            }

            override fun onToastDisplayed() {
                super.onToastDisplayed()
                listeners.forEach { it.onToastDisplayed() }
            }
        }
    }

}
