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
import com.ivianuu.injekt.ApplicationComponent
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.Unscoped
import com.ivianuu.injekt.get

object SystemServiceModule {
    @Unscoped
    @Reader
    fun accessibilityManager() = systemService<AccessibilityManager>()

    @Unscoped
    @Reader
    fun accountManager() = systemService<AccountManager>()

    @Unscoped
    @Reader
    fun activityManager() = systemService<ActivityManager>()

    @Unscoped
    @Reader
    fun alarmManager() = systemService<AlarmManager>()

    @Unscoped
    @Reader
    fun audioManager() = systemService<AudioManager>()

    @Unscoped
    @Reader
    fun clipboardManager() = systemService<ClipboardManager>()

    @Unscoped
    @Reader
    fun connectivityManager() = systemService<ConnectivityManager>()

    @Unscoped
    @Reader
    fun devicePolicyManager() = systemService<DevicePolicyManager>()

    @Unscoped
    @Reader
    fun downloadManager() = systemService<DownloadManager>()

    @Unscoped
    @Reader
    fun dropBoxManager() = systemService<DropBoxManager>()

    @Unscoped
    @Reader
    fun inputMethodManager() = systemService<InputMethodManager>()

    @Unscoped
    @Reader
    fun keyguardManager() = systemService<KeyguardManager>()

    @Unscoped
    @Reader
    fun layoutInflater() = systemService<LayoutInflater>()

    @Unscoped
    @Reader
    fun locationManager() = systemService<LocationManager>()

    @Unscoped
    @Reader
    fun nfcManager() = systemService<NfcManager>()

    @Unscoped
    @Reader
    fun notificationManager() = systemService<NotificationManager>()

    @Unscoped
    @Reader
    fun powerManager() = systemService<PowerManager>()

    @Unscoped
    @Reader
    fun searchManager() = systemService<SearchManager>()

    @Unscoped
    @Reader
    fun sensorManager() = systemService<SensorManager>()

    @Unscoped
    @Reader
    fun storageManager() = systemService<StorageManager>()

    @Unscoped
    @Reader
    fun telephonyManager() = systemService<TelephonyManager>()

    @Unscoped
    @Reader
    fun textServicesManager() = systemService<TextServicesManager>()

    @Unscoped
    @Reader
    fun uiModeManager() = systemService<UiModeManager>()

    @Unscoped
    @Reader
    fun usbManager() = systemService<UsbManager>()

    @Unscoped
    @Reader
    fun vibrator() = systemService<Vibrator>()

    @Unscoped
    @Reader
    fun wallpaperManager() = systemService<WallpaperManager>()

    @Unscoped
    @Reader
    fun wifiP2pManager() = systemService<WifiP2pManager>()

    @Unscoped
    @Reader
    fun wifiManager() = systemService<WifiManager>()

    @Unscoped
    @Reader
    fun windowManager() = systemService<WindowManager>()

    @Unscoped
    @Reader
    fun inputManager() = systemService<InputManager>()

    @Unscoped
    @Reader
    fun mediaRouter() = systemService<MediaRouter>()

    @Unscoped
    @Reader
    fun nsdManager() = systemService<NsdManager>()

    @Unscoped
    @Reader
    fun displayManager() = systemService<DisplayManager>()

    @Unscoped
    @Reader
    fun userManager() = systemService<UserManager>()

    @Unscoped
    @Reader
    fun bluetoothManager() = systemService<BluetoothManager>()

    @Unscoped
    @Reader
    fun appOpsManager() = systemService<AppOpsManager>()

    @Unscoped
    @Reader
    fun captioningManager() = systemService<CaptioningManager>()

    @Unscoped
    @Reader
    fun consumerIrManager() = systemService<ConsumerIrManager>()

    @Unscoped
    @Reader
    fun printManager() = systemService<PrintManager>()

    @Unscoped
    @Reader
    fun appWidgetManager() = systemService<AppWidgetManager>()

    @Unscoped
    @Reader
    fun batteryManager() = systemService<BatteryManager>()

    @Unscoped
    @Reader
    fun cameraManager() = systemService<CameraManager>()

    @Unscoped
    @Reader
    fun jobScheduler() = systemService<JobScheduler>()

    @Unscoped
    @Reader
    fun launcherApps() = systemService<LauncherApps>()

    @Unscoped
    @Reader
    fun mediaProjectionManager() = systemService<MediaProjectionManager>()

    @Unscoped
    @Reader
    fun mediaSessionManager() = systemService<MediaSessionManager>()

    @Unscoped
    @Reader
    fun restrictionsManager() = systemService<RestrictionsManager>()

    @Unscoped
    @Reader
    fun telecomManager() = systemService<TelecomManager>()

    @Unscoped
    @Reader
    fun tvInputManager() = systemService<TvInputManager>()

    @Unscoped
    @Reader
    fun subscriptionManager() = systemService<SubscriptionManager>()

    @Unscoped
    @Reader
    fun usageStatsManager() = systemService<UsageStatsManager>()

    @Unscoped
    @Reader
    fun carrierConfigManager() = systemService<CarrierConfigManager>()

    @Unscoped
    @Reader
    fun fingerprintManager() = systemService<FingerprintManager>()

    @Unscoped
    @Reader
    fun midiManager() = systemService<MidiManager>()

    @Unscoped
    @Reader
    fun networkStatsManager() = systemService<NetworkStatsManager>()

    @Unscoped
    @Reader
    fun hardwarePropertiesManager() = systemService<HardwarePropertiesManager>()

    @Unscoped
    @Reader
    fun systemHealthManager() = systemService<SystemHealthManager>()

    @Unscoped
    @Reader
    fun shortcutManager() = systemService<ShortcutManager>()

    @Reader
    private inline fun <reified T : Any> systemService() =
        ContextCompat.getSystemService(get<ApplicationContext>(), T::class.java)!!

}
