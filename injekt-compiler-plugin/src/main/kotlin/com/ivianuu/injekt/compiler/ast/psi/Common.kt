package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.tree.AstExpectActual
import com.ivianuu.injekt.compiler.ast.tree.AstModality
import com.ivianuu.injekt.compiler.ast.tree.AstVariance
import com.ivianuu.injekt.compiler.ast.tree.AstVisibility
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyGetterDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.types.Variance

fun FunctionDescriptor.toAstFunctionKind() = when (this) {
    is ConstructorDescriptor -> AstFunction.Kind.CONSTRUCTOR
    is PropertyGetterDescriptor -> AstFunction.Kind.PROPERTY_GETTER
    is PropertySetterDescriptor -> AstFunction.Kind.PROPERTY_SETTER
    is SimpleFunctionDescriptor -> AstFunction.Kind.SIMPLE_FUNCTION
    else -> error("Unexpected function $this")
}

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
    else -> AstVisibility.PUBLIC
}

fun ClassKind.toAstClassKind() = when (this) {
    ClassKind.CLASS -> AstClass.Kind.CLASS
    ClassKind.INTERFACE -> AstClass.Kind.INTERFACE
    ClassKind.ENUM_CLASS -> AstClass.Kind.ENUM_CLASS
    ClassKind.ENUM_ENTRY -> AstClass.Kind.ENUM_ENTRY
    ClassKind.ANNOTATION_CLASS -> AstClass.Kind.ANNOTATION
    ClassKind.OBJECT -> AstClass.Kind.OBJECT
}

fun Variance.toAstVariance() = when (this) {
    Variance.INVARIANT -> null
    Variance.IN_VARIANCE -> AstVariance.IN
    Variance.OUT_VARIANCE -> AstVariance.OUT
}

fun expectActualOf(
    isActual: Boolean,
    isExpect: Boolean
): AstExpectActual? = when {
    isActual -> AstExpectActual.ACTUAL
    isExpect -> AstExpectActual.EXPECT
    else -> null
}
