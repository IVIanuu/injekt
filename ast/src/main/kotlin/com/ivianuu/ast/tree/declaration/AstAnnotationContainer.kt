package com.ivianuu.ast.tree.declaration

import com.ivianuu.ast.tree.AstElement
import com.ivianuu.ast.tree.expression.AstQualifiedAccess
import com.ivianuu.ast.tree.type.classOrFail
import org.jetbrains.kotlin.name.FqName

interface AstAnnotationContainer : AstElement {
    val annotations: MutableList<AstQualifiedAccess>
}

fun AstAnnotationContainer.hasAnnotation(fqName: FqName): Boolean =
    annotations.any {
        (it.callee as AstFunction).returnType.classOrFail.fqName == fqName
    }
