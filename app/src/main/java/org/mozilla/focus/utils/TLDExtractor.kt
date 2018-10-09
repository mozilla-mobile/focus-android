/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.utils

import android.content.Context
import kotlinx.coroutines.experimental.runBlocking
import java.io.BufferedReader
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.util.ArrayList
import java.util.Collections
import java.util.Comparator
import java.util.regex.Pattern

/**
 * Finds the Top Level (or Secondary Level) Domain using a public list of TLD domains: http://publicsuffix.org/list/
 * Initialize as soon as possible in application.
 * Based on:
 *  http://www.supermind.org/blog/1078/extracting-second-level-domains-and-top-level-domains-tld-from-a-url-in-java
 **/
object TLDExtractor {
    private val stringBuilder: StringBuilder = StringBuilder()
    private var pattern: Pattern? = null

    @JvmStatic
    fun init(context: Context) {
        try {
            val terms = ArrayList<String>()

            val assetManager = context.assets
            val inputStream: InputStream = assetManager.open("effective_tld_names.dat")
            val inputStreamReader = InputStreamReader(inputStream)
            val bufferedReader = BufferedReader(inputStreamReader as Reader)

            runBlocking {
                while (true) {
                    var s = bufferedReader.readLine() ?: break
                    runBlocking {
                        s = s.trim { it <= ' ' }
                        if (!s.isEmpty() && !s.startsWith("//") && !s.startsWith("!")) {
                            terms.add(s)
                        }
                    }
                }
            }
            Collections.sort(terms, StringLengthComparator)
            for (t in terms) add(t)
            compile()
            bufferedReader.close()
            inputStream.close()
            inputStreamReader.close()
        } catch (e: IOException) {
            throw IllegalStateException(e)
        }
    }

    private fun add(s: String) {
        var newString = s
        newString = newString.replace(".", "\\.")
        newString = "\\.$newString"
        if (newString.startsWith("*")) {
            newString = newString.replace("*", ".+")
            stringBuilder.append(newString).append("|")
        } else {
            stringBuilder.append(newString).append("|")
        }
    }

    private fun compile() {
        if (stringBuilder.isNotEmpty()) stringBuilder.deleteCharAt(stringBuilder.length - 1)
        stringBuilder.insert(0, "[^.]+?(")
        stringBuilder.append(")$")
        pattern = Pattern.compile(stringBuilder.toString())
    }

    fun extract2LD(host: String): String? {
        if (pattern == null) return null
        val matcher = pattern!!.matcher(host)
        return if (matcher.find()) {
            matcher.group(0)
        } else null
    }

    fun extractTLD(host: String): String? {
        if (pattern == null) return null
        val m = pattern!!.matcher(host)
        return if (m.find()) {
            m.group(1)
        } else null
    }

    private object StringLengthComparator : Comparator<String> {
        override fun compare(s1: String, s2: String): Int {
            if (s1.length > s2.length) return -1
            return if (s1.length < s2.length) 1 else 0
        }
    }
}
