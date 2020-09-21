package com.ivianuu.injekt.compiler.generator

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.KotlinType

sealed class TypeRef

data class KotlinTypeRef(val kotlinType: KotlinType) : TypeRef()

data class FqNameTypeRef(val fqName: FqName) : TypeRef()
