/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.declarations

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.symbols.impl.AstRegularClassSymbol
import com.ivianuu.ast.types.AstSimpleType
import com.ivianuu.ast.types.AstType
import org.jetbrains.kotlin.name.ClassId

val AstClass<*>.classId get() = symbol.classId

val AstType.regularClassOrFail: AstRegularClassSymbol
    get() = regularClassOrNull ?: error("Could not get type for $this")

val AstType.regularClassOrNull: AstRegularClassSymbol?
    get() = (this as? AstSimpleType)?.classifier as? AstRegularClassSymbol

fun AstAnnotationContainer.hasAnnotation(classId: ClassId): Boolean {
    return annotations.any { it.callee.callableId.classId == classId }
}
