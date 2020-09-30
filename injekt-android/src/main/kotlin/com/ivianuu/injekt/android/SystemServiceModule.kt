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
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.Module

@Module
object SystemServiceModule {
    @Given
    val Context.accessibilityManager
        get() = systemService<AccessibilityManager>()

    @Given
    val Context.accountManager
        get() = systemService<AccountManager>()

    @Given
    val Context.activityManager
        get() = systemService<ActivityManager>()

    @Given
    val Context.alarmManager
        get() = systemService<AlarmManager>()

    @Given
    val Context.audioManager
        get() = systemService<AudioManager>()

    @Given
    val Context.clipboardManager
        get() = systemService<ClipboardManager>()

    @Given
    val Context.connectivityManager
        get() = systemService<ConnectivityManager>()

    @Given
    val Context.devicePolicyManager
        get() = systemService<DevicePolicyManager>()

    @Given
    val Context.downloadManager
        get() = systemService<DownloadManager>()

    @Given
    val Context.dropBoxManager
        get() = systemService<DropBoxManager>()

    @Given
    val Context.inputMethodManager
        get() = systemService<InputMethodManager>()

    @Given
    val Context.keyguardManager
        get() = systemService<KeyguardManager>()

    @Given
    val Context.layoutInflater
        get() = systemService<LayoutInflater>()

    @Given
    val Context.locationManager
        get() = systemService<LocationManager>()

    @Given
    val Context.nfcManager
        get() = systemService<NfcManager>()

    @Given
    val Context.notificationManager
        get() = systemService<NotificationManager>()

    @Given
    val Context.powerManager
        get() = systemService<PowerManager>()

    @Given
    val Context.searchManager
        get() = systemService<SearchManager>()

    @Given
    val Context.sensorManager
        get() = systemService<SensorManager>()

    @Given
    val Context.storageManager
        get() = systemService<StorageManager>()

    @Given
    val Context.telephonyManager
        get() = systemService<TelephonyManager>()

    @Given
    val Context.textServicesManager
        get() = systemService<TextServicesManager>()

    @Given
    val Context.uiModeManager
        get() = systemService<UiModeManager>()

    @Given
    val Context.usbManager
        get() = systemService<UsbManager>()

    @Given
    val Context.vibrator
        get() = systemService<Vibrator>()

    @Given
    val Context.wallpaperManager
        get() = systemService<WallpaperManager>()

    @Given
    val Context.wifiP2pManager
        get() = systemService<WifiP2pManager>()

    @Given
    val Context.wifiManager
        get() = systemService<WifiManager>()

    @Given
    val Context.windowManager
        get() = systemService<WindowManager>()

    @Given
    val Context.inputManager
        get() = systemService<InputManager>()

    @Given
    val Context.mediaRouter
        get() = systemService<MediaRouter>()

    @Given
    val Context.nsdManager
        get() = systemService<NsdManager>()

    @Given
    val Context.displayManager
        get() = systemService<DisplayManager>()

    @Given
    val Context.userManager
        get() = systemService<UserManager>()

    @Given
    val Context.bluetoothManager
        get() = systemService<BluetoothManager>()

    @Given
    val Context.appOpsManager
        get() = systemService<AppOpsManager>()

    @Given
    val Context.captioningManager
        get() = systemService<CaptioningManager>()

    @Given
    val Context.consumerIrManager
        get() = systemService<ConsumerIrManager>()

    @Given
    val Context.printManager
        get() = systemService<PrintManager>()

    @Given
    val Context.appWidgetManager
        get() = systemService<AppWidgetManager>()

    @Given
    val Context.batteryManager
        get() = systemService<BatteryManager>()

    @Given
    val Context.cameraManager
        get() = systemService<CameraManager>()

    @Given
    val Context.jobScheduler
        get() = systemService<JobScheduler>()

    @Given
    val Context.launcherApps
        get() = systemService<LauncherApps>()

    @Given
    val Context.mediaProjectionManager
        get() = systemService<MediaProjectionManager>()

    @Given
    val Context.mediaSessionManager
        get() = systemService<MediaSessionManager>()

    @Given
    val Context.restrictionsManager
        get() = systemService<RestrictionsManager>()

    @Given
    val Context.telecomManager
        get() = systemService<TelecomManager>()

    @Given
    val Context.tvInputManager
        get() = systemService<TvInputManager>()

    @Given
    val Context.subscriptionManager
        get() = systemService<SubscriptionManager>()

    @Given
    val Context.usageStatsManager
        get() = systemService<UsageStatsManager>()

    @Given
    val Context.carrierConfigManager
        get() = systemService<CarrierConfigManager>()

    @Given
    val Context.fingerprintManager
        get() = systemService<FingerprintManager>()

    @Given
    val Context.midiManager
        get() = systemService<MidiManager>()

    @Given
    val Context.networkStatsManager
        get() = systemService<NetworkStatsManager>()

    @Given
    val Context.hardwarePropertiesManager
        get() = systemService<HardwarePropertiesManager>()

    @Given
    val Context.systemHealthManager
        get() = systemService<SystemHealthManager>()

    @Given
    val Context.shortcutManager
        get() = systemService<ShortcutManager>()

    private inline fun <reified T : Any> Context.systemService() =
        ContextCompat.getSystemService(this, T::class.java)!!
}
