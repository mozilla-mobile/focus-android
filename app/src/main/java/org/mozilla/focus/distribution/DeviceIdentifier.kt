/* This Source Code Form is subject to the terms of the Mozilla Public
 * License, v. 2.0. If a copy of the MPL was not distributed with this
 * file, You can obtain one at http://mozilla.org/MPL/2.0/. */

package org.mozilla.focus.distribution

import java.io.BufferedReader
import java.io.InputStreamReader

interface DeviceIdentifier {
    fun identify(): Device?
}

@SuppressWarnings("TooGenericExceptionCaught")
class BrandAndDeviceDeviceIdentifier : DeviceIdentifier {
    companion object {
        private val BRAND_PROP = "ro.vendor.product.brand"
        private val DEVICE_PROP = "ro.vendor.product.device"
    }

    override fun identify(): Device? {
        val brand: String
        val device: String

        try {
            brand = get(BRAND_PROP)
            device = get(DEVICE_PROP)
        } catch (e: Exception) {
            return null
        }

        return Device.create(brand, device)
    }

    private fun get(property: String): String {
        val value: String?
        val getPropProcess = Runtime.getRuntime().exec("getprop $property")
        val osRes = BufferedReader(InputStreamReader(getPropProcess.inputStream))

        value = osRes.readLine()

        osRes.close()

        return value
    }
}
