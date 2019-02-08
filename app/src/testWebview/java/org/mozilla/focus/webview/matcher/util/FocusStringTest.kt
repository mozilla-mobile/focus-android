package org.mozilla.focus.webview.matcher.util

import org.junit.Test
import org.junit.Assert.assertEquals

class FocusStringTest {
    @Test(expected = StringIndexOutOfBoundsException::class)
    @Throws(StringIndexOutOfBoundsException::class)
    fun outOfBounds() {
        val fullStringRaw = "a"
        val fullString = FocusString.create(fullStringRaw)
        // Is beyond the raw input string
        fullString.charAt(1)
    }

    @Test(expected = StringIndexOutOfBoundsException::class)
    @Throws(StringIndexOutOfBoundsException::class)
    fun outofBoundsAfterSubstring() {
        val fullStringRaw = "abcd"
        val fullString = FocusString.create(fullStringRaw)
        val substring = fullString.substring(3)
        // substring == "d"
        substring.charAt(1)
    }

    @Test(expected = StringIndexOutOfBoundsException::class)
    @Throws(StringIndexOutOfBoundsException::class)
    fun outofBoundsSubstringLarge() {
        val fullStringRaw = "abcd"
        val fullString = FocusString.create(fullStringRaw)
        val substring = fullString.substring(5)
    }

    @Test(expected = StringIndexOutOfBoundsException::class)
    @Throws(StringIndexOutOfBoundsException::class)
    fun outofBoundsSubstringNegative() {
        val fullStringRaw = "abcd"
        val fullString = FocusString.create(fullStringRaw)
        val substring = fullString.substring(-1)
    }

    @Test
    fun testSubstringLength() {
        val fullStringRaw = "a"
        val fullString = FocusString.create(fullStringRaw)

        assertEquals(
            "FocusString length must match input string length",
            fullStringRaw.length(), fullString.length()
        )
        val sameString = fullString.substring(0)
        assertEquals(
            "substring(0) should equal input String",
            fullStringRaw.length(), sameString.length()
        )
        assertEquals(
            "substring(0) should equal input String",
            fullStringRaw.charAt(0), sameString.charAt(0)
        )
        val emptyString = fullString.substring(1)
        assertEquals(
            "empty substring should be empty",
            0, emptyString.length()
        )
    }

    @Test(expected = StringIndexOutOfBoundsException::class)
    @Throws(StringIndexOutOfBoundsException::class)
    fun outofBoundsAfterSubstringEmpty() {
        val fullStringRaw = "abcd"
        val fullString = FocusString.create(fullStringRaw)
        val substring = fullString.substring(4)
        // substring == ""
        substring.charAt(0)
    }

    @Test
    fun testForwardString() {
        val fullStringRaw = "abcd"
        val fullString = FocusString.create(fullStringRaw)

        assertEquals(
            "FocusString length must match input string length",
            fullStringRaw.length(), fullString.length()
        )

        for (i in 0 until fullStringRaw.length()) {
            assertEquals(
                "FocusString character doesn't match input string character",
                fullStringRaw.charAt(i), fullString.charAt(i)
            )
        }
        val substringRaw = fullStringRaw.substring(2)
        val substring = fullString.substring(2)

        for (i in 0 until substringRaw.length()) {
            assertEquals(
                "FocusString character doesn't match input string character",
                substringRaw.charAt(i), substring.charAt(i)
            )
        }
    }

    @Test
    fun testReverseString() {
        val fullUnreversedStringRaw = "abcd"
        val fullStringRaw = StringBuffer(fullUnreversedStringRaw).reverse().toString()
        val fullString = FocusString.create(fullUnreversedStringRaw).reverse()

        assertEquals(
            "FocusString length must match input string length",
            fullStringRaw.length(), fullString.length()
        )

        for (i in 0 until fullStringRaw.length()) {
            assertEquals(
                "FocusString character doesn't match input string character",
                fullStringRaw.charAt(i), fullString.charAt(i)
            )
        }
        val substringRaw = fullStringRaw.substring(2)
        val substring = fullString.substring(2)

        for (i in 0 until substringRaw.length()) {
            assertEquals(
                "FocusString character doesn't match input string character",
                substringRaw.charAt(i), substring.charAt(i)
            )
        }
    }
}