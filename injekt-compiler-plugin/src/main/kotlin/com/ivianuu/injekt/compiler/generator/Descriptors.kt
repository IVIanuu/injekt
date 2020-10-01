package com.ivianuu.injekt.compiler.generator

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

data class Callable(
    val packageFqName: FqName,
    val fqName: FqName,
    val name: Name,
    val type: TypeRef,
    val typeParameters: List<ClassifierRef>,
    val valueParameters: List<ValueParameterRef>,
    val targetComponent: TypeRef?,
    val givenKind: GivenKind?,
    val isCall: Boolean,
    val isSuspend: Boolean
) {
    enum class GivenKind {
        GIVEN, GIVEN_MAP_ENTRIES, GIVEN_SET_ELEMENTS, MODULE
    }
}

data class ValueParameterRef(
    val type: TypeRef,
    val isExtensionReceiver: Boolean = false,
    val isAssisted: Boolean = false,
    val name: Name,
)

data class ModuleDescriptor(
    val type: TypeRef,
    val callables: List<Callable>,
)
