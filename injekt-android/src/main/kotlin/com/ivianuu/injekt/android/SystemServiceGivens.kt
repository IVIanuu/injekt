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
import com.ivianuu.injekt.Reader
import com.ivianuu.injekt.given

object SystemServiceGivens {

    @Given
    fun accessibilityManager() = systemService<AccessibilityManager>()

    @Given
    fun accountManager() = systemService<AccountManager>()

    @Given
    fun activityManager() = systemService<ActivityManager>()

    @Given
    fun alarmManager() = systemService<AlarmManager>()

    @Given
    fun audioManager() = systemService<AudioManager>()

    @Given
    fun clipboardManager() = systemService<ClipboardManager>()

    @Given
    fun connectivityManager() = systemService<ConnectivityManager>()

    @Given
    fun devicePolicyManager() = systemService<DevicePolicyManager>()

    @Given
    fun downloadManager() = systemService<DownloadManager>()

    @Given
    fun dropBoxManager() = systemService<DropBoxManager>()

    @Given
    fun inputMethodManager() = systemService<InputMethodManager>()

    @Given
    fun keyguardManager() = systemService<KeyguardManager>()

    @Given
    fun layoutInflater() = systemService<LayoutInflater>()

    @Given
    fun locationManager() = systemService<LocationManager>()

    @Given
    fun nfcManager() = systemService<NfcManager>()

    @Given
    fun notificationManager() = systemService<NotificationManager>()

    @Given
    fun powerManager() = systemService<PowerManager>()

    @Given
    fun searchManager() = systemService<SearchManager>()

    @Given
    fun sensorManager() = systemService<SensorManager>()

    @Given
    fun storageManager() = systemService<StorageManager>()

    @Given
    fun telephonyManager() = systemService<TelephonyManager>()

    @Given
    fun textServicesManager() = systemService<TextServicesManager>()

    @Given
    fun uiModeManager() = systemService<UiModeManager>()

    @Given
    fun usbManager() = systemService<UsbManager>()

    @Given
    fun vibrator() = systemService<Vibrator>()

    @Given
    fun wallpaperManager() = systemService<WallpaperManager>()

    @Given
    fun wifiP2pManager() = systemService<WifiP2pManager>()

    @Given
    fun wifiManager() = systemService<WifiManager>()

    @Given
    fun windowManager() = systemService<WindowManager>()

    @Given
    fun inputManager() = systemService<InputManager>()

    @Given
    fun mediaRouter() = systemService<MediaRouter>()

    @Given
    fun nsdManager() = systemService<NsdManager>()

    @Given
    fun displayManager() = systemService<DisplayManager>()

    @Given
    fun userManager() = systemService<UserManager>()

    @Given
    fun bluetoothManager() = systemService<BluetoothManager>()

    @Given
    fun appOpsManager() = systemService<AppOpsManager>()

    @Given
    fun captioningManager() = systemService<CaptioningManager>()

    @Given
    fun consumerIrManager() = systemService<ConsumerIrManager>()

    @Given
    fun printManager() = systemService<PrintManager>()

    @Given
    fun appWidgetManager() = systemService<AppWidgetManager>()

    @Given
    fun batteryManager() = systemService<BatteryManager>()

    @Given
    fun cameraManager() = systemService<CameraManager>()

    @Given
    fun jobScheduler() = systemService<JobScheduler>()

    @Given
    fun launcherApps() = systemService<LauncherApps>()

    @Given
    fun mediaProjectionManager() = systemService<MediaProjectionManager>()

    @Given
    fun mediaSessionManager() = systemService<MediaSessionManager>()

    @Given
    fun restrictionsManager() = systemService<RestrictionsManager>()

    @Given
    fun telecomManager() = systemService<TelecomManager>()

    @Given
    fun tvInputManager() = systemService<TvInputManager>()

    @Given
    fun subscriptionManager() = systemService<SubscriptionManager>()

    @Given
    fun usageStatsManager() = systemService<UsageStatsManager>()

    @Given
    fun carrierConfigManager() = systemService<CarrierConfigManager>()

    @Given
    fun fingerprintManager() = systemService<FingerprintManager>()

    @Given
    fun midiManager() = systemService<MidiManager>()

    @Given
    fun networkStatsManager() = systemService<NetworkStatsManager>()

    @Given
    fun hardwarePropertiesManager() = systemService<HardwarePropertiesManager>()

    @Given
    fun systemHealthManager() = systemService<SystemHealthManager>()

    @Given
    fun shortcutManager() = systemService<ShortcutManager>()

    @Reader
    private inline fun <reified T : Any> systemService() =
        ContextCompat.getSystemService(given<ApplicationAndroidContext>(), T::class.java)!!

}
