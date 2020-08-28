package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.ast.tree.AstTarget
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
import com.ivianuu.injekt.compiler.ast.tree.expression.AstCall
import com.ivianuu.injekt.compiler.ast.tree.expression.AstConst
import com.ivianuu.injekt.compiler.ast.tree.expression.AstExpression
import com.ivianuu.injekt.compiler.ast.tree.expression.AstReturn
import com.ivianuu.injekt.compiler.ast.tree.expression.AstStatement
import com.ivianuu.injekt.compiler.ast.tree.type.AstStarProjection
import com.ivianuu.injekt.compiler.ast.tree.type.AstType
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeAbbreviation
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeArgument
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjection
import com.ivianuu.injekt.compiler.ast.tree.type.AstTypeProjectionImpl
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
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtReturnExpression
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

    fun generateModule(files: List<KtFile>): AstModuleFragment {
        return AstModuleFragment(module.name).apply {
            this.files += files.map { it.toAstFile() }
        }
    }

    private fun KtFile.toAstFile(): AstFile {
        return storage.files.getOrPut(this) {
            AstFile(
                packageFqName = packageFqName,
                name = name.asNameId()
            ).apply {
                // todo annotations += generateAnnotations()
                this@toAstFile.declarations.toAstDeclarations()
                    .forEach { addChild(it) }
            }
        }
    }

    private fun KtDeclaration.toAstDeclaration(): AstDeclaration =
        when (this) {
            is KtClassOrObject -> (bindingTrace.get(
                BindingContext.DECLARATION_TO_DESCRIPTOR,
                this
            ) as ClassDescriptor).toAstClass()
            is KtFunction -> (
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
            is KtTypeAlias -> (
                    bindingTrace.get(
                        BindingContext.DECLARATION_TO_DESCRIPTOR,
                        this
                    ) as TypeAliasDescriptor
                    ).toAstTypeAlias()
            else -> error("Unexpected declaration $this $javaClass $text")
        }

    private fun List<KtDeclaration>.toAstDeclarations() =
        map { it.toAstDeclaration() }

    private fun ClassDescriptor.toAstClass(): AstClass {
        val declaration = findPsi() as? KtClassOrObject
            ?: return stubGenerator.get(this) as AstClass
        return storage.classes.getOrPut(this) {
            AstClass(
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
                declaration.declarations.toAstDeclarations()
                    .forEach { addChild(it) }
            }
        }
    }

    private fun SimpleFunctionDescriptor.toAstSimpleFunction(): AstSimpleFunction {
        val declaration = findPsi() as? KtFunction
            ?: return stubGenerator.get(this) as AstSimpleFunction
        return storage.simpleFunctions.getOrPut(this) {
            AstSimpleFunction(
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
    }

    private fun ConstructorDescriptor.toAstConstructor(): AstConstructor {
        val declaration = findPsi() as? KtConstructor<*>
            ?: return stubGenerator.get(this) as AstConstructor
        return storage.constructors.getOrPut(this) {
            AstConstructor(
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
    }

    private fun PropertyDescriptor.toAstProperty(): AstProperty {
        val declaration = findPsi() as? KtProperty
            ?: return stubGenerator.get(this) as AstProperty
        return storage.properties.getOrPut(this) {
            AstProperty(
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
            }
        }
    }

    private fun TypeAliasDescriptor.toAstTypeAlias(): AstTypeAlias {
        val declaration = findPsi() as? KtTypeAlias
            ?: return stubGenerator.get(this) as AstTypeAlias
        return storage.typeAliases.getOrPut(this) {
            AstTypeAlias(
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
    }

    // todo
    private fun AnnotationDescriptor.toAstAnnotation() = AstCall(
        type.toAstType(),
        TODO()
    )

    private fun List<AnnotationDescriptor>.toAstAnnotations() = map { it.toAstAnnotation() }
    private fun Annotations.toAstAnnotations() = map { it.toAstAnnotation() }

    private fun TypeParameterDescriptor.toAstTypeParameter(): AstTypeParameter {
        return storage.typeParameters.getOrPut(this) {
            AstTypeParameter(
                name = name,
                isReified = isReified,
                variance = variance.toAstVariance()
            ).apply {
                annotations += this@toAstTypeParameter.annotations.toAstAnnotations()
                superTypes += this@toAstTypeParameter.upperBounds.map { it.toAstType() }
            }
        }
    }

    private fun List<TypeParameterDescriptor>.toAstTypeParameters() =
        map { it.toAstTypeParameter() }

    private fun ValueParameterDescriptor.toAstValueParameter(): AstValueParameter {
        val declaration = findPsi() as KtParameter
        val defaultValueExpression = declaration.defaultValue
        return storage.valueParameters.getOrPut(this) {
            AstValueParameter(
                name = name,
                type = type.toAstType(),
                isVarArg = isVararg,
                inlineHint = when {
                    isCrossinline -> AstValueParameter.InlineHint.CROSSINLINE
                    isNoinline -> AstValueParameter.InlineHint.NOINLINE
                    else -> null
                },
                defaultValue = defaultValueExpression?.toAstExpression()
            ).apply {
                annotations += this@toAstValueParameter.annotations.toAstAnnotations()
            }
        }
    }

    private fun List<ValueParameterDescriptor>.toAstValueParameters() =
        map { it.toAstValueParameter() }

    private fun KtElement.toAstStatement(): AstStatement = when (this) {
        is KtExpression -> toAstExpression()
        is KtDeclaration -> toAstDeclaration()
        else -> error("Unexpected element $this $javaClass $text")
    }

    private fun KtExpression.toAstExpression(): AstExpression =
        when (this) {
            is KtConstantExpression -> toAstConst()
            is KtBlockExpression -> toAstBlock()
            is KtCallExpression -> toAstCall()
            else -> error("Unexpected expression $this $javaClass $text")
        }

    private fun KtConstantExpression.toAstConst(): AstConst<*> {
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

    private fun KtExpression.toAstBlock(scopeOwner: AstDeclaration): AstBlock {
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

    private fun KtBlockExpression.toAstBlock(): AstBlock {
        return AstBlock(getExpressionTypeWithCoercionToUnitOrFail(this).toAstType()).apply {
            statements += this@toAstBlock.statements.map { it.toAstStatement() }
        }
    }

    private fun KtCallExpression.toAstCall(): AstCall {
        val resolvedCall = getResolvedCall(this)
            ?: error("Couldn't find call for $this $javaClass $text")
        return AstCall(
            type = resolvedCall.getReturnType().toAstType(),
            callee = when (val callee = resolvedCall.resultingDescriptor) {
                is SimpleFunctionDescriptor -> callee.toAstSimpleFunction()
                else -> error("Unexpected callee $callee")
            }
        ).apply {
            typeArguments += resolvedCall.typeArguments.values.map { it.toAstType() }
            //receiver = calleeExpression?.toAstExpression()

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
        return storage.types.getOrPut(this) {
            val approximatedType = upperIfFlexible()
            AstType().apply {
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
    }

    private fun SimpleType.toAstTypeAbbreviation(): AstTypeAbbreviation {
        return storage.typeAbbreviations.getOrPut(this) {
            val typeAliasDescriptor = constructor.declarationDescriptor.let {
                it as? TypeAliasDescriptor
                    ?: throw AssertionError("TypeAliasDescriptor expected: $it")
            }
            AstTypeAbbreviation(typeAliasDescriptor.toAstTypeAlias()).apply {
                hasQuestionMark = isMarkedNullable
                arguments += this@toAstTypeAbbreviation.arguments.map { it.toAstTypeArgument() }
                annotations += this@toAstTypeAbbreviation.annotations.toList().toAstAnnotations()
            }
        }
    }

    private fun TypeArgumentMarker.toAstTypeArgument(): AstTypeArgument {
        return storage.typeArguments.getOrPut(this) {
            when (this) {
                is StarProjectionImpl -> AstStarProjection
                is TypeProjection -> toAstTypeProjection()
                else -> error("Unexpected type $this")
            }
        }
    }

    private fun TypeProjection.toAstTypeProjection(): AstTypeProjection {
        return storage.typeProjections.getOrPut(this) {
            AstTypeProjectionImpl(
                variance = projectionKind.toAstVariance(),
                type = type.toAstType()
            )
        }
    }

    private fun getTypeInferredByFrontend(key: KtExpression): KotlinType? =
        bindingTrace.getType(key)

    private fun getTypeInferredByFrontendOrFail(key: KtExpression): KotlinType =
        getTypeInferredByFrontend(key) ?: error("No type for expression: ${key.text}")

    private fun getExpressionTypeWithCoercionToUnit(key: KtExpression): KotlinType? =
        if (key.isUsedAsExpression(bindingTrace.bindingContext))
            getTypeInferredByFrontend(key)
        else
            module.builtIns.unitType

    private fun getExpressionTypeWithCoercionToUnitOrFail(key: KtExpression): KotlinType =
        getExpressionTypeWithCoercionToUnit(key) ?: error("No type for expression: ${key.text}")

    private fun getResolvedCall(key: KtElement): ResolvedCall<out CallableDescriptor>? =
        key.getResolvedCall(bindingTrace.bindingContext)

    private fun scopeOwnerAsCallable(scopeOwner: DeclarationDescriptor) =
        (scopeOwner as? CallableDescriptor)
            ?: throw AssertionError("'return' in a non-callable: $scopeOwner")

    private fun getReturnExpressionTarget(
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
