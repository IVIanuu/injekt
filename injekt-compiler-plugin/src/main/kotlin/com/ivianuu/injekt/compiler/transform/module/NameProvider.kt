package com.ivianuu.injekt.compiler.transform.module

class NameProvider {
    private val indicesByGroup = mutableMapOf<String, Int>()
    fun allocate(group: String): String {
        val index = indicesByGroup[group] ?: 0
        indicesByGroup[group] = index + 1
        return "${group}_$index"
    }
}
