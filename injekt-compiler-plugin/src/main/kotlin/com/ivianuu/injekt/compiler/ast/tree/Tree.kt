package com.ivianuu.injekt.compiler.ast.tree

/*
interface AstTypeParameterContainer {
    val typeParameters: MutableList<AstTypeParameter>
}

class AstTypeParameter(
    var isReified: Boolean = false
) : AstDeclaration

val AstType.classIdOrFail: AstClassId get() = TODO()

class AstCall(
    var callee: AstFunction,
    var arguments: List<AstExpression>
) : AstExpression

class AstGetValueParameter(var valueParameter: AstValueParameter) : AstExpression()

val AstClass.defaultType: AstType get() = TODO()

sealed class AstFunction : AstDeclaration(), AstAnnotationContainer,
    AstDeclarationParent,
    AstVisibilityOwner,
    AstExpectActualDeclaration,
    AstTypeParameterContainer {
    override var annotations: List<AstCall> = emptyList()

    abstract var callableId: AstCallableId

    override var typeParameters: List<AstTypeParameter> = emptyList()
    var valueParameters: List<AstValueParameter> = emptyList()

    abstract var returnType: AstType

    var body: AstBody? = null
}

class AstSimpleFunction(
    override var callableId: AstCallableId,
    override var visibility: AstVisibility = AstVisibility.PUBLIC,
    override var expectActual: AstExpectActual? = null,
    override var modality: AstModality = AstModality.FINAL,
    override var returnType: AstType,
    var isInfix: Boolean = false,
    var isOperator: Boolean = false,
    var isTailrec: Boolean = false,
    var isSuspend: Boolean = false
) : AstFunction(), AstModalityOwner {
    var overriddenFunctions: List<AstSimpleFunction> = emptyList()
}

class AstConstructor(
    override var callableId: AstCallableId,
    override var visibility: AstVisibility = AstVisibility.PUBLIC,
    override var expectActual: AstExpectActual? = null,
    override var returnType: AstType,
    var isPrimary: Boolean = false
) : AstFunction()

class AstValueParameter(
    var name: Name,
    var type: AstType,
    var inlineHint: InlineHint? = null,
    var isVararg: Boolean = false
) : AstDeclaration(), AstAnnotationContainer {
    override var annotations: List<AstCall> = emptyList()

    enum class InlineHint {
        INLINE,
        NOINLINE,
        CROSSINLINE
    }
}

class AstBody(
    var type: AstType
) : AstElement() {
    var statements: List<AstElement> = emptyList()
}
*/