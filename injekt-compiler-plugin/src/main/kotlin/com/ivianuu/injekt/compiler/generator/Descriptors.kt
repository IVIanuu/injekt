package com.ivianuu.injekt.compiler.generator

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

data class ContextFactoryDescriptor(
    val factoryType: TypeRef,
    val contextType: TypeRef,
    val inputTypes: List<TypeRef>
)

data class ContextFactoryImplDescriptor(
    val factoryImplFqName: FqName,
    val factory: ContextFactoryDescriptor
)

data class CallableRef(
    val packageFqName: FqName,
    val fqName: FqName,
    val name: Name,
    val uniqueKey: String,
    val typeRef: TypeRef,
    val receiver: TypeRef?,
    val parameters: List<ParameterRef>,
    val targetContext: TypeRef?,
    val isExternal: Boolean,
    val isPropertyAccessor: Boolean
)

data class ParameterRef(
    val typeRef: TypeRef,
    val isExtensionReceiver: Boolean = false
)
