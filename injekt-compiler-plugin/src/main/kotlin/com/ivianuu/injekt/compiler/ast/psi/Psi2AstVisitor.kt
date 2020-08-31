package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstAnonymousInitializer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.addChild
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBlock
import com.ivianuu.injekt.compiler.ast.tree.expression.AstCatch
import com.ivianuu.injekt.compiler.ast.tree.expression.AstConst
import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression
import com.ivianuu.injekt.compiler.ast.tree.expression.AstQualifiedAccess
import com.ivianuu.injekt.compiler.ast.tree.expression.AstReturn
import com.ivianuu.injekt.compiler.ast.tree.expression.AstStringConcatenation
import com.ivianuu.injekt.compiler.ast.tree.expression.AstThis
import com.ivianuu.injekt.compiler.ast.tree.expression.AstThrow
import com.ivianuu.injekt.compiler.ast.tree.expression.AstTry
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.js.translate.callTranslator.getReturnType
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
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
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.constants.ByteValue
import org.jetbrains.kotlin.resolve.constants.CharValue
import org.jetbrains.kotlin.resolve.constants.DoubleValue
import org.jetbrains.kotlin.resolve.constants.FloatValue
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.constants.LongValue
import org.jetbrains.kotlin.resolve.constants.NullValue
import org.jetbrains.kotlin.resolve.constants.ShortValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.constants.UByteValue
import org.jetbrains.kotlin.resolve.constants.UIntValue
import org.jetbrains.kotlin.resolve.constants.ULongValue
import org.jetbrains.kotlin.resolve.constants.UShortValue
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassValueReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.SuperCallReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisClassReceiver
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils

class Psi2AstVisitor(
    override val context: GeneratorContext
) : KtVisitor<AstElement, Nothing?>(), Generator {

    private val functionStack = mutableListOf<AstFunction>()

    override fun visitElement(element: PsiElement?) {
        error("Unhandled element $element ${element?.javaClass} ${element?.text}")
    }

    override fun visitKtFile(file: KtFile, data: Nothing?): AstElement {
        return AstFile(
            packageFqName = file.packageFqName,
            name = file.name.asNameId()
        ).apply {
            // todo annotations += generateAnnotations()
            file.declarations
                .map { it.accept<AstDeclaration>() }
                .forEach { addChild(it) }
        }
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject, data: Nothing?): AstElement {
        val descriptor = classOrObject.descriptor<ClassDescriptor>()
        context.storage.classes[descriptor]?.let { return it }
        return AstClass(
            name = descriptor.name,
            kind = descriptor.kind.toAstClassKind(),
            visibility = descriptor.visibility.toAstVisibility(),
            expectActual = expectActualOf(descriptor.isActual, descriptor.isExpect),
            modality = descriptor.modality.toAstModality(),
            isCompanion = descriptor.isCompanionObject,
            isFun = descriptor.isFun,
            isData = descriptor.isData,
            isInner = descriptor.isInner,
            isExternal = descriptor.isExternal
        ).apply {
            context.storage.classes[descriptor] = this
            annotations += classOrObject.annotationEntries.map { it.accept() }
            typeParameters += classOrObject.typeParameters
                .map { it.accept<AstTypeParameter>() }
                .onEach { it.parent = this }

            val primaryConstructor = classOrObject.primaryConstructor
            if (primaryConstructor != null) {
                val astPrimaryConstructor = primaryConstructor.accept<AstFunction>()
                this.primaryConstructor = astPrimaryConstructor
                // todo? addChild(astPrimaryConstructor)
                /*primaryConstructor
                    .valueParameters
                    .mapNotNull { get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, it) }
                    .map { property ->
                        property.toAstProperty(
                            astPrimaryConstructor.valueParameters
                                .single { it.name == property.name }
                        )
                    }
                    .forEach { addChild(it) }*/
            }

            classOrObject.declarations
                .filterNot { it is KtPropertyAccessor }
                .map { it.accept<AstDeclaration>() }
                .forEach { addChild(it) }
        }
    }

    override fun visitNamedFunction(function: KtNamedFunction, data: Nothing?): AstFunction =
        visitFunction(function)

    override fun visitPrimaryConstructor(
        constructor: KtPrimaryConstructor,
        data: Nothing?
    ): AstFunction = visitFunction(constructor)

    override fun visitSecondaryConstructor(
        constructor: KtSecondaryConstructor,
        data: Nothing?
    ): AstFunction = visitFunction(constructor)

    private fun visitFunction(function: KtFunction): AstFunction {
        val descriptor = function.descriptor<FunctionDescriptor>()
        context.storage.functions[descriptor]?.let { return it }
        return AstFunction(
            name = descriptor.name,
            kind = descriptor.toAstFunctionKind(),
            visibility = descriptor.visibility.toAstVisibility(),
            expectActual = expectActualOf(descriptor.isActual, descriptor.isExpect),
            modality = descriptor.modality.toAstModality(),
            returnType = descriptor.returnType!!.toAstType(),
            isInfix = descriptor.isInfix,
            isOperator = descriptor.isOperator,
            isTailrec = descriptor.isTailrec,
            isSuspend = descriptor.isSuspend
        ).apply {
            context.storage.functions[descriptor] = this
            functionStack.push(this)
            annotations += function.annotationEntries.map { it.accept() }
            typeParameters += function.typeParameters
                .map { it.accept<AstTypeParameter>() }
                .onEach { it.parent = this }
            dispatchReceiverType = descriptor.dispatchReceiverParameter?.type?.toAstType()
            extensionReceiverType = descriptor.extensionReceiverParameter?.type?.toAstType()
            valueParameters += function.valueParameters
                .map { it.accept<AstValueParameter>() }
                .onEach { it.parent = this }
            overriddenDeclarations += descriptor.overriddenDescriptors
                .map { context.astProvider.get(it) }
            body = function.bodyExpression?.let { visitExpressionForBlock(it) }
            functionStack.pop()
        }
    }

    override fun visitProperty(property: KtProperty, data: Nothing?): AstElement {
        val descriptor = property.descriptor<PropertyDescriptor>()
        context.storage.properties[descriptor]?.let { return it }
        return AstProperty(
            name = descriptor.name,
            type = descriptor.type.toAstType(),
            kind = when {
                descriptor.isConst -> AstProperty.Kind.CONST_VAL
                descriptor.isLateInit -> AstProperty.Kind.LATEINIT_VAR
                descriptor.isVar -> AstProperty.Kind.VAR
                else -> AstProperty.Kind.VAl
            },
            modality = descriptor.modality.toAstModality(),
            visibility = descriptor.visibility.toAstVisibility(),
            expectActual = expectActualOf(descriptor.isActual, descriptor.isExpect),
            isExternal = descriptor.isExternal
        ).apply {
            context.storage.properties[descriptor] = this
            annotations += property.annotations.map { it.accept() }
            typeParameters += property.typeParameters
                .map { it.accept<AstTypeParameter>() }
                .onEach { it.parent = this }
            dispatchReceiverType = descriptor.dispatchReceiverParameter?.type?.toAstType()
            extensionReceiverType = descriptor.extensionReceiverParameter?.type?.toAstType()
            getter = property
                .getter
                ?.accept<AstFunction>()
                ?.also { it.parent = this }
            setter = property
                .setter
                ?.accept<AstFunction>()
                ?.also { it.parent = this }
            initializer = when {
                // todo valueParameter != null -> AstQualifiedAccess(valueParameter, valueParameter.type)
                property is KtProperty -> property.initializer?.accept()
                else -> null
            }
            delegate = if (property is KtProperty)
                property.delegateExpression?.accept()
            else null
        }
    }

    override fun visitAnonymousInitializer(
        initializer: KtAnonymousInitializer,
        data: Nothing?
    ): AstAnonymousInitializer {
        return AstAnonymousInitializer().apply {
            body = visitExpressionForBlock(initializer.body!!)
        }
    }

    private fun visitExpressionForBlock(expression: KtExpression): AstBlock {
        return if (expression is KtBlockExpression) expression.accept()
        else {
            val type = expression.getTypeInferredByFrontendOrFail().toAstType()
            val astExpression = expression.accept<AstExpression>()
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

    override fun visitTypeParameter(parameter: KtTypeParameter, data: Nothing?): AstTypeParameter {
        val descriptor = parameter.descriptor<TypeParameterDescriptor>()
        context.storage.typeParameters[descriptor]?.let { return it }
        return AstTypeParameter(
            name = descriptor.name,
            isReified = descriptor.isReified,
            variance = descriptor.variance.toAstVariance()
        ).apply {
            annotations += parameter.annotationEntries.map { it.accept() }
            superTypes += descriptor.upperBounds.map { it.toAstType() }
        }
    }

    override fun visitParameter(parameter: KtParameter, data: Nothing?): AstValueParameter {
        println(parameter.text)
        val descriptor = parameter.descriptor<VariableDescriptor>()
        context.storage.valueParameters[descriptor]?.let { return it }
        return AstValueParameter(
            name = descriptor.name,
            type = descriptor.type.toAstType(),
            isVarArg = if (descriptor is ValueParameterDescriptor) descriptor.isVararg else false,
            inlineHint = when {
                descriptor is ValueParameterDescriptor && descriptor.isCrossinline -> AstValueParameter.InlineHint.CROSSINLINE
                descriptor is ValueParameterDescriptor && descriptor.isNoinline -> AstValueParameter.InlineHint.NOINLINE
                else -> null
            }
        ).apply {
            context.storage.valueParameters[descriptor] = this
            defaultValue = (descriptor.findPsi() as? KtParameter)?.defaultValue?.accept()
            annotations += parameter.annotationEntries.map { it.accept() }
        }
    }

    override fun visitTypeAlias(typeAlias: KtTypeAlias, data: Nothing?): AstTypeAlias {
        val descriptor = typeAlias.descriptor<TypeAliasDescriptor>()
        context.storage.typeAliases[descriptor]?.let { return it }
        return AstTypeAlias(
            name = descriptor.name,
            type = descriptor.expandedType.toAstType(),
            visibility = descriptor.visibility.toAstVisibility(),
            expectActual = expectActualOf(descriptor.isActual, descriptor.isExpect)
        ).apply {
            context.storage.typeAliases[descriptor] = this
            annotations += typeAlias.annotationEntries.map { it.accept() }
            typeParameters += typeAlias.typeParameters
                .map { it.accept<AstTypeParameter>() }
                .onEach { it.parent = this }
        }
    }

    override fun visitAnnotationEntry(
        annotationEntry: KtAnnotationEntry,
        data: Nothing?
    ): AstQualifiedAccess {
        TODO("$annotationEntry")
    }

    override fun visitBlockExpression(expression: KtBlockExpression, data: Nothing?): AstBlock {
        return AstBlock(expression.getExpressionTypeWithCoercionToUnitOrFail().toAstType()).apply {
            statements += expression.statements.map { it.accept() }
        }
    }

    override fun visitStringTemplateExpression(
        expression: KtStringTemplateExpression,
        data: Nothing?
    ): AstElement {
        val resultType = expression.getTypeInferredByFrontendOrFail().toAstType()
        val entries = expression.entries.map { it.accept<AstExpression>() }
        return when (entries.size) {
            0 -> AstConst.string(resultType, "")
            1 -> {
                val entry = entries.single()
                if (entry is AstConst<*> && entry.kind == AstConst.Kind.String)
                    entry
                else
                    AstStringConcatenation(resultType).apply {
                        arguments += entry
                    }
            }
            else -> AstStringConcatenation(resultType).apply {
                arguments += entries
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
    ): AstExpression = entry.expression!!.accept()

    override fun visitReferenceExpression(
        expression: KtReferenceExpression,
        data: Nothing?
    ): AstElement {
        val resolvedCall = expression.getResolvedCall()
            ?: error("Couldn't find call for $this $javaClass ${expression.text}")
        return AstQualifiedAccess(
            type = resolvedCall.getReturnType().toAstType(),
            callee = context.astProvider.get(resolvedCall.resultingDescriptor)
        ).apply {
            typeArguments += resolvedCall.typeArguments.values.map { it.toAstType() }

            dispatchReceiver = resolvedCall.dispatchReceiver?.toAstExpression()
            extensionReceiver = resolvedCall.extensionReceiver?.toAstExpression()

            val sortedValueArguments = resolvedCall.valueArguments
                .toList()
                .sortedBy { it.first.index }

            valueArguments += sortedValueArguments.map { (_, valueArgument) ->
                when (valueArgument) {
                    is DefaultValueArgument -> null
                    is ExpressionValueArgument -> {
                        val valueArgument1 = valueArgument.valueArgument
                            ?: throw AssertionError("No value argument: $valueArgument")
                        val argumentExpression = valueArgument1.getArgumentExpression()
                            ?: throw AssertionError("No argument expression: $valueArgument1")
                        argumentExpression.accept<AstExpression>()
                    }
                    // todo
                    is VarargValueArgument -> null
                    else -> TODO("Unexpected valueArgument: ${valueArgument::class.java.simpleName}")
                }
            }
        }
    }

    override fun visitThisExpression(expression: KtThisExpression, data: Nothing?): AstElement {
        val referenceTarget =
            getOrFail(BindingContext.REFERENCE_TARGET, expression.instanceReference)
        TODO("What $referenceTarget")
    }

    override fun visitConstantExpression(
        expression: KtConstantExpression,
        data: Nothing?
    ) = visitExpressionForConstant(expression)

    private fun visitExpressionForConstant(expression: KtExpression): AstConst<*> {
        // todo check KtEscapeStringTemplateEntry
        val constantValue =
            ConstantExpressionEvaluator.getConstant(expression, context.bindingContext)
                ?.toConstantValue(expression.getTypeInferredByFrontendOrFail())
                ?: error("KtConstantExpression was not evaluated: ${expression.text}")
        val constantType = constantValue.getType(context.module).toAstType()
        return when (constantValue) {
            is StringValue -> AstConst.string(constantType, constantValue.value)
            is IntValue -> AstConst.int(constantType, constantValue.value)
            is UIntValue -> AstConst.int(constantType, constantValue.value)
            is NullValue -> AstConst.constNull(constantType)
            is BooleanValue -> AstConst.boolean(constantType, constantValue.value)
            is LongValue -> AstConst.long(constantType, constantValue.value)
            is ULongValue -> AstConst.long(constantType, constantValue.value)
            is DoubleValue -> AstConst.double(constantType, constantValue.value)
            is FloatValue -> AstConst.float(constantType, constantValue.value)
            is CharValue -> AstConst.char(constantType, constantValue.value)
            is ByteValue -> AstConst.byte(constantType, constantValue.value)
            is UByteValue -> AstConst.byte(constantType, constantValue.value)
            is ShortValue -> AstConst.short(constantType, constantValue.value)
            is UShortValue -> AstConst.short(constantType, constantValue.value)
            else -> error("Unexpected constant value: ${constantValue.javaClass.simpleName} $constantValue")
        }
    }

    override fun visitTryExpression(expression: KtTryExpression, data: Nothing?): AstElement {
        return AstTry(
            type = expression.getExpressionTypeWithCoercionToUnitOrFail().toAstType(),
            tryResult = expression.tryBlock.accept(),
            catches = expression.catchClauses
                .map { catchClause ->
                    AstCatch(
                        catchParameter = catchClause.catchParameter!!.accept(),
                        result = visitExpressionForBlock(catchClause.catchBody!!)
                    )
                }
                .toMutableList(),
            finally = expression.finallyBlock?.finalExpression?.accept()
        )
    }

    override fun visitReturnExpression(expression: KtReturnExpression, data: Nothing?): AstElement {
        val returnTarget = getReturnExpressionTarget(expression, null) as FunctionDescriptor
        return AstReturn(
            type = context.builtIns.nothingType,
            target = context.astProvider.get(returnTarget),
            expression = expression.returnedExpression?.accept() ?: AstQualifiedAccess(
                callee = context.builtIns.unitClass,
                type = context.builtIns.unitType
            )
        )
    }

    override fun visitThrowExpression(expression: KtThrowExpression, data: Nothing?): AstElement {
        return AstThrow(
            type = context.builtIns.nothingType,
            expression = expression.thrownExpression!!.accept()
        )
    }

    override fun visitDotQualifiedExpression(
        expression: KtDotQualifiedExpression,
        data: Nothing?
    ): AstElement {
        return expression.selectorExpression!!.accept<AstQualifiedAccess>().also {
            it.safe = false
        }
    }

    override fun visitSafeQualifiedExpression(
        expression: KtSafeQualifiedExpression,
        data: Nothing?
    ): AstElement {
        return expression.selectorExpression!!.accept<AstQualifiedAccess>().also {
            it.safe = true
        }
    }

    fun ReceiverValue.toAstExpression(): AstExpression {
        return when (this) {
            is ImplicitClassReceiver -> {
                val receiverClass = context.astProvider.get<AstClass>(classDescriptor)
                AstThis(type.toAstType(), receiverClass)
            }
            is ThisClassReceiver -> {
                val receiverClass = context.astProvider.get<AstClass>(classDescriptor)
                AstThis(type.toAstType(), receiverClass)
            }
            is SuperCallReceiverValue -> TODO()
            is ExpressionReceiver -> expression.accept()
            is ClassValueReceiver -> {
                val receiverClass = context.astProvider.get<AstClass>(
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

    fun scopeOwnerAsCallable(scopeOwner: FunctionDescriptor?) =
        (scopeOwner as? CallableDescriptor)
            ?: throw AssertionError("'return' in a non-callable: $scopeOwner")

    fun getReturnExpressionTarget(
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

    fun <T : AstElement> KtElement.accept() = accept(this@Psi2AstVisitor, null) as T

}
