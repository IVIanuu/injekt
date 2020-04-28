package com.ivianuu.injekt.android

import android.content.BroadcastReceiver
import android.content.Context
import com.ivianuu.injekt.ComponentDsl
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.keyOf

fun ComponentDsl.receiver(context: Context, instance: BroadcastReceiver) {
    instance(instance, Key.SimpleKey(instance::class))
    alias(Key.SimpleKey(instance::class), keyOf<BroadcastReceiver>())
    context(context, ForReceiver::class)
}

@Scope
annotation class ReceiverScoped

@Qualifier
annotation class ForReceiver
