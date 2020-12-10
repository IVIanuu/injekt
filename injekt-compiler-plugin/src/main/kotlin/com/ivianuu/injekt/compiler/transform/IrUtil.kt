package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.resolution.TypeRef
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.TypeProjectionImpl
import org.jetbrains.kotlin.types.replace

fun TypeRef.toIrType(pluginContext: IrPluginContext): IrType =
    pluginContext.typeTranslator.translateType(toKotlinType())

fun TypeRef.toKotlinType(): KotlinType {
    val defaultType = classifier.descriptor!!.defaultType
    return defaultType
        .replace(newArguments = typeArguments.map {
            TypeProjectionImpl(
                it.variance,
                it.toKotlinType()
            )
        })
        .makeNullableAsSpecified(isMarkedNullable)
}
