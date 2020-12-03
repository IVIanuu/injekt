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
    @Binding
    inline val ApplicationContext.accessibilityManager
        get() = systemService<AccessibilityManager>()

    @Binding
    inline val ApplicationContext.accountManager
        get() = systemService<AccountManager>()

    @Binding
    inline val ApplicationContext.activityManager
        get() = systemService<ActivityManager>()

    @Binding
    inline val ApplicationContext.alarmManager
        get() = systemService<AlarmManager>()

    @Binding
    inline val ApplicationContext.audioManager
        get() = systemService<AudioManager>()

    @Binding
    inline val ApplicationContext.clipboardManager
        get() = systemService<ClipboardManager>()

    @Binding
    inline val ApplicationContext.connectivityManager
        get() = systemService<ConnectivityManager>()

    @Binding
    inline val ApplicationContext.devicePolicyManager
        get() = systemService<DevicePolicyManager>()

    @Binding
    inline val ApplicationContext.downloadManager
        get() = systemService<DownloadManager>()

    @Binding
    inline val ApplicationContext.dropBoxManager
        get() = systemService<DropBoxManager>()

    @Binding
    inline val ApplicationContext.inputMethodManager
        get() = systemService<InputMethodManager>()

    @Binding
    inline val ApplicationContext.keyguardManager
        get() = systemService<KeyguardManager>()

    @Binding
    inline val ApplicationContext.layoutInflater
        get() = systemService<LayoutInflater>()

    @Binding
    inline val ApplicationContext.locationManager
        get() = systemService<LocationManager>()

    @Binding
    inline val ApplicationContext.nfcManager
        get() = systemService<NfcManager>()

    @Binding
    inline val ApplicationContext.notificationManager
        get() = systemService<NotificationManager>()

    @Binding
    inline val ApplicationContext.powerManager
        get() = systemService<PowerManager>()

    @Binding
    inline val ApplicationContext.searchManager
        get() = systemService<SearchManager>()

    @Binding
    inline val ApplicationContext.sensorManager
        get() = systemService<SensorManager>()

    @Binding
    inline val ApplicationContext.storageManager
        get() = systemService<StorageManager>()

    @Binding
    inline val ApplicationContext.telephonyManager
        get() = systemService<TelephonyManager>()

    @Binding
    inline val ApplicationContext.textServicesManager
        get() = systemService<TextServicesManager>()

    @Binding
    inline val ApplicationContext.uiModeManager
        get() = systemService<UiModeManager>()

    @Binding
    inline val ApplicationContext.usbManager
        get() = systemService<UsbManager>()

    @Binding
    inline val ApplicationContext.vibrator
        get() = systemService<Vibrator>()

    @Binding
    inline val ApplicationContext.wallpaperManager
        get() = systemService<WallpaperManager>()

    @Binding
    inline val ApplicationContext.wifiP2pManager
        get() = systemService<WifiP2pManager>()

    @Binding
    inline val ApplicationContext.wifiManager
        get() = systemService<WifiManager>()

    @Binding
    inline val ApplicationContext.windowManager
        get() = systemService<WindowManager>()

    @Binding
    inline val ApplicationContext.inputManager
        get() = systemService<InputManager>()

    @Binding
    inline val ApplicationContext.mediaRouter
        get() = systemService<MediaRouter>()

    @Binding
    inline val ApplicationContext.nsdManager
        get() = systemService<NsdManager>()

    @Binding
    inline val ApplicationContext.displayManager
        get() = systemService<DisplayManager>()

    @Binding
    inline val ApplicationContext.userManager
        get() = systemService<UserManager>()

    @Binding
    inline val ApplicationContext.bluetoothManager
        get() = systemService<BluetoothManager>()

    @Binding
    inline val ApplicationContext.appOpsManager
        get() = systemService<AppOpsManager>()

    @Binding
    inline val ApplicationContext.captioningManager
        get() = systemService<CaptioningManager>()

    @Binding
    inline val ApplicationContext.consumerIrManager
        get() = systemService<ConsumerIrManager>()

    @Binding
    inline val ApplicationContext.printManager
        get() = systemService<PrintManager>()

    @Binding
    inline val ApplicationContext.appWidgetManager
        get() = systemService<AppWidgetManager>()

    @Binding
    inline val ApplicationContext.batteryManager
        get() = systemService<BatteryManager>()

    @Binding
    inline val ApplicationContext.cameraManager
        get() = systemService<CameraManager>()

    @Binding
    inline val ApplicationContext.jobScheduler
        get() = systemService<JobScheduler>()

    @Binding
    inline val ApplicationContext.launcherApps
        get() = systemService<LauncherApps>()

    @Binding
    inline val ApplicationContext.mediaProjectionManager
        get() = systemService<MediaProjectionManager>()

    @Binding
    inline val ApplicationContext.mediaSessionManager
        get() = systemService<MediaSessionManager>()

    @Binding
    inline val ApplicationContext.restrictionsManager
        get() = systemService<RestrictionsManager>()

    @Binding
    inline val ApplicationContext.telecomManager
        get() = systemService<TelecomManager>()

    @Binding
    inline val ApplicationContext.tvInputManager
        get() = systemService<TvInputManager>()

    @Binding
    inline val ApplicationContext.subscriptionManager
        get() = systemService<SubscriptionManager>()

    @Binding
    inline val ApplicationContext.usageStatsManager
        get() = systemService<UsageStatsManager>()

    @Binding
    inline val ApplicationContext.carrierConfigManager
        get() = systemService<CarrierConfigManager>()

    @Binding
    inline val ApplicationContext.fingerprintManager
        get() = systemService<FingerprintManager>()

    @Binding
    inline val ApplicationContext.midiManager
        get() = systemService<MidiManager>()

    @Binding
    inline val ApplicationContext.networkStatsManager
        get() = systemService<NetworkStatsManager>()

    @Binding
    inline val ApplicationContext.hardwarePropertiesManager
        get() = systemService<HardwarePropertiesManager>()

    @Binding
    inline val ApplicationContext.systemHealthManager
        get() = systemService<SystemHealthManager>()

    @Binding
    inline val ApplicationContext.shortcutManager
        get() = systemService<ShortcutManager>()

    @PublishedApi
    internal inline fun <reified T : Any> Context.systemService() =
        ContextCompat.getSystemService(this, T::class.java)!!
}
