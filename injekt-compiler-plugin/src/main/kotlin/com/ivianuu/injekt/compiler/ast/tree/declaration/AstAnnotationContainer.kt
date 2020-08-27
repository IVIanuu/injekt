package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstClassId
import com.ivianuu.injekt.compiler.ast.tree.expression.AstCall

interface AstAnnotationContainer {
    val annotations: MutableList<AstCall>
}

fun AstAnnotationContainer.hasAnnotation(classId: AstClassId): Boolean =
    TODO()//annotations.any { it.callee.returnType.classIdOrFail == classId }
