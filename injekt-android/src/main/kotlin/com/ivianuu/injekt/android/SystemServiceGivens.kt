/*
 * Copyright 2020 Manuel Wrage
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

@file:Suppress("NOTHING_TO_INLINE")

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
import android.content.Context
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
import com.ivianuu.injekt.Given

object SystemServiceGivens {
    @Given inline val @Given AppContext.accessibilityManager get() = systemService<AccessibilityManager>()

    @Given inline val @Given AppContext.accountManager get() = systemService<AccountManager>()

    @Given inline val @Given AppContext.activityManager get() = systemService<ActivityManager>()

    @Given inline val @Given AppContext.alarmManager get() = systemService<AlarmManager>()

    @Given inline val @Given AppContext.audioManager get() = systemService<AudioManager>()

    @Given inline val @Given AppContext.clipboardManager get() = systemService<ClipboardManager>()

    @Given inline val @Given AppContext.connectivityManager
        get() =
            systemService<ConnectivityManager>()

    @Given inline val @Given AppContext.devicePolicyManager
        get() =
            systemService<DevicePolicyManager>()

    @Given inline val @Given AppContext.downloadManager get() = systemService<DownloadManager>()

    @Given inline val @Given AppContext.dropBoxManager get() = systemService<DropBoxManager>()

    @Given inline val @Given AppContext.inputMethodManager
        get() =
            systemService<InputMethodManager>()

    @Given inline val @Given AppContext.keyguardManager get() = systemService<KeyguardManager>()

    @Given inline val @Given AppContext.layoutInflater get() = systemService<LayoutInflater>()

    @Given inline val @Given AppContext.locationManager get() = systemService<LocationManager>()

    @Given inline val @Given AppContext.nfcManager get() = systemService<NfcManager>()

    @Given inline val @Given AppContext.notificationManager
        get() =
            systemService<NotificationManager>()

    @Given inline val @Given AppContext.powerManager get() = systemService<PowerManager>()

    @Given inline val @Given AppContext.searchManager get() = systemService<SearchManager>()

    @Given inline val @Given AppContext.sensorManager get() = systemService<SensorManager>()

    @Given inline val @Given AppContext.storageManager get() = systemService<StorageManager>()

    @Given inline val @Given AppContext.telephonyManager get() = systemService<TelephonyManager>()

    @Given inline val @Given AppContext.textServicesManager
        get() =
            systemService<TextServicesManager>()

    @Given inline val @Given AppContext.uiModeManager get() = systemService<UiModeManager>()

    @Given inline val @Given AppContext.usbManager get() = systemService<UsbManager>()

    @Given inline val @Given AppContext.vibrator get() = systemService<Vibrator>()

    @Given inline val @Given AppContext.wallpaperManager get() = systemService<WallpaperManager>()

    @Given inline val @Given AppContext.wifiP2pManager get() = systemService<WifiP2pManager>()

    @Given inline val @Given AppContext.wifiManager get() = systemService<WifiManager>()

    @Given inline val @Given AppContext.windowManager get() = systemService<WindowManager>()

    @Given inline val @Given AppContext.inputManager get() = systemService<InputManager>()

    @Given inline val @Given AppContext.mediaRouter get() = systemService<MediaRouter>()

    @Given inline val @Given AppContext.nsdManager get() = systemService<NsdManager>()

    @Given inline val @Given AppContext.displayManager get() = systemService<DisplayManager>()

    @Given inline val @Given AppContext.userManager get() = systemService<UserManager>()

    @Given inline val @Given AppContext.bluetoothManager get() = systemService<BluetoothManager>()

    @Given inline val @Given AppContext.appOpsManager get() = systemService<AppOpsManager>()

    @Given inline val @Given AppContext.captioningManager get() = systemService<CaptioningManager>()

    @Given inline val @Given AppContext.consumerIrManager get() = systemService<ConsumerIrManager>()

    @Given inline val @Given AppContext.printManager get() = systemService<PrintManager>()

    @Given inline val @Given AppContext.appWidgetManager get() = systemService<AppWidgetManager>()

    @Given inline val @Given AppContext.batteryManager get() = systemService<BatteryManager>()

    @Given inline val @Given AppContext.cameraManager get() = systemService<CameraManager>()

    @Given inline val @Given AppContext.jobScheduler get() = systemService<JobScheduler>()

    @Given inline val @Given AppContext.launcherApps get() = systemService<LauncherApps>()

    @Given inline val @Given AppContext.mediaProjectionManager
        get() =
            systemService<MediaProjectionManager>()

    @Given inline val @Given AppContext.mediaSessionManager
        get() =
            systemService<MediaSessionManager>()

    @Given inline val @Given AppContext.restrictionsManager
        get() =
            systemService<RestrictionsManager>()

    @Given inline val @Given AppContext.telecomManager get() = systemService<TelecomManager>()

    @Given inline val @Given AppContext.tvInputManager get() = systemService<TvInputManager>()

    @Given inline val @Given AppContext.subscriptionManager
        get() =
            systemService<SubscriptionManager>()

    @Given inline val @Given AppContext.usageStatsManager get() = systemService<UsageStatsManager>()

    @Given inline val @Given AppContext.carrierConfigManager
        get() =
            systemService<CarrierConfigManager>()

    @Given inline val @Given AppContext.fingerprintManager
        get() =
            systemService<FingerprintManager>()

    @Given inline val @Given AppContext.midiManager get() = systemService<MidiManager>()

    @Given inline val @Given AppContext.networkStatsManager
        get() =
            systemService<NetworkStatsManager>()

    @Given inline val @Given AppContext.hardwarePropertiesManager
        get() =
            systemService<HardwarePropertiesManager>()

    @Given inline val @Given AppContext.systemHealthManager
        get() =
            systemService<SystemHealthManager>()

    @Given inline val @Given AppContext.shortcutManager get() = systemService<ShortcutManager>()

    @PublishedApi
    internal inline fun <reified T : Any> Context.systemService() =
        ContextCompat.getSystemService(this, T::class.java)!!
}
