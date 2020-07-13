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

object SystemServiceModule {

    @Given
    @Reader
    fun accessibilityManager() = systemService<AccessibilityManager>()

    @Given
    @Reader
    fun accountManager() = systemService<AccountManager>()

    @Given
    @Reader
    fun activityManager() = systemService<ActivityManager>()

    @Given
    @Reader
    fun alarmManager() = systemService<AlarmManager>()

    @Given
    @Reader
    fun audioManager() = systemService<AudioManager>()

    @Given
    @Reader
    fun clipboardManager() = systemService<ClipboardManager>()

    @Given
    @Reader
    fun connectivityManager() = systemService<ConnectivityManager>()

    @Given
    @Reader
    fun devicePolicyManager() = systemService<DevicePolicyManager>()

    @Given
    @Reader
    fun downloadManager() = systemService<DownloadManager>()

    @Given
    @Reader
    fun dropBoxManager() = systemService<DropBoxManager>()

    @Given
    @Reader
    fun inputMethodManager() = systemService<InputMethodManager>()

    @Given
    @Reader
    fun keyguardManager() = systemService<KeyguardManager>()

    @Given
    @Reader
    fun layoutInflater() = systemService<LayoutInflater>()

    @Given
    @Reader
    fun locationManager() = systemService<LocationManager>()

    @Given
    @Reader
    fun nfcManager() = systemService<NfcManager>()

    @Given
    @Reader
    fun notificationManager() = systemService<NotificationManager>()

    @Given
    @Reader
    fun powerManager() = systemService<PowerManager>()

    @Given
    @Reader
    fun searchManager() = systemService<SearchManager>()

    @Given
    @Reader
    fun sensorManager() = systemService<SensorManager>()

    @Given
    @Reader
    fun storageManager() = systemService<StorageManager>()

    @Given
    @Reader
    fun telephonyManager() = systemService<TelephonyManager>()

    @Given
    @Reader
    fun textServicesManager() = systemService<TextServicesManager>()

    @Given
    @Reader
    fun uiModeManager() = systemService<UiModeManager>()

    @Given
    @Reader
    fun usbManager() = systemService<UsbManager>()

    @Given
    @Reader
    fun vibrator() = systemService<Vibrator>()

    @Given
    @Reader
    fun wallpaperManager() = systemService<WallpaperManager>()

    @Given
    @Reader
    fun wifiP2pManager() = systemService<WifiP2pManager>()

    @Given
    @Reader
    fun wifiManager() = systemService<WifiManager>()

    @Given
    @Reader
    fun windowManager() = systemService<WindowManager>()

    @Given
    @Reader
    fun inputManager() = systemService<InputManager>()

    @Given
    @Reader
    fun mediaRouter() = systemService<MediaRouter>()

    @Given
    @Reader
    fun nsdManager() = systemService<NsdManager>()

    @Given
    @Reader
    fun displayManager() = systemService<DisplayManager>()

    @Given
    @Reader
    fun userManager() = systemService<UserManager>()

    @Given
    @Reader
    fun bluetoothManager() = systemService<BluetoothManager>()

    @Given
    @Reader
    fun appOpsManager() = systemService<AppOpsManager>()

    @Given
    @Reader
    fun captioningManager() = systemService<CaptioningManager>()

    @Given
    @Reader
    fun consumerIrManager() = systemService<ConsumerIrManager>()

    @Given
    @Reader
    fun printManager() = systemService<PrintManager>()

    @Given
    @Reader
    fun appWidgetManager() = systemService<AppWidgetManager>()

    @Given
    @Reader
    fun batteryManager() = systemService<BatteryManager>()

    @Given
    @Reader
    fun cameraManager() = systemService<CameraManager>()

    @Given
    @Reader
    fun jobScheduler() = systemService<JobScheduler>()

    @Given
    @Reader
    fun launcherApps() = systemService<LauncherApps>()

    @Given
    @Reader
    fun mediaProjectionManager() = systemService<MediaProjectionManager>()

    @Given
    @Reader
    fun mediaSessionManager() = systemService<MediaSessionManager>()

    @Given
    @Reader
    fun restrictionsManager() = systemService<RestrictionsManager>()

    @Given
    @Reader
    fun telecomManager() = systemService<TelecomManager>()

    @Given
    @Reader
    fun tvInputManager() = systemService<TvInputManager>()

    @Given
    @Reader
    fun subscriptionManager() = systemService<SubscriptionManager>()

    @Given
    @Reader
    fun usageStatsManager() = systemService<UsageStatsManager>()

    @Given
    @Reader
    fun carrierConfigManager() = systemService<CarrierConfigManager>()

    @Given
    @Reader
    fun fingerprintManager() = systemService<FingerprintManager>()

    @Given
    @Reader
    fun midiManager() = systemService<MidiManager>()

    @Given
    @Reader
    fun networkStatsManager() = systemService<NetworkStatsManager>()

    @Given
    @Reader
    fun hardwarePropertiesManager() = systemService<HardwarePropertiesManager>()

    @Given
    @Reader
    fun systemHealthManager() = systemService<SystemHealthManager>()

    @Given
    @Reader
    fun shortcutManager() = systemService<ShortcutManager>()

    @Reader
    private inline fun <reified T : Any> systemService() =
        ContextCompat.getSystemService(given<ApplicationContext>(), T::class.java)!!

}
