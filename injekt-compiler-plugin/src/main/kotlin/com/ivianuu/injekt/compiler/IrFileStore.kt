package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.Given

@Given(ApplicationContext::class)
class IrFileStore {
    val map = mutableMapOf<String, String>()
    fun put(key: String, value: String) {
        map[key] = value
    }

    fun get(key: String) = map[key]
    fun clear() {
        map.clear()
    }
}
