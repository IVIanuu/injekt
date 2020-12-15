package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import org.jetbrains.kotlin.backend.common.descriptors.isSuspend
import org.jetbrains.kotlin.descriptors.CallableDescriptor

enum class CallContext {
    DEFAULT, COMPOSABLE, SUSPEND
}

fun CallContext.canCall(other: CallContext) =
    this == other || other == CallContext.DEFAULT

val CallableDescriptor.callContext: CallContext
    get() = when {
        isSuspend -> CallContext.SUSPEND
        hasAnnotation(InjektFqNames.Composable) -> CallContext.COMPOSABLE
        else -> CallContext.DEFAULT
    }

val TypeRef.callContext: CallContext
    get() = when {
        isComposable -> CallContext.COMPOSABLE
        classifier.fqName.asString()
            .startsWith("kotlin.coroutines.SuspendFunction") -> CallContext.SUSPEND
        else -> CallContext.DEFAULT
    }
