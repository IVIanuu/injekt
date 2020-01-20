/*
 * Copyright 2019 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.android

import android.accounts.AccountManager
import android.app.ActivityManager
import android.app.AlarmManager
import android.app.AppOpsManager
import android.app.DownloadManager
import android.app.KeyguardManager
import android.app.NotificationManager
import android.app.SearchManager
import android.app.UiModeManager
import android.app.WallpaperManager
import android.app.admin.DevicePolicyManager
import android.app.job.JobScheduler
import android.app.usage.NetworkStatsManager
import android.app.usage.UsageStatsManager
import android.appwidget.AppWidgetManager
import android.bluetooth.BluetoothManager
import android.content.ClipboardManager
import android.content.RestrictionsManager
import android.content.pm.LauncherApps
import android.content.pm.ShortcutManager
import android.hardware.ConsumerIrManager
import android.hardware.SensorManager
import android.hardware.camera2.CameraManager
import android.hardware.display.DisplayManager
import android.hardware.fingerprint.FingerprintManager
import android.hardware.input.InputManager
import android.hardware.usb.UsbManager
import android.location.LocationManager
import android.media.AudioManager
import android.media.MediaRouter
import android.media.midi.MidiManager
import android.media.projection.MediaProjectionManager
import android.media.session.MediaSessionManager
import android.media.tv.TvInputManager
import android.net.ConnectivityManager
import android.net.nsd.NsdManager
import android.net.wifi.WifiManager
import android.net.wifi.p2p.WifiP2pManager
import android.nfc.NfcManager
import android.os.BatteryManager
import android.os.Build
import android.os.DropBoxManager
import android.os.HardwarePropertiesManager
import android.os.PowerManager
import android.os.UserManager
import android.os.Vibrator
import android.os.health.SystemHealthManager
import android.os.storage.StorageManager
import android.print.PrintManager
import android.telecom.TelecomManager
import android.telephony.CarrierConfigManager
import android.telephony.SubscriptionManager
import android.telephony.TelephonyManager
import android.view.LayoutInflater
import android.view.WindowManager
import android.view.accessibility.AccessibilityManager
import android.view.accessibility.CaptioningManager
import android.view.inputmethod.InputMethodManager
import android.view.textservice.TextServicesManager
import androidx.core.content.ContextCompat
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.get
import com.ivianuu.injekt.typeOf
import kotlin.reflect.KClass

val SystemServiceModule = Module {
    getSystemServices()
        .forEach { service ->
            factory(type = typeOf(service)) {
                ContextCompat.getSystemService(get(), service.java)!!
            }
        }
}

@Suppress("DEPRECATION")
private fun getSystemServices(): Set<KClass<*>> = mutableSetOf<KClass<out Any>>().apply {
    this += AccessibilityManager::class
    this += AccountManager::class
    this += ActivityManager::class
    this += AlarmManager::class
    this += AudioManager::class
    this += ClipboardManager::class
    this += ConnectivityManager::class
    this += DevicePolicyManager::class
    this += DownloadManager::class
    this += DropBoxManager::class
    this += InputMethodManager::class
    this += KeyguardManager::class
    this += LayoutInflater::class
    this += LocationManager::class
    this += NfcManager::class
    this += NotificationManager::class
    this += PowerManager::class
    this += SearchManager::class
    this += SensorManager::class
    this += StorageManager::class
    this += TelephonyManager::class
    this += TextServicesManager::class
    this += UiModeManager::class
    this += UsbManager::class
    this += Vibrator::class
    this += WallpaperManager::class
    this += WifiP2pManager::class
    this += WifiManager::class
    this += WindowManager::class

    if (Build.VERSION.SDK_INT > 16) {
        this += InputManager::class
        this += MediaRouter::class
        this += NsdManager::class
    }
    if (Build.VERSION.SDK_INT > 17) {
        this += DisplayManager::class
        this += UserManager::class
    }
    if (Build.VERSION.SDK_INT > 18) {
        this += BluetoothManager::class
    }
    if (Build.VERSION.SDK_INT > 19) {
        this += AppOpsManager::class
        this += CaptioningManager::class
        this += ConsumerIrManager::class
        this += PrintManager::class
    }
    if (Build.VERSION.SDK_INT > 21) {
        this += AppWidgetManager::class
        this += BatteryManager::class
        this += CameraManager::class
        this += JobScheduler::class
        this += LauncherApps::class
        this += MediaProjectionManager::class
        this += MediaSessionManager::class
        this += RestrictionsManager::class
        this += TelecomManager::class
        this += TvInputManager::class
    }
    if (Build.VERSION.SDK_INT > 22) {
        this += SubscriptionManager::class
        this += UsageStatsManager::class
    }
    if (Build.VERSION.SDK_INT >= 23) {
        this += CarrierConfigManager::class
        this += FingerprintManager::class
        this += MidiManager::class
        this += NetworkStatsManager::class
    }
    if (Build.VERSION.SDK_INT >= 24) {
        this += HardwarePropertiesManager::class
        this += SystemHealthManager::class
    }
    if (Build.VERSION.SDK_INT >= 25) {
        this += ShortcutManager::class
    }
}
