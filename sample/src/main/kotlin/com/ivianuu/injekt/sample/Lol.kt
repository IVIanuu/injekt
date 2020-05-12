package com.ivianuu.injekt.sample

import com.ivianuu.injekt.Module
import com.ivianuu.injekt.instance

fun inlined(block: @Module (String, Int) -> Unit) {
    block("", 0)
}

class InlinedImpl

fun calling(instance: Any) {
    inlined { _, _ -> instance(instance) }
    @Module
    fun lambdaLocal(p0: String, p1: Int, instance: Any) {
        instance(instance)
    }
    lambdaLocal(null, null, instance)
}

class LambdaLocalImpl(p0: String, p1: Int, instance: Any) {
    val instance_0 = instance
}

class CallingImpl(instance: Any) {
    init {
        inlined { _, _ -> instance(instance) }
        lambdaLocal(null, null)
    }
}