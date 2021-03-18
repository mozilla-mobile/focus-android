/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.customtabs

import mozilla.components.concept.engine.Engine
import mozilla.components.feature.customtabs.AbstractCustomTabsService
import mozilla.components.feature.customtabs.store.CustomTabsServiceStore
import org.mozilla.focus.ext.components

class CustomTabsService : AbstractCustomTabsService() {
    override val customTabsServiceStore: CustomTabsServiceStore by lazy { components.customTabsStore }
    override val engine: Engine by lazy { components.engine }
}
