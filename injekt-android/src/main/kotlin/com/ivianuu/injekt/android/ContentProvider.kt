package com.ivianuu.injekt.android

import android.content.ContentProvider
import com.ivianuu.injekt.ComponentDsl
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.Scope
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.factory
import com.ivianuu.injekt.instance
import com.ivianuu.injekt.instanceKeyOf
import com.ivianuu.injekt.keyOf

fun ComponentDsl.contentProvider(instance: ContentProvider) {
    instance(instance, instanceKeyOf(instance))
    alias(instanceKeyOf(instance), keyOf<ContentProvider>())
    factory(ForContentProvider::class) { instance.context!! }
    factory(ForContentProvider::class) { instance.context!!.resources!! }
}

@Scope
annotation class ContentProviderScoped

@Qualifier
annotation class ForContentProvider
