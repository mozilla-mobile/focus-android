/* -*- Mode: Java; c-basic-offset: 4; tab-width: 4; indent-tabs-mode: nil; -*-
 * This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.web

import org.junit.Test

import org.junit.Assert.*

class BrowsingSessionTest {
    @Test
    fun testSingleton() {
        assertNotNull(BrowsingSession.getInstance())

        assertEquals(BrowsingSession.getInstance(), BrowsingSession.getInstance())
    }

    @Test
    fun testActive() {
        assertFalse(BrowsingSession.getInstance().isActive)

        BrowsingSession.getInstance().start()

        assertTrue(BrowsingSession.getInstance().isActive)

        BrowsingSession.getInstance().stop()

        assertFalse(BrowsingSession.getInstance().isActive)

        BrowsingSession.getInstance().let{
            it.start()
            it.start()
            it.stop()
        }
        assertFalse(BrowsingSession.getInstance().isActive)
    }
}