package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.service.CommandProcessor
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Telegram SMS", appName)
  }

  @Test
  fun `command processor handles ping`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val response = CommandProcessor.processCommand(context, "/ping")
    assertTrue(response.contains("Pong"))
  }

  @Test
  fun `command processor handles help`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val response = CommandProcessor.processCommand(context, "/help")
    assertTrue(response.contains("/send"))
    assertTrue(response.contains("/battery"))
  }

  @Test
  fun `command processor handles sim and info`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val simResponse = CommandProcessor.processCommand(context, "/sim")
    assertTrue(simResponse.contains("SIM"))

    val infoResponse = CommandProcessor.processCommand(context, "/info")
    assertTrue(infoResponse.contains("Device Information"))
  }
}
