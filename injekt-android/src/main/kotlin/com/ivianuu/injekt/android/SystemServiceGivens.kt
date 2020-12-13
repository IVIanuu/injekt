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
    @Given inline val @Given ApplicationContext.accessibilityManager get() = systemService<AccessibilityManager>()

    @Given inline val @Given ApplicationContext.accountManager get() = systemService<AccountManager>()

    @Given inline val @Given ApplicationContext.activityManager get() = systemService<ActivityManager>()

    @Given inline val @Given ApplicationContext.alarmManager get() = systemService<AlarmManager>()

    @Given inline val @Given ApplicationContext.audioManager get() = systemService<AudioManager>()

    @Given inline val @Given ApplicationContext.clipboardManager get() = systemService<ClipboardManager>()

    @Given inline val @Given ApplicationContext.connectivityManager
        get() =
            systemService<ConnectivityManager>()

    @Given inline val @Given ApplicationContext.devicePolicyManager
        get() =
            systemService<DevicePolicyManager>()

    @Given inline val @Given ApplicationContext.downloadManager get() = systemService<DownloadManager>()

    @Given inline val @Given ApplicationContext.dropBoxManager get() = systemService<DropBoxManager>()

    @Given inline val @Given ApplicationContext.inputMethodManager
        get() =
            systemService<InputMethodManager>()

    @Given inline val @Given ApplicationContext.keyguardManager get() = systemService<KeyguardManager>()

    @Given inline val @Given ApplicationContext.layoutInflater get() = systemService<LayoutInflater>()

    @Given inline val @Given ApplicationContext.locationManager get() = systemService<LocationManager>()

    @Given inline val @Given ApplicationContext.nfcManager get() = systemService<NfcManager>()

    @Given inline val @Given ApplicationContext.notificationManager
        get() =
            systemService<NotificationManager>()

    @Given inline val @Given ApplicationContext.powerManager get() = systemService<PowerManager>()

    @Given inline val @Given ApplicationContext.searchManager get() = systemService<SearchManager>()

    @Given inline val @Given ApplicationContext.sensorManager get() = systemService<SensorManager>()

    @Given inline val @Given ApplicationContext.storageManager get() = systemService<StorageManager>()

    @Given inline val @Given ApplicationContext.telephonyManager get() = systemService<TelephonyManager>()

    @Given inline val @Given ApplicationContext.textServicesManager
        get() =
            systemService<TextServicesManager>()

    @Given inline val @Given ApplicationContext.uiModeManager get() = systemService<UiModeManager>()

    @Given inline val @Given ApplicationContext.usbManager get() = systemService<UsbManager>()

    @Given inline val @Given ApplicationContext.vibrator get() = systemService<Vibrator>()

    @Given inline val @Given ApplicationContext.wallpaperManager get() = systemService<WallpaperManager>()

    @Given inline val @Given ApplicationContext.wifiP2pManager get() = systemService<WifiP2pManager>()

    @Given inline val @Given ApplicationContext.wifiManager get() = systemService<WifiManager>()

    @Given inline val @Given ApplicationContext.windowManager get() = systemService<WindowManager>()

    @Given inline val @Given ApplicationContext.inputManager get() = systemService<InputManager>()

    @Given inline val @Given ApplicationContext.mediaRouter get() = systemService<MediaRouter>()

    @Given inline val @Given ApplicationContext.nsdManager get() = systemService<NsdManager>()

    @Given inline val @Given ApplicationContext.displayManager get() = systemService<DisplayManager>()

    @Given inline val @Given ApplicationContext.userManager get() = systemService<UserManager>()

    @Given inline val @Given ApplicationContext.bluetoothManager get() = systemService<BluetoothManager>()

    @Given inline val @Given ApplicationContext.appOpsManager get() = systemService<AppOpsManager>()

    @Given inline val @Given ApplicationContext.captioningManager get() = systemService<CaptioningManager>()

    @Given inline val @Given ApplicationContext.consumerIrManager get() = systemService<ConsumerIrManager>()

    @Given inline val @Given ApplicationContext.printManager get() = systemService<PrintManager>()

    @Given inline val @Given ApplicationContext.appWidgetManager get() = systemService<AppWidgetManager>()

    @Given inline val @Given ApplicationContext.batteryManager get() = systemService<BatteryManager>()

    @Given inline val @Given ApplicationContext.cameraManager get() = systemService<CameraManager>()

    @Given inline val @Given ApplicationContext.jobScheduler get() = systemService<JobScheduler>()

    @Given inline val @Given ApplicationContext.launcherApps get() = systemService<LauncherApps>()

    @Given inline val @Given ApplicationContext.mediaProjectionManager
        get() =
            systemService<MediaProjectionManager>()

    @Given inline val @Given ApplicationContext.mediaSessionManager
        get() =
            systemService<MediaSessionManager>()

    @Given inline val @Given ApplicationContext.restrictionsManager
        get() =
            systemService<RestrictionsManager>()

    @Given inline val @Given ApplicationContext.telecomManager get() = systemService<TelecomManager>()

    @Given inline val @Given ApplicationContext.tvInputManager get() = systemService<TvInputManager>()

    @Given inline val @Given ApplicationContext.subscriptionManager
        get() =
            systemService<SubscriptionManager>()

    @Given inline val @Given ApplicationContext.usageStatsManager get() = systemService<UsageStatsManager>()

    @Given inline val @Given ApplicationContext.carrierConfigManager
        get() =
            systemService<CarrierConfigManager>()

    @Given inline val @Given ApplicationContext.fingerprintManager
        get() =
            systemService<FingerprintManager>()

    @Given inline val @Given ApplicationContext.midiManager get() = systemService<MidiManager>()

    @Given inline val @Given ApplicationContext.networkStatsManager
        get() =
            systemService<NetworkStatsManager>()

    @Given inline val @Given ApplicationContext.hardwarePropertiesManager
        get() =
            systemService<HardwarePropertiesManager>()

    @Given inline val @Given ApplicationContext.systemHealthManager
        get() =
            systemService<SystemHealthManager>()

    @Given inline val @Given ApplicationContext.shortcutManager get() = systemService<ShortcutManager>()

    @PublishedApi
    internal inline fun <reified T : Any> Context.systemService() =
        ContextCompat.getSystemService(this, T::class.java)!!
}
