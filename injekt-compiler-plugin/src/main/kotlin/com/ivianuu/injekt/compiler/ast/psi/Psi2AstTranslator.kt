package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.ast.tree.AstTarget
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstAnonymousInitializer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstConstructor
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstFile
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstModuleFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstProperty
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstSimpleFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstValueParameter
import com.ivianuu.injekt.compiler.ast.tree.declaration.addChild
import com.ivianuu.injekt.compiler.ast.tree.expression.AstBlock
import com.ivianuu.injekt.compiler.ast.tree.expression.AstConst
import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression
import com.ivianuu.injekt.compiler.ast.tree.expression.AstQualifiedAccess
import com.ivianuu.injekt.compiler.ast.tree.expression.AstReturn
import com.ivianuu.injekt.compiler.ast.tree.expression.AstStatement
import com.ivianuu.injekt.compiler.ast.tree.expression.AstStringConcatenation
import com.ivianuu.injekt.compiler.ast.tree.type.AstStarProjection
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeAbbreviation
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeArgument
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjection
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjectionImpl
import com.ivianuu.injekt.compiler.ast.tree.type.classOrFail
import org.jetbrains.kotlin.descriptors.CallableDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.js.translate.callTranslator.getReturnType
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtClassInitializer
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingContextUtils
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.bindingContextUtil.isUsedAsExpression
import org.jetbrains.kotlin.resolve.calls.callUtil.getResolvedCall
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
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
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.SimpleType
import org.jetbrains.kotlin.types.StarProjectionImpl
import org.jetbrains.kotlin.types.TypeProjection
import org.jetbrains.kotlin.types.expressions.ExpressionTypingUtils
import org.jetbrains.kotlin.types.getAbbreviation
import org.jetbrains.kotlin.types.model.TypeArgumentMarker
import org.jetbrains.kotlin.types.upperIfFlexible

class Psi2AstTranslator(
    private val bindingTrace: BindingTrace,
    private val module: ModuleDescriptor,
    private val stubGenerator: Psi2AstStubGenerator,
    private val storage: Psi2AstStorage
) {

    init {
        stubGenerator.translator = this
    }

    fun generateModule(files: List<KtFile>): AstModuleFragment {
        return AstModuleFragment(module.name).apply {
            this.files += files.map { it.toAstFile() }
        }
    }

    fun KtFile.toAstFile(): AstFile {
        storage.files[this]?.let { return it }
        return AstFile(
            packageFqName = packageFqName,
            name = name.asNameId()
        ).apply {
            // todo annotations += generateAnnotations()
            this@toAstFile.declarations.toAstDeclarations()
                .forEach { addChild(it) }
        }
    }

    fun KtDeclaration.toAstDeclaration(): AstDeclaration =
        when (this) {
            is KtClassOrObject -> (bindingTrace.get(
                BindingContext.DECLARATION_TO_DESCRIPTOR,
                this
            ) as ClassDescriptor).toAstClass()
            is KtNamedFunction -> (
                    bindingTrace.get(
                        BindingContext.DECLARATION_TO_DESCRIPTOR,
                        this
                    ) as SimpleFunctionDescriptor
                    ).toAstSimpleFunction()
            is KtConstructor<*> -> (
                    bindingTrace.get(
                        BindingContext.DECLARATION_TO_DESCRIPTOR,
                        this
                    ) as ConstructorDescriptor
                    ).toAstConstructor()
            is KtProperty -> (
                    bindingTrace.get(
                        BindingContext.DECLARATION_TO_DESCRIPTOR,
                        this
                    ) as PropertyDescriptor
                    ).toAstProperty()
            is KtClassInitializer -> toAstAnonymousInitializer()
            is KtTypeAlias -> (
                    bindingTrace.get(
                        BindingContext.DECLARATION_TO_DESCRIPTOR,
                        this
                    ) as TypeAliasDescriptor
                    ).toAstTypeAlias()
            else -> error("Unexpected declaration $this $javaClass $text")
        }

    fun List<KtDeclaration>.toAstDeclarations() =
        map { it.toAstDeclaration() }

    fun ClassDescriptor.toAstClass(): AstClass {
        storage.classes[this]?.let { return it }
        val declaration = findPsi() as? KtClassOrObject
            ?: return stubGenerator.get(this) as AstClass
        return AstClass(
            name = name,
            kind = kind.toAstClassKind(),
            visibility = visibility.toAstVisibility(),
            expectActual = expectActualOf(isActual, isExpect),
            modality = modality.toAstModality(),
            isCompanion = isCompanionObject,
            isFun = isFun,
            isData = isData,
            isInner = isInner,
            isExternal = isExternal
        ).apply {
            storage.classes[this@toAstClass] = this
            annotations += this@toAstClass.annotations.toAstAnnotations()
            typeParameters += declaredTypeParameters.toAstTypeParameters()
                .onEach { it.parent = this }

            val primaryConstructor = declaration.primaryConstructor
            if (primaryConstructor != null) {
                val astPrimaryConstructor = primaryConstructor.toAstDeclaration() as AstConstructor
                addChild(astPrimaryConstructor)
                primaryConstructor
                    .valueParameters
                    .mapNotNull {
                        bindingTrace.get(
                            BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER,
                            it
                        )
                    }
                    .map { property ->
                        property.toAstProperty(
                            astPrimaryConstructor.valueParameters
                                .single { it.name == property.name }
                        )
                    }
                    .forEach { addChild(it) }
            }

            declaration.declarations.toAstDeclarations()
                .forEach { addChild(it) }
        }
    }

    fun SimpleFunctionDescriptor.toAstSimpleFunction(): AstSimpleFunction {
        storage.simpleFunctions[this]?.let { return it }
        val declaration = findPsi() as? KtFunction
            ?: return stubGenerator.get(this) as AstSimpleFunction
        return AstSimpleFunction(
            name = name,
            visibility = visibility.toAstVisibility(),
            expectActual = expectActualOf(isActual, isExpect),
            modality = modality.toAstModality(),
            returnType = returnType!!.toAstType(),
            isInfix = isInfix,
            isOperator = isOperator,
            isTailrec = isTailrec,
            isSuspend = isSuspend
        ).apply {
            storage.simpleFunctions[this@toAstSimpleFunction] = this
            annotations += this@toAstSimpleFunction.annotations.toAstAnnotations()
            typeParameters += this@toAstSimpleFunction.typeParameters.toAstTypeParameters()
                .onEach { it.parent = this }
            valueParameters += this@toAstSimpleFunction.valueParameters.toAstValueParameters()
                .onEach { it.parent = this }
            overriddenFunctions += overriddenDescriptors
                .map { (it as SimpleFunctionDescriptor).toAstSimpleFunction() }
            body = declaration.bodyExpression?.toAstBlock(this)
        }
    }

    fun ConstructorDescriptor.toAstConstructor(): AstConstructor {
        storage.constructors[this]?.let { return it }
        val declaration = findPsi() as? KtConstructor<*>
            ?: return stubGenerator.get(this) as AstConstructor
        return AstConstructor(
            constructedClass = returnType.toAstType().classifier as AstClass,
            returnType = returnType.toAstType(),
            visibility = visibility.toAstVisibility(),
            expectActual = expectActualOf(isActual, isExpect),
            isPrimary = isPrimary
        ).apply {
            storage.constructors[this@toAstConstructor] = this// todo fixes recursion issues
            annotations += this@toAstConstructor.annotations.toAstAnnotations()
            typeParameters += this@toAstConstructor.typeParameters.toAstTypeParameters()
                .onEach { it.parent = this }
            valueParameters += this@toAstConstructor.valueParameters.toAstValueParameters()
                .onEach { it.parent = this }
            body = declaration.bodyExpression?.toAstBlock(this)
        }
    }

    fun PropertyDescriptor.toAstProperty(
        valueParameter: AstValueParameter? = null
    ): AstProperty {
        storage.properties[this]?.let { return it }
        val declaration = findPsi() ?: return stubGenerator.get(this) as AstProperty
        return AstProperty(
            name = name,
            type = type.toAstType(),
            kind = when {
                isConst -> AstProperty.Kind.CONST_VAL
                isLateInit -> AstProperty.Kind.LATEINIT_VAR
                isVar -> AstProperty.Kind.VAR
                else -> AstProperty.Kind.VAl
            },
            modality = modality.toAstModality(),
            visibility = visibility.toAstVisibility(),
            expectActual = expectActualOf(isActual, isExpect),
            isExternal = isExternal
        ).apply {
            storage.properties[this@toAstProperty] = this
            annotations += this@toAstProperty.annotations.toAstAnnotations()
            typeParameters += this@toAstProperty.typeParameters.toAstTypeParameters()
                .onEach { it.parent = this }
            delegate = if (declaration is KtProperty)
                declaration.delegateExpression?.toAstExpression()
            else null
            initializer = when {
                valueParameter != null -> AstQualifiedAccess(valueParameter, valueParameter.type)
                declaration is KtProperty -> declaration.initializer?.toAstExpression()
                else -> null
            }
        }
    }

    fun TypeAliasDescriptor.toAstTypeAlias(): AstTypeAlias {
        storage.typeAliases[this]?.let { return it }
        val declaration = findPsi() as? KtTypeAlias
            ?: return stubGenerator.get(this) as AstTypeAlias
        return AstTypeAlias(
            name = name,
            type = expandedType.toAstType(),
            visibility = visibility.toAstVisibility(),
            expectActual = expectActualOf(isActual, isExpect)
        ).apply {
            storage.typeAliases[this@toAstTypeAlias] = this
            annotations += this@toAstTypeAlias.annotations.toAstAnnotations()
            typeParameters += this@toAstTypeAlias.declaredTypeParameters.toAstTypeParameters()
                .onEach { it.parent = this }
        }
    }

    // todo
    fun AnnotationDescriptor.toAstAnnotation() = AstQualifiedAccess(
        type.toAstType().classOrFail.declarations.filterIsInstance<AstConstructor>().first(),
        type.toAstType()
    )

    // todo
    fun List<AnnotationDescriptor>.toAstAnnotations() =
        emptyList<AstQualifiedAccess>() //map { it.toAstAnnotation() }

    // todo
    fun Annotations.toAstAnnotations() =
        emptyList<AstQualifiedAccess>()//map { it.toAstAnnotation() }

    fun KtClassInitializer.toAstAnonymousInitializer() = AstAnonymousInitializer().apply {
        body = this@toAstAnonymousInitializer.body!!.toAstBlock(this)
    }

    fun TypeParameterDescriptor.toAstTypeParameter(): AstTypeParameter {
        storage.typeParameters[this]?.let { return it }
        return AstTypeParameter(
            name = name,
            isReified = isReified,
            variance = variance.toAstVariance()
        ).apply {
            annotations += this@toAstTypeParameter.annotations.toAstAnnotations()
            superTypes += this@toAstTypeParameter.upperBounds.map { it.toAstType() }
        }
    }

    fun List<TypeParameterDescriptor>.toAstTypeParameters() =
        map { it.toAstTypeParameter() }

    fun ValueParameterDescriptor.toAstValueParameter(): AstValueParameter {
        storage.valueParameters[this]?.let { return it }
        return AstValueParameter(
            name = name,
            type = type.toAstType(),
            isVarArg = isVararg,
            inlineHint = when {
                isCrossinline -> AstValueParameter.InlineHint.CROSSINLINE
                isNoinline -> AstValueParameter.InlineHint.NOINLINE
                else -> null
            },
            // todo what to do for stubs?
            defaultValue = (findPsi() as? KtParameter)?.defaultValue?.toAstExpression()
        ).apply {
            annotations += this@toAstValueParameter.annotations.toAstAnnotations()
        }
    }

    fun List<ValueParameterDescriptor>.toAstValueParameters() =
        map { it.toAstValueParameter() }

    fun KtElement.toAstStatement(): AstStatement = when (this) {
        is KtExpression -> toAstExpression()
        is KtDeclaration -> toAstDeclaration()
        is KtLiteralStringTemplateEntry -> toAstConst()
        is KtEscapeStringTemplateEntry -> toAstConst()
        is KtStringTemplateEntryWithExpression -> toAstExpression2()
        else -> error("Unexpected element $this $javaClass $text")
    }

    fun KtExpression.toAstExpression(): AstExpression =
        when (this) {
            is KtConstantExpression -> toAstConst()
            is KtBlockExpression -> toAstBlock()
            is KtStringTemplateExpression -> toAstExpression()
            is KtReferenceExpression -> toAstQualifiedAccess()
            else -> error("Unexpected expression $this $javaClass $text")
        }

    fun KtConstantExpression.toAstConst(): AstConst<*> {
        val constantValue =
            ConstantExpressionEvaluator.getConstant(this, bindingTrace.bindingContext)
                ?.toConstantValue(getTypeInferredByFrontendOrFail(this))
                ?: error("KtConstantExpression was not evaluated: $text")
        val constantType = constantValue.getType(module).toAstType()
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
            else -> TODO("Unexpected constant value: ${constantValue.javaClass.simpleName} $constantValue")
        }
    }

    fun KtExpression.toAstBlock(scopeOwner: AstDeclaration): AstBlock {
        return if (this is KtBlockExpression) {
            toAstBlock()
        } else {
            val type = getTypeInferredByFrontendOrFail(this).toAstType()
            val expression = toAstExpression()
            AstBlock(type).apply {
                statements += AstReturn(
                    type,
                    scopeOwner as AstTarget,
                    expression
                )
            }
        }
    }

    fun KtBlockExpression.toAstBlock(): AstBlock {
        return AstBlock(getExpressionTypeWithCoercionToUnitOrFail(this).toAstType()).apply {
            statements += this@toAstBlock.statements.map { it.toAstStatement() }
        }
    }

    fun KtStringTemplateExpression.toAstExpression(): AstExpression {
        val resultType = getTypeInferredByFrontendOrFail(this).toAstType()
        val entries = entries.map { it.toAstStatement() as AstExpression }

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

    fun KtLiteralStringTemplateEntry.toAstConst() =
        AstConst.string(module.builtIns.stringType.toAstType(), text)

    fun KtEscapeStringTemplateEntry.toAstConst() =
        AstConst.string(module.builtIns.stringType.toAstType(), unescapedValue)

    fun KtStringTemplateEntryWithExpression.toAstExpression2() =
        expression!!.toAstExpression()

    fun KtReferenceExpression.toAstQualifiedAccess(): AstQualifiedAccess {
        val resolvedCall = getResolvedCall(this)
            ?: error("Couldn't find call for $this $javaClass $text")
        return AstQualifiedAccess(
            type = resolvedCall.getReturnType().toAstType(),
            callee = when (val callee = resolvedCall.resultingDescriptor) {
                is ConstructorDescriptor -> callee.toAstConstructor()
                is SimpleFunctionDescriptor -> callee.toAstSimpleFunction()
                is PropertyDescriptor -> callee.toAstProperty()
                is FakeCallableDescriptorForObject -> callee.classDescriptor.toAstClass()
                is ValueParameterDescriptor -> callee.toAstValueParameter()
                else -> error("Unexpected callee $callee")
            }
        ).apply {
            typeArguments += resolvedCall.typeArguments.values.map { it.toAstType() }
            // todo receiver = calleeExpression?.toAstExpression()

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
                        argumentExpression.toAstExpression()
                    }
                    is VarargValueArgument -> {
                        TODO()
                        //generateVarargExpressionUsing(valueArgument, valueParameter, resolvedCall, generateArgumentExpression)
                    }
                    else -> TODO("Unexpected valueArgument: ${valueArgument::class.java.simpleName}")
                }
            }
        }
    }

    fun KotlinType.toAstType(): AstType {
        storage.types[this]?.let { return it }
        val approximatedType = upperIfFlexible()
        return AstType().apply {
            storage.types[this@toAstType] = this
            classifier =
                when (val classifier = approximatedType.constructor.declarationDescriptor) {
                    is ClassDescriptor -> classifier.toAstClass()
                    is TypeParameterDescriptor -> classifier.toAstTypeParameter()
                    else -> error("Unexpected classifier $classifier")
                }
            hasQuestionMark = approximatedType.isMarkedNullable
            arguments += approximatedType.arguments.map { it.toAstTypeArgument() }
            abbreviation = approximatedType.getAbbreviation()?.toAstTypeAbbreviation()
        }
    }

    fun SimpleType.toAstTypeAbbreviation(): AstTypeAbbreviation {
        storage.typeAbbreviations[this]?.let { return it }
        val typeAliasDescriptor = constructor.declarationDescriptor.let {
            it as? TypeAliasDescriptor
                ?: throw AssertionError("TypeAliasDescriptor expected: $it")
        }
        return AstTypeAbbreviation(typeAliasDescriptor.toAstTypeAlias()).apply {
            hasQuestionMark = isMarkedNullable
            arguments += this@toAstTypeAbbreviation.arguments.map { it.toAstTypeArgument() }
            annotations += this@toAstTypeAbbreviation.annotations.toList().toAstAnnotations()
        }
    }

    fun TypeArgumentMarker.toAstTypeArgument(): AstTypeArgument {
        storage.typeArguments[this]?.let { return it }
        return when (this) {
            is StarProjectionImpl -> AstStarProjection
            is TypeProjection -> toAstTypeProjection()
            else -> error("Unexpected type $this")
        }
    }

    fun TypeProjection.toAstTypeProjection(): AstTypeProjection {
        storage.typeProjections[this]?.let { return it }
        return AstTypeProjectionImpl(
            variance = projectionKind.toAstVariance(),
            type = type.toAstType()
        )
    }

    fun getTypeInferredByFrontend(key: KtExpression): KotlinType? =
        bindingTrace.getType(key)

    fun getTypeInferredByFrontendOrFail(key: KtExpression): KotlinType =
        getTypeInferredByFrontend(key) ?: error("No type for expression: ${key.text}")

    fun getExpressionTypeWithCoercionToUnit(key: KtExpression): KotlinType? =
        if (key.isUsedAsExpression(bindingTrace.bindingContext))
            getTypeInferredByFrontend(key)
        else
            module.builtIns.unitType

    fun getExpressionTypeWithCoercionToUnitOrFail(key: KtExpression): KotlinType =
        getExpressionTypeWithCoercionToUnit(key) ?: error("No type for expression: ${key.text}")

    fun getResolvedCall(key: KtElement): ResolvedCall<out CallableDescriptor>? =
        key.getResolvedCall(bindingTrace.bindingContext)

    fun scopeOwnerAsCallable(scopeOwner: DeclarationDescriptor) =
        (scopeOwner as? CallableDescriptor)
            ?: throw AssertionError("'return' in a non-callable: $scopeOwner")

    fun getReturnExpressionTarget(
        expression: KtReturnExpression,
        scopeOwner: DeclarationDescriptor
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
                    val labelTarget =
                        bindingTrace.get(BindingContext.LABEL_TARGET, label)!!
                    val labelTargetDescriptor =
                        bindingTrace.get(BindingContext.DECLARATION_TO_DESCRIPTOR, labelTarget)!!
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
}
