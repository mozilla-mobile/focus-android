/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.settings

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.compose.foundation.layout.Column
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.Scaffold
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.fragment.app.Fragment
import com.google.accompanist.insets.LocalWindowInsets
import com.google.accompanist.insets.rememberInsetsPaddingValues
import com.google.accompanist.insets.ui.TopAppBar
import org.mozilla.focus.R
import org.mozilla.focus.ui.theme.FocusTheme
import org.mozilla.focus.ui.theme.focusColors
import org.mozilla.focus.utils.StatusBarUtils

/**
 * Fragment acting as a wrapper over a [Composable] which will be shown below a [TopAppBar].
 *
 * Useful for Fragments shown in otherwise fullscreen Activities such that they would be shown
 * beneath the status bar not below it.
 *
 * Classes extending this are expected to provide the [Composable] content and a basic behavior
 * for the toolbar: title and navigate up callback.
 */
abstract class BaseComposeFragment : Fragment() {
    /**
     * Screen title shown in toolbar.
     */
    abstract val titleRes: Int

    /**
     * Callback for the up navigation button shown in toolbar.
     */
    abstract fun onNavigateUp(): () -> Unit

    /**
     * content of the screen in compose. That will be shown below Toolbar
     */
    @Composable
    abstract fun Content()

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?):
        View = ComposeView(requireContext()).apply {
        StatusBarUtils.getStatusBarHeight(this) { statusBarHeight ->
            setContent {
                FocusTheme {
                    Scaffold {
                        Column {
                            CompositionLocalProvider {
                                TopAppBar(
                                    title = {
                                        Text(
                                            text = getString(titleRes),
                                            color = focusColors.toolbarColor
                                        )
                                    },
                                    contentPadding = rememberInsetsPaddingValues(
                                        insets = LocalWindowInsets.current.statusBars,
                                        additionalTop = LocalDensity.current.run {
                                            (statusBarHeight - LocalWindowInsets.current.statusBars.top)
                                                .toDp()
                                        }
                                    ),
                                    navigationIcon = {
                                        IconButton(
                                            onClick = onNavigateUp()
                                        ) {
                                            Icon(
                                                Icons.Filled.ArrowBack,
                                                stringResource(R.string.go_back),
                                                tint = focusColors.toolbarColor
                                            )
                                        }
                                    },
                                    backgroundColor = colorResource(R.color.settings_background)
                                )
                            }
                            this@BaseComposeFragment.Content()
                        }
                    }
                }
            }
        }
    }
}
