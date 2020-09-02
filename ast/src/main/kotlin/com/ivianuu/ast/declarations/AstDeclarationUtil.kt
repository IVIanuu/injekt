/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.declarations

import com.ivianuu.ast.declarations.impl.AstFileImpl
import com.ivianuu.ast.declarations.impl.AstModuleFragmentImpl
import com.ivianuu.ast.declarations.impl.AstRegularClassImpl
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.types.AstSimpleType
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.ClassId

val AstClass<*>.classId get() = symbol.classId

fun AstFile.addDeclaration(declaration: AstDeclaration) {
    require(this is AstFileImpl)
    declarations += declaration
}

fun AstRegularClass.addDeclaration(declaration: AstDeclaration) {
    @Suppress("LiftReturnOrAssignment")
    when (this) {
        is AstRegularClassImpl -> declarations += declaration
        else -> throw IllegalStateException()
    }
}

fun AstModuleFragment.addFile(file: AstFile) {
    @Suppress("LiftReturnOrAssignment")
    when (this) {
        is AstModuleFragmentImpl -> files += file
        else -> throw IllegalStateException()
    }
}

val AstType.regularClassOrFail: AstRegularClassSymbol
    get() = regularClassOrNull ?: error("Could not get type for $this")

val AstType.regularClassOrNull: AstRegularClassSymbol?
    get() = (this as? AstSimpleType)?.classifier as? AstRegularClassSymbol

fun AstAnnotatedDeclaration.hasAnnotation(classId: ClassId): Boolean {
    return annotations.any { it.callee.callableId.classId == classId }
}
