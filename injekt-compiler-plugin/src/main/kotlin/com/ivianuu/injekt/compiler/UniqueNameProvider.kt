package com.ivianuu.injekt.compiler

import com.ivianuu.injekt.compiler.generator.removeIllegalChars

class UniqueNameProvider {

    private val existingNames = mutableSetOf<String>()

    operator fun invoke(base: String): String {
        val finalBase = base.removeIllegalChars()
        var name = finalBase
        var differentiator = 2
        while (name in existingNames) {
            name = finalBase + differentiator
            differentiator++
        }
        existingNames += name
        return name
    }
}
