package com.ivianuu.injekt.android

import android.app.Service
import com.ivianuu.injekt.ComponentDsl
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.instanceKeyOf
import com.ivianuu.injekt.keyOf

fun ComponentDsl.serviceModule(instance: Service) {
    val instanceKey = instanceKeyOf(instance)
    instance(instance, instanceKey)
    alias(instanceKey, keyOf<Service>())
    context(instanceKey, ForService::class)
}

@Scope
annotation class ServiceScoped

@Qualifier
annotation class ForService
