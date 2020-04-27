package com.ivianuu.injekt.android

import android.app.Service
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.keyOf

fun ServiceModule(instance: Service) = Module {
    instance(instance, Key.SimpleKey(instance::class))
    alias(Key.SimpleKey(instance::class), keyOf<Service>())
    include(ContextModule(instance, ForService::class))
}

@Scope
annotation class ServiceScoped

@Qualifier
annotation class ForService
