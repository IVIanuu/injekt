package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstFunctionTarget
import com.ivianuu.ast.AstLoopTarget
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstModuleFragment
import com.ivianuu.ast.declarations.builder.buildConstructor
import com.ivianuu.ast.declarations.builder.buildFile
import com.ivianuu.ast.declarations.builder.buildModuleFragment
import com.ivianuu.ast.declarations.builder.buildProperty
import com.ivianuu.ast.declarations.builder.buildRegularClass
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstConstKind
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.declarations.AstNamedFunction
import com.ivianuu.ast.declarations.builder.AstAnonymousFunctionBuilder
import com.ivianuu.ast.declarations.builder.AstFunctionBuilder
import com.ivianuu.ast.declarations.builder.AstNamedFunctionBuilder
import com.ivianuu.ast.declarations.builder.buildAnonymousInitializer
import com.ivianuu.ast.declarations.builder.buildPropertyAccessor
import com.ivianuu.ast.declarations.builder.buildTypeAlias
import com.ivianuu.ast.declarations.builder.buildTypeParameter
import com.ivianuu.ast.declarations.builder.buildValueParameter
import com.ivianuu.ast.expressions.AstConst
import com.ivianuu.ast.expressions.AstDelegatedConstructorCallKind
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.expressions.AstTypeOperator
import com.ivianuu.ast.expressions.buildConstBoolean
import com.ivianuu.ast.expressions.buildConstString
import com.ivianuu.ast.expressions.buildTemporaryVariable
import com.ivianuu.ast.expressions.buildUnitExpression
import com.ivianuu.ast.expressions.builder.AstBaseQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.AstBlockBuilder
import com.ivianuu.ast.expressions.builder.AstBreakBuilder
import com.ivianuu.ast.expressions.builder.AstContinueBuilder
import com.ivianuu.ast.expressions.builder.AstDoWhileLoopBuilder
import com.ivianuu.ast.expressions.builder.AstFunctionCallBuilder
import com.ivianuu.ast.expressions.builder.AstLoopBuilder
import com.ivianuu.ast.expressions.builder.AstLoopJumpBuilder
import com.ivianuu.ast.expressions.builder.AstQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.AstWhileLoopBuilder
import com.ivianuu.ast.expressions.builder.buildBlock
import com.ivianuu.ast.expressions.builder.buildCatch
import com.ivianuu.ast.expressions.builder.buildDelegatedConstructorCall
import com.ivianuu.ast.expressions.builder.buildFunctionCall
import com.ivianuu.ast.expressions.builder.buildQualifiedAccess
import com.ivianuu.ast.expressions.builder.buildReturn
import com.ivianuu.ast.expressions.builder.buildSuperReference
import com.ivianuu.ast.expressions.builder.buildThisReference
import com.ivianuu.ast.expressions.builder.buildThrow
import com.ivianuu.ast.expressions.builder.buildTry
import com.ivianuu.ast.expressions.builder.buildTypeOperation
import com.ivianuu.ast.expressions.builder.buildVariableAssignment
import com.ivianuu.ast.expressions.builder.buildWhen
import com.ivianuu.ast.expressions.builder.buildWhenBranch
import com.ivianuu.ast.symbols.CallableId
import com.ivianuu.ast.symbols.impl.AstAnonymousFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstAnonymousInitializerSymbol
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
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.js.translate.callTranslator.getReturnType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtExpressionWithLabel
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtUnaryExpression
import org.jetbrains.kotlin.psi.KtVisitor
import org.jetbrains.kotlin.psi.KtWhileExpression
import org.jetbrains.kotlin.psi2ir.deparenthesize
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

class Psi2AstBuilder(override val context: Psi2AstGeneratorContext) : Generator, KtVisitor<AstElement, Nothing?>() {

    private val calleeNamesForLambda = mutableListOf<String>()
    private val functionTargets = mutableListOf<AstFunctionTarget>()
    private val loopTargets = mutableListOf<AstLoopTarget>()
    private val labels = mutableListOf<String>()

    fun buildModule(files: List<KtFile>): AstModuleFragment {
        return buildModuleFragment {
            name = module.name
            this.files += files.map { it.convert() }
            symbolTable.unboundSymbols
                .forEach { (descriptor, symbol) ->
                    if (!symbol.isBound) {
                        stubGenerator.getDeclaration(symbol, descriptor)
                    }
                }
        }
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
            symbol = symbolTable.getClassSymbol(descriptor)
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
                            symbol = symbolTable.getConstructorSymbol(primaryConstructorDescriptor)
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
                            symbol = symbolTable.getPropertySymbol(propertyDescriptor)
                            returnType = propertyDescriptor.type.toAstType()
                            isVar = propertyDescriptor.isVar
                            visibility = propertyDescriptor.visibility.toAstVisibility()
                            dispatchReceiverType = propertyDescriptor.dispatchReceiverParameter!!.type.toAstType()
                            initializer = buildQualifiedAccess {
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
        val target = AstFunctionTarget(null, false)
        val isAnonymousFunction = function.name == null && !function.parent.let { it is KtFile || it is KtClassBody }
        val functionBuilder: AstFunctionBuilder = if (isAnonymousFunction) {
            AstAnonymousFunctionBuilder(context).apply {
                symbol = AstAnonymousFunctionSymbol()
                extensionReceiverType = descriptor.extensionReceiverParameter?.type?.toAstType()
            }
        } else {
            AstNamedFunctionBuilder(context).apply {
                symbol = symbolTable.getNamedFunctionSymbol(descriptor)
                visibility = descriptor.visibility.toAstVisibility()
                modality = descriptor.modality
                platformStatus = descriptor.platformStatus
                isInfix = descriptor.isInfix
                isOperator = descriptor.isOperator
                isTailrec = descriptor.isTailrec
                isSuspend = descriptor.isSuspend
                typeParameters += function.typeParameters.map { it.convert() }
                dispatchReceiverType = descriptor.dispatchReceiverParameter?.type?.toAstType()
                extensionReceiverType = descriptor.extensionReceiverParameter?.type?.toAstType()
            }
        }

        return functionBuilder.apply {
            returnType = descriptor.returnType!!.toAstType()
            annotations += function.annotationEntries.map { it.convert() }
            valueParameters += function.valueParameters.map { it.convert() }
            functionTargets.push(target)
            body = function.bodyExpression?.toAstBlock()
            functionTargets.pop()
        }.build().also { target.bind(it) }
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
        val target = AstFunctionTarget(null, false)
        return buildConstructor {
            symbol = symbolTable.getConstructorSymbol(descriptor)
            AstConstructorSymbol(CallableId(descriptor.fqNameSafe))
            isPrimary = constructor is KtPrimaryConstructor
            returnType = descriptor.returnType.toAstType()
            visibility = descriptor.visibility.toAstVisibility()
            annotations += constructor.annotationEntries.map { it.convert() }
            valueParameters += constructor.valueParameters.map { it.convert() }
            if (constructor is KtSecondaryConstructor) {
                delegatedConstructor = constructor.getDelegationCallOrNull()?.convert()
            }
            functionTargets.push(target)
            body = constructor.bodyExpression?.toAstBlock()
            functionTargets.pop()
        }.also { target.bind(it) }
    }

    override fun visitProperty(property: KtProperty, data: Nothing?): AstElement {
        val descriptor = property.descriptor<VariableDescriptor>()
        return buildProperty {
            symbol = symbolTable.getPropertySymbol(descriptor)
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
            isLocal = property.isLocal
            isInline = descriptor is PropertyDescriptor &&
                    (descriptor.accessors.any { it.isInline })
            isConst = descriptor.isConst
            isLateinit = descriptor.isLateInit
        }
    }

    override fun visitPropertyAccessor(accessor: KtPropertyAccessor, data: Nothing?): AstElement {
        val descriptor = accessor.descriptor<PropertyAccessorDescriptor>()
        val target = AstFunctionTarget(null, false)
        return buildPropertyAccessor {
            symbol = symbolTable.getPropertyAccessorSymbol(descriptor)
            isSetter = accessor.isSetter
            returnType = descriptor.returnType!!.toAstType()
            valueParameters += accessor.valueParameters.map { it.convert() }
            functionTargets.push(target)
            body = accessor.bodyExpression?.toAstBlock()
            functionTargets.pop()
            visibility = descriptor.visibility.toAstVisibility()
            modality = descriptor.modality
            annotations += accessor.annotationEntries.map { it.convert() }
        }.also { target.bind(it) }
    }

    override fun visitAnonymousInitializer(
        initializer: KtAnonymousInitializer,
        data: Nothing?
    ): AstElement {
        return buildAnonymousInitializer {
            symbol = AstAnonymousInitializerSymbol()
            body = initializer.body!!.toAstBlock()
            annotations += initializer.annotationEntries.map { it.convert() }
        }
    }

    override fun visitTypeParameter(parameter: KtTypeParameter, data: Nothing?): AstElement {
        val descriptor = parameter.descriptor<TypeParameterDescriptor>()
        return buildTypeParameter {
            symbol = symbolTable.getTypeParameterSymbol(descriptor)
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
            symbol = symbolTable.getValueParameterSymbol(descriptor)
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
            symbol = symbolTable.getTypeAliasSymbol(descriptor)
            expandedType = descriptor.expandedType.toAstType()
            visibility = descriptor.visibility.toAstVisibility()
            platformStatus = descriptor.platformStatus
            annotations += typeAlias.annotationEntries.map { it.convert() }
            typeParameters += typeAlias.typeParameters.map { it.convert() }
        }
    }

    /*override fun visitLambdaExpression(expression: KtLambdaExpression, data: Nothing?): AstElement {
    return AstAnonymousFunctionExpression(
        type = expression.getTypeInferredByFrontendOrFail().toAstType(),
        anonymousFunction = visitFunction(
            function = expression.functionLiteral,
            mode = mode,
            body = expression.bodyExpression
        )
    )
}*/

    /*override fun visitLambdaExpression(expression: KtLambdaExpression, data: Nothing?): AstElement {
        val descriptor = expression.functionLiteral.descriptor<AnonymousFunctionDescriptor>()
        val label = if (labels.isNotEmpty()) labels.pop() else calleeNamesForLambda.lastOrNull()
        val target = AstFunctionTarget(label, true)
        return buildAnonymousFunction {
            symbol = AstAnonymousFunctionSymbol()

            var destructuringBlock: AstExpression? = null
            for (valueParameter in expression.functionLiteral.valueParameters) {
                val multiDeclaration = valueParameter.destructuringDeclaration
                valueParameters += if (multiDeclaration != null) {
                    val name = Name.special("<destruct>")
                    val multiParameter = buildValueParameter {
                        returnType = buildImplicitTypeRef {
                        }
                        this.name = name
                        symbol = AstVariableSymbol(name)
                        isCrossinline = false
                        isNoinline = false
                        isVararg = false
                    }
                    destructuringBlock = generateDestructuringBlock(
                        baseSession,
                        multiDeclaration,
                        multiParameter,
                        tmpVariable = false,
                        extractAnnotationsTo = { extractAnnotationsTo(it) },
                    ) { toAstOrImplicitType() }
                    multiParameter
                } else {
                    val typeRef = buildImplicitTypeRef {
                    }
                    valueParameter.toAstValueParameter(typeRef)
                }
            }
            this.label = label
            functionTargets.push(target)
            val ktBody = expression.functionLiteral.bodyExpression!!
            body = ktBody.toAstBlockBuilder().apply {
                if (statements.isEmpty()) {
                    statements += buildReturn {
                        result =
                    }
                    statements.add(
                        buildReturnExpression {
                            this.target = target
                            result = buildUnitExpression {
                            }
                        }
                    )
                }
                if (destructuringBlock is AstBlock) {
                    for ((index, statement) in destructuringBlock.statements.withIndex()) {
                        statements.add(index, statement)
                    }
                }
            }.build()
            functionTargets.pop()
        }.also {
            target.bind(it)
        }
    }*/

    override fun visitBlockExpression(expression: KtBlockExpression, data: Nothing?): AstElement {
        return expression.toAstBlockBuilder().build()
    }

    private fun KtBlockExpression.toAstBlockBuilder(): AstBlockBuilder {
        return AstBlockBuilder(this@Psi2AstBuilder.context).apply {
            type = getExpressionTypeWithCoercionToUnitOrFail().toAstType()
            statements += this@toAstBlockBuilder.statements.map { it.convert() }
        }
    }

    private fun KtExpression?.toAstBlock(): AstBlock {
        return if (this is KtBlockExpression) convert()
        else if (this == null) buildBlock()
        else {
            val astExpression = convert<AstExpression>()
            val type = getTypeInferredByFrontendOrFail().toAstType()
            buildBlock {
                this.type = type
                statements += if (functionTargets.isNotEmpty()) {
                    buildReturn {
                        target = functionTargets.last()
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
        val entries = expression.entries.map { it.convert<AstExpression>() }
        return when (entries.size) {
            0 -> buildConstString("")
            1 -> {
                val entry = entries.single()
                if (entry is AstConst<*> && entry.kind == AstConstKind.String) {
                    entry
                } else {
                    val toString = builtIns.anySymbol
                        .owner
                        .declarations
                        .filterIsInstance<AstNamedFunction>()
                        .first { it.name.asString() == "toString" }
                    buildFunctionCall {
                        callee = toString.symbol
                        dispatchReceiver = entry
                    }
                }
            }
            else -> {
                val stringPlus = builtIns.stringSymbol
                    .owner
                    .declarations
                    .filterIsInstance<AstNamedFunction>()
                    .first { it.name.asString() == "plus" }
                entries.reduce { acc, entry ->
                    buildFunctionCall {
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
    ): AstElement = buildConstString(entry.text)

    override fun visitEscapeStringTemplateEntry(
        entry: KtEscapeStringTemplateEntry,
        data: Nothing?
    ): AstElement = buildConstString(entry.unescapedValue)

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
            callee = symbolTable.getConstructorSymbol(resolvedCall.resultingDescriptor as ConstructorDescriptor)
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
            is SimpleFunctionDescriptor -> symbolTable.getNamedFunctionSymbol(calleeDescriptor)
            is ConstructorDescriptor -> symbolTable.getConstructorSymbol(calleeDescriptor)
            is PropertyDescriptor -> symbolTable.getPropertySymbol(calleeDescriptor)
            is LocalVariableDescriptor -> symbolTable.getPropertySymbol(calleeDescriptor)
            is ValueParameterDescriptor -> symbolTable.getValueParameterSymbol(calleeDescriptor)
            else -> error("Unexpected callee $calleeDescriptor for ${expression.text}")
        }
        val result: AstBaseQualifiedAccessBuilder = if (expression is KtCallExpression) {
            AstFunctionCallBuilder(context).apply {
                this.callee = callee as AstFunctionSymbol<*>
                valueArguments += expression.valueArguments.map {
                    it.getArgumentExpression()!!.convert()
                }
            }
        } else {
            AstQualifiedAccessBuilder(context).apply {
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

    override fun visitUnaryExpression(expression: KtUnaryExpression, data: Nothing?): AstElement {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val resolvedCall = expression.getResolvedCall()!!
        val argument = expression.baseExpression!!
        val prefix = expression is KtPrefixExpression
        return if (ktOperator == KtTokens.PLUSPLUS ||
            ktOperator == KtTokens.MINUSMINUS) {
            buildBlock {
                /*println("resolved call ${resolvedCall.resultingDescriptor} arg ${expression.text} $expression prefix $prefix")

                val resultVariable = buildTemporaryVariable(
                    buildFunctionCall {
                        this.callee = symbolTable.getClassSymbol(resolvedCall.resultingDescriptor as ClassDescriptor)
                        dispatchReceiver = resolvedCall.dispatchReceiver?.toAstExpression()
                        extensionReceiver = resolvedCall.extensionReceiver?.toAstExpression()
                    }
                )
                val assignment = buildVariableAssignment {
                    this.
                }

                val assignment = buildVariableAssignment {
                    callee = resultVariable.symbol
                    value = if (prefix && argument.elementType != KtNodeTypes.REFERENCE_EXPRESSION)
                        generateResolvedAccessExpression(resultVariable)
                    else
                        resultInitializer
                }

                    argument.generateAssignment(
                    argument,
                    ,
                    AstOperation.ASSIGN,
                    convert
                )

                if (prefix) {
                    if (argument.elementType != KtNodeTypes.REFERENCE_EXPRESSION) {
                        statements += resultVariable
                        statements += assignment
                        statements += generateResolvedAccessExpression(resultVariable)
                    } else {
                        statements += assignment
                        statements += generateAccessExpression(argument.getReferencedNameAsName())
                    }
                } else {
                    statements += assignment
                    statements += generateResolvedAccessExpression(temporaryVariable)
                }*/
            }
        }
        else error("Unexpected token $ktOperator ${expression.text}")
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, data: Nothing?): AstElement {
        val ktOperator = expression.operationReference.getReferencedNameElementType()

        if (ktOperator == KtTokens.IDENTIFIER) {
            calleeNamesForLambda.push(expression.operationReference.getReferencedName())
        }

        val lazyLeft by lazy { expression.left!!.convert<AstExpression>() }
        val right = expression.right!!.convert<AstExpression>()

        if (ktOperator == KtTokens.IDENTIFIER) {
            calleeNamesForLambda.pop()
        }

        return when (ktOperator) {
            KtTokens.EQ -> buildVariableAssignment {
                val resolvedCall = expression.left!!.getResolvedCall()!!
                callee = when (val callee = resolvedCall.resultingDescriptor) {
                    is PropertyDescriptor -> symbolTable.getPropertySymbol(callee)
                    else -> error("Unexpected callee $callee")
                }
                value = right
            }
            KtTokens.LT -> buildFunctionCall {
                callee = builtIns.lessThanSymbol
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.LTEQ -> buildFunctionCall {
                callee = builtIns.lessThanEqualSymbol
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.GT -> buildFunctionCall {
                callee = builtIns.greaterThanSymbol
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.GTEQ -> buildFunctionCall {
                callee = builtIns.greaterThanEqualSymbol
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.EQEQ -> buildFunctionCall {
                callee = builtIns.structuralEqualSymbol
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.EXCLEQ -> buildFunctionCall {
                callee = builtIns.structuralNotEqualSymbol
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.EXCLEQ -> buildFunctionCall {
                callee = builtIns.identityEqualSymbol
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.EXCLEQEQEQ -> buildFunctionCall {
                callee = builtIns.identityNotEqualSymbol
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.ANDAND -> buildFunctionCall {
                callee = builtIns.lazyAndSymbol
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.EXCLEQEQEQ -> buildFunctionCall {
                callee = builtIns.identityNotEqualSymbol
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.ANDAND -> buildFunctionCall {
                callee = builtIns.lazyAndSymbol
                valueArguments += lazyLeft
                valueArguments += right
            }
            KtTokens.OROR -> buildFunctionCall {
                callee = builtIns.lazyOrSymbol
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

            KtTokens.IN_KEYWORD, KtTokens.NOT_IN -> {
                val containsCall = expression.right!!.getResolvedCall()!!
                val astContainsCall = buildFunctionCall {
                    callee = symbolTable.getNamedFunctionSymbol(containsCall.resultingDescriptor as SimpleFunctionDescriptor)
                }
                if (ktOperator == KtTokens.IN_KEYWORD) {
                    astContainsCall
                } else {
                    buildFunctionCall {
                        callee = builtIns.booleanNotSymbol
                        dispatchReceiver = astContainsCall
                    }
                }
            }

            KtTokens.ELVIS -> {
                buildBlock {
                    val tmp = buildTemporaryVariable(lazyLeft)
                        .also { statements += it }


                    /*+irIfNull(
                        resultType,
                        irGet(temporary.type, temporary.symbol),
                        irArgument1,
                        irGet(temporary.type, temporary.symbol)
                    )*/
                }
            }
            else -> error("Unexpected token $ktOperator ${expression.text}")
        }
    }

    override fun visitBinaryWithTypeRHSExpression(
        expression: KtBinaryExpressionWithTypeRHS,
        data: Nothing?
    ): AstElement {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        return buildTypeOperation {
            operator = when (ktOperator) {
                KtTokens.AS_KEYWORD -> AstTypeOperator.AS
                KtTokens.AS_SAFE -> AstTypeOperator.SAFE_AS
                else -> error("Unexpected operator $ktOperator ${expression.text}")
            }
            argument = expression.left.convert()
            typeOperand = expression.right!!.toAstType()
        }
    }

    override fun visitIsExpression(expression: KtIsExpression, data: Nothing?): AstElement {
        return buildTypeOperation {
            operator = if (expression.isNegated) AstTypeOperator.SAFE_AS else AstTypeOperator.AS
            argument = expression.leftHandSide.convert()
            typeOperand = expression.typeReference!!.toAstType()
        }
    }

    override fun visitIfExpression(expression: KtIfExpression, data: Nothing?): AstElement {
        return buildWhen {
            type = expression.getTypeInferredByFrontendOrFail().toAstType()
            var ktLastIf: KtIfExpression = expression
            var elseBranch: AstExpression? = null
            whenBranches@ while (true) {
                branches += buildWhenBranch {
                    condition = ktLastIf.condition!!.convert()
                    result = ktLastIf.then!!.convert()
                }

                when (val ktElse = ktLastIf.`else`?.deparenthesize()) {
                    null -> break@whenBranches
                    is KtIfExpression -> ktLastIf = ktElse
                    is KtExpression -> {
                        elseBranch = ktElse.convert()
                        break@whenBranches
                    }
                    else -> error("Unexpected else expression: ${ktElse.text}")
                }
            }
            if (elseBranch != null) {
                branches += buildWhenBranch {
                    condition = buildConstBoolean(true)
                    result = elseBranch
                }
            }
        }
    }

    override fun visitWhileExpression(expression: KtWhileExpression, data: Nothing?): AstElement {
        return AstWhileLoopBuilder(context).apply {
            condition = expression.condition!!.convert()
        }.configure { expression.body.toAstBlock() }
    }

    override fun visitDoWhileExpression(
        expression: KtDoWhileExpression,
        data: Nothing?
    ): AstElement {
        return AstDoWhileLoopBuilder(context).apply {
            condition = expression.condition!!.convert()
        }.configure { expression.body.toAstBlock() }
    }

    private fun AstLoopBuilder.configure(generateBlock: () -> AstBlock): AstLoop {
        if (labels.isNotEmpty()) label = labels.pop()
        val target = AstLoopTarget(label)
        loopTargets.push(target)
        body = generateBlock()
        val loop = build()
        loopTargets.pop()
        target.bind(loop)
        return loop
    }

    override fun visitBreakExpression(expression: KtBreakExpression, data: Nothing?): AstElement =
        AstBreakBuilder(context).configure(expression).build()

    override fun visitContinueExpression(expression: KtContinueExpression, data: Nothing?): AstElement =
        AstContinueBuilder(context).configure(expression).build()

    private fun AstLoopJumpBuilder.configure(expression: KtExpressionWithLabel): AstLoopJumpBuilder {
        val labelName = expression.getLabelName()
        val lastLoopTarget = loopTargets.last()
        if (labelName == null) {
            target = lastLoopTarget
        } else {
            for (astLoopTarget in loopTargets.asReversed()) {
                if (astLoopTarget.labelName == labelName) {
                    target = astLoopTarget
                    return this
                }
            }
        }
        return this
    }

    override fun visitReturnExpression(expression: KtReturnExpression, data: Nothing?): AstElement {
        return buildReturn {
            target = functionTargets.last { it.labelName == expression.getLabelName() }
            result = expression.returnedExpression?.convert() ?: buildUnitExpression()
        }
    }

    override fun visitThrowExpression(expression: KtThrowExpression, data: Nothing?): AstElement {
        return buildThrow { exception = expression.thrownExpression!!.convert() }
    }

    override fun visitTryExpression(expression: KtTryExpression, data: Nothing?): AstElement {
        return buildTry {
            type = expression.getExpressionTypeWithCoercionToUnitOrFail().toAstType()
            tryBody = expression.tryBlock.convert()
            catches += expression.catchClauses
                .map { catchClause ->
                    buildCatch {
                        parameter = catchClause.catchParameter!!.convert()
                        body = catchClause.catchBody!!.convert()
                    }
                }
            finallyBody = expression.finallyBlock?.finalExpression?.convert()
        }
    }

    override fun visitDotQualifiedExpression(
        expression: KtDotQualifiedExpression,
        data: Nothing?
    ): AstElement = expression.selectorExpression!!.convert()

    override fun visitParenthesizedExpression(expression: KtParenthesizedExpression, data: Nothing?): AstElement =
        expression.expression!!.convert()

    override fun visitAnnotatedExpression(expression: KtAnnotatedExpression, data: Nothing?): AstElement {
        val rawResult = expression.baseExpression!!.accept(this, data) as AstAnnotationContainer
        rawResult.replaceAnnotations(expression.annotationEntries.map { it.convert() })
        return rawResult
    }

    override fun visitLabeledExpression(expression: KtLabeledExpression, data: Nothing?): AstElement {
        val label = expression.getTargetLabel()
        val size = labels.size
        if (label != null) {
            labels += label.getReferencedName()
        }
        val result = expression.baseExpression!!.convert<AstStatement>()
        if (size != labels.size) {
            labels.removeLast()
            println("Unused label: ${expression.text}")
        }
        return result
    }

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, data: Nothing?): AstElement {
        val descriptor = getOrFail(BindingContext.ANNOTATION, annotationEntry)
        return context.constantValueGenerator.generateAnnotationConstructorCall(descriptor)!!
    }

    override fun visitElement(element: PsiElement?) {
        error("Unhandled element $element ${element?.javaClass} ${element?.text}")
    }

    private fun <R : AstElement> KtElement.convert(): R =
        this.accept(this@Psi2AstBuilder, null) as R

    private fun ReceiverValue.toAstExpression(): AstExpression {
        return when (this) {
            is ImplicitClassReceiver -> buildThisReference {
                this.type = this@toAstExpression.type.toAstType()
                labelName = classDescriptor.name.asString()
            }
            is ThisClassReceiver -> buildThisReference {
                this.type = this@toAstExpression.type.toAstType()
                labelName = classDescriptor.name.asString()
            }
            is SuperCallReceiverValue -> buildSuperReference {
                this.type = this@toAstExpression.type.toAstType()
            }
            is ExpressionReceiver -> expression.convert()
            is ClassValueReceiver -> buildQualifiedAccess {
                this.type = this@toAstExpression.type.toAstType()
                callee = symbolTable.getClassSymbol(this@toAstExpression.classQualifier.descriptor as ClassDescriptor)
            }
            is ExtensionReceiver -> buildThisReference {
                this.type = this@toAstExpression.type.toAstType()
                labelName = this@toAstExpression.declarationDescriptor.name.asString()
            }
            else -> error("Unexpected receiver value $this")
        }
    }

}
