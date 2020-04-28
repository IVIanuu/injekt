package com.ivianuu.injekt.android

import android.app.Service
import com.ivianuu.injekt.ComponentDsl
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.keyOf

fun ComponentDsl.serviceModule(instance: Service) {
    instance(instance, Key.SimpleKey(instance::class))
    alias(Key.SimpleKey(instance::class), keyOf<Service>())
    context(instance, ForService::class)
}

@Scope
annotation class ServiceScoped

@Qualifier
annotation class ForService
