package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.expression.AstCall
import org.jetbrains.kotlin.name.FqName

interface AstAnnotationContainer {
    val annotations: MutableList<AstCall>
}

fun AstAnnotationContainer.hasAnnotation(fqName: FqName): Boolean =
    TODO()//annotations.any { it.callee.returnType.classIdOrFail == classId }
