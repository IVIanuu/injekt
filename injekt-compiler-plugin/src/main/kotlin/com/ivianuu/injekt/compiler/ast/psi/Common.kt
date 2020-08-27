package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.tree.AstClassId
import com.ivianuu.injekt.compiler.ast.tree.AstExpectActual
import com.ivianuu.injekt.compiler.ast.tree.AstModality
import com.ivianuu.injekt.compiler.ast.tree.AstVisibility
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.name.ClassId

fun ClassId.toAstClassId() = AstClassId(
    packageFqName,
    shortClassName
)

fun Modality.toAstModality() = when (this) {
    Modality.FINAL -> AstModality.FINAL
    Modality.SEALED -> AstModality.SEALED
    Modality.OPEN -> AstModality.OPEN
    Modality.ABSTRACT -> AstModality.ABSTRACT
}

fun Visibility.toAstVisibility() = when (this) {
    Visibilities.PUBLIC -> AstVisibility.PUBLIC
    Visibilities.INTERNAL -> AstVisibility.INTERNAL
    Visibilities.PROTECTED -> AstVisibility.PROTECTED
    Visibilities.PRIVATE -> AstVisibility.PRIVATE
    Visibilities.LOCAL -> AstVisibility.LOCAL
    else -> error("Unexpected visibility $this")
}

fun ClassKind.toAstClassKind() = when (this) {
    ClassKind.CLASS -> AstClass.Kind.CLASS
    ClassKind.INTERFACE -> AstClass.Kind.INTERFACE
    ClassKind.ENUM_CLASS -> AstClass.Kind.ENUM_CLASS
    ClassKind.ENUM_ENTRY -> AstClass.Kind.ENUM_ENTRY
    ClassKind.ANNOTATION_CLASS -> AstClass.Kind.ANNOTATION
    ClassKind.OBJECT -> AstClass.Kind.OBJECT
}

fun multiPlatformModalityOf(
    isActual: Boolean,
    isExpect: Boolean
): AstExpectActual? = when {
    isActual -> AstExpectActual.ACTUAL
    isExpect -> AstExpectActual.EXPECT
    else -> null
}
