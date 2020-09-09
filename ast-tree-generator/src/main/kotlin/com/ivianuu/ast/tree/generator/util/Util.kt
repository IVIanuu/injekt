package com.ivianuu.ast.tree.generator.util

import com.ivianuu.ast.tree.generator.model.Element

fun Element.traverseParents(block: (Element) -> Unit) {
    block(this)
    parents.forEach { it.traverseParents(block) }
}

operator fun <K, V, U> MutableMap<K, MutableMap<V, U>>.set(k1: K, k2: V, value: U) {
    this.putIfAbsent(k1, mutableMapOf())
    val map = getValue(k1)
    map[k2] = value
}

operator fun <K, V, U> Map<K, Map<V, U>>.get(k1: K, k2: V): U {
    return getValue(k1).getValue(k2)
}