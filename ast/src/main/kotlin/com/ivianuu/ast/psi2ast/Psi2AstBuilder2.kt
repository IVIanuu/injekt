package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.Visibilities
import com.ivianuu.ast.builder.AstAnnotationContainerBuilder
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.declarations.builder.AstAnonymousFunctionBuilder
import com.ivianuu.ast.declarations.builder.AstAnonymousObjectBuilder
import com.ivianuu.ast.declarations.builder.AstClassBuilder
import com.ivianuu.ast.declarations.builder.AstNamedFunctionBuilder
import com.ivianuu.ast.declarations.builder.AstPropertyBuilder
import com.ivianuu.ast.declarations.builder.AstRegularClassBuilder
import com.ivianuu.ast.declarations.builder.buildAnonymousObject
import com.ivianuu.ast.declarations.builder.buildConstructor
import com.ivianuu.ast.declarations.builder.buildEnumEntry
import com.ivianuu.ast.declarations.builder.buildFile
import com.ivianuu.ast.declarations.builder.buildProperty
import com.ivianuu.ast.declarations.builder.buildRegularClass
import com.ivianuu.ast.declarations.builder.buildValueParameter
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstConstKind
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.declarations.builder.buildAnonymousInitializer
import com.ivianuu.ast.declarations.builder.buildPropertyAccessor
import com.ivianuu.ast.declarations.builder.buildTypeParameter
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.expressions.AstReturn
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.expressions.AstWhen
import com.ivianuu.ast.expressions.builder.AstBlockBuilder
import com.ivianuu.ast.expressions.builder.AstDoWhileLoopBuilder
import com.ivianuu.ast.expressions.builder.AstFunctionCallBuilder
import com.ivianuu.ast.expressions.builder.AstLoopBuilder
import com.ivianuu.ast.expressions.builder.AstLoopJumpBuilder
import com.ivianuu.ast.expressions.builder.AstQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.AstWhileLoopBuilder
import com.ivianuu.ast.expressions.builder.buildBlock
import com.ivianuu.ast.expressions.builder.buildDelegatedConstructorCall
import com.ivianuu.ast.expressions.builder.buildFunctionCall
import com.ivianuu.ast.expressions.builder.buildQualifiedAccess
import com.ivianuu.ast.expressions.builder.buildReturn
import com.ivianuu.ast.expressions.builder.buildVariableAssignment
import com.ivianuu.ast.expressions.builder.buildWhenBranch
import com.ivianuu.ast.symbols.AstSymbol
import com.ivianuu.ast.symbols.CallableId
import com.ivianuu.ast.symbols.impl.AstAnonymousFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstAnonymousInitializerSymbol
import com.ivianuu.ast.symbols.impl.AstClassLikeSymbol
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstNamedFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstPropertyAccessorSymbol
import com.ivianuu.ast.symbols.impl.AstPropertySymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.types.AstTypeProjection
import org.jetbrains.kotlin.KtNodeTypes
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.com.intellij.psi.tree.IElementType
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.PropertySetterDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.lexer.KtTokens.*
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotated
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClass
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtCollectionLiteralExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtDeclarationWithBody
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtExpressionWithLabel
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectDeclaration
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtSimpleNameExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtSuperTypeEntry
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeReference
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.KtVariableDeclaration
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.KtWhenCondition
import org.jetbrains.kotlin.psi.KtWhenConditionInRange
import org.jetbrains.kotlin.psi.KtWhenConditionIsPattern
import org.jetbrains.kotlin.psi.KtWhenConditionWithExpression
import org.jetbrains.kotlin.psi.KtWhenExpression
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi.ValueArgument
import org.jetbrains.kotlin.psi.psiUtil.hasActualModifier
import org.jetbrains.kotlin.psi.psiUtil.hasExpectModifier
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.types.expressions.OperatorConventions
import org.jetbrains.kotlin.util.OperatorNameConventions

class Psi2AstBuilder2(override val context: Psi2AstGeneratorContext) : Generator, KtVisitor<AstElement, Nothing?>() {

    /**
    private val functionRefs = mutableListOf<AstElementRef<AstFunction<*>>>()
    private val calleeNamesForLambda = mutableListOf<AstElementRef<AstLoop>>()
    private val labels = mutableListOf<String>()
    private val loopRefs =mutableListOf<AstElementRef<AstFunction<*>>>()
    private val arraySetArgument = mutableMapOf<PsiElement, AstExpression>()

    private val PsiElement.elementType: IElementType
        get() = node.elementType

    private val PsiElement.asText: String
        get() = text

    private fun PsiElement.getReferencedNameAsName(): Name {
        return (this as KtSimpleNameExpression).getReferencedNameAsName()
    }

    private fun PsiElement.getLabelName(): String? {
        return (this as? KtExpressionWithLabel)?.getLabelName()
    }

    private fun <R : AstElement> KtElement?.convertSafe(): R? =
        this?.accept(this@Psi2AstBuilder, null) as? R

    private fun <R : AstElement> KtElement.convert(): R =
        this.accept(this@Psi2AstBuilder, null) as R

    private fun KtDeclaration.toAstDeclaration(
        delegatedSuperType: AstType,
        delegatedSelfType: AstResolvedTypeRef,
        owner: KtClassOrObject,
        ownerClassBuilder: AstClassBuilder,
        ownerTypeParameters: List<AstTypeParameterRef>
    ): AstDeclaration {
        return when (this) {
            is KtSecondaryConstructor -> {
                toAstConstructor(
                    delegatedSuperType,
                    delegatedSelfType,
                    owner,
                    ownerTypeParameters
                )
            }
            is KtEnumEntry -> {
                val primaryConstructor = owner.primaryConstructor
                val ownerClassHasDefaultConstructor =
                    primaryConstructor?.valueParameters?.isEmpty() ?: owner.secondaryConstructors.let { constructors ->
                        constructors.isEmpty() || constructors.any { it.valueParameters.isEmpty() }
                    }
                toAstEnumEntry(delegatedSelfType, ownerClassHasDefaultConstructor)
            }
            is KtProperty -> {
                toAstProperty()
            }
            else -> convert()
        }
    \

    private fun KtClassOrObject.extractSuperTypeListEntriesTo(
        container: AstClassBuilder,
        delegatedSelfTypeRef: AstType?,
        delegatedEnumSuperTypeRef: AstType?,
        classKind: ClassKind,
        containerTypeParameters: List<AstTypeParameterRef>,
        containerSymbol: AstSymbol<*>
    ): AstType {
        var superTypeCallEntry: KtSuperTypeCallEntry? = null
        var delegatedSuperTypeRef: AstType? = null
        var delegateNumber = 0
        val initializeDelegateStatements = mutableListOf<AstStatement>()
        for (superTypeListEntry in superTypeListEntries) {
            when (superTypeListEntry) {
                is KtSuperTypeEntry -> {
                    container.superTypeRefs += superTypeListEntry.typeReference.toAstType()
                }
                is KtSuperTypeCallEntry -> {
                    delegatedSuperTypeRef = superTypeListEntry.calleeExpression.typeReference.toAstType()
                    container.superTypeRefs += delegatedSuperTypeRef
                    superTypeCallEntry = superTypeListEntry
                }
                is KtDelegatedSuperTypeEntry -> {
                    val type = superTypeListEntry.typeReference.toAstType()
                    val delegateExpression = { superTypeListEntry.delegateExpression }.toAstExpression("Should have delegate")
                    container.superTypeRefs += type
                    val delegateName = Name.special("<\$\$delegate_$delegateNumber>")
                    val delegateField = buildField {
                        name = delegateName
                        returnType = type
                        symbol = AstFieldSymbol(CallableId(name))
                        isVar = false
                        status = AstDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                    }
                    initializeDelegateStatements.add(
                        buildVariableAssignment {
                            calleeReference =
                                buildResolvedNamedReference {
                                    name = delegateName
                                    resolvedSymbol = delegateField.symbol
                                }
                            rValue = delegateExpression
                            dispatchReceiver = buildThisReceiverExpression {
                                calleeReference = buildImplicitThisReference {
                                    boundSymbol = containerSymbol
                                }
                                delegatedSelfTypeRef?.let { typeRef = it }
                            }
                        }
                    )
                    container.declarations.add(delegateField)
                    delegateNumber ++
                }
            }
        }

        when {
            this is KtClass && classKind == ClassKind.ENUM_CLASS -> {
                /*
                 * kotlin.Enum constructor has (name: String, ordinal: Int) signature,
                 *   so we should generate non-trivial constructors for enum and it's entry
                 *   for correct resolve of super constructor call or just call kotlin.Any constructor
                 *   and convert it to right call at backend, because of it doesn't affects frontend work
                 */
                delegatedSuperTypeRef = buildResolvedTypeRef {
                    type = ConeClassLikeTypeImpl(
                        implicitEnumType.type.lookupTag,
                        delegatedSelfTypeRef?.coneType?.let { arrayOf(it) } ?: emptyArray(),
                        isNullable = false,
                    )
                }
                container.superTypeRefs += delegatedSuperTypeRef
            }
            this is KtClass && classKind == ClassKind.ANNOTATION_CLASS -> {
                container.superTypeRefs += implicitAnnotationType
                delegatedSuperTypeRef = implicitAnyType
            }
        }

        val defaultDelegatedSuperTypeRef =
            when {
                classKind == ClassKind.ENUM_ENTRY && this is KtClass -> delegatedEnumSuperTypeRef ?: implicitAnyType
                container.superTypeRefs.isEmpty() -> implicitAnyType
                else -> buildImplicitTypeRef()
            }


        if (container.superTypeRefs.isEmpty()) {
            container.superTypeRefs += implicitAnyType
            delegatedSuperTypeRef = implicitAnyType
        }
        if (this is KtClass && this.isInterface()) return delegatedSuperTypeRef ?: implicitAnyType

        // TODO: in case we have no primary constructor,
        // it may be not possible to determine delegated super type right here
        delegatedSuperTypeRef = delegatedSuperTypeRef ?: defaultDelegatedSuperTypeRef
        if (!this.hasPrimaryConstructor()) return delegatedSuperTypeRef

        val astPrimaryConstructor = primaryConstructor.toAstConstructor(
            superTypeCallEntry,
            delegatedSuperTypeRef,
            delegatedSelfTypeRef ?: delegatedSuperTypeRef,
            owner = this,
            containerTypeParameters,
            body = if (initializeDelegateStatements.isNotEmpty()) buildBlock {
                for (statement in initializeDelegateStatements) {
                    statements += statement
                }
            } else null
        )

        container.declarations += astPrimaryConstructor
        return delegatedSuperTypeRef
    }

    private fun KtPrimaryConstructor?.toAstConstructor(
        superTypeCallEntry: KtSuperTypeCallEntry?,
        delegatedSuperTypeRef: AstType,
        delegatedSelfTypeRef: AstType,
        owner: KtClassOrObject,
        ownerTypeParameters: List<AstTypeParameterRef>,
        body: AstBlock? = null
    ): AstConstructor {
        val constructorCallee = superTypeCallEntry?.calleeExpression?.toAstSourceElement()
        val astDelegatedCall = buildDelegatedConstructorCall {
            constructedTypeRef = delegatedSuperTypeRef
            isThis = false
            superTypeCallEntry?.extractArgumentsTo(this)
        }

        // See DescriptorUtils#getDefaultConstructorVisibility in core.descriptors
        fun defaultVisibility() = when {
            owner is KtObjectDeclaration || owner.hasModifier(ENUM_KEYWORD) || owner is KtEnumEntry -> Visibilities.Private
            owner.hasModifier(SEALED_KEYWORD) -> Visibilities.Private
            else -> Visibilities.Unknown
        }

        val explicitVisibility = this?.visibility
        val status = AstDeclarationStatusImpl(explicitVisibility ?: defaultVisibility(), Modality.FINAL).apply {
            isExpect = this@toAstConstructor?.hasExpectModifier() == true || owner.hasExpectModifier()
            isActual = this@toAstConstructor?.hasActualModifier() == true
            isInner = owner.hasModifier(INNER_KEYWORD)
            isFromSealedClass = owner.hasModifier(SEALED_KEYWORD) && explicitVisibility !== Visibilities.Private
            isFromEnumClass = owner.hasModifier(ENUM_KEYWORD)
        }
        return buildConstructor {
            isPrimary = true
            returnType = delegatedSelfTypeRef
            this.status = status
            symbol = AstConstructorSymbol(callableIdForClassConstructor())
            delegatedConstructor = astDelegatedCall
            typeParameters += constructorTypeParametersFromConstructedClass(ownerTypeParameters)
            this@toAstConstructor?.extractAnnotationsTo(this)
            this@toAstConstructor?.extractValueParametersTo(this)
            this.body = body
        }
    }

    private fun KtEnumEntry.toAstEnumEntry(
        delegatedEnumSelfTypeRef: AstResolvedTypeRef,
        ownerClassHasDefaultConstructor: Boolean
    ): AstDeclaration {
        val ktEnumEntry = this@toAstEnumEntry
        return buildEnumEntry {
            returnType = delegatedEnumSelfTypeRef
            name = nameAsSafeName
            status = AstDeclarationStatusImpl(Visibilities.Public, Modality.FINAL).apply {
                isStatic = true
                isExpect = containingClassOrObject?.hasExpectModifier() == true
            }
            symbol = AstVariableSymbol(callableIdForName(nameAsSafeName))
            if (ownerClassHasDefaultConstructor && ktEnumEntry.initializerList == null &&
                ktEnumEntry.annotationEntries.isEmpty() && ktEnumEntry.body == null
            ) {
                return@buildEnumEntry
            }
            extractAnnotationsTo(this)
            initializer = withChildClassName(nameAsSafeName) {
                buildAnonymousObject {
                    classKind = ClassKind.ENUM_ENTRY
                    symbol = AstAnonymousObjectSymbol()

                    extractAnnotationsTo(this)
                    val delegatedEntrySelfType = buildResolvedTypeRef {
                        type = ConeClassLikeTypeImpl(this@buildAnonymousObject.symbol.toLookupTag(), emptyArray(), isNullable = false)
                    }
                    superTypeRefs += delegatedEnumSelfTypeRef
                    val superTypeCallEntry = superTypeListEntries.firstIsInstanceOrNull<KtSuperTypeCallEntry>()
                    val correctedEnumSelfTypeRef = buildResolvedTypeRef {
                        type = delegatedEnumSelfTypeRef.type
                    }
                    declarations += primaryConstructor.toAstConstructor(
                        superTypeCallEntry,
                        correctedEnumSelfTypeRef,
                        delegatedEntrySelfType,
                        owner = ktEnumEntry,
                        typeParameters
                    )
                    for (declaration in ktEnumEntry.declarations) {
                        declarations += declaration.toAstDeclaration(
                            correctedEnumSelfTypeRef,
                            delegatedSelfType = delegatedEntrySelfType,
                            ktEnumEntry,
                            ownerClassBuilder = this,
                            ownerTypeParameters = emptyList()
                        )
                    }
                }
            }
        }
    }

    override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression, data: Nothing?): AstElement {
        val objectDeclaration = expression.objectDeclaration
        return withChildClassName(ANONYMOUS_OBJECT_NAME) {
            buildAnonymousObject {
                classKind = ClassKind.OBJECT
                scopeProvider = baseScopeProvider
                symbol = AstAnonymousObjectSymbol()
                val delegatedSelfType = objectDeclaration.toDelegatedSelfType(this)
                objectDeclaration.extractAnnotationsTo(this)
                val delegatedSuperType = objectDeclaration.extractSuperTypeListEntriesTo(
                    this,
                    delegatedSelfType,
                    null,
                    ClassKind.CLASS,
                    containerTypeParameters = emptyList(),
                    symbol
                )
                typeRef = delegatedSelfType


                for (declaration in objectDeclaration.declarations) {
                    declarations += declaration.toAstDeclaration(
                        delegatedSuperType,
                        delegatedSelfType,
                        owner = objectDeclaration,
                        ownerClassBuilder = this,
                        ownerTypeParameters = emptyList()
                    )
                }
            }
        }
    }

    private fun KtSecondaryConstructor.toAstConstructor(
        delegatedSuperTypeRef: AstType,
        delegatedSelfTypeRef: AstType,
        owner: KtClassOrObject,
        ownerTypeParameters: List<AstTypeParameterRef>
    ): AstConstructor {
        val target = AstFunctionTarget(labelName = null, isLambda = false)
        return buildConstructor {
            returnType = delegatedSelfTypeRef
            val explicitVisibility = visibility
            status = AstDeclarationStatusImpl(explicitVisibility, Modality.FINAL).apply {
                isExpect = hasExpectModifier() || owner.hasExpectModifier()
                isActual = hasActualModifier()
                isInner = owner.hasModifier(INNER_KEYWORD)
                isFromSealedClass = owner.hasModifier(SEALED_KEYWORD) && explicitVisibility !== Visibilities.Private
                isFromEnumClass = owner.hasModifier(ENUM_KEYWORD)
            }
            symbol = AstConstructorSymbol(callableIdForClassConstructor())
            delegatedConstructor = getDelegationCall().convert(
                delegatedSuperTypeRef,
                delegatedSelfTypeRef,
            )
            functionRefs += target
            extractAnnotationsTo(this)
            typeParameters += constructorTypeParametersFromConstructedClass(ownerTypeParameters)
            extractValueParametersTo(this)
            val (body, _) = buildAstBody()
            this.body = body
            functionRefs.removeLast()
        }.also {
            target.bind(it)
        }
    }

    override fun visitSimpleNameExpression(expression: KtSimpleNameExpression, data: Nothing?): AstElement {
        return generateAccessExpression(expression.toAstSourceElement(), expression.getReferencedNameAsName())
    }

    override fun visitWhenExpression(expression: KtWhenExpression, data: Nothing?): AstElement {
        val ktSubjectExpression = expression.subjectExpression
        val subjectExpression = when (ktSubjectExpression) {
            is KtVariableDeclaration -> ktSubjectExpression.initializer
            else -> ktSubjectExpression
        }!!.convert<AstExpression>()
        val subjectVariable = when (ktSubjectExpression) {
            is KtVariableDeclaration -> {
                val name = ktSubjectExpression.nameAsSafeName
                buildProperty {
                    returnType = ktSubjectExpression.typeReference.toAstOrImplicitType()
                    receiverTypeRef = null
                    this.name = name
                    initializer = subjectExpression
                    delegate = null
                    isVar = false
                    symbol = AstPropertySymbol(name)
                    isLocal = true
                    status = AstDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                }
            }
            else -> null
        }
        val hasSubject = subjectExpression != null

        @OptIn(AstContractViolation::class)
        val ref = AstElementRef<AstWhen>()
        return buildWhenExpression {
            this.subject = subjectExpression
            this.subjectVariable = subjectVariable

            for (entry in expression.entries) {
                val branchBody = entry.expression.toAstBlock()
                branches += if (!entry.isElse) {
                    if (hasSubject) {
                        buildWhenBranch {
                            condition = entry.conditions.toAstWhenCondition(
                                ref,
                                { toAstExpression(it) },
                                { toAstType() },
                            )
                            result = branchBody
                        }
                    } else {
                        val ktCondition = entry.conditions.first() as? KtWhenConditionWithExpression
                        buildWhenBranch {
                            condition = ktCondition?.expression.toAstExpression("No expression in condition with expression")
                            result = branchBody
                        }
                    }
                } else {
                    buildWhenBranch {
                        condition = buildElseIfTrueCondition()
                        result = branchBody
                    }
                }
            }
        }.also {
            if (hasSubject) {
                ref.bind(it)
            }
        }
    }

    override fun visitForExpression(expression: KtForExpression, data: Nothing??): AstElement {
        val rangeExpression = expression.loopRange.toAstExpression("No range in for loop")
        val ktParameter = expression.loopParameter
        return buildBlock {
            val iteratorVal = generateTemporaryVariable(Name.special("<iterator>"),
                buildFunctionCall {
                    calleeReference = buildSimpleNamedReference {
                        name = Name.identifier("iterator")
                    }
                    explicitReceiver = rangeExpression
                },
            )
            statements += iteratorVal
            statements += AstWhileLoopBuilder().apply {
                condition = buildFunctionCall {
                    calleeReference = buildSimpleNamedReference {
                        name = Name.identifier("hasNext")
                    }
                    explicitReceiver = generateResolvedAccessExpression(iteratorVal)
                }
            }.configure {
                // NB: just body.toAstBlock() isn't acceptable here because we need to add some statements
                val blockBuilder = when (val body = expression.body) {
                    is KtBlockExpression -> configureBlockWithoutBuilding(body)
                    null -> AstBlockBuilder()
                    else -> AstBlockBuilder().apply {
                        statements += body.convert<AstStatement>()
                    }
                }
                if (ktParameter != null) {
                    val multiDeclaration = ktParameter.destructuringDeclaration
                    val astLoopParameter = generateTemporaryVariable(
                        name = if (multiDeclaration != null) Name.special("<destruct>") else ktParameter.nameAsSafeName,
                        initializer = buildFunctionCall {
                            calleeReference = buildSimpleNamedReference {
                                name = Name.identifier("next")
                            }
                            explicitReceiver = generateResolvedAccessExpression(iteratorVal)
                        },
                        typeRef = ktParameter.typeReference.toAstOrImplicitType(),
                    )
                    if (multiDeclaration != null) {
                        val destructuringBlock = generateDestructuringBlock(
                            multiDeclaration = multiDeclaration,
                            container = astLoopParameter,
                            tmpVariable = true,
                            extractAnnotationsTo = { extractAnnotationsTo(it) },
                        ) { toAstOrImplicitType() }
                        if (destructuringBlock is AstBlock) {
                            for ((index, statement) in destructuringBlock.statements.withIndex()) {
                                blockBuilder.statements.add(index, statement)
                            }
                        }
                    } else {
                        blockBuilder.statements.add(0, astLoopParameter)
                    }
                }
                blockBuilder.build()
            }
        }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, data: Nothing?): AstElement {

        when (operationToken) {
            ELVIS ->
                return leftArgument.generateNotNullOrOther(rightArgument)
            ANDAND, OROR ->
                return leftArgument.generateLazyLogicalOperation(rightArgument, operationToken == ANDAND)
            in OperatorConventions.IN_OPERATIONS ->
                return rightArgument.generateContainsOperation(
                    leftArgument, operationToken == NOT_IN
                )
            in OperatorConventions.COMPARISON_OPERATIONS ->
                return leftArgument.generateComparisonExpression(
                    rightArgument, operationToken
                )
        }
        val conventionCallName = operationToken.toBinaryName()
        return if (conventionCallName != null || operationToken == IDENTIFIER) {
            buildFunctionCall {
                calleeReference = buildSimpleNamedReference {
                    name = conventionCallName ?: expression.operationReference.getReferencedNameAsName()
                }
                explicitReceiver = leftArgument
                argumentList = buildUnaryArgumentList(rightArgument)
            }
        } else {
            val astOperation = operationToken.toAstOperation()
            if (astOperation in AstOperation.ASSIGNMENTS) {
                return expression.left.generateAssignment(expression.right, rightArgument, astOperation) {
                    (this as KtExpression).toAstExpression("Incorrect expression in assignment: ${expression.text}")
                }
            } else {
                buildEqualityOperatorCall {
                    operation = astOperation
                    argumentList = buildBinaryArgumentList(leftArgument, rightArgument)
                }
            }
        }
    }

    override fun visitBinaryWithTypeRHSExpression(expression: KtBinaryExpressionWithTypeRHS, data: Nothing?): AstElement {
        return buildTypeOperatorCall {
            operation = expression.operationReference.getReferencedNameElementType().toAstOperation()
            conversionTypeRef = expression.right.toAstType()
            argumentList = buildUnaryArgumentList(expression.left.toAstExpression("No left operand"))
        }
    }

    override fun visitIsExpression(expression: KtIsExpression, data: Nothing?): AstElement {
        return buildTypeOperatorCall {
            operation = if (expression.isNegated) AstOperation.NOT_IS else AstOperation.IS
            conversionTypeRef = expression.typeReference.toAstType()
            argumentList = buildUnaryArgumentList(expression.leftHandSide.toAstExpression("No left operand"))
        }
    }

    override fun visitUnaryExpression(expression: KtUnaryExpression, data: Nothing?): AstElement {
        val operationToken = expression.operationToken
        val argument = expression.baseExpression
        val conventionCallName = operationToken.toUnaryName()
        return when {
            operationToken == EXCLEXCL -> {
                buildCheckNotNullCall {
                    argumentList = buildUnaryArgumentList(argument.toAstExpression("No operand"))
                }
            }
            conventionCallName != null -> {
                if (operationToken in OperatorConventions.INCREMENT_OPERATIONS) {
                    return generateIncrementOrDecrementBlock(
                        expression, expression.operationReference, argument,
                        callName = conventionCallName,
                        prefix = expression is KtPrefixExpression,
                    ) { (this as KtExpression).toAstExpression("Incorrect expression inside inc/dec") }
                }
                buildFunctionCall {
                    calleeReference = buildSimpleNamedReference {
                        name = conventionCallName
                    }
                    explicitReceiver = argument.toAstExpression("No operand")
                }
            }
            else -> throw IllegalStateException("Unexpected expression: ${expression.text}")
        }
    }

    private fun splitToCalleeAndReceiver(
        calleeExpression: KtExpression?
    ): Pair<AstNamedReference, AstExpression?> {
        return when (calleeExpression) {
            is KtSimpleNameExpression -> buildSimpleNamedReference {
                name = calleeExpression.getReferencedNameAsName()
            } to null

            is KtParenthesizedExpression -> splitToCalleeAndReceiver(calleeExpression.expression, defaultSource)

            null -> {
                buildErrorNamedReference { diagnostic = ConeSimpleDiagnostic("Call has no callee", DiagnosticKind.Syntax) } to null
            }

            is KtSuperExpression -> {
                buildErrorNamedReference {
                    diagnostic = ConeSimpleDiagnostic("Super cannot be a callee", DiagnosticKind.SuperNotAllowed)
                } to null
            }

            else -> {
                buildSimpleNamedReference {
                    name = OperatorNameConventions.INVOKE
                } to calleeExpression.toAstExpression("Incorrect invoke receiver")
            }
        }
    }

    override fun visitCallExpression(expression: KtCallExpression, data: Nothing?): AstElement {
        val (calleeReference, explicitReceiver) = splitToCalleeAndReceiver(expression.calleeExpression, source)

        val result: AstQualifiedAccessBuilder = if (expression.valueArgumentList == null && expression.lambdaArguments.isEmpty()) {
            AstQualifiedAccessExpressionBuilder().apply {
                this.calleeReference = calleeReference
            }
        } else {
            AstFunctionCallBuilder().apply {
                this.calleeReference = calleeReference
                calleeNamesForLambda += calleeReference.name
                expression.extractArgumentsTo(this)
                calleeNamesForLambda.removeLast()
            }
        }

        return result.apply {
            this.explicitReceiver = explicitReceiver
            for (typeArgument in expression.typeArguments) {
                typeArguments += typeArgument.convert<AstTypeProjection>()
            }
        }.build()
    }

    override fun visitArrayAccessExpression(expression: KtArrayAccessExpression, data: Nothing?): AstElement {
        val arrayExpression = expression.arrayExpression
        return buildFunctionCall {
            val source: AstPsiSourceElement<*>
            val getArgument = arraySetArgument.remove(expression)
            if (getArgument != null) {
                calleeReference = buildSimpleNamedReference {
                    name = OperatorNameConventions.SET
                }
            } else {
                calleeReference = buildSimpleNamedReference {
                    name = OperatorNameConventions.GET
                }
            }
            explicitReceiver = arrayExpression.toAstExpression("No array expression")
            argumentList = buildArgumentList {
                for (indexExpression in expression.indexExpressions) {
                    arguments += indexExpression.toAstExpression("Incorrect index expression")
                }
                if (getArgument != null) {
                    arguments += getArgument
                }
            }
        }
    }

    override fun visitQualifiedExpression(expression: KtQualifiedExpression, data: Nothing?): AstElement {
        val selector = expression.selectorExpression
            ?: return buildErrorExpression(
                expression.toAstSourceElement(), ConeSimpleDiagnostic("Qualified expression without selector", DiagnosticKind.Syntax),
            )
        val astSelector = selector.toAstExpression("Incorrect selector expression")
        if (astSelector is AstQualifiedAccess) {
            val receiver = expression.receiverExpression.toAstExpression("Incorrect receiver expression")

            if (expression is KtSafeQualifiedExpression) {
                return astSelector.wrapWithSafeCall(receiver)
            }

            astSelector.replaceExplicitReceiver(receiver)
        }
        return astSelector
    }

    override fun visitThisExpression(expression: KtThisExpression, data: Nothing?): AstElement {
        return buildThisReceiverExpression {
            calleeReference = buildExplicitThisReference {
                labelName = expression.getLabelName()
            }
        }
    }

    override fun visitSuperExpression(expression: KtSuperExpression, data: Nothing?): AstElement {
        val superType = expression.superTypeQualifier
        return buildQualifiedAccessExpression {
            calleeReference = buildExplicitSuperReference {
                labelName = expression.getLabelName()
                superTypeRef = superType.toAstOrImplicitType()
            }
        }
    }

    override fun visitDestructuringDeclaration(multiDeclaration: KtDestructuringDeclaration, data: Nothing?): AstElement {
        val baseVariable = generateTemporaryVariable(
            baseSession, multiDeclaration.toAstSourceElement(), "destruct",
            multiDeclaration.initializer.toAstExpression("Destructuring declaration without initializer"),
        )
        return generateDestructuringBlock(
            baseSession,
            multiDeclaration,
            baseVariable,
            tmpVariable = true,
            extractAnnotationsTo = { extractAnnotationsTo(it) },
        ) {
            toAstOrImplicitType()
        }
    }

    override fun visitClassLiteralExpression(expression: KtClassLiteralExpression, data: Nothing?): AstElement {
        return buildGetClassCall {
            argumentList = buildUnaryArgumentList(expression.receiverExpression.toAstExpression("No receiver in class literal"))
        }
    }

    override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression, data: Nothing?): AstElement {
        return buildCallableReferenceAccess {
            calleeReference = buildSimpleNamedReference {
                name = expression.callableReference.getReferencedNameAsName()
            }
            explicitReceiver = expression.receiverExpression?.toAstExpression("Incorrect receiver expression")
            hasQuestionMarkAtLHS = expression.hasQuestionMarks
        }
    }

    override fun visitCollectionLiteralExpression(expression: KtCollectionLiteralExpression, data: Nothing?): AstElement {
        return buildArrayOfCall {
            argumentList = buildArgumentList {
                for (innerExpression in expression.getInnerExpressions()) {
                    arguments += innerExpression.toAstExpression("Incorrect collection literal argument")
                }
            }
        }
    }

    companion object {
        val ANONYMOUS_OBJECT_NAME = Name.special("<anonymous>")
    }

    fun AstExpression.toReturn(labelName: String? = null): AstReturn {
        return buildReturn {
            result = this@toReturn
            if (labelName == null) {
                target = functionRefs.last { !it.isLambda }
            } else {
                for (functionTarget in functionRefs.asReversed()) {
                    if (functionTarget.labelName == labelName) {
                        target = functionTarget
                        return@buildReturn
                    }
                }
            }
        }
    }

    fun PsiElement?.toDelegatedSelfType(astClass: AstRegularClassBuilder): AstResolvedTypeRef =
        toDelegatedSelfType(astClass, astClass.symbol)

    fun PsiElement?.toDelegatedSelfType(astObject: AstAnonymousObjectBuilder): AstResolvedTypeRef =
        toDelegatedSelfType(astObject, astObject.symbol)

    private fun PsiElement?.toDelegatedSelfType(astClass: AstClassBuilder, symbol: AstClassLikeSymbol<*>): AstResolvedTypeRef {
        return buildResolvedTypeRef {
            type = ConeClassLikeTypeImpl(
                symbol.toLookupTag(),
                astClass.typeParameters.map { ConeTypeParameterTypeImpl(it.symbol.toLookupTag(), false) }.toTypedArray(),
                false
            )
        }
    }

    fun constructorTypeParametersFromConstructedClass(ownerTypeParameters: List<AstTypeParameterRef>): List<AstTypeParameterRef> {
        return ownerTypeParameters.mapNotNull {
            val declaredTypeParameter = (it as? AstTypeParameter) ?: return@mapNotNull null
            buildConstructedClassTypeParameterRef { symbol = declaredTypeParameter.symbol }
        }
    }

    fun AstLoopBuilder.configure(generateBlock: () -> AstBlock): AstLoop {
        label = labels.pop()
        val target = AstLoopTarget(label?.name)
        loopRefs += target
        body = generateBlock()
        val loop = build()
        loopRefs.removeLast()
        target.bind(loop)
        return loop
    }

    fun generateIncrementOrDecrementBlock(
        baseExpression: PsiElement,
        operationReference: PsiElement?,
        argument: PsiElement,
        callName: Name,
        prefix: Boolean,
        convert: PsiElement.() -> AstExpression
    ): AstExpression {
        return buildBlock {
            val tmpName = Name.special("<unary>")
            val temporaryVariable = generateTemporaryVariable(
                tmpName,
                argument.convert()
            )
            statements += temporaryVariable
            val resultName = Name.special("<unary-result>")
            val resultInitializer = buildFunctionCall {
                calleeReference = buildSimpleNamedReference {
                    name = callName
                }
                explicitReceiver = generateResolvedAccessExpression(temporaryVariable)
            }
            val resultVar = generateTemporaryVariable(resultName, resultInitializer)
            val assignment = argument.generateAssignment(
                argument,
                if (prefix && argument.elementType != KtNodeTypes.REFERENCE_EXPRESSION)
                    generateResolvedAccessExpression(resultVar)
                else
                    resultInitializer,
                AstOperation.ASSIGN,
                convert
            )

            fun appendAssignment() {
                if (assignment is AstBlock) {
                    statements += assignment.statements
                } else {
                    statements += assignment
                }
            }

            if (prefix) {
                if (argument.elementType != KtNodeTypes.REFERENCE_EXPRESSION) {
                    statements += resultVar
                    appendAssignment()
                    statements += generateResolvedAccessExpression(resultVar)
                } else {
                    appendAssignment()
                    statements += generateAccessExpression(argument.getReferencedNameAsName())
                }
            } else {
                appendAssignment()
                statements += generateResolvedAccessExpression(temporaryVariable)
            }
        }
    }

    private fun AstQualifiedAccessBuilder.initializeLValue(
        left: PsiElement?,
        convertQualified: PsiElement.() -> AstQualifiedAccess?
    ): AstReference {
        val tokenType = left?.elementType
        if (left != null) {
            when (tokenType) {
                KtNodeTypes.REFERENCE_EXPRESSION -> {
                    return buildSimpleNamedReference {
                        name = left.getReferencedNameAsName()
                    }
                }
                KtNodeTypes.THIS_EXPRESSION -> {
                    return buildExplicitThisReference {
                        labelName = left.getLabelName()
                    }
                }
                KtNodeTypes.DOT_QUALIFIED_EXPRESSION, KtNodeTypes.SAFE_ACCESS_EXPRESSION -> {
                    val astMemberAccess = left.convertQualified()
                    return if (astMemberAccess != null) {
                        explicitReceiver = astMemberAccess.explicitReceiver
                        astMemberAccess.calleeReference
                    } else {
                        buildErrorNamedReference {
                            diagnostic = ConeSimpleDiagnostic("Unsupported qualified LValue: ${left.asText}", DiagnosticKind.Syntax)
                        }
                    }
                }
                KtNodeTypes.PARENTHESIZED -> {
                    return initializeLValue(left.getExpressionInParentheses(), convertQualified)
                }
                KtNodeTypes.ANNOTATED_EXPRESSION -> {
                    return initializeLValue(left.getAnnotatedExpression(), convertQualified)
                }
            }
        }
        return buildErrorNamedReference {
            diagnostic = ConeSimpleDiagnostic("Unsupported LValue: $tokenType", DiagnosticKind.VariableExpected)
        }
    }

    fun PsiElement?.generateAssignment(
        rhs: PsiElement?,
        value: AstExpression, // value is AST for rhs
        operation: AstOperation,
        convert: PsiElement.() -> AstExpression
    ): AstStatement {
        val tokenType = this?.elementType
        if (tokenType == org.jetbrains.kotlin.KtNodeTypes.PARENTHESIZED) {
            return this!!.getExpressionInParentheses().generateAssignment(baseSource, rhs, value, operation, convert)
        }
        if (tokenType == org.jetbrains.kotlin.KtNodeTypes.ARRAY_ACCESS_EXPRESSION) {
            require(this != null)
            if (operation == AstOperation.ASSIGN) {
                arraySetArgument[this] = value
            }
            return if (operation == AstOperation.ASSIGN) {
                this.convert()
            } else {
                generateAugmentedArraySetCall(baseSource, operation, rhs, convert)
            }
        }

        if (operation in AstOperation.ASSIGNMENTS && operation != AstOperation.ASSIGN) {
            return buildAssignmentOperatorStatement {
                this.operation = operation
                // TODO: take good psi
                leftArgument = this@generateAssignment?.convert() ?: buildErrorExpression {
                    diagnostic = ConeSimpleDiagnostic(
                        "Unsupported left value of assignment: ${baseSource?.psi?.text}", DiagnosticKind.ExpressionRequired
                    )
                }
                rightArgument = value
            }
        }
        require(operation == AstOperation.ASSIGN)

        if (this?.elementType == org.jetbrains.kotlin.KtNodeTypes.SAFE_ACCESS_EXPRESSION && this != null) {
            val safeCallNonAssignment = convert() as? AstSafeCallExpression
            if (safeCallNonAssignment != null) {
                return putAssignmentToSafeCall(safeCallNonAssignment, baseSource, value)
            }
        }

        return buildVariableAssignment {
            source = baseSource
            rValue = value
            calleeReference = initializeLValue(this@generateAssignment) { convert() as? AstQualifiedAccess }
        }
    }

    // gets a?.{ $subj.x } and turns it to a?.{ $subj.x = v }
    private fun putAssignmentToSafeCall(
        safeCallNonAssignment: AstSafeCallExpression,
        value: AstExpression
    ): AstSafeCallExpression {
        val nestedAccess = safeCallNonAssignment.regularQualifiedAccess

        val assignment = buildVariableAssignment {
            rValue = value
            calleeReference = nestedAccess.calleeReference
            explicitReceiver = safeCallNonAssignment.checkedSubjectRef.value
        }

        safeCallNonAssignment.replaceRegularQualifiedAccess(
            assignment
        )

        return safeCallNonAssignment
    }

    private fun PsiElement.generateAugmentedArraySetCall(
        operation: AstOperation,
        rhs: PsiElement?,
        convert: PsiElement.() -> AstExpression
    ): AstStatement {
        return buildAugmentedArraySetCall {
            this.operation = operation
            assignCall = generateAugmentedCallForAugmentedArraySetCall(operation, rhs, convert)
            setGetBlock = generateSetGetBlockForAugmentedArraySetCall(operation, rhs, convert)
        }
    }

    private fun PsiElement.generateAugmentedCallForAugmentedArraySetCall(
        operation: AstOperation,
        rhs: PsiElement?,
        convert: PsiElement.() -> AstExpression
    ): AstFunctionCall {
        /*
         * Desugarings of a[x, y] += z to
         * a.get(x, y).plusAssign(z)
         */
        return buildFunctionCall {
            calleeReference = buildSimpleNamedReference {
                name = AstOperationNameConventions.ASSIGNMENTS.getValue(operation)
            }
            explicitReceiver = convert()
            argumentList = buildArgumentList {
                arguments += rhs?.convert() ?: buildErrorExpression(
                    null,
                    ConeSimpleDiagnostic("No value for array set", DiagnosticKind.Syntax)
                )
            }
        }
    }


    private fun PsiElement.generateSetGetBlockForAugmentedArraySetCall(
        operation: AstOperation,
        rhs: PsiElement?,
        convert: PsiElement.() -> AstExpression
    ): AstBlock {
        /*
         * Desugarings of a[x, y] += z to
         * {
         *     val tmp_a = a
         *     val tmp_x = x
         *     val tmp_y = y
         *     tmp_a.set(tmp_x, tmp_a.get(tmp_x, tmp_y).plus(z))
         * }
         */
        return buildBlock {
            val baseCall = convert() as AstFunctionCall

            val arrayVariable = generateTemporaryVariable(
                baseSession,
                "<array>",
                baseCall.explicitReceiver!!
            )
            statements += arrayVariable
            val indexVariables = baseCall.arguments.mapIndexed { i, index ->
                generateTemporaryVariable(baseSession, "<index_$i>", index)
            }
            statements += indexVariables
            statements += buildFunctionCall {
                explicitReceiver = arrayVariable.toQualifiedAccess()
                calleeReference = buildSimpleNamedReference {
                    name = org.jetbrains.kotlin.util.OperatorNameConventions.SET
                }
                argumentList = buildArgumentList {
                    for (indexVariable in indexVariables) {
                        arguments += indexVariable.toQualifiedAccess()
                    }

                    val getCall = buildFunctionCall {
                        explicitReceiver = arrayVariable.toQualifiedAccess()
                        calleeReference = buildSimpleNamedReference {
                            name = org.jetbrains.kotlin.util.OperatorNameConventions.GET
                        }
                        argumentList = buildArgumentList {
                            for (indexVariable in indexVariables) {
                                arguments += indexVariable.toQualifiedAccess()
                            }
                        }
                    }

                    val operatorCall = buildFunctionCall {
                        calleeReference = buildSimpleNamedReference {
                            name = AstOperationNameConventions.ASSIGNMENTS_TO_SIMPLE_OPERATOR.getValue(operation)
                        }
                        explicitReceiver = getCall
                        argumentList = buildArgumentList {
                            arguments += rhs?.convert() ?: buildErrorExpression(
                                null,
                                ConeSimpleDiagnostic(
                                    "No value for array set",
                                    DiagnosticKind.Syntax
                                )
                            )
                        }
                    }
                    arguments += operatorCall
                }
            }
        }
    }

    fun IElementType.toBinaryName(): Name? {
        return OperatorConventions.BINARY_OPERATION_NAMES[this]
    }

    fun IElementType.toUnaryName(): Name? {
        return OperatorConventions.UNARY_OPERATION_NAMES[this]
    }

    fun AstExpression.generateNotNullOrOther(other: AstExpression): AstElvisExpression {
        return buildElvisExpression {
            lhs = this@generateNotNullOrOther
            rhs = other
        }
    }

    private fun generateTemporaryVariable(
        name: Name, initializer: AstExpression, typeRef: AstType? = null,
    ): AstVariable<*> =
        buildProperty {
            returnType = typeRef ?: buildImplicitTypeRef {
            }
            this.name = name
            this.initializer = initializer
            symbol = AstPropertySymbol(name)
            isVar = false
            isLocal = true
            status = AstDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
        }

    private fun generateTemporaryVariable(
        specialName: String, initializer: AstExpression,
    ): AstVariable<*> = generateTemporaryVariable(Name.special("<$specialName>"), initializer)

    private fun AstPropertyBuilder.generateAccessorsByDelegate(
        delegateBuilder: AstWrappedDelegateExpressionBuilder?,
        ownerClassBuilder: AstClassBuilder?,
        isExtension: Boolean,
        receiver: AstExpression?
    ) {
        if (delegateBuilder == null) return
        val delegateFieldSymbol = AstDelegateFieldSymbol<AstProperty>(symbol.callableId).also {
            this.delegateFieldSymbol = it
        }
        val ownerSymbol = when (ownerClassBuilder) {
            is AstAnonymousObjectBuilder -> ownerClassBuilder.symbol
            is AstRegularClassBuilder -> ownerClassBuilder.symbol
            else -> null
        }
        val isMember = ownerSymbol != null

        fun thisRef(): AstExpression =
            when {
                ownerSymbol != null -> buildThisReceiverExpression {
                    calleeReference = buildImplicitThisReference {
                        boundSymbol = ownerSymbol
                    }
                    typeRef = buildResolvedTypeRef {
                        val typeParameterNumber = (ownerClassBuilder as? AstRegularClassBuilder)?.typeParameters?.size ?: 0
                        type = ownerSymbol.constructStarProjectedType(typeParameterNumber)
                    }
                }
                isExtension -> buildThisReceiverExpression {
                    calleeReference = buildImplicitThisReference {
                        boundSymbol = this@generateAccessorsByDelegate.symbol
                    }
                }
                else -> buildConst(null, AstConstKind.Null, null)
            }

        fun delegateAccess() = buildQualifiedAccessExpression {
            calleeReference = buildDelegateFieldReference {
                resolvedSymbol = delegateFieldSymbol
            }
            if (ownerSymbol != null) {
                dispatchReceiver = thisRef()
            }
        }

        val isVar = this@generateAccessorsByDelegate.isVar
        fun propertyRef() = buildCallableReferenceAccess {
            calleeReference = buildResolvedNamedReference {
                name = this@generateAccessorsByDelegate.name
                resolvedSymbol = this@generateAccessorsByDelegate.symbol
            }
            typeRef = when {
                !isMember && !isExtension -> if (isVar) {
                    AstImplicitKMutableProperty0TypeRef(null, ConeStarProjection)
                } else {
                    AstImplicitKProperty0TypeRef(null, ConeStarProjection)
                }
                isMember && isExtension -> if (isVar) {
                    AstImplicitKMutableProperty2TypeRef(null, ConeStarProjection, ConeStarProjection, ConeStarProjection)
                } else {
                    AstImplicitKProperty2TypeRef(null, ConeStarProjection, ConeStarProjection, ConeStarProjection)
                }
                else -> if (isVar) {
                    AstImplicitKMutableProperty1TypeRef(null, ConeStarProjection, ConeStarProjection)
                } else {
                    AstImplicitKProperty1TypeRef(null, ConeStarProjection, ConeStarProjection)
                }
            }
        }

        delegateBuilder.delegateProvider = buildFunctionCall {
            explicitReceiver = receiver
            calleeReference = buildSimpleNamedReference {
                name = PROVIDE_DELEGATE
            }
            argumentList = buildBinaryArgumentList(thisRef(), propertyRef())
        }
        delegate = delegateBuilder.build()
        if (getter == null || getter is AstDefaultPropertyAccessor) {
            val returnTarget = AstFunctionTarget(null, isLambda = false)
            getter = buildPropertyAccessor {
                returnType = buildImplicitTypeRef()
                isGetter = true
                status = AstDeclarationStatusImpl(Visibilities.Unknown, Modality.FINAL)
                symbol = AstPropertyAccessorSymbol()

                body = AstSingleExpressionBlock(
                    buildReturnExpression {
                        result = buildFunctionCall {
                            explicitReceiver = delegateAccess()
                            calleeReference = buildSimpleNamedReference {
                                name = GET_VALUE
                            }
                            argumentList = buildBinaryArgumentList(thisRef(), propertyRef())
                        }
                        target = returnTarget
                    }
                )
            }.also {
                returnTarget.bind(it)
            }
        }
        if (isVar && (setter == null || setter is AstDefaultPropertyAccessor)) {
            setter = buildPropertyAccessor {
                returnType = session.builtinTypes.unitType
                isGetter = false
                status = AstDeclarationStatusImpl(Visibilities.Unknown, Modality.FINAL)
                val parameter = buildValueParameter {
                    returnType = buildImplicitTypeRef()
                    name = DELEGATED_SETTER_PARAM
                    symbol = AstVariableSymbol(this@generateAccessorsByDelegate.name)
                    isCrossinline = false
                    isNoinline = false
                    isVararg = false
                }
                valueParameters += parameter
                symbol = AstPropertyAccessorSymbol()
                body = AstSingleExpressionBlock(
                    buildFunctionCall {
                        explicitReceiver = delegateAccess()
                        calleeReference = buildSimpleNamedReference {
                            name = SET_VALUE
                        }
                        argumentList = buildArgumentList {
                            arguments += thisRef()
                            arguments += propertyRef()
                            arguments += buildQualifiedAccessExpression {
                                calleeReference = buildResolvedNamedReference {
                                    name = DELEGATED_SETTER_PARAM
                                    resolvedSymbol = parameter.symbol
                                }
                            }
                        }
                    }
                )
            }
        }
    }

    private val GET_VALUE = Name.identifier("getValue")
    private val SET_VALUE = Name.identifier("setValue")
    private val PROVIDE_DELEGATE = Name.identifier("provideDelegate")
    private val DELEGATED_SETTER_PARAM = Name.special("<set-?>")

    private fun AstExpression.checkReceiver(name: String?): Boolean {
        if (this !is AstQualifiedAccessExpression) return false
        val receiver = explicitReceiver as? AstQualifiedAccessExpression ?: return false
        val receiverName = (receiver.calleeReference as? AstNamedReference)?.name?.asString() ?: return false
        return receiverName == name
    }


    private fun AstQualifiedAccess.wrapWithSafeCall(receiver: AstExpression): AstSafeCallExpression {
        val checkedSafeCallSubject = buildCheckedSafeCallSubject {
            @OptIn(AstContractViolation::class)
            this.originalReceiverRef = AstElementRef<AstExpression>().apply {
                bind(receiver)
            }
        }

        replaceExplicitReceiver(checkedSafeCallSubject)
        return buildSafeCallExpression {
            this.receiver = receiver
            @OptIn(AstContractViolation::class)
            this.checkedSubjectRef = AstElementRef<AstCheckedSafeCallSubject>().apply {
                bind(checkedSafeCallSubject)
            }
            this.regularQualifiedAccess = this@wrapWithSafeCall
        }
    }

    private fun KtWhenCondition.toAstWhenCondition(
        whenRefWithSubject: AstElementRef<AstWhen>,
        convert: KtExpression?.(String) -> AstExpression,
        toAstOrErrorTypeRef: KtTypeReference?.() -> AstType,
    ): AstExpression {
        val astSubjectExpression = buildWhenSubjectExpression {
            whenRef = whenRefWithSubject
        }
        return when (this) {
            is KtWhenConditionWithExpression -> {
                buildEqualityOperatorCall {
                    operation = AstOperation.EQ
                    argumentList = buildBinaryArgumentList(
                        astSubjectExpression, expression.convert("No expression in condition with expression")
                    )
                }
            }
            is KtWhenConditionInRange -> {
                val astRange = rangeExpression.convert("No range in condition with range")
                astRange.generateContainsOperation(
                    astSubjectExpression,
                    isNegated
                )
            }
            is KtWhenConditionIsPattern -> {
                buildTypeOperatorCall {
                    operation = if (isNegated) AstOperation.NOT_IS else AstOperation.IS
                    conversionTypeRef = typeReference.toAstOrErrorTypeRef()
                    argumentList = buildUnaryArgumentList(astSubjectExpression)
                }
            }
            else -> error("Unsupported when condition $this")
        }
    }

    private fun Array<KtWhenCondition>.toAstWhenCondition(
        subject: AstElementRef<AstWhen>,
        convert: KtExpression?.(String) -> AstExpression,
        toAstOrErrorTypeRef: KtTypeReference?.() -> AstType,
    ): AstExpression {
        var astCondition: AstExpression? = null
        for (condition in this) {
            val astConditionElement = condition.toAstWhenCondition(subject, convert, toAstOrErrorTypeRef)
            astCondition = when (astCondition) {
                null -> astConditionElement
                else -> astCondition.generateLazyLogicalOperation(astConditionElement, false,)
            }
        }
        return astCondition!!
    }

    private fun generateDestructuringBlock(
        multiDeclaration: KtDestructuringDeclaration,
        container: AstVariable<*>,
        tmpVariable: Boolean,
        extractAnnotationsTo: KtAnnotated.(AstAnnotationContainerBuilder) -> Unit,
        toAstOrImplicitTypeRef: KtTypeReference?.() -> AstType,
    ): AstExpression {
        return buildBlock {
            if (tmpVariable) {
                statements += container
            }
            val isVar = multiDeclaration.isVar
            for ((index, entry) in multiDeclaration.entries.withIndex()) {
                val name = entry.nameAsSafeName
                statements += buildProperty {
                    returnType = entry.typeReference.toAstOrImplicitTypeRef()
                    this.name = name
                    initializer = buildComponentCall {
                        explicitReceiver = generateResolvedAccessExpression(container)
                        componentIndex = index + 1
                    }
                    this.isVar = isVar
                    isLocal = true
                    status = AstDeclarationStatusImpl(Visibilities.Local, Modality.FINAL)
                    symbol = AstPropertySymbol(name)
                    entry.extractAnnotationsTo(this)
                }
            }
        }
    }*/

}
