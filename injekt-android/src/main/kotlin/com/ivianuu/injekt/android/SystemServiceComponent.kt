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
import com.ivianuu.injekt.merge.ApplicationComponent
import com.ivianuu.injekt.merge.MergeInto

@MergeInto(ApplicationComponent::class)
object SystemServiceComponent {
    @Binding
    val Context.accessibilityManager
        get() = systemService<AccessibilityManager>()

    @Binding
    val Context.accountManager
        get() = systemService<AccountManager>()

    @Binding
    val Context.activityManager
        get() = systemService<ActivityManager>()

    @Binding
    val Context.alarmManager
        get() = systemService<AlarmManager>()

    @Binding
    val Context.audioManager
        get() = systemService<AudioManager>()

    @Binding
    val Context.clipboardManager
        get() = systemService<ClipboardManager>()

    @Binding
    val Context.connectivityManager
        get() = systemService<ConnectivityManager>()

    @Binding
    val Context.devicePolicyManager
        get() = systemService<DevicePolicyManager>()

    @Binding
    val Context.downloadManager
        get() = systemService<DownloadManager>()

    @Binding
    val Context.dropBoxManager
        get() = systemService<DropBoxManager>()

    @Binding
    val Context.inputMethodManager
        get() = systemService<InputMethodManager>()

    @Binding
    val Context.keyguardManager
        get() = systemService<KeyguardManager>()

    @Binding
    val Context.layoutInflater
        get() = systemService<LayoutInflater>()

    @Binding
    val Context.locationManager
        get() = systemService<LocationManager>()

    @Binding
    val Context.nfcManager
        get() = systemService<NfcManager>()

    @Binding
    val Context.notificationManager
        get() = systemService<NotificationManager>()

    @Binding
    val Context.powerManager
        get() = systemService<PowerManager>()

    @Binding
    val Context.searchManager
        get() = systemService<SearchManager>()

    @Binding
    val Context.sensorManager
        get() = systemService<SensorManager>()

    @Binding
    val Context.storageManager
        get() = systemService<StorageManager>()

    @Binding
    val Context.telephonyManager
        get() = systemService<TelephonyManager>()

    @Binding
    val Context.textServicesManager
        get() = systemService<TextServicesManager>()

    @Binding
    val Context.uiModeManager
        get() = systemService<UiModeManager>()

    @Binding
    val Context.usbManager
        get() = systemService<UsbManager>()

    @Binding
    val Context.vibrator
        get() = systemService<Vibrator>()

    @Binding
    val Context.wallpaperManager
        get() = systemService<WallpaperManager>()

    @Binding
    val Context.wifiP2pManager
        get() = systemService<WifiP2pManager>()

    @Binding
    val Context.wifiManager
        get() = systemService<WifiManager>()

    @Binding
    val Context.windowManager
        get() = systemService<WindowManager>()

    @Binding
    val Context.inputManager
        get() = systemService<InputManager>()

    @Binding
    val Context.mediaRouter
        get() = systemService<MediaRouter>()

    @Binding
    val Context.nsdManager
        get() = systemService<NsdManager>()

    @Binding
    val Context.displayManager
        get() = systemService<DisplayManager>()

    @Binding
    val Context.userManager
        get() = systemService<UserManager>()

    @Binding
    val Context.bluetoothManager
        get() = systemService<BluetoothManager>()

    @Binding
    val Context.appOpsManager
        get() = systemService<AppOpsManager>()

    @Binding
    val Context.captioningManager
        get() = systemService<CaptioningManager>()

    @Binding
    val Context.consumerIrManager
        get() = systemService<ConsumerIrManager>()

    @Binding
    val Context.printManager
        get() = systemService<PrintManager>()

    @Binding
    val Context.appWidgetManager
        get() = systemService<AppWidgetManager>()

    @Binding
    val Context.batteryManager
        get() = systemService<BatteryManager>()

    @Binding
    val Context.cameraManager
        get() = systemService<CameraManager>()

    @Binding
    val Context.jobScheduler
        get() = systemService<JobScheduler>()

    @Binding
    val Context.launcherApps
        get() = systemService<LauncherApps>()

    @Binding
    val Context.mediaProjectionManager
        get() = systemService<MediaProjectionManager>()

    @Binding
    val Context.mediaSessionManager
        get() = systemService<MediaSessionManager>()

    @Binding
    val Context.restrictionsManager
        get() = systemService<RestrictionsManager>()

    @Binding
    val Context.telecomManager
        get() = systemService<TelecomManager>()

    @Binding
    val Context.tvInputManager
        get() = systemService<TvInputManager>()

    @Binding
    val Context.subscriptionManager
        get() = systemService<SubscriptionManager>()

    @Binding
    val Context.usageStatsManager
        get() = systemService<UsageStatsManager>()

    @Binding
    val Context.carrierConfigManager
        get() = systemService<CarrierConfigManager>()

    @Binding
    val Context.fingerprintManager
        get() = systemService<FingerprintManager>()

    @Binding
    val Context.midiManager
        get() = systemService<MidiManager>()

    @Binding
    val Context.networkStatsManager
        get() = systemService<NetworkStatsManager>()

    @Binding
    val Context.hardwarePropertiesManager
        get() = systemService<HardwarePropertiesManager>()

    @Binding
    val Context.systemHealthManager
        get() = systemService<SystemHealthManager>()

    @Binding
    val Context.shortcutManager
        get() = systemService<ShortcutManager>()

    private inline fun <reified T : Any> Context.systemService() =
        ContextCompat.getSystemService(this, T::class.java)!!
}
