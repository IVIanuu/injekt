package com.ivianuu.injekt.compiler.ast

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class AstElement

interface AstAnnotationContainer {
    var annotations: List<AstCall>
}

enum class AstVisibility {
    PUBLIC, INTERNAL, PROTECTED, PRIVATE, LOCAL
}

interface AstVisibilityOwner {
    var visibility: AstVisibility
}

enum class AstModality {
    FINAL, OPEN, ABSTRACT, SEALED
}

interface AstModalityOwner {
    var modality: AstModality
}

// todo rename
enum class AstMultiPlatformModality {
    EXPECT, ACTUAL
}

// todo rename
interface AstMultiPlatformDeclaration {
    var multiPlatformModality: AstMultiPlatformModality?
}

interface AstTypeParameterContainer {
    var typeParameters: List<AstTypeParameter>
}

class AstTypeParameter(
    var isReified: Boolean = false
) : AstDeclaration()

enum class AstVariance {
    IN, OUT
}
/*
// todo IN OUT VARIANCE
interface AstType : AstAnnotationContainer {
    val classifier: AstClassifier
    val hasQuestionMark: Boolean
    val arguments: List<Astty>
    val abbreviation: AstTypeAbbreviation?
}*/

interface AstType : AstAnnotationContainer

val AstType.classIdOrFail: AstClassId get() = TODO()

/*
interface AstSimpleType {
    val classifier: IrClassifierSymbol
    val hasQuestionMark: Boolean
    val arguments: List<IrTypeArgument>
    val abbreviation: IrTypeAbbreviation?
}

interface AstTypeArgument

interface AstStarProjection : AstTypeArgument

interface AstTypeProjection : AstTypeArgument {
    val variance: Variance
    val type: IrType
}

interface IrTypeAbbreviation : IrAnnotationContainer {
    val typeAlias: IrTypeAliasSymbol
    val hasQuestionMark: Boolean
    val arguments: List<IrTypeArgument>
}

interface AstTypeAbbreviation
*/
interface AstDeclarationContainer : AstDeclarationParent {
    var declarations: List<AstDeclaration>
}

fun AstDeclarationContainer.addChild(declaration: AstDeclaration) {
    declarations += declaration
    declaration.parent = this
}

interface AstDeclarationParent

sealed class AstDeclaration : AstElement() {
    lateinit var parent: AstDeclarationParent
}

sealed class AstExpression : AstElement()

class AstCall(
    var callee: AstFunction,
    var arguments: List<AstExpression>
) : AstExpression()

class AstGetValueParameter(var valueParameter: AstValueParameter) : AstExpression()

interface AstClassifier

class AstFile(
    var packageFqName: FqName,
    var name: Name
) : AstElement(), AstAnnotationContainer, AstDeclarationContainer {
    override var annotations: List<AstCall> = emptyList()
    override var declarations: List<AstDeclaration> = emptyList()
}

data class AstClassId(
    val packageName: FqName,
    val className: Name,
)

data class AstCallableId(
    val packageName: FqName,
    val className: FqName?,
    val callableName: Name
)

val AstClass.defaultType: AstType get() = TODO()

class AstModuleFragment(
    val name: Name
) : AstElement() {
    var files = emptyList<AstFile>()
}

class AstClass(
    var classId: AstClassId,
    var kind: Kind = Kind.CLASS,
    override var visibility: AstVisibility = AstVisibility.PUBLIC,
    override var multiPlatformModality: AstMultiPlatformModality? = null,
    override var modality: AstModality = AstModality.FINAL,
    var isCompanion: Boolean = false,
    var isFun: Boolean = false,
    var isData: Boolean = false,
    var isInner: Boolean = false,
    var isExternal: Boolean = false
) : AstDeclaration(), AstAnnotationContainer,
    AstVisibilityOwner,
    AstMultiPlatformDeclaration,
    AstModalityOwner,
    AstTypeParameterContainer,
    AstDeclarationContainer {

    override var annotations: List<AstCall> = emptyList()
    override var typeParameters: List<AstTypeParameter> = emptyList()
    override var declarations: List<AstDeclaration> = emptyList()

    enum class Kind {
        CLASS,
        INTERFACE,
        ENUM_CLASS,
        ENUM_ENTRY,
        ANNOTATION,
        OBJECT
    }
}

sealed class AstFunction : AstDeclaration(), AstAnnotationContainer,
    AstDeclarationParent,
    AstVisibilityOwner,
    AstMultiPlatformDeclaration,
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
    override var multiPlatformModality: AstMultiPlatformModality? = null,
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
    override var multiPlatformModality: AstMultiPlatformModality? = null,
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
