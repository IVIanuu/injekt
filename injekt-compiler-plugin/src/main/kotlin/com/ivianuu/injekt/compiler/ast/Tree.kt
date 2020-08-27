package com.ivianuu.injekt.compiler.ast

import org.jetbrains.kotlin.backend.common.serialization.proto.IrExpression
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class AstElement

interface AstAnnotationContainer {
    val annotations: MutableList<AstCall>
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
    val typeParameters: MutableList<AstTypeParameter>
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
    val arguments: MutableList<Astty>
    val abbreviation: AstTypeAbbreviation?
}*/

interface AstType : AstAnnotationContainer

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
interface AstDeclarationContainer {
    val declarations: MutableList<AstDeclaration>
}

sealed class AstDeclaration : AstElement() {
    lateinit var parent: AstDeclarationContainer
}

sealed class AstExpression : AstElement()

class AstCall(
    var callee: AstFunction,
    val arguments: MutableList<IrExpression>
) : AstExpression()

interface AstClassifier

class AstFile(
    var packageFqName: FqName,
    var name: Name,
    override val annotations: MutableList<AstCall> = mutableListOf(),
    override val declarations: MutableList<AstDeclaration> = mutableListOf()
) : AstElement(), AstAnnotationContainer, AstDeclarationContainer

data class AstClassId(
    val packageName: FqName,
    val className: Name,
)

data class AstCallableId(
    val packageName: FqName,
    val className: FqName?,
    val callableName: Name
)

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

    override val annotations: MutableList<AstCall> = mutableListOf()
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    override val declarations: MutableList<AstDeclaration> = mutableListOf()

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
    AstVisibilityOwner,
    AstMultiPlatformDeclaration {
    override val annotations: MutableList<AstCall> = mutableListOf()

    val valueParameters: MutableList<AstValueParameter> = mutableListOf()
    abstract var callableId: AstCallableId
}

class AstSimpleFunction(
    override var callableId: AstCallableId,
    override var visibility: AstVisibility = AstVisibility.PUBLIC,
    override var multiPlatformModality: AstMultiPlatformModality? = null,
    override var modality: AstModality = AstModality.FINAL,
    var isInfix: Boolean = false,
    var isOperator: Boolean = false,
    var isTailrec: Boolean = false,
    var isSuspend: Boolean = false
) : AstFunction(), AstModalityOwner, AstTypeParameterContainer {
    override val typeParameters: MutableList<AstTypeParameter> = mutableListOf()
    val overriddenFunctions: MutableList<AstSimpleFunction> = mutableListOf()
}

class AstConstructor(
    override var callableId: AstCallableId,
    override var visibility: AstVisibility = AstVisibility.PUBLIC,
    override var multiPlatformModality: AstMultiPlatformModality? = null,
    var isPrimary: Boolean = false
) : AstFunction()

class AstValueParameter(
    var name: Name,
    var inlineHint: InlineHint? = null,
    var isVararg: Boolean = false
) : AstDeclaration(), AstAnnotationContainer {
    override val annotations: MutableList<AstCall> = mutableListOf()

    enum class InlineHint {
        INLINE,
        NOINLINE,
        CROSSINLINE
    }
}
