package org.mozilla.focus.webview.matcher

import org.junit.Test
import org.junit.runner.RunWith
import org.mozilla.focus.webview.matcher.Trie.WhiteListTrie
import org.mozilla.focus.webview.matcher.util.FocusString
import org.robolectric.RobolectricTestRunner
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull

@RunWith(RobolectricTestRunner::class)
class TrieTest {
    @Test
    @Throws(Exception::class)
    fun findNode() {
        val trie = Trie.createRootNode()

        assertNull(trie.findNode(FocusString.create("hello")))
        val putNode = trie.put(FocusString.create("hello"))
        val foundNode = trie.findNode(FocusString.create("hello"))

        assertNotNull(putNode)
        assertNotNull(foundNode)
        assertEquals(putNode, foundNode)
        // Substring matching: doesn't happen (except for subdomains, we test those later)
        assertNull(trie.findNode(FocusString.create("hell")))
        assertNull(trie.findNode(FocusString.create("hellop")))

        trie.put(FocusString.create("hellohello"))
        // Ensure both old and new overlapping strings can still be found
        assertNotNull(trie.findNode(FocusString.create("hello")))
        assertNotNull(trie.findNode(FocusString.create("hellohello")))
        // These still don't match:
        assertNull(trie.findNode(FocusString.create("hell")))
        assertNull(trie.findNode(FocusString.create("hellop")))
        // Domain specific / partial domain tests:
        trie.put(FocusString.create("foo.com").reverse())
        // Domain and subdomain can be found
        assertNotNull(trie.findNode(FocusString.create("foo.com").reverse()))
        assertNotNull(trie.findNode(FocusString.create("bar.foo.com").reverse()))
        // But other domains with some overlap don't match
        assertNull(trie.findNode(FocusString.create("bar-foo.com").reverse()))
        assertNull(trie.findNode(FocusString.create("oo.com").reverse()))
    }

    @Test
    fun testWhiteListTrie() {
        var trie: WhiteListTrie

        run {
            val whitelist = Trie.createRootNode()

            whitelist.put(FocusString.create("abc"))

            trie = WhiteListTrie.createRootNode()
            trie.putWhiteList(FocusString.create("def"), whitelist)
        }

        assertNull(trie.findNode(FocusString.create("abc")))
        // In practice EntityList uses it's own search in order to cover all possible matching notes
        // (e.g. in case we have separate whitelists for mozilla.org and foo.mozilla.org), however
        // we don't need to test that here yet.
        val foundWhitelist = trie.findNode(FocusString.create("def")) as WhiteListTrie
        assertNotNull(foundWhitelist)

        assertNotNull(foundWhitelist.whitelist.findNode(FocusString.create("abc")))
    }
}