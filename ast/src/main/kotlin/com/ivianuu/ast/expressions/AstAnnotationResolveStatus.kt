package com.ivianuu.ast.expressions

enum class AstAnnotationResolveStatus {
    Unresolved,
    PartiallyResolved, // only literals, annotations, class literals and enums
    Resolved
}