package com.ivianuu.ast.symbols.impl

import java.lang.ref.WeakReference

class OneElementWeakMap<K, V> private constructor(
    private val keyReference: WeakReference<K>,
    private val valueReference: WeakReference<V>
) {
    constructor(key: K, value: V) : this(WeakReference(key), WeakReference(value))

    val key: K?
        get() = keyReference.get()

    val value: V?
        get() = valueReference.get()
}
