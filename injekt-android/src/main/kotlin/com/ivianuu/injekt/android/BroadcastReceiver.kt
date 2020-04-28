package com.ivianuu.injekt.android

import android.content.BroadcastReceiver
import android.content.Context
import com.ivianuu.injekt.ComponentDsl
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.instanceKeyOf
import com.ivianuu.injekt.keyOf

fun ComponentDsl.receiver(context: Context, instance: BroadcastReceiver) {
    instance(instance, instanceKeyOf(instance))
    alias(instanceKeyOf(instance), keyOf())
    factory(ForReceiver::class) { context }
    factory(ForReceiver::class) { context.resources!! }
}

@Scope
annotation class ReceiverScoped

@Qualifier
annotation class ForReceiver
