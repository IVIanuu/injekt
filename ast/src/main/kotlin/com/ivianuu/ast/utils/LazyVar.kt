package com.ivianuu.ast.utils

import kotlin.properties.ReadWriteProperty
import kotlin.reflect.KProperty

fun <T> lazyVar(init: () -> T): ReadWriteProperty<Any?, T> {
    return object : ReadWriteProperty<Any?, T> {
        private var _value: Any? = this
        override fun getValue(thisRef: Any?, property: KProperty<*>): T {
            if (_value === this) {
                _value = init()
            }
            return _value as T
        }

        override fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
            _value = value
        }
    }
}
