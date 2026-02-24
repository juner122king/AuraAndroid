package com.aura.football.util

import org.junit.Assert.assertEquals
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneId

class DateTimeUtilsTest {

    @Test
    fun `parseDateTime handles Z suffix UTC format`() {
        val result = parseDateTime("2026-02-06T19:00:00Z")
        val expected = LocalDateTime.of(2026, 2, 6, 19, 0, 0)
            .atZone(ZoneId.of("UTC"))
            .withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
        assertEquals(expected, result)
    }

    @Test
    fun `parseDateTime handles offset format`() {
        val result = parseDateTime("2026-02-06T19:00:00+00:00")
        val expected = LocalDateTime.of(2026, 2, 6, 19, 0, 0)
            .atZone(ZoneId.of("UTC"))
            .withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
        assertEquals(expected, result)
    }

    @Test
    fun `parseDateTime handles no timezone format as UTC`() {
        val result = parseDateTime("2026-02-06T19:00:00")
        val expected = LocalDateTime.of(2026, 2, 6, 19, 0, 0)
            .atZone(ZoneId.of("UTC"))
            .withZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
        assertEquals(expected, result)
    }

    @Test
    fun `parseDateTime returns current time for invalid input`() {
        val before = LocalDateTime.now().minusSeconds(1)
        val result = parseDateTime("not-a-date")
        val after = LocalDateTime.now().plusSeconds(1)
        assert(result.isAfter(before) && result.isBefore(after))
    }
}
