/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, you can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.animation

import android.graphics.drawable.TransitionDrawable

import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

import org.mockito.Mockito.mock
import org.mockito.Mockito.verify

@RunWith(RobolectricTestRunner::class)
class TransitionDrawableGroupTest {
    @Test
    fun testStartIsCalledOnAllItems() {
        val transitionDrawable1 = mock(TransitionDrawable::class.java)
        val transitionDrawable2 = mock(TransitionDrawable::class.java)

        val group = TransitionDrawableGroup(
                transitionDrawable1, transitionDrawable2)

        group.startTransition(2500)

        verify(transitionDrawable1).startTransition(2500)
        verify(transitionDrawable2).startTransition(2500)
    }

    @Test
    fun testResetIsCalledOnAllItems() {
        val transitionDrawable1 = mock(TransitionDrawable::class.java)
        val transitionDrawable2 = mock(TransitionDrawable::class.java)

        val group = TransitionDrawableGroup(
                transitionDrawable1, transitionDrawable2)

        group.resetTransition()

        verify(transitionDrawable1).resetTransition()
        verify(transitionDrawable2).resetTransition()
    }
}
