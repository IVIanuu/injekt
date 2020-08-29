package com.ivianuu.injekt.compiler.ast.tree.declaration

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.expression.AstQualifiedAccess
import org.jetbrains.kotlin.name.FqName

interface AstAnnotationContainer : AstElement {
    val annotations: MutableList<AstQualifiedAccess>
}

fun AstAnnotationContainer.hasAnnotation(fqName: FqName): Boolean =
    annotations.any {
        ((it.callee as AstConstructor).constructedClass).fqName == fqName
    }
