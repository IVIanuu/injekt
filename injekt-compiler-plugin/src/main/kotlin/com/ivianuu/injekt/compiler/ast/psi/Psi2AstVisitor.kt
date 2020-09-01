package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.AstModality
import com.ivianuu.injekt.compiler.ast.tree.AstVisibility
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstAnonymousInitializer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationBase
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationParent
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.addChild
import com.ivianuu.injekt.compiler.ast.tree.expression.AstAnonymousObjectExpression
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBlock
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBreak
import com.ivianuu.injekt.compiler.ast.tree.expression.AstCatch
import com.ivianuu.injekt.compiler.ast.tree.expression.AstConst
import com.ivianuu.injekt.compiler.ast.tree.expression.AstContinue
import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression
import com.ivianuu.injekt.compiler.ast.tree.expression.AstForLoop
import com.ivianuu.injekt.compiler.ast.tree.expression.AstLoop
import com.ivianuu.injekt.compiler.ast.tree.expression.AstQualifiedAccess
import com.ivianuu.injekt.compiler.ast.tree.expression.AstReturn
import com.ivianuu.injekt.compiler.ast.tree.expression.AstThis
import com.ivianuu.injekt.compiler.ast.tree.expression.AstThrow
import com.ivianuu.injekt.compiler.ast.tree.expression.AstTry
import com.ivianuu.injekt.compiler.ast.tree.expression.AstWhileLoop
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.NotFoundClasses
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.js.translate.callTranslator.getReturnType
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
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
import org.jetbrains.kotlin.psi.KtLabeledExpression
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
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.constants.AnnotationValue
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.constants.ByteValue
import org.jetbrains.kotlin.resolve.constants.CharValue
import org.jetbrains.kotlin.resolve.constants.ConstantValue
import org.jetbrains.kotlin.resolve.constants.DoubleValue
import org.jetbrains.kotlin.resolve.constants.EnumValue
import org.jetbrains.kotlin.resolve.constants.ErrorValue
import org.jetbrains.kotlin.resolve.constants.FloatValue
import org.jetbrains.kotlin.resolve.constants.IntValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
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
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.typeUtil.builtIns

class Psi2AstVisitor(
    override val context: GeneratorContext
) : KtVisitor<AstElement, Psi2AstVisitor.Mode>(), Generator {

    enum class Mode {
        PARTIAL, FULL
    }

    private val functionStack = mutableListOf<AstFunction>()
    private val parentStack = mutableListOf<AstDeclarationParent>()
    private val loops = mutableMapOf<KtLoopExpression, AstLoop>()

    private fun <K, V> K.cached(
        cache: MutableMap<K, V>,
        init: () -> V,
        block: V.() -> Unit
    ) = cache.getOrPut(if (this is DeclarationDescriptor) original as K else this, init)
        .apply(block)

    private fun <T : AstElement> KtElement.accept(mode: Mode) =
        accept(this@Psi2AstVisitor, mode) as T

    private inline fun <R> withDeclarationParent(
        parent: AstDeclarationParent,
        block: () -> R
    ): R {
        parentStack.push(parent)
        val result = block()
        parentStack.pop()
        return result
    }

    private fun <T : AstDeclarationBase> T.applyParentFromStack() =
        apply {
            parent = parentStack.last()
        }

    override fun visitElement(element: PsiElement?) {
        error("Unhandled element $element ${element?.javaClass} ${element?.text}")
    }

    override fun visitKtFile(file: KtFile, mode: Mode): AstElement {
        return file.cached(
            context.storage.files,
            {
                AstFile(
                    packageFqName = file.packageFqName,
                    name = file.name.asNameId()
                )
            }
        ) {
            withDeclarationParent(this) {
                if (mode == Mode.FULL) {
                    annotations.clear()
                    annotations += file.annotationEntries.map { it.accept(mode) }
                }

                declarations.clear()
                declarations += file.declarations
                    .map { it.accept(mode) }
            }
        }
    }

    override fun visitClassOrObject(classOrObject: KtClassOrObject, mode: Mode): AstElement {
        val descriptor = classOrObject.descriptor<ClassDescriptor>()
        return descriptor.cached(
            context.storage.classes,
            {
                AstClass(
                    name = descriptor.name,
                    kind = descriptor.toAstClassKind(),
                    visibility = descriptor.visibility.toAstVisibility(),
                    expectActual = expectActualOf(descriptor.isActual, descriptor.isExpect),
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

    override fun visitNamedFunction(function: KtNamedFunction, mode: Mode): AstFunction =
        visitFunction(function, mode)

    override fun visitPrimaryConstructor(
        constructor: KtPrimaryConstructor,
        mode: Mode
    ): AstFunction = visitFunction(constructor, mode)

    override fun visitSecondaryConstructor(
        constructor: KtSecondaryConstructor,
        mode: Mode
    ): AstFunction = visitFunction(constructor, mode)

    private fun visitFunction(
        function: KtFunction,
        mode: Mode
    ): AstFunction {
        val descriptor = function.descriptor<FunctionDescriptor>()
        return descriptor.cached(
            context.storage.functions,
            {
                AstFunction(
                    name = descriptor.name,
                    kind = descriptor.toAstFunctionKind(),
                    visibility = descriptor.visibility.toAstVisibility(),
                    expectActual = expectActualOf(descriptor.isActual, descriptor.isExpect),
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
                    .map { context.astProvider.get(it) }
                if (mode == Mode.FULL) {
                    body = function.bodyExpression?.let { visitExpressionForBlock(it, mode) }
                }
                functionStack.pop()
            }
        }
    }

    override fun visitProperty(property: KtProperty, mode: Mode): AstElement {
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
                    expectActual = if (descriptor is PropertyDescriptor) expectActualOf(
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
        mode: Mode
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

    override fun visitTypeParameter(parameter: KtTypeParameter, mode: Mode): AstTypeParameter {
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

    override fun visitParameter(parameter: KtParameter, mode: Mode): AstValueParameter {
        val descriptor = parameter.descriptor<VariableDescriptor>()
        return descriptor.cached(
            context.storage.valueParameters,
            {
                AstValueParameter(
                    name = descriptor.name,
                    type = UninitializedType,
                    isVarArg = if (descriptor is ValueParameterDescriptor) descriptor.isVararg else false,
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

    override fun visitTypeAlias(typeAlias: KtTypeAlias, mode: Mode): AstTypeAlias {
        val descriptor = typeAlias.descriptor<TypeAliasDescriptor>()
        return descriptor.cached(
            context.storage.typeAliases,
            {
                AstTypeAlias(
                    name = descriptor.name,
                    type = UninitializedType,
                    visibility = descriptor.visibility.toAstVisibility(),
                    expectActual = expectActualOf(descriptor.isActual, descriptor.isExpect)
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
        mode: Mode
    ): AstAnonymousObjectExpression {
        // important to compute the ast declaration before asking for it's type
        val anonymousObject = expression.objectDeclaration.accept<AstClass>(mode)
        return AstAnonymousObjectExpression(
            expression.getTypeInferredByFrontendOrFail().toAstType(),
            anonymousObject
        )
    }

    override fun visitBlockExpression(expression: KtBlockExpression, mode: Mode): AstBlock {
        return AstBlock(expression.getExpressionTypeWithCoercionToUnitOrFail().toAstType()).apply {
            statements += expression.statements.map { it.accept(mode) }
        }
    }

    private fun visitExpressionForBlock(expression: KtExpression, mode: Mode): AstBlock {
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
        mode: Mode
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
        mode: Mode
    ): AstConst<String> = AstConst.string(context.builtIns.stringType, entry.text)

    override fun visitEscapeStringTemplateEntry(
        entry: KtEscapeStringTemplateEntry,
        mode: Mode
    ): AstConst<String> = AstConst.string(context.builtIns.stringType, entry.unescapedValue)

    override fun visitStringTemplateEntryWithExpression(
        entry: KtStringTemplateEntryWithExpression,
        mode: Mode
    ): AstExpression = entry.expression!!.accept(mode)

    override fun visitReferenceExpression(
        expression: KtReferenceExpression,
        mode: Mode
    ): AstElement {
        val resolvedCall = expression.getResolvedCall()
            ?: error("Couldn't find call for $this $javaClass ${expression.text}")
        return AstQualifiedAccess(
            type = resolvedCall.getReturnType().toAstType(),
            callee = context.astProvider.get(resolvedCall.resultingDescriptor)
        ).apply {
            typeArguments += resolvedCall.typeArguments.values.map { it.toAstType() }

            dispatchReceiver = resolvedCall.dispatchReceiver?.toAstExpression(mode)
            extensionReceiver = resolvedCall.extensionReceiver?.toAstExpression(mode)

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
                        argumentExpression.accept<AstExpression>(mode)
                    }
                    // todo
                    is VarargValueArgument -> null
                    else -> TODO("Unexpected valueArgument: ${valueArgument::class.java.simpleName}")
                }
            }
        }
    }

    override fun visitDestructuringDeclaration(
        multiDeclaration: KtDestructuringDeclaration,
        mode: Mode
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
                            callee = context.astProvider.get(componentResolvedCall.resultingDescriptor),
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

    override fun visitThisExpression(expression: KtThisExpression, mode: Mode): AstExpression {
        val referenceTarget =
            getOrFail(BindingContext.REFERENCE_TARGET, expression.instanceReference)
        TODO("What $referenceTarget")
    }

    override fun visitConstantExpression(
        expression: KtConstantExpression,
        mode: Mode
    ) = visitExpressionForConstant(expression)

    private fun visitExpressionForConstant(expression: KtExpression): AstExpression {
        val constantValue =
            ConstantExpressionEvaluator.getConstant(expression, context.bindingContext)
                ?.toConstantValue(expression.getTypeInferredByFrontendOrFail())
                ?: error("KtConstantExpression was not evaluated: ${expression.text}")
        return generateConstantValueAsExpression(constantValue)
    }

    override fun visitWhileExpression(expression: KtWhileExpression, mode: Mode) =
        visitWhile(expression, mode)

    override fun visitDoWhileExpression(
        expression: KtDoWhileExpression,
        mode: Mode
    ) = visitWhile(expression, mode)

    private fun visitWhile(expression: KtWhileExpressionBase, mode: Mode) =
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

    override fun visitForExpression(expression: KtForExpression, mode: Mode): AstElement {
        val ktLoopParameter = expression.loopParameter
        val ktLoopDestructuringDeclaration = expression.destructuringDeclaration
        if (ktLoopParameter == null && ktLoopDestructuringDeclaration == null) {
            throw AssertionError("Either loopParameter or destructuringParameter should be present:\n${expression.text}")
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

    override fun visitBreakExpression(expression: KtBreakExpression, mode: Mode): AstElement {
        val parentLoop = findParentLoop(expression)
            ?: error("No loop found for ${expression.text}")
        return AstBreak(
            type = context.builtIns.nothingType,
            loop = parentLoop
        )
    }

    override fun visitContinueExpression(
        expression: KtContinueExpression,
        mode: Mode
    ): AstElement {
        val parentLoop = findParentLoop(expression)
            ?: error("No loop found for ${expression.text}")
        return AstContinue(
            type = context.builtIns.nothingType,
            loop = parentLoop
        )
    }

    override fun visitTryExpression(expression: KtTryExpression, mode: Mode): AstElement {
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

    override fun visitReturnExpression(expression: KtReturnExpression, mode: Mode): AstElement {
        val returnTarget = getReturnExpressionTarget(expression, null) as FunctionDescriptor
        return AstReturn(
            type = context.builtIns.nothingType,
            target = context.astProvider.get(returnTarget),
            expression = expression.returnedExpression?.accept(mode) ?: AstQualifiedAccess(
                callee = context.builtIns.unitClass,
                type = context.builtIns.unitType
            )
        )
    }

    override fun visitThrowExpression(expression: KtThrowExpression, mode: Mode): AstElement {
        return AstThrow(
            type = context.builtIns.nothingType,
            expression = expression.thrownExpression!!.accept(mode)
        )
    }

    override fun visitDotQualifiedExpression(
        expression: KtDotQualifiedExpression,
        mode: Mode
    ): AstElement {
        return expression.selectorExpression!!.accept<AstQualifiedAccess>(mode).also {
            it.safe = false
        }
    }

    override fun visitSafeQualifiedExpression(
        expression: KtSafeQualifiedExpression,
        mode: Mode
    ): AstElement {
        return expression.selectorExpression!!.accept<AstQualifiedAccess>(mode).also {
            it.safe = true
        }
    }

    fun ReceiverValue.toAstExpression(mode: Mode): AstExpression {
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
            is ExpressionReceiver -> expression.accept(mode)
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
        return generateAnnotationConstructorCall(descriptor)!!
    }

    private fun generateConstantValueAsExpression(
        constantValue: ConstantValue<*>,
        varargElementType: KotlinType? = null
    ): AstExpression =
        // Assertion is safe here because annotation calls and class literals are not allowed in constant initializers
        generateConstantOrAnnotationValueAsExpression(constantValue, varargElementType)!!

    private fun generateConstantOrAnnotationValueAsExpression(
        constantValue: ConstantValue<*>,
        varargElementType: KotlinType? = null
    ): AstExpression? {
        val constantKtType = constantValue.getType(context.module)
        val constantType = constantKtType.toAstType()

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
            is ArrayValue -> {
                /*val arrayElementType = varargElementType ?: constantKtType.getArrayElementType()
                IrVarargImpl(
                    startOffset, endOffset,
                    constantType,
                    arrayElementType.toAstType(),
                    constantValue.value.mapNotNull {
                        generateConstantOrAnnotationValueAsExpression(it, null)
                    }
                )*/
                TODO()
            }
            is EnumValue -> {
                val enumEntryDescriptor =
                    constantKtType.memberScope.getContributedClassifier(
                        constantValue.enumEntryName,
                        NoLookupLocation.FROM_BACKEND
                    )!!
                AstQualifiedAccess(
                    callee = context.astProvider.get(enumEntryDescriptor) as AstClass,
                    type = constantType
                )
            }
            is AnnotationValue -> generateAnnotationConstructorCall(constantValue.value)
            is KClassValue -> {
                /*val classifierKtType = constantValue.getArgumentType(moduleDescriptor)
                if (classifierKtType.isError) null
                else {
                    val classifierDescriptor = classifierKtType.constructor.declarationDescriptor
                        ?: throw AssertionError("Unexpected KClassValue: $classifierKtType")

                    IrClassReferenceImpl(
                        startOffset, endOffset,
                        constantValue.getType(moduleDescriptor).toIrType(),
                        symbolTable.referenceClassifier(classifierDescriptor),
                        classifierKtType.toIrType()
                    )
                }*/ TODO()
            }
            is ErrorValue -> null
            else -> error("Unexpected constant value: ${constantValue.javaClass.simpleName} $constantValue")
        }
    }

    private fun generateAnnotationConstructorCall(annotationDescriptor: AnnotationDescriptor): AstQualifiedAccess? {
        val annotationType = annotationDescriptor.type
        val annotationClassDescriptor = annotationType.constructor.declarationDescriptor
        if (annotationClassDescriptor !is ClassDescriptor) return null
        if (annotationClassDescriptor is NotFoundClasses.MockClassDescriptor) return null

        val primaryConstructorDescriptor = annotationClassDescriptor.unsubstitutedPrimaryConstructor
            ?: annotationClassDescriptor.constructors.singleOrNull()
            ?: throw AssertionError("No constructor for annotation class $annotationClassDescriptor")
        val astPrimaryConstructor =
            context.astProvider.get<AstFunction>(primaryConstructorDescriptor)

        return AstQualifiedAccess(
            callee = astPrimaryConstructor,
            type = annotationType.toAstType()
        ).apply {
            valueArguments += primaryConstructorDescriptor.valueParameters
                .map { valueParameter ->
                    val argumentValue = annotationDescriptor.allValueArguments[valueParameter.name]
                        ?: return null
                    generateConstantOrAnnotationValueAsExpression(
                        argumentValue,
                        valueParameter.varargElementType
                    )
                }
        }
    }

    private fun KotlinType.getArrayElementType() = builtIns.getArrayElementType(this)

}
