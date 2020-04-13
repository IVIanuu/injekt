package com.ivianuu.injekt

@BehaviorMarker
fun BindAs(key: Key<*>) = InterceptingBehavior(
    name = "BindAs($key)"
) { it.copy(key = key as Key<Any?>) }
