package com.ivianuu.injekt.android

import android.content.ContentProvider
import com.ivianuu.injekt.ComponentDsl
import com.ivianuu.injekt.Key
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.keyOf

fun ComponentDsl.contentProvider(instance: ContentProvider) {
    instance(instance, Key.SimpleKey(instance::class))
    alias(Key.SimpleKey(instance::class), keyOf<ContentProvider>())
    context(instance.context!!, ForContentProvider::class)
}

@Scope
annotation class ContentProviderScoped

@Qualifier
annotation class ForContentProvider
