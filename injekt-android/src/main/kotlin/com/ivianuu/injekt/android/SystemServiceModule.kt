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
import com.ivianuu.injekt.InstallIn
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.classOf
import com.ivianuu.injekt.transient

@InstallIn<ApplicationComponent>
@Module
fun systemServiceModule() {
    systemService<AccessibilityManager>()
    systemService<AccountManager>()
    systemService<ActivityManager>()
    systemService<AlarmManager>()
    systemService<AudioManager>()
    systemService<ClipboardManager>()
    systemService<ConnectivityManager>()
    systemService<DevicePolicyManager>()
    systemService<DownloadManager>()
    systemService<DropBoxManager>()
    systemService<InputMethodManager>()
    systemService<KeyguardManager>()
    systemService<LayoutInflater>()
    systemService<LocationManager>()
    systemService<NfcManager>()
    systemService<NotificationManager>()
    systemService<PowerManager>()
    systemService<SearchManager>()
    systemService<SensorManager>()
    systemService<StorageManager>()
    systemService<TelephonyManager>()
    systemService<TextServicesManager>()
    systemService<UiModeManager>()
    systemService<UsbManager>()
    systemService<Vibrator>()
    systemService<WallpaperManager>()
    systemService<WifiP2pManager>()
    systemService<WifiManager>()
    systemService<WindowManager>()
    systemService<InputManager>()
    systemService<MediaRouter>()
    systemService<NsdManager>()
    systemService<DisplayManager>()
    systemService<UserManager>()
    systemService<BluetoothManager>()
    systemService<AppOpsManager>()
    systemService<CaptioningManager>()
    systemService<ConsumerIrManager>()
    systemService<PrintManager>()
    systemService<AppWidgetManager>()
    systemService<BatteryManager>()
    systemService<CameraManager>()
    systemService<JobScheduler>()
    systemService<LauncherApps>()
    systemService<MediaProjectionManager>()
    systemService<MediaSessionManager>()
    systemService<RestrictionsManager>()
    systemService<TelecomManager>()
    systemService<TvInputManager>()
    systemService<SubscriptionManager>()
    systemService<UsageStatsManager>()
    systemService<CarrierConfigManager>()
    systemService<FingerprintManager>()
    systemService<MidiManager>()
    systemService<NetworkStatsManager>()
    systemService<HardwarePropertiesManager>()
    systemService<SystemHealthManager>()
    systemService<ShortcutManager>()
}

@Module
private inline fun <T : Any> systemService() {
    val clazz = classOf<T>()
    transient<T> {
        ContextCompat.getSystemService(
            get<@ForApplication Context>(),
            clazz.java
        )!!
    }
}
