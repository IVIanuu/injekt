package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

fun DeclarationDescriptor.uniqueKey(): String {
    return "$fqNameSafe"
}
