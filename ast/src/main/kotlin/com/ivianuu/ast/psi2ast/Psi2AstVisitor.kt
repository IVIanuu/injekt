package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.AstElement
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.declarations.AstNamedFunction
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.builder.buildConstructor
import com.ivianuu.ast.declarations.builder.buildFile
import com.ivianuu.ast.declarations.builder.buildNamedFunction
import com.ivianuu.ast.declarations.builder.buildProperty
import com.ivianuu.ast.declarations.builder.buildPropertyAccessor
import com.ivianuu.ast.declarations.builder.buildRegularClass
import com.ivianuu.ast.declarations.builder.buildTypeAlias
import com.ivianuu.ast.declarations.builder.buildTypeParameter
import com.ivianuu.ast.declarations.builder.buildValueParameter
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstConst
import com.ivianuu.ast.expressions.AstConstKind
import com.ivianuu.ast.expressions.AstDelegatedConstructorCallKind
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.expressions.builder.AstBaseQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.AstFunctionCallBuilder
import com.ivianuu.ast.expressions.builder.AstQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.buildBlock
import com.ivianuu.ast.expressions.builder.buildConst
import com.ivianuu.ast.expressions.builder.buildDelegatedConstructorCall
import com.ivianuu.ast.expressions.builder.buildFunctionCall
import com.ivianuu.ast.expressions.builder.buildQualifiedAccess
import com.ivianuu.ast.expressions.builder.buildReturn
import com.ivianuu.ast.expressions.builder.buildSuperReference
import com.ivianuu.ast.expressions.builder.buildThisReference
import com.ivianuu.ast.expressions.builder.buildVariableAssignment
import com.ivianuu.ast.symbols.CallableId
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.TypeAliasConstructorDescriptor
import org.jetbrains.kotlin.js.translate.callTranslator.getReturnType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtLoopExpression
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassValueReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.SuperCallReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisClassReceiver

class Psi2AstVisitor(
    override val context: Psi2AstGeneratorContext
) : KtVisitor<AstElement, Nothing?>(), Generator {

    private val functionStack = mutableListOf<AstFunctionSymbol<*>>()
    private val loops = mutableMapOf<KtLoopExpression, AstLoop>()

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

    override fun visitClassOrObject(classOrObject: KtClassOrObject, data: Nothing?): AstElement {
        val descriptor = classOrObject.descriptor<ClassDescriptor>()
        return buildRegularClass {
            symbol = context.symbolTable.getClassSymbol(descriptor)
            name = classOrObject.nameAsSafeName
            classKind = descriptor.kind
            visibility = descriptor.visibility.toAstVisibility()
            platformStatus = descriptor.platformStatus
            modality = descriptor.modality
            isCompanion = descriptor.isCompanionObject
            isFun = descriptor.isFun
            isData = descriptor.isData
            isInner = descriptor.isInner
            isExternal = descriptor.isExternal

            annotations += classOrObject.annotationEntries.map { it.convert() }
            typeParameters += classOrObject.typeParameters.map { it.convert() }

            val primaryConstructor = classOrObject.primaryConstructor
                ?.convert()
                ?: descriptor.unsubstitutedPrimaryConstructor
                    ?.takeIf { descriptor.kind != ClassKind.OBJECT }
                    ?.let { primaryConstructorDescriptor ->
                        buildConstructor {
                            symbol = context.symbolTable.getConstructorSymbol(primaryConstructorDescriptor)
                            isPrimary = true
                            returnType = primaryConstructorDescriptor.returnType.toAstType()
                        }
                }
            if (primaryConstructor != null) {
                declarations += primaryConstructor
                classOrObject
                    .primaryConstructor
                    ?.valueParameters
                    ?.mapNotNull { get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, it) }
                    ?.map { propertyDescriptor ->
                        buildProperty {
                            symbol = context.symbolTable.getPropertySymbol(propertyDescriptor)
                            returnType = propertyDescriptor.type.toAstType()
                            name = propertyDescriptor.name
                            isVar = propertyDescriptor.isVar
                            visibility = propertyDescriptor.visibility.toAstVisibility()
                            dispatchReceiverType = propertyDescriptor.dispatchReceiverParameter!!.type.toAstType()
                            initializer = buildQualifiedAccess {
                                type = propertyDescriptor.type.toAstType()
                                callee = primaryConstructor.valueParameters
                                    .single { it.name == propertyDescriptor.name }
                                    .symbol
                            }
                        }
                    }
                    ?.forEach { declarations += it }
            }

            declarations += classOrObject.declarations.map { it.convert() }
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction, data: Nothing?): AstElement {
        val descriptor = function.descriptor<SimpleFunctionDescriptor>()
        return buildNamedFunction {
            symbol = context.symbolTable.getNamedFunctionSymbol(descriptor)
            name = function.nameAsSafeName
            returnType = descriptor.returnType!!.toAstType()
            visibility = descriptor.visibility.toAstVisibility()
            modality = descriptor.modality
            platformStatus = descriptor.platformStatus
            isInfix = descriptor.isInfix
            isOperator = descriptor.isOperator
            isTailrec = descriptor.isTailrec
            isSuspend = descriptor.isSuspend
            annotations += function.annotationEntries.map { it.convert() }
            typeParameters += function.typeParameters.map { it.convert() }
            dispatchReceiverType = descriptor.dispatchReceiverParameter?.type?.toAstType()
            extensionReceiverType = descriptor.extensionReceiverParameter?.type?.toAstType()
            valueParameters += function.valueParameters.map { it.convert() }
            functionStack.push(symbol)
            body = function.bodyExpression?.toAstBlock()
            functionStack.pop()
        }
    }

    override fun visitPrimaryConstructor(
        constructor: KtPrimaryConstructor,
        data: Nothing?
    ) = toAstConstructor(constructor)

    override fun visitSecondaryConstructor(
        constructor: KtSecondaryConstructor,
        data: Nothing?
    ) = toAstConstructor(constructor)

    private fun toAstConstructor(constructor: KtConstructor<*>): AstConstructor {
        val descriptor = constructor.descriptor<ConstructorDescriptor>()
        return buildConstructor {
            symbol = context.symbolTable.getConstructorSymbol(descriptor)
                AstConstructorSymbol(CallableId(descriptor.fqNameSafe))
            isPrimary = constructor is KtPrimaryConstructor
            returnType = descriptor.returnType.toAstType()
            annotations += constructor.annotationEntries.map { it.convert() }
            valueParameters += constructor.valueParameters.map { it.convert() }
            if (constructor is KtSecondaryConstructor) {
                delegatedConstructor = constructor.getDelegationCallOrNull()?.convert()
            }
            functionStack.push(symbol)
            body = constructor.bodyExpression?.toAstBlock()
            functionStack.pop()
        }
    }

    override fun visitProperty(property: KtProperty, data: Nothing?): AstElement {
        val descriptor = property.descriptor<VariableDescriptor>()
        return buildProperty {
            symbol = context.symbolTable.getPropertySymbol(descriptor)
            name = property.nameAsSafeName
            returnType = descriptor.type.toAstType()
            dispatchReceiverType = descriptor.dispatchReceiverParameter?.type?.toAstType()
            extensionReceiverType = descriptor.extensionReceiverParameter?.type?.toAstType()
            annotations += property.annotationEntries.map { it.convert() }
            typeParameters += property.typeParameters.map { it.convert() }
            isVar = property.isVar
            initializer = property.initializer?.convert()
            delegate = property.delegate?.expression?.convert()
            getter = property.getter?.convert()
            setter = property.setter?.convert()
            visibility = descriptor.visibility.toAstVisibility()
            if (descriptor is PropertyDescriptor) {
                modality = descriptor.modality
                platformStatus = descriptor.platformStatus
                isExternal = descriptor.isExternal
            }
            isLocal = descriptor !is PropertyDescriptor // todo??
            isInline = false // todo
            isConst = descriptor.isConst
            isLateinit = descriptor.isLateInit
        }
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor, data: Nothing?): AstElement {
        val descriptor = accessor.descriptor<PropertyAccessorDescriptor>()
        return buildPropertyAccessor {
            symbol = context.symbolTable.getPropertyAccessorSymbol(descriptor)
            name = descriptor.name
            isSetter = accessor.isSetter
            returnType = descriptor.returnType!!.toAstType()
            valueParameters += accessor.valueParameters.map { it.convert() }
            functionStack.push(symbol)
            body = accessor.bodyExpression?.toAstBlock()
            functionStack.pop()
            visibility = descriptor.visibility.toAstVisibility()
            modality = descriptor.modality
            annotations += accessor.annotationEntries.map { it.convert() }
        }
    }

    /*
    override fun visitAnonymousInitializer(
        initializer: KtAnonymousInitializer,
        data: Nothing?
    ): AstElement {
        return initializer.cached(
            context.storage.anonymousInitializers,
            { AstAnonymousInitializer().applyParentFromStack() }
        ) {
            if (mode == Mode.FULL) {
                body = visitExpressionForBlock(initializer.body!!, mode)
            }
        }
    }*/

    override fun visitTypeParameter(parameter: KtTypeParameter, data: Nothing?): AstElement {
        val descriptor = parameter.descriptor<TypeParameterDescriptor>()
        return buildTypeParameter {
            symbol = context.symbolTable.getTypeParameterSymbol(descriptor)
            name = parameter.nameAsSafeName
            isReified = descriptor.isReified
            variance = descriptor.variance
            annotations += parameter.annotationEntries.map { it.convert() }
            bounds += descriptor.upperBounds.map { it.toAstType() }
        }
    }

    override fun visitParameter(parameter: KtParameter, data: Nothing?): AstElement {
        val descriptor = parameter.descriptor<ParameterDescriptor>()
        return buildValueParameter {
            symbol = context.symbolTable.getValueParameterSymbol(descriptor)
            name = parameter.nameAsSafeName
            returnType = descriptor.type.toAstType()
            if (descriptor is ValueParameterDescriptor) {
                isCrossinline = descriptor.isCrossinline
                isNoinline = descriptor.isNoinline
                isVararg = descriptor.isVararg
            }
            annotations += parameter.annotationEntries.map { it.convert() }
            defaultValue = parameter.defaultValue?.convert()
        }
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias, data: Nothing?): AstElement {
        val descriptor = typeAlias.descriptor<TypeAliasDescriptor>()
        return buildTypeAlias {
            symbol = context.symbolTable.getTypeAliasSymbol(descriptor)
            name = typeAlias.nameAsSafeName
            expandedType = descriptor.expandedType.toAstType()
            visibility = descriptor.visibility.toAstVisibility()
            platformStatus = descriptor.platformStatus
            annotations += typeAlias.annotationEntries.map { it.convert() }
            typeParameters += typeAlias.typeParameters.map { it.convert() }
        }
    }

    /*

    override fun visitObjectLiteralExpression(
        expression: KtObjectLiteralExpression,
        data: Nothing?
    ): AstElement {
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
    }*/

    override fun visitBlockExpression(expression: KtBlockExpression, data: Nothing?): AstElement {
        return buildBlock {
            type = expression.getExpressionTypeWithCoercionToUnitOrFail().toAstType()
            statements += expression.statements.map { it.convert() }
        }
    }

    private fun KtExpression.toAstBlock(): AstBlock {
        return if (this is KtBlockExpression) convert()
        else {
            val astExpression = convert<AstExpression>()
            val type = getTypeInferredByFrontendOrFail().toAstType()
            buildBlock {
                this.type = type
                statements += if (functionStack.isNotEmpty()) {
                    buildReturn {
                        this.type = type
                        target = functionStack.last()
                        result = astExpression
                    }
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
        val entries = expression.entries.map { it.convert<AstExpression>() }
        return when (entries.size) {
            0 -> buildConst(resultType, AstConstKind.String, "")
            1 -> {
                val entry = entries.single()
                if (entry is AstConst<*> && entry.kind == AstConstKind.String) {
                    entry
                } else {
                    val toString = context.builtIns.anySymbol
                        .owner
                        .declarations
                        .filterIsInstance<AstNamedFunction>()
                        .first { it.name.asString() == "toString" }
                    buildFunctionCall {
                        type = context.builtIns.stringType
                        callee = toString.symbol
                        dispatchReceiver = entry
                    }
                }
            }
            else -> {
                val stringPlus = context.builtIns.stringSymbol
                    .owner
                    .declarations
                    .filterIsInstance<AstNamedFunction>()
                    .first { it.name.asString() == "plus" }
                entries.reduce { acc, entry ->
                    buildFunctionCall {
                        type = context.builtIns.stringType
                        callee = stringPlus.symbol
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
    ): AstElement = buildConst(context.builtIns.stringType, AstConstKind.String, entry.text)

    override fun visitEscapeStringTemplateEntry(
        entry: KtEscapeStringTemplateEntry,
        data: Nothing?
    ): AstElement = buildConst(context.builtIns.stringType, AstConstKind.String, entry.unescapedValue)

    override fun visitStringTemplateEntryWithExpression(
        entry: KtStringTemplateEntryWithExpression,
        data: Nothing?
    ): AstElement = entry.expression!!.convert()

    override fun visitConstructorDelegationCall(
        call: KtConstructorDelegationCall,
        data: Nothing?
    ): AstElement {
        val resolvedCall = call.getResolvedCall()!!
        return buildDelegatedConstructorCall {
            type = resolvedCall.getReturnType().toAstType()
            callee = context.symbolTable.getConstructorSymbol(resolvedCall.resultingDescriptor as ConstructorDescriptor)
            kind = if (call.isCallToThis) AstDelegatedConstructorCallKind.THIS
            else AstDelegatedConstructorCallKind.SUPER
            valueArguments += call.valueArguments.map {
                it.getArgumentExpression()!!.convert()
            }
        }
    }

    override fun visitReferenceExpression(
        expression: KtReferenceExpression,
        data: Nothing?
    ): AstElement {
        val resolvedCall = expression.getResolvedCall()!!
        val callee = when (val calleeDescriptor = resolvedCall.resultingDescriptor) {
            is SimpleFunctionDescriptor -> context.symbolTable.getNamedFunctionSymbol(calleeDescriptor)
            is ConstructorDescriptor -> context.symbolTable.getConstructorSymbol(calleeDescriptor)
            is PropertyDescriptor -> context.symbolTable.getPropertySymbol(calleeDescriptor)
            is ValueParameterDescriptor -> context.symbolTable.getValueParameterSymbol(calleeDescriptor)
            else -> error("Unexpected callee $calleeDescriptor for ${expression.text}")
        }
        val result: AstBaseQualifiedAccessBuilder =
            if (expression is KtCallExpression) {
                AstFunctionCallBuilder().apply {
                    this.callee = callee as AstFunctionSymbol<*>
                    valueArguments += expression.valueArguments.map {
                        it.getArgumentExpression()!!.convert()
                    }
                }
            } else {
                AstQualifiedAccessBuilder().apply {
                    this.callee = callee
                }
            }

        return result.apply {
            type = resolvedCall.getReturnType().toAstType()
            dispatchReceiver = resolvedCall.dispatchReceiver?.toAstExpression()
            extensionReceiver = resolvedCall.extensionReceiver?.toAstExpression()
            if (expression is KtCallElement) {
                typeArguments += expression.typeArguments.map { it.convert() }
            }
        }.build()
    }

    /*
    override fun visitDestructuringDeclaration(
        multiDeclaration: KtDestructuringDeclaration,
        data: Nothing?
    ): AstElement {
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

    override fun visitThisExpression(expression: KtThisExpression, data: Nothing?): AstElement {
        val referenceTarget =
            getOrFail(BindingContext.REFERENCE_TARGET, expression.instanceReference)
        TODO("What $referenceTarget")
    }*/

    override fun visitConstantExpression(
        expression: KtConstantExpression,
        data: Nothing?
    ): AstElement {
        val constantValue =
            ConstantExpressionEvaluator.getConstant(expression, context.bindingContext)
                ?.toConstantValue(expression.getTypeInferredByFrontendOrFail())
                ?: error("KtConstantExpression was not evaluated: ${expression.text}")
        return context.constantValueGenerator.generateConstantValueAsExpression(constantValue)
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, data: Nothing?): AstElement {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        if (ktOperator == KtTokens.IDENTIFIER) {
            error("lol ? $expression ${expression.text}")
        }

        val lazyLeft by lazy { expression.left!!.convert<AstExpression>() }
        val right = expression.right!!.convert<AstExpression>()

        return when (ktOperator) {
            KtTokens.EQ -> buildVariableAssignment {
                type = context.builtIns.unitType
                val resolvedCall = expression.left!!.getResolvedCall()!!
                callee = when (val callee = resolvedCall.resultingDescriptor) {
                    is PropertyDescriptor -> context.symbolTable.getPropertySymbol(callee)
                    else -> error("Unexpected callee $callee")
                }
                value = right
            }
            KtTokens.LT -> buildFunctionCall {
                type = context.builtIns.booleanType
                callee = context.builtIns.lessThan
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.LTEQ -> buildFunctionCall {
                type = context.builtIns.booleanType
                callee = context.builtIns.lessThanEqual
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.GT -> buildFunctionCall {
                type = context.builtIns.booleanType
                callee = context.builtIns.greaterThan
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.GTEQ -> buildFunctionCall {
                type = context.builtIns.booleanType
                callee = context.builtIns.greaterThanEqual
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.EQEQ -> buildFunctionCall {
                type = context.builtIns.booleanType
                callee = context.builtIns.structuralEqual
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.EXCLEQ -> buildFunctionCall {
                type = context.builtIns.booleanType
                callee = context.builtIns.structuralNotEqual
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.EXCLEQ -> buildFunctionCall {
                type = context.builtIns.booleanType
                callee = context.builtIns.identityEqual
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.EXCLEQEQEQ -> buildFunctionCall {
                type = context.builtIns.booleanType
                callee = context.builtIns.identityNotEqual
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.ANDAND -> buildFunctionCall {
                type = context.builtIns.booleanType
                callee = context.builtIns.lazyAnd
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.EXCLEQEQEQ -> buildFunctionCall {
                type = context.builtIns.booleanType
                callee = context.builtIns.identityNotEqual
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.ANDAND -> buildFunctionCall {
                type = context.builtIns.booleanType
                callee = context.builtIns.lazyAnd
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.ANDAND -> buildFunctionCall {
                type = context.builtIns.booleanType
                callee = context.builtIns.lazyOr
                valueArguments += lazyLeft
                valueArguments += right
            }

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

    /*override fun visitIfExpression(expression: KtIfExpression, data: Nothing?): AstElement {
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


     */

    override fun visitParenthesizedExpression(
        expression: KtParenthesizedExpression,
        data: Nothing?
    ): AstElement = expression.expression?.accept(this, null)
        ?: error("$expression ${expression.text}")

    override fun visitAnnotatedExpression(
        expression: KtAnnotatedExpression,
        data: Nothing?
    ): AstElement {
        return expression.baseExpression?.accept(this, null)
            ?.also {
                (it as AstAnnotationContainer)
                    .replaceAnnotations(
                        expression.annotationEntries
                            .map { it.convert() }
                    )
            }
            ?: error("$expression ${expression.text}")
    }

     /*
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
    }*/

    fun ReceiverValue.toAstExpression(): AstExpression {
        return when (this) {
            is ImplicitClassReceiver -> buildThisReference {
                type = this@toAstExpression.type.toAstType()
            }
            is ThisClassReceiver -> buildThisReference {
                type = this@toAstExpression.type.toAstType()
            }
            is SuperCallReceiverValue -> buildSuperReference {
                type = this@toAstExpression.type.toAstType()
            }
            is ExpressionReceiver -> expression.convert()
            is ClassValueReceiver -> buildThisReference {
                type = this@toAstExpression.type.toAstType()
            }
            is ExtensionReceiver -> buildQualifiedAccess {
                type = this@toAstExpression.type.toAstType()
                callee = context.symbolTable.getValueParameterSymbol(
                    declarationDescriptor.extensionReceiverParameter!!
                )
            }
            else -> error("Unexpected receiver value $this")
        }
    }

    /*private fun scopeOwnerAsCallable(scopeOwner: FunctionDescriptor?) =
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
*/
    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, data: Nothing?): AstElement {
        val descriptor = getOrFail(BindingContext.ANNOTATION, annotationEntry)
        return context.constantValueGenerator.generateAnnotationConstructorCall(descriptor)!!
    }

}
