package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.declarations.builder.buildFile
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.extension.AstGeneratorContext
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.js.translate.callTranslator.getReturnType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtExpressionWithLabel
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi.KtWhileExpressionBase
import org.jetbrains.kotlin.psi.psiUtil.getParentOfType
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassValueReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.SuperCallReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisClassReceiver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils

class Psi2AstVisitor(
    override val context: Psi2AstGeneratorContext
) : KtVisitor<AstElement, Nothing?>(), Generator {

    private val annotationGenerator = AnnotationGenerator()

    private val functionStack = mutableListOf<AstFunction<*>>()
    private val loops = mutableMapOf<KtLoopExpression, AstLoop>()

    private fun <K, V> K.cached(
        cache: MutableMap<K, V>,
        init: () -> V,
        block: V.() -> Unit
    ) = cache.getOrPut(if (this is DeclarationDescriptor) original as K else this, init)
        .apply(block)

    private fun <T : AstElement> KtElement.convert() =
        accept(this@Psi2AstVisitor, null) as T

    override fun visitElement(element: PsiElement?) {
        error("Unhandled element $element ${element?.javaClass} ${element?.text}")
    }

    override fun visitKtFile(file: KtFile, data: Nothing?): AstElement {
        return buildFile {
            packageFqName = file.packageFqName
            name = file.name
            annotations += file.annotationEntries.map { it.convert() }
            declarations += file.declarations.map { it.convert() }
        }
    }

    /*override fun visitClassOrObject(classOrObject: KtClassOrObject, data: Nothing?): AstElement {
        val descriptor = classOrObject.descriptor<ClassDescriptor>()
        return descriptor.cached(
            context.storage.classes,
            {
                AstClass(
                    name = descriptor.name,
                    kind = descriptor.toAstClassKind(),
                    visibility = descriptor.visibility.toAstVisibility(),
                    expectActual = platformStatusOf(descriptor.isActual, descriptor.isExpect),
                    modality = descriptor.modality.toAstModality(),
                    isCompanion = descriptor.isCompanionObject,
                    isFun = descriptor.isFun,
                    isData = descriptor.isData,
                    isInner = descriptor.isInner,
                    isExternal = descriptor.isExternal
                ).applyParentFromStack()
            }
        ) {
            withDeclarationParent(this) {
                if (mode == Mode.FULL) {
                    annotations.clear()
                    annotations += classOrObject.annotationEntries.map { it.accept(mode) }
                }
                typeParameters.clear()
                typeParameters += classOrObject.typeParameters
                    .map { it.accept(mode) }

                val primaryConstructor = classOrObject.primaryConstructor

                if (primaryConstructor != null) {
                    val astPrimaryConstructor = primaryConstructor.accept<AstFunction>(mode)
                    this.primaryConstructor = astPrimaryConstructor
                    // todo? addChild(astPrimaryConstructor)
                    /*primaryConstructor
                        .valueParameters
                        .mapNotNull { get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, it) }
                        .map { property ->
                            property.findPsi()!!
                                .accept<AstProperty>(mode)
                            property.toAstProperty(
                                astPrimaryConstructor.valueParameters
                                    .single { it.name == property.name }
                            )
                        }
                        .forEach { addChild(it) }*/
                }

                declarations.clear()
                classOrObject.declarations
                    .filterNot { it is KtPropertyAccessor }
                    .map { it.accept<AstDeclaration>(mode) }
                    .forEach { addChild(it) }
            }
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction, data: Nothing?): AstFunction =
        visitFunction(function, mode)

    override fun visitPrimaryConstructor(
        constructor: KtPrimaryConstructor,
        data: Nothing?
    ): AstFunction = visitFunction(constructor, mode)

    override fun visitSecondaryConstructor(
        constructor: KtSecondaryConstructor,
        data: Nothing?
    ): AstFunction = visitFunction(constructor, mode)

    private fun visitFunction(
        function: KtFunction,
        data: Nothing?,
        body: KtExpression? = function.bodyExpression
    ): AstFunction {
        val descriptor = function.descriptor<FunctionDescriptor>()
        return descriptor.cached(
            context.storage.functions,
            {
                AstFunction(
                    name = descriptor.name,
                    kind = descriptor.toAstFunctionKind(),
                    visibility = descriptor.visibility.toAstVisibility(),
                    expectActual = platformStatusOf(descriptor.isActual, descriptor.isExpect),
                    modality = descriptor.modality.toAstModality(),
                    returnType = UninitializedType,
                    isInfix = descriptor.isInfix,
                    isOperator = descriptor.isOperator,
                    isTailrec = descriptor.isTailrec,
                    isSuspend = descriptor.isSuspend
                ).applyParentFromStack()
            }
        ) {
            if (mode == Mode.FULL) {
                returnType = descriptor.returnType!!.toAstType()
            }
            withDeclarationParent(this) {
                functionStack.push(this)
                if (mode == Mode.FULL) {
                    annotations.clear()
                    annotations += function.annotationEntries.map { it.accept(mode) }
                }
                typeParameters.clear()
                typeParameters += function.typeParameters
                    .map { it.accept(mode) }
                if (mode == Mode.FULL) {
                    dispatchReceiverType = descriptor.dispatchReceiverParameter?.type?.toAstType()
                    extensionReceiverType = descriptor.extensionReceiverParameter?.type?.toAstType()
                }
                valueParameters.clear()
                valueParameters += function.valueParameters
                    .map { it.accept(mode) }
                overriddenDeclarations.clear()
                overriddenDeclarations += descriptor.overriddenDescriptors
                    .map { context.provider.get(it) }
                if (mode == Mode.FULL) {
                    this.body = body?.let { visitExpressionForBlock(it, mode) }
                }
                functionStack.pop()
            }
        }
    }

    override fun visitProperty(property: KtProperty, data: Nothing?): AstElement {
        val descriptor = property.descriptor<VariableDescriptor>()
        return descriptor.cached(
            context.storage.properties,
            {
                AstProperty(
                    name = descriptor.name,
                    type = UninitializedType,
                    kind = when {
                        descriptor.isConst -> AstProperty.Kind.CONST_VAL
                        descriptor.isLateInit -> AstProperty.Kind.LATEINIT_VAR
                        descriptor.isVar -> AstProperty.Kind.VAR
                        else -> AstProperty.Kind.VAl
                    },
                    modality = if (descriptor is PropertyDescriptor) descriptor.modality.toAstModality()
                    else AstModality.FINAL,
                    visibility = descriptor.visibility.toAstVisibility(),
                    expectActual = if (descriptor is PropertyDescriptor) platformStatusOf(
                        descriptor.isActual,
                        descriptor.isExpect
                    )
                    else null,
                    isExternal = if (descriptor is PropertyDescriptor) descriptor.isExternal else false
                ).applyParentFromStack()
            }
        ) {
            if (mode == Mode.FULL) {
                type = descriptor.type.toAstType()
                annotations.clear()
                annotations += property.annotations.map { it.accept(mode) }
            }
            typeParameters.clear()
            typeParameters += property.typeParameters
                .map { it.accept(mode) }
            if (mode == Mode.FULL) {
                dispatchReceiverType = descriptor.dispatchReceiverParameter?.type?.toAstType()
                extensionReceiverType = descriptor.extensionReceiverParameter?.type?.toAstType()
            }
            getter = property.getter?.accept(mode)
            setter = property.setter?.accept(mode)
            if (mode == Mode.FULL) {
                initializer = when {
                    // todo valueParameter != null -> AstQualifiedAccess(valueParameter, valueParameter.type)
                    property is KtProperty -> property.initializer?.accept(mode)
                    else -> null
                }
                delegate = if (property is KtProperty)
                    property.delegateExpression?.accept(mode)
                else null
            }
        }
    }

    override fun visitAnonymousInitializer(
        initializer: KtAnonymousInitializer,
        data: Nothing?
    ): AstAnonymousInitializer {
        return initializer.cached(
            context.storage.anonymousInitializers,
            { AstAnonymousInitializer().applyParentFromStack() }
        ) {
            if (mode == Mode.FULL) {
                body = visitExpressionForBlock(initializer.body!!, mode)
            }
        }
    }

    override fun visitTypeParameter(parameter: KtTypeParameter, data: Nothing?): AstTypeParameter {
        val descriptor = parameter.descriptor<TypeParameterDescriptor>()
        return descriptor.cached(
            context.storage.typeParameters,
            {
                AstTypeParameter(
                    name = descriptor.name,
                    isReified = descriptor.isReified,
                    variance = descriptor.variance.toAstVariance()
                ).applyParentFromStack()
            }
        ) {
            if (mode == Mode.FULL) {
                annotations.clear()
                annotations += parameter.annotationEntries.map { it.accept(mode) }
                superTypes.clear()
                superTypes += descriptor.upperBounds.map { it.toAstType() }
            }
        }
    }

    override fun visitParameter(parameter: KtParameter, data: Nothing?): AstValueParameter {
        val descriptor = parameter.descriptor<VariableDescriptor>()
        return descriptor.cached(
            context.storage.valueParameters,
            {
                AstValueParameter(
                    name = descriptor.name,
                    type = UninitializedType,
                    isVararg = if (descriptor is ValueParameterDescriptor) descriptor.isVararg else false,
                    inlineHint = when {
                        descriptor is ValueParameterDescriptor && descriptor.isCrossinline -> AstValueParameter.InlineHint.CROSSINLINE
                        descriptor is ValueParameterDescriptor && descriptor.isNoinline -> AstValueParameter.InlineHint.NOINLINE
                        else -> null
                    }
                ).applyParentFromStack()
            }
        ) {
            if (mode == Mode.FULL) {
                annotations.clear()
                annotations += parameter.annotationEntries.map { it.accept(mode) }
                type = descriptor.type.toAstType()
                defaultValue = (descriptor.findPsi() as? KtParameter)?.defaultValue?.accept(mode)
            }
        }
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias, data: Nothing?): AstTypeAlias {
        val descriptor = typeAlias.descriptor<TypeAliasDescriptor>()
        return descriptor.cached(
            context.storage.typeAliases,
            {
                AstTypeAlias(
                    name = descriptor.name,
                    type = UninitializedType,
                    visibility = descriptor.visibility.toAstVisibility(),
                    expectActual = platformStatusOf(descriptor.isActual, descriptor.isExpect)
                ).applyParentFromStack()
            }
        ) {
            if (mode == Mode.FULL) {
                annotations.clear()
                annotations += typeAlias.annotationEntries.map { it.accept(mode) }
                type = descriptor.expandedType.toAstType()
            }

            typeParameters.clear()
            typeParameters += typeAlias.typeParameters
                .map { it.accept(mode) }
        }
    }

    override fun visitObjectLiteralExpression(
        expression: KtObjectLiteralExpression,
        data: Nothing?
    ): AstAnonymousObjectExpression {
        // important to compute the ast declaration before asking for it's type
        val anonymousObject = expression.objectDeclaration.accept<AstClass>(mode)
        return AstAnonymousObjectExpression(
            expression.getTypeInferredByFrontendOrFail().toAstType(),
            anonymousObject
        )
    }

    override fun visitLambdaExpression(expression: KtLambdaExpression, data: Nothing?): AstElement {
        return AstAnonymousFunctionExpression(
            type = expression.getTypeInferredByFrontendOrFail().toAstType(),
            anonymousFunction = visitFunction(
                function = expression.functionLiteral,
                mode = mode,
                body = expression.bodyExpression
            )
        )
    }

    override fun visitBlockExpression(expression: KtBlockExpression, data: Nothing?): AstBlock {
        return AstBlock(expression.getExpressionTypeWithCoercionToUnitOrFail().toAstType()).apply {
            statements += expression.statements.map { it.accept(mode) }
        }
    }

    private fun visitExpressionForBlock(expression: KtExpression, data: Nothing?): AstBlock {
        return if (expression is KtBlockExpression) expression.accept(mode)
        else {
            val type = expression.getTypeInferredByFrontendOrFail().toAstType()
            val astExpression = expression.accept<AstExpression>(mode)
            AstBlock(type).apply {
                statements += if (functionStack.isNotEmpty()) {
                    AstReturn(
                        type,
                        functionStack.last(),
                        astExpression
                    )
                } else {
                    astExpression
                }
            }
        }
    }

    override fun visitStringTemplateExpression(
        expression: KtStringTemplateExpression,
        data: Nothing?
    ): AstElement {
        val resultType = expression.getTypeInferredByFrontendOrFail().toAstType()
        val entries = expression.entries.map { it.accept<AstExpression>(mode) }
        return when (entries.size) {
            0 -> AstConst.string(resultType, "")
            1 -> {
                val entry = entries.single()
                if (entry is AstConst<*> && entry.kind == AstConst.Kind.String) {
                    entry
                } else {
                    val toString = context.builtIns.anyClass
                        .declarations
                        .filterIsInstance<AstFunction>()
                        .first { it.name.asString() == "toString" }

                    AstQualifiedAccess(
                        callee = toString,
                        type = context.builtIns.stringType
                    ).apply {
                        dispatchReceiver = entry
                    }
                }
            }
            else -> {
                val stringPlus = context.builtIns.stringClass
                    .declarations
                    .filterIsInstance<AstFunction>()
                    .first { it.name.asString() == "plus" }
                entries.reduce { acc, entry ->
                    AstQualifiedAccess(
                        callee = stringPlus,
                        type = context.builtIns.stringType
                    ).apply {
                        dispatchReceiver = acc
                        valueArguments += entry
                    }
                }
            }
        }
    }

    override fun visitLiteralStringTemplateEntry(
        entry: KtLiteralStringTemplateEntry,
        data: Nothing?
    ): AstConst<String> = AstConst.string(context.builtIns.stringType, entry.text)

    override fun visitEscapeStringTemplateEntry(
        entry: KtEscapeStringTemplateEntry,
        data: Nothing?
    ): AstConst<String> = AstConst.string(context.builtIns.stringType, entry.unescapedValue)

    override fun visitStringTemplateEntryWithExpression(
        entry: KtStringTemplateEntryWithExpression,
        data: Nothing?
    ): AstExpression = entry.expression!!.accept(mode)

    override fun visitReferenceExpression(
        expression: KtReferenceExpression,
        data: Nothing?
    ): AstElement {
        val resolvedCall = expression.getResolvedCall()
            ?: error("Couldn't find call for $this $javaClass ${expression.text}")
        return AstQualifiedAccess(
            type = resolvedCall.getReturnType().toAstType(),
            callee = context.provider.get(resolvedCall.resultingDescriptor)
        ).apply {
            typeArguments += resolvedCall.typeArguments.values.map { it.toAstType() }

            dispatchReceiver = resolvedCall.dispatchReceiver?.toAstExpression(mode)
            extensionReceiver = resolvedCall.extensionReceiver?.toAstExpression(mode)

            val sortedValueArguments = resolvedCall.valueArguments
                .toList()
                .sortedBy { it.first.index }

            valueArguments += sortedValueArguments.map { (valueParameter, valueArgument) ->
                when (valueArgument) {
                    is DefaultValueArgument -> null
                    is ExpressionValueArgument -> {
                        val valueArgument1 = valueArgument.valueArgument!!
                        val argumentExpression = valueArgument1.getArgumentExpression()!!
                        argumentExpression.accept<AstExpression>(mode)
                    }
                    is VarargValueArgument -> {
                        if (valueArgument.arguments.isEmpty()) {
                            null
                        } else {
                            AstVararg(valueParameter.type.toAstType()).apply {
                                elements += valueArgument.arguments.map { varargArgument ->
                                    val ktArgumentExpression =
                                        varargArgument.getArgumentExpression()!!
                                    val astArgumentExpression =
                                        ktArgumentExpression.accept<AstExpression>(mode)
                                    if (varargArgument.getSpreadElement() != null || varargArgument.isNamed())
                                        AstSpreadElement(astArgumentExpression)
                                    else astArgumentExpression
                                }

                            }
                        }
                    }
                    else -> error("Unexpected valueArgument: ${valueArgument::class.java.simpleName}")
                }
            }
        }
    }

    override fun visitDestructuringDeclaration(
        multiDeclaration: KtDestructuringDeclaration,
        data: Nothing?
    ): AstExpression {
        return AstBlock(context.builtIns.unitType).apply {
            val ktInitializer = multiDeclaration.initializer!!
            val containerProperty = AstProperty(
                name = Name.special("<destructuring container>"),
                type = ktInitializer.getTypeInferredByFrontendOrFail().toAstType(),
                visibility = AstVisibility.LOCAL
            ).apply {
                applyParentFromStack()
                initializer = ktInitializer.accept(mode)
            }

            statements += containerProperty

            statements += multiDeclaration.entries
                .mapNotNull { ktEntry ->
                    val componentVariable = getOrFail(BindingContext.VARIABLE, ktEntry)
                    // componentN for '_' SHOULD NOT be evaluated
                    if (componentVariable.name.isSpecial) return@mapNotNull null
                    val componentResolvedCall =
                        getOrFail(BindingContext.COMPONENT_RESOLVED_CALL, ktEntry)
                    componentResolvedCall.getReturnType()

                    AstProperty(
                        name = componentVariable.name,
                        type = componentVariable.type.toAstType(),
                        visibility = AstVisibility.LOCAL
                    ).apply {
                        applyParentFromStack()
                        initializer = AstQualifiedAccess(
                            callee = context.provider.get(componentResolvedCall.resultingDescriptor),
                            type = componentVariable.type.toAstType()
                        ).apply {
                            dispatchReceiver = AstQualifiedAccess(
                                callee = containerProperty,
                                type = containerProperty.type
                            )
                        }
                    }
                }
        }
    }

    override fun visitThisExpression(expression: KtThisExpression, data: Nothing?): AstExpression {
        val referenceTarget =
            getOrFail(BindingContext.REFERENCE_TARGET, expression.instanceReference)
        TODO("What $referenceTarget")
    }

    override fun visitConstantExpression(
        expression: KtConstantExpression,
        data: Nothing?
    ) = visitExpressionForConstant(expression)

    private fun visitExpressionForConstant(expression: KtExpression): AstExpression {
        val constantValue =
            ConstantExpressionEvaluator.getConstant(expression, context.bindingContext)
                ?.toConstantValue(expression.getTypeInferredByFrontendOrFail())
                ?: error("KtConstantExpression was not evaluated: ${expression.text}")
        return context.generateConstantValueAsExpression(constantValue)
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, data: Nothing?): AstElement {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        if (ktOperator == KtTokens.IDENTIFIER) {
            error("lol ? $expression ${expression.text}")
        }

        val left = expression.left!!.accept<AstExpression>(mode)
        val right = expression.right!!.accept<AstExpression>(mode)

        return when (ktOperator) {
            KtTokens.EQ -> TODO()

            KtTokens.LT, KtTokens.LTEQ, KtTokens.GT, KtTokens.GTEQ -> AstComparisonOperation(
                context.builtIns.booleanType,
                when (ktOperator) {
                    KtTokens.LT -> AstComparisonOperation.Kind.LESS_THAN
                    KtTokens.LTEQ -> AstComparisonOperation.Kind.LESS_THEN_EQUALS
                    KtTokens.GT -> AstComparisonOperation.Kind.GREATER_THAN
                    KtTokens.GTEQ -> AstComparisonOperation.Kind.GREATER_THEN_EQUALS
                    else -> error("Unexpected operator $ktOperator")
                },
                left,
                right
            )

            KtTokens.EQEQ, KtTokens.EXCLEQ, KtTokens.EQEQEQ, KtTokens.EXCLEQEQEQ -> AstEqualityOperation(
                context.builtIns.booleanType,
                when (ktOperator) {
                    KtTokens.EQEQ -> AstEqualityOperation.Kind.EQUALS
                    KtTokens.EXCLEQ -> AstEqualityOperation.Kind.NOT_EQUALS
                    KtTokens.EQEQEQ -> AstEqualityOperation.Kind.IDENTITY
                    KtTokens.EXCLEQEQEQ -> AstEqualityOperation.Kind.NOT_IDENTITY
                    else -> error("Unexpected operator $ktOperator")
                },
                left,
                right
            )

            KtTokens.ANDAND, KtTokens.OROR -> AstLogicOperation(
                context.builtIns.booleanType,
                when (ktOperator) {
                    KtTokens.ANDAND -> AstLogicOperation.Kind.AND
                    KtTokens.OROR -> AstLogicOperation.Kind.OR
                    else -> error("Unexpected operator $ktOperator")
                },
                left,
                right
            )

            KtTokens.PLUSEQ -> TODO()
            KtTokens.MINUSEQ -> TODO()
            KtTokens.MULTEQ -> TODO()
            KtTokens.DIVEQ -> TODO()
            KtTokens.PERCEQ -> TODO()
            KtTokens.PLUS -> TODO()
            KtTokens.MINUS -> TODO()
            KtTokens.MUL -> TODO()
            KtTokens.DIV -> TODO()
            KtTokens.PERC -> TODO()
            KtTokens.RANGE -> TODO()

            KtTokens.IN_KEYWORD -> TODO()
            KtTokens.NOT_IN -> TODO()

            KtTokens.ELVIS -> TODO()
            else -> TODO()
        }
    }

    override fun visitIfExpression(expression: KtIfExpression, data: Nothing?): AstElement {
        var ktLastIf: KtIfExpression = expression
        val astBranches = mutableListOf<AstBranch>()
        var astElseBranch: AstExpression? = null

        whenBranches@ while (true) {
            val astCondition = ktLastIf.condition!!.accept<AstExpression>(mode)
            val astThenBranch = ktLastIf.then?.accept<AstExpression>(mode)
                ?: AstBlock(context.builtIns.unitType)
            astBranches += AstConditionBranch(astCondition, astThenBranch)

            when (val ktElse = ktLastIf.`else`?.deparenthesize()) {
                null -> break@whenBranches
                is KtIfExpression -> ktLastIf = ktElse
                is KtExpression -> {
                    astElseBranch = ktElse.accept(mode)
                    break@whenBranches
                }
                else -> error("Unexpected else expression: ${ktElse.text}")
            }
        }

        return AstWhen(expression.getTypeInferredByFrontendOrFail().toAstType()).apply {
            branches += astBranches
            if (astElseBranch != null) branches += AstElseBranch(astElseBranch)
        }
    }

    /*override fun visitWhenExpression(expression: KtWhenExpression, data: Nothing?): AstElement {
        val subjectVariable = expression.subjectVariable
        val subjectExpression = expression.subjectExpression
        val astSubject = when {
            subjectVariable != null -> subjectVariable.accept(mode)
            subjectExpression != null -> {
                val astSubjectExpression = subjectExpression.accept<AstExpression>(mode)
                AstProperty(
                    name = Name.special("<when subject>"),
                    type = astSubjectExpression.type,
                    visibility = AstVisibility.LOCAL
                ).apply {
                    applyParentFromStack()
                    initializer = astSubjectExpression
                }
            }
            else -> null
        }
        val astWhen = AstWhen(expression.getTypeInferredByFrontendOrFail().toAstType())
        for (ktEntry in expression.entries) {
            if (ktEntry.isElse) {
                val astElseResult = ktEntry.expression!!.accept<AstExpression>(mode)
                astWhen.branches.add(AstElseBranch(astElseResult))
                break
            }
            var astBranchCondition: AstExpression? = null
            for (ktCondition in ktEntry.conditions) {
                val astCondition = if (astSubject != null)
                    generateWhenConditionWithSubject(ktCondition, astSubject)
                else
                    generateWhenConditionNoSubject(ktCondition)
                astBranchCondition =
                    astBranchCondition?.let { context.whenComma(it, astCondition) } ?: astCondition
            }
            val astBranchResult = ktEntry.expression!!.accept<AstExpression>(mode)
            astWhen.branches += AstConditionBranch(astBranchCondition!!, astBranchResult)
        }
        addElseBranchForExhaustiveWhenIfNeeded(irWhen, expression)
        if (irSubject == null) {
            if (irWhen.branches.isEmpty())
                IrBlockImpl(
                    expression.startOffsetSkippingComments,
                    expression.endOffset,
                    context.irBuiltIns.unitType,
                    IrStatementOrigin.WHEN
                )
            else
                irWhen
        } else {
            if (irWhen.branches.isEmpty()) {
                val irBlock = IrBlockImpl(
                    expression.startOffsetSkippingComments,
                    expression.endOffset,
                    context.irBuiltIns.unitType,
                    IrStatementOrigin.WHEN
                )
                irBlock.statements.add(irSubject)
                irBlock
            } else {
                val irBlock = IrBlockImpl(
                    expression.startOffsetSkippingComments,
                    expression.endOffset,
                    irWhen.type,
                    IrStatementOrigin.WHEN
                )
                irBlock.statements.add(irSubject)
                irBlock.statements.add(irWhen)
                irBlock
            }
        }
    }
    private fun addElseBranchForExhaustiveWhenIfNeeded(
        irWhen: IrWhen,
        whenExpression: KtWhenExpression
    ) {
        if (irWhen.branches.filterIsInstance<IrElseBranch>().isEmpty()) {
            //TODO: check condition: seems it's safe to always generate exception
            val isExhaustive = whenExpression.isExhaustiveWhen()
            if (isExhaustive) {
                val call = IrCallImpl(
                    UNDEFINED_OFFSET, UNDEFINED_OFFSET,
                    context.irBuiltIns.nothingType,
                    context.irBuiltIns.noWhenBranchMatchedExceptionSymbol
                )
                irWhen.branches.add(elseBranch(call))
            }
        }
    }
    private fun KtWhenExpression.isExhaustiveWhen(): Boolean =
        elseExpression != null // TODO front-end should provide correct exhaustiveness information
                || true == get(BindingContext.EXHAUSTIVE_WHEN, this)
                || true == get(BindingContext.IMPLICIT_EXHAUSTIVE_WHEN, this)
    private fun generateWhenConditionNoSubject(ktCondition: KtWhenCondition): IrExpression =
        (ktCondition as KtWhenConditionWithExpression).expression!!.genExpr()
    private fun generateWhenConditionWithSubject(
        ktCondition: KtWhenCondition,
        irSubject: IrVariable
    ): IrExpression {
        return when (ktCondition) {
            is KtWhenConditionWithExpression ->
                generateEqualsCondition(irSubject, ktCondition)
            is KtWhenConditionInRange ->
                generateInRangeCondition(irSubject, ktCondition)
            is KtWhenConditionIsPattern ->
                generateIsPatternCondition(irSubject, ktCondition)
            else ->
                error("Unexpected 'when' condition: ${ktCondition.text}")
        }
    }
    private fun generateIsPatternCondition(irSubject: IrVariable, ktCondition: KtWhenConditionIsPattern): IrExpression {
        val typeOperand = getOrFail(BindingContext.TYPE, ktCondition.typeReference)
        val irTypeOperand = typeOperand.toAstType()
        val startOffset = ktCondition.startOffsetSkippingComments
        val endOffset = ktCondition.endOffset
        val irInstanceOf = IrTypeOperatorCallImpl(
            startOffset, endOffset,
            context.builtIns.booleanType,
            IrTypeOperator.INSTANCEOF,
            irTypeOperand,
            irSubject.loadAt(startOffset, startOffset)
        )
        return if (ktCondition.isNegated)
            primitiveOp1(
                ktCondition.startOffsetSkippingComments, ktCondition.endOffset,
                context.irBuiltIns.booleanNotSymbol,
                context.irBuiltIns.booleanType,
                IrStatementOrigin.EXCL,
                irInstanceOf
            )
        else
            irInstanceOf
    }
    private fun generateInRangeCondition(irSubject: IrVariable, ktCondition: KtWhenConditionInRange): IrExpression {
        val inCall = statementGenerator.pregenerateCall(getResolvedCall(ktCondition.operationReference)!!)
        val startOffset = ktCondition.startOffsetSkippingComments
        val endOffset = ktCondition.endOffset
        inCall.irValueArgumentsByIndex[0] = irSubject.loadAt(startOffset, startOffset)
        val inOperator = getInfixOperator(ktCondition.operationReference.getReferencedNameElementType())
        val irInCall = CallGenerator(statementGenerator).generateCall(ktCondition, inCall, inOperator)
        return when (inOperator) {
            IrStatementOrigin.IN ->
                irInCall
            IrStatementOrigin.NOT_IN ->
                primitiveOp1(
                    startOffset, endOffset,
                    context.irBuiltIns.booleanNotSymbol,
                    context.irBuiltIns.booleanType,
                    IrStatementOrigin.EXCL,
                    irInCall
                )
            else -> error("Expected 'in' or '!in', got $inOperator")
        }
    }
    private fun generateEqualsCondition(irSubject: IrVariable, ktCondition: KtWhenConditionWithExpression): IrExpression {
        val ktExpression = ktCondition.expression
        val irExpression = ktExpression!!.genExpr()
        val startOffset = ktCondition.startOffsetSkippingComments
        val endOffset = ktCondition.endOffset
        return OperatorExpressionGenerator(statementGenerator).generateEquality(
            startOffset, endOffset, IrStatementOrigin.EQEQ,
            irSubject.loadAt(startOffset, startOffset), irExpression,
            context.bindingContext[BindingContext.PRIMITIVE_NUMERIC_COMPARISON_INFO, ktExpression]
        )
    }*/

    override fun visitWhileExpression(expression: KtWhileExpression, data: Nothing?) =
        visitWhile(expression, mode)

    override fun visitDoWhileExpression(
        expression: KtDoWhileExpression,
        data: Nothing?
    ) = visitWhile(expression, mode)

    private fun visitWhile(expression: KtWhileExpressionBase, data: Nothing?) =
        AstWhileLoop(
            context.builtIns.unitType,
            when (expression) {
                is KtWhileExpression -> AstWhileLoop.Kind.WHILE
                is KtDoWhileExpression -> AstWhileLoop.Kind.DO_WHILE
                else -> error("Unexpected while expression $expression")
            }
        ).apply {
            loops[expression] = this
            body = visitExpressionForBlock(expression.body!!, mode)
            condition = expression.condition!!.accept(mode)
        }

    override fun visitForExpression(expression: KtForExpression, data: Nothing?): AstElement {
        val ktLoopParameter = expression.loopParameter
        val ktLoopDestructuringDeclaration = expression.destructuringDeclaration
        if (ktLoopParameter == null && ktLoopDestructuringDeclaration == null) {
            error("Either loopParameter or destructuringParameter should be present:\n${expression.text}")
        }
        return AstForLoop(context.builtIns.unitType)
            .apply {
                loops[expression] = this
                loopParameter =
                    ktLoopParameter?.accept(mode) ?: TODO("Destructuring not supported in loops")
                loopRange = expression.loopRange!!.accept(mode)
                body = visitExpressionForBlock(expression.body!!, mode)
            }
    }

    override fun visitBreakExpression(expression: KtBreakExpression, data: Nothing?): AstElement {
        val parentLoop = findParentLoop(expression)
            ?: error("No loop found for ${expression.text}")
        return AstBreak(
            type = context.builtIns.nothingType,
            loop = parentLoop
        )
    }

    override fun visitContinueExpression(
        expression: KtContinueExpression,
        data: Nothing?
    ): AstElement {
        val parentLoop = findParentLoop(expression)
            ?: error("No loop found for ${expression.text}")
        return AstContinue(
            type = context.builtIns.nothingType,
            loop = parentLoop
        )
    }

    override fun visitTryExpression(expression: KtTryExpression, data: Nothing?): AstElement {
        return AstTry(
            type = expression.getExpressionTypeWithCoercionToUnitOrFail().toAstType(),
            tryResult = expression.tryBlock.accept(mode),
            catches = expression.catchClauses
                .map { catchClause ->
                    AstCatch(
                        catchParameter = catchClause.catchParameter!!.accept(mode),
                        result = visitExpressionForBlock(catchClause.catchBody!!, mode)
                    )
                }
                .toMutableList(),
            finally = expression.finallyBlock?.finalExpression?.accept(mode)
        )
    }

    override fun visitReturnExpression(expression: KtReturnExpression, data: Nothing?): AstElement {
        val returnTarget = getReturnExpressionTarget(expression, null) as FunctionDescriptor
        return AstReturn(
            type = context.builtIns.nothingType,
            target = context.provider.get(returnTarget),
            expression = expression.returnedExpression?.accept(mode) ?: AstQualifiedAccess(
                callee = context.builtIns.unitClass,
                type = context.builtIns.unitType
            )
        )
    }

    override fun visitThrowExpression(expression: KtThrowExpression, data: Nothing?): AstElement {
        return AstThrow(
            type = context.builtIns.nothingType,
            expression = expression.thrownExpression!!.accept(mode)
        )
    }

    override fun visitDotQualifiedExpression(
        expression: KtDotQualifiedExpression,
        data: Nothing?
    ): AstElement {
        return expression.selectorExpression!!.accept<AstQualifiedAccess>(mode).also {
            it.safe = false
        }
    }

    override fun visitSafeQualifiedExpression(
        expression: KtSafeQualifiedExpression,
        data: Nothing?
    ): AstElement {
        return expression.selectorExpression!!.accept<AstQualifiedAccess>(mode).also {
            it.safe = true
        }
    }

    fun ReceiverValue.toAstExpression(data: Nothing?): AstExpression {
        return when (this) {
            is ImplicitClassReceiver -> {
                val receiverClass = context.provider.get<AstClass>(classDescriptor)
                AstThis(type.toAstType(), receiverClass)
            }
            is ThisClassReceiver -> {
                val receiverClass = context.provider.get<AstClass>(classDescriptor)
                AstThis(type.toAstType(), receiverClass)
            }
            is SuperCallReceiverValue -> TODO()
            is ExpressionReceiver -> expression.accept(mode)
            is ClassValueReceiver -> {
                val receiverClass = context.provider.get<AstClass>(
                    classQualifier.descriptor as ClassDescriptor
                )
                AstThis(type.toAstType(), receiverClass)
            }
            /*is ExtensionReceiver -> {
                AstQualifiedAccess(declarationDescriptor.extensionReceiverParameter!!)
            }*/
            else -> error("Unexpected receiver value $this")
        }
    }

    private fun scopeOwnerAsCallable(scopeOwner: FunctionDescriptor?) =
        (scopeOwner as? CallableDescriptor)
            ?: error("'return' in a non-callable: $scopeOwner")

    private fun getReturnExpressionTarget(
        expression: KtReturnExpression,
        scopeOwner: FunctionDescriptor?
    ): CallableDescriptor =
        if (!ExpressionTypingUtils.isFunctionLiteral(scopeOwner) && !ExpressionTypingUtils.isFunctionExpression(
                scopeOwner
            )
        ) {
            scopeOwnerAsCallable(scopeOwner)
        } else {
            val label = expression.getTargetLabel()
            when {
                label != null -> {
                    val labelTarget = getOrFail(BindingContext.LABEL_TARGET, label)
                    val labelTargetDescriptor =
                        getOrFail(BindingContext.DECLARATION_TO_DESCRIPTOR, labelTarget)
                    labelTargetDescriptor as CallableDescriptor
                }
                ExpressionTypingUtils.isFunctionLiteral(scopeOwner) -> {
                    BindingContextUtils.getContainingFunctionSkipFunctionLiterals(
                        scopeOwner,
                        true
                    ).first
                }
                else -> {
                    scopeOwnerAsCallable(scopeOwner)
                }
            }
        }

    private fun findParentLoop(ktWithLabel: KtExpressionWithLabel): AstLoop? =
        findParentLoop(ktWithLabel, ktWithLabel.getLabelName())

    private fun findParentLoop(ktExpression: KtExpression, targetLabel: String?): AstLoop? {
        var current: KtExpression? = ktExpression

        loops@ while (current != null) {
            current = current.getParentOfType<KtLoopExpression>(true)
            if (current == null) {
                break
            }
            if (targetLabel == null) {
                return loops[current] ?: continue@loops
            } else {
                var parent = current.parent
                while (parent is KtLabeledExpression) {
                    val label = parent.getLabelName()!!
                    if (targetLabel == label) {
                        return loops[current] ?: continue@loops
                    }
                    parent = parent.parent
                }
            }
        }
        return null
    }

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, data: Mode?): AstElement {
        val descriptor = getOrFail(BindingContext.ANNOTATION, annotationEntry)
        return context.generateAnnotationConstructorCall(descriptor)!!
    }*/

}
