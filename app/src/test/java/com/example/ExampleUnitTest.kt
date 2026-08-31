package com.example

import com.example.data.network.TelegramApiClient
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {
  @Test
  fun addition_isCorrect() {
    assertEquals(4, 2 + 2)
  }

  @Test
  fun testParseChatIds_single() {
    val result = TelegramApiClient.parseChatIds("123456789")
    assertEquals(listOf("123456789"), result)
  }

  @Test
  fun testParseChatIds_commaSeparated() {
    val result = TelegramApiClient.parseChatIds("123456789, -100987654321, 55443322")
    assertEquals(listOf("123456789", "-100987654321", "55443322"), result)
  }

  @Test
  fun testParseChatIds_mixedSeparatorsAndDeduplication() {
    val result = TelegramApiClient.parseChatIds("12345; 67890\n-100112233 12345,  99999  ")
    assertEquals(listOf("12345", "67890", "-100112233", "99999"), result)
  }

  @Test
  fun testParseChatIds_empty() {
    val result = TelegramApiClient.parseChatIds("   , ; \n  ")
    assertTrue(result.isEmpty())
  }
}

