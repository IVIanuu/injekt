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
import com.ivianuu.injekt.Binding

object SystemServiceBindings {
    @Binding inline fun ApplicationContext.accessibilityManager() =
        systemService<AccessibilityManager>()

    @Binding inline fun ApplicationContext.accountManager() = systemService<AccountManager>()

    @Binding inline fun ApplicationContext.activityManager() = systemService<ActivityManager>()

    @Binding inline fun ApplicationContext.alarmManager() = systemService<AlarmManager>()

    @Binding inline fun ApplicationContext.audioManager() = systemService<AudioManager>()

    @Binding inline fun ApplicationContext.clipboardManager() = systemService<ClipboardManager>()

    @Binding inline fun ApplicationContext.connectivityManager() =
        systemService<ConnectivityManager>()

    @Binding inline fun ApplicationContext.devicePolicyManager() =
        systemService<DevicePolicyManager>()

    @Binding inline fun ApplicationContext.downloadManager() = systemService<DownloadManager>()

    @Binding inline fun ApplicationContext.dropBoxManager() = systemService<DropBoxManager>()

    @Binding inline fun ApplicationContext.inputMethodManager() =
        systemService<InputMethodManager>()

    @Binding inline fun ApplicationContext.keyguardManager() = systemService<KeyguardManager>()

    @Binding inline fun ApplicationContext.layoutInflater() = systemService<LayoutInflater>()

    @Binding inline fun ApplicationContext.locationManager() = systemService<LocationManager>()

    @Binding inline fun ApplicationContext.nfcManager() = systemService<NfcManager>()

    @Binding inline fun ApplicationContext.notificationManager() =
        systemService<NotificationManager>()

    @Binding inline fun ApplicationContext.powerManager() = systemService<PowerManager>()

    @Binding inline fun ApplicationContext.searchManager() = systemService<SearchManager>()

    @Binding inline fun ApplicationContext.sensorManager() = systemService<SensorManager>()

    @Binding inline fun ApplicationContext.storageManager() = systemService<StorageManager>()

    @Binding inline fun ApplicationContext.telephonyManager() = systemService<TelephonyManager>()

    @Binding inline fun ApplicationContext.textServicesManager() =
        systemService<TextServicesManager>()

    @Binding inline fun ApplicationContext.uiModeManager() = systemService<UiModeManager>()

    @Binding inline fun ApplicationContext.usbManager() = systemService<UsbManager>()

    @Binding inline fun ApplicationContext.vibrator() = systemService<Vibrator>()

    @Binding inline fun ApplicationContext.wallpaperManager() = systemService<WallpaperManager>()

    @Binding inline fun ApplicationContext.wifiP2pManager() = systemService<WifiP2pManager>()

    @Binding inline fun ApplicationContext.wifiManager() = systemService<WifiManager>()

    @Binding inline fun ApplicationContext.windowManager() = systemService<WindowManager>()

    @Binding inline fun ApplicationContext.inputManager() = systemService<InputManager>()

    @Binding inline fun ApplicationContext.mediaRouter() = systemService<MediaRouter>()

    @Binding inline fun ApplicationContext.nsdManager() = systemService<NsdManager>()

    @Binding inline fun ApplicationContext.displayManager() = systemService<DisplayManager>()

    @Binding inline fun ApplicationContext.userManager() = systemService<UserManager>()

    @Binding inline fun ApplicationContext.bluetoothManager() = systemService<BluetoothManager>()

    @Binding inline fun ApplicationContext.appOpsManager() = systemService<AppOpsManager>()

    @Binding inline fun ApplicationContext.captioningManager() = systemService<CaptioningManager>()

    @Binding inline fun ApplicationContext.consumerIrManager() = systemService<ConsumerIrManager>()

    @Binding inline fun ApplicationContext.printManager() = systemService<PrintManager>()

    @Binding inline fun ApplicationContext.appWidgetManager() = systemService<AppWidgetManager>()

    @Binding inline fun ApplicationContext.batteryManager() = systemService<BatteryManager>()

    @Binding inline fun ApplicationContext.cameraManager() = systemService<CameraManager>()

    @Binding inline fun ApplicationContext.jobScheduler() = systemService<JobScheduler>()

    @Binding inline fun ApplicationContext.launcherApps() = systemService<LauncherApps>()

    @Binding inline fun ApplicationContext.mediaProjectionManager() =
        systemService<MediaProjectionManager>()

    @Binding inline fun ApplicationContext.mediaSessionManager() =
        systemService<MediaSessionManager>()

    @Binding inline fun ApplicationContext.restrictionsManager() =
        systemService<RestrictionsManager>()

    @Binding inline fun ApplicationContext.telecomManager() = systemService<TelecomManager>()

    @Binding inline fun ApplicationContext.tvInputManager() = systemService<TvInputManager>()

    @Binding inline fun ApplicationContext.subscriptionManager() =
        systemService<SubscriptionManager>()

    @Binding inline fun ApplicationContext.usageStatsManager() = systemService<UsageStatsManager>()

    @Binding inline fun ApplicationContext.carrierConfigManager() =
        systemService<CarrierConfigManager>()

    @Binding inline fun ApplicationContext.fingerprintManager() =
        systemService<FingerprintManager>()

    @Binding inline fun ApplicationContext.midiManager() = systemService<MidiManager>()

    @Binding inline fun ApplicationContext.networkStatsManager() =
        systemService<NetworkStatsManager>()

    @Binding inline fun ApplicationContext.hardwarePropertiesManager() =
        systemService<HardwarePropertiesManager>()

    @Binding inline fun ApplicationContext.systemHealthManager() =
        systemService<SystemHealthManager>()

    @Binding inline fun ApplicationContext.shortcutManager() = systemService<ShortcutManager>()

    @PublishedApi
    internal inline fun <reified T : Any> Context.systemService() =
        ContextCompat.getSystemService(this, T::class.java)!!
}
