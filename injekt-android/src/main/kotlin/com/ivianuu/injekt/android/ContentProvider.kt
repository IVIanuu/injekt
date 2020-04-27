package com.ivianuu.injekt.android

import android.content.ContentProvider
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.keyOf

fun ContentProviderModule(instance: ContentProvider) = Module {
    instance(instance, Key.SimpleKey(instance::class))
    alias(Key.SimpleKey(instance::class), keyOf<ContentProvider>())
    include(ContextModule(instance.context!!, ForContentProvider::class))
}

@Scope
annotation class ContentProviderScoped

@Qualifier
annotation class ForContentProvider
