package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.generator.ContextFactoryDescriptor

@Given(ApplicationContext::class)
class InjektAttributes {
    private val map = mutableMapOf<Any, Any>()
    operator fun <K : Key<V>, V : Any> get(key: K): V? = map[key] as? V
    operator fun <K : Key<V>, V : Any> set(key: K, value: V) {
        map[key] = value
    }

    interface Key<V : Any>

    data class ContextFactoryKey(val filePath: String, val startOffset: Int) :
        Key<ContextFactoryDescriptor>
}
