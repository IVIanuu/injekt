package com.ivianuu.injekt.compiler

data class Callable(
    val packageFqName: String,
    val fqName: String,
    val name: String,
    val type: TypeRef,
    val typeParameters: List<ClassifierRef>,
    val valueParameters: List<ValueParameterRef>,
    val targetComponent: TypeRef?,
    val contributionKind: ContributionKind?,
    val isCall: Boolean,
    val isSuspend: Boolean,
    val isExternal: Boolean
) {
    enum class ContributionKind {
        BINDING, MAP_ENTRIES, SET_ELEMENTS, MODULE
    }
}

data class ValueParameterRef(
    val type: TypeRef,
    val isExtensionReceiver: Boolean = false,
    val isAssisted: Boolean = false,
    val name: String,
)

data class ModuleDescriptor(
    val type: TypeRef,
    val callables: List<Callable>,
)
