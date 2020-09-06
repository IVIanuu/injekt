/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.impl.AstClassifierSymbol
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.types.AstSimpleType
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.builder.buildSimpleType
import com.ivianuu.ast.types.builder.buildTypeProjectionWithVariance
import com.ivianuu.ast.types.typeWith
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.name.ClassId

val AstClass<*>.defaultType: AstType get() = context.buildSimpleType {
    classifier = symbol
    if (this@defaultType is AstRegularClass) {
        arguments += typeParameters.map { it.defaultType.toTypeProjection() }
    }
}

fun AstClass<*>.typeWith(arguments: List<AstTypeProjection>) =
    defaultType.typeWith(arguments)

@JvmName("typeWithTypes")
fun AstClass<*>.typeWith(arguments: List<AstType>) =
    defaultType.typeWith(arguments)

fun AstType.toTypeProjection() = context.buildTypeProjectionWithVariance {
    type = this@toTypeProjection
}

val AstTypeParameter.defaultType: AstType get() = context.buildSimpleType {
    classifier = symbol
}

val AstClass<*>.classId get() = symbol.classId

val AstType.regularClassOrFail: AstRegularClassSymbol
    get() = regularClassOrNull ?: error("Could not get regular class for $this")
val AstType.regularClassOrNull: AstRegularClassSymbol?
    get() = classifierOrNull as? AstRegularClassSymbol

val AstType.classifierOrFail: AstClassifierSymbol<*>
    get() = classifierOrNull ?: error("Could not get classifier for $this")
val AstType.classifierOrNull: AstClassifierSymbol<*>?
    get() = (this as? AstSimpleType)?.classifier as? AstClassifierSymbol

fun AstAnnotationContainer.hasAnnotation(classId: ClassId): Boolean {
    return annotations.any { it.callee.callableId.classId == classId }
}

fun AstModuleFragment.addFile(file: AstFile) {
    replaceFiles(files + file)
}

fun AstDeclarationContainer.addDeclaration(declaration: AstDeclaration) {
    replaceDeclarations(declarations + declaration)
}
