package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstFunctionTarget
import com.ivianuu.ast.AstLoopTarget
import com.ivianuu.ast.builder.buildSpreadElement
import com.ivianuu.ast.declarations.AstAnonymousFunction
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
import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.declarations.builder.AstAnonymousFunctionBuilder
import com.ivianuu.ast.declarations.builder.AstFunctionBuilder
import com.ivianuu.ast.declarations.builder.AstNamedFunctionBuilder
import com.ivianuu.ast.declarations.builder.buildAnonymousFunction
import com.ivianuu.ast.declarations.builder.buildAnonymousInitializer
import com.ivianuu.ast.declarations.builder.buildAnonymousObject
import com.ivianuu.ast.declarations.builder.buildEnumEntry
import com.ivianuu.ast.declarations.builder.buildPropertyAccessor
import com.ivianuu.ast.declarations.builder.buildTypeAlias
import com.ivianuu.ast.declarations.builder.buildTypeParameter
import com.ivianuu.ast.declarations.builder.buildValueParameter
import com.ivianuu.ast.declarations.replaceExplicitReceiver
import com.ivianuu.ast.declarations.typeWith
import com.ivianuu.ast.expressions.AstBaseQualifiedAccess
import com.ivianuu.ast.expressions.AstCallableReference
import com.ivianuu.ast.expressions.AstClassReference
import com.ivianuu.ast.expressions.AstConst
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstDelegatedConstructorCallKind
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.expressions.AstTypeOperator
import com.ivianuu.ast.expressions.AstVararg
import com.ivianuu.ast.expressions.AstVariableAssignment
import com.ivianuu.ast.expressions.buildConstBoolean
import com.ivianuu.ast.expressions.buildConstNull
import com.ivianuu.ast.expressions.buildConstString
import com.ivianuu.ast.expressions.buildElseBranch
import com.ivianuu.ast.expressions.buildElvisExpression
import com.ivianuu.ast.expressions.buildTemporaryVariable
import com.ivianuu.ast.expressions.buildUnitExpression
import com.ivianuu.ast.expressions.builder.AstBaseQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.AstBlockBuilder
import com.ivianuu.ast.expressions.builder.AstBreakBuilder
import com.ivianuu.ast.expressions.builder.AstCallBuilder
import com.ivianuu.ast.expressions.builder.AstContinueBuilder
import com.ivianuu.ast.expressions.builder.AstDelegatedConstructorCallBuilder
import com.ivianuu.ast.expressions.builder.AstDoWhileLoopBuilder
import com.ivianuu.ast.expressions.builder.AstExpressionBuilder
import com.ivianuu.ast.expressions.builder.AstForLoopBuilder
import com.ivianuu.ast.expressions.builder.AstFunctionCallBuilder
import com.ivianuu.ast.expressions.builder.AstLoopBuilder
import com.ivianuu.ast.expressions.builder.AstLoopJumpBuilder
import com.ivianuu.ast.expressions.builder.AstQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.AstVariableAssignmentBuilder
import com.ivianuu.ast.expressions.builder.AstWhileLoopBuilder
import com.ivianuu.ast.expressions.builder.buildBlock
import com.ivianuu.ast.expressions.builder.buildCallableReference
import com.ivianuu.ast.expressions.builder.buildCatch
import com.ivianuu.ast.expressions.builder.buildClassReference
import com.ivianuu.ast.expressions.builder.buildDelegateInitializer
import com.ivianuu.ast.expressions.builder.buildDelegatedConstructorCall
import com.ivianuu.ast.expressions.builder.buildFunctionCall
import com.ivianuu.ast.expressions.builder.buildQualifiedAccess
import com.ivianuu.ast.expressions.builder.buildReturn
import com.ivianuu.ast.expressions.builder.buildSuperReference
import com.ivianuu.ast.expressions.builder.buildThisReference
import com.ivianuu.ast.expressions.builder.buildThrow
import com.ivianuu.ast.expressions.builder.buildTry
import com.ivianuu.ast.expressions.builder.buildTypeOperation
import com.ivianuu.ast.expressions.builder.buildVararg
import com.ivianuu.ast.expressions.builder.buildVariableAssignment
import com.ivianuu.ast.expressions.builder.buildWhen
import com.ivianuu.ast.expressions.builder.buildWhenBranch
import com.ivianuu.ast.symbols.impl.AstAnonymousInitializerSymbol
import com.ivianuu.ast.symbols.impl.AstConstructorSymbol
import com.ivianuu.ast.symbols.impl.AstFunctionSymbol
import com.ivianuu.ast.symbols.impl.AstValueParameterSymbol
import com.ivianuu.ast.symbols.impl.AstVariableSymbol
import com.ivianuu.ast.types.builder.buildStarProjection
import com.ivianuu.ast.types.builder.buildTypeProjectionWithVariance
import com.ivianuu.ast.types.makeNullable
import org.jetbrains.kotlin.backend.common.pop
import org.jetbrains.kotlin.backend.common.push
import org.jetbrains.kotlin.com.intellij.psi.PsiElement
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyAccessorDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeAliasDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.VariableDescriptor
import org.jetbrains.kotlin.descriptors.impl.AnonymousFunctionDescriptor
import org.jetbrains.kotlin.descriptors.impl.LocalVariableDescriptor
import org.jetbrains.kotlin.js.translate.callTranslator.getReturnType
import org.jetbrains.kotlin.lexer.KtTokens
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtAnnotatedExpression
import org.jetbrains.kotlin.psi.KtAnnotationEntry
import org.jetbrains.kotlin.psi.KtAnonymousInitializer
import org.jetbrains.kotlin.psi.KtArrayAccessExpression
import org.jetbrains.kotlin.psi.KtBinaryExpression
import org.jetbrains.kotlin.psi.KtBinaryExpressionWithTypeRHS
import org.jetbrains.kotlin.psi.KtBlockExpression
import org.jetbrains.kotlin.psi.KtBreakExpression
import org.jetbrains.kotlin.psi.KtCallElement
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtCallableReferenceExpression
import org.jetbrains.kotlin.psi.KtClassBody
import org.jetbrains.kotlin.psi.KtClassLiteralExpression
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtConstructorDelegationCall
import org.jetbrains.kotlin.psi.KtContinueExpression
import org.jetbrains.kotlin.psi.KtDelegatedSuperTypeEntry
import org.jetbrains.kotlin.psi.KtDestructuringDeclaration
import org.jetbrains.kotlin.psi.KtDoWhileExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtElement
import org.jetbrains.kotlin.psi.KtEnumEntry
import org.jetbrains.kotlin.psi.KtEscapeStringTemplateEntry
import org.jetbrains.kotlin.psi.KtExpression
import org.jetbrains.kotlin.psi.KtExpressionWithLabel
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtForExpression
import org.jetbrains.kotlin.psi.KtIfExpression
import org.jetbrains.kotlin.psi.KtIsExpression
import org.jetbrains.kotlin.psi.KtLabeledExpression
import org.jetbrains.kotlin.psi.KtLambdaExpression
import org.jetbrains.kotlin.psi.KtLiteralStringTemplateEntry
import org.jetbrains.kotlin.psi.KtNamedFunction
import org.jetbrains.kotlin.psi.KtObjectLiteralExpression
import org.jetbrains.kotlin.psi.KtParameter
import org.jetbrains.kotlin.psi.KtParenthesizedExpression
import org.jetbrains.kotlin.psi.KtPrefixExpression
import org.jetbrains.kotlin.psi.KtPrimaryConstructor
import org.jetbrains.kotlin.psi.KtProjectionKind
import org.jetbrains.kotlin.psi.KtProperty
import org.jetbrains.kotlin.psi.KtPropertyAccessor
import org.jetbrains.kotlin.psi.KtQualifiedExpression
import org.jetbrains.kotlin.psi.KtReferenceExpression
import org.jetbrains.kotlin.psi.KtReturnExpression
import org.jetbrains.kotlin.psi.KtSafeQualifiedExpression
import org.jetbrains.kotlin.psi.KtSecondaryConstructor
import org.jetbrains.kotlin.psi.KtStringTemplateEntryWithExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtSuperExpression
import org.jetbrains.kotlin.psi.KtSuperTypeCallEntry
import org.jetbrains.kotlin.psi.KtThisExpression
import org.jetbrains.kotlin.psi.KtThrowExpression
import org.jetbrains.kotlin.psi.KtTryExpression
import org.jetbrains.kotlin.psi.KtTypeAlias
import org.jetbrains.kotlin.psi.KtTypeParameter
import org.jetbrains.kotlin.psi.KtTypeProjection
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
import org.jetbrains.kotlin.psi2ir.deparenthesize
import org.jetbrains.kotlin.psi2ir.isValueArgumentReorderingRequired
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.FunctionImportedFromObject
import org.jetbrains.kotlin.resolve.ImportedFromObjectCallableDescriptor
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.calls.model.DefaultValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ExpressionValueArgument
import org.jetbrains.kotlin.resolve.calls.model.ResolvedCall
import org.jetbrains.kotlin.resolve.calls.model.ResolvedValueArgument
import org.jetbrains.kotlin.resolve.calls.model.VarargValueArgument
import org.jetbrains.kotlin.resolve.calls.util.FakeCallableDescriptorForObject
import org.jetbrains.kotlin.resolve.constants.evaluate.ConstantExpressionEvaluator
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.receivers.ClassValueReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExpressionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ExtensionReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ImplicitClassReceiver
import org.jetbrains.kotlin.resolve.scopes.receivers.ReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.SuperCallReceiverValue
import org.jetbrains.kotlin.resolve.scopes.receivers.ThisClassReceiver
import org.jetbrains.kotlin.types.Variance
import org.jetbrains.kotlin.utils.addToStdlib.cast

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
                        stubGenerator.getDeclaration(descriptor)
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
            superTypes += descriptor.typeConstructor.supertypes.map { it.toAstType() }
            delegateInitializers += classOrObject.superTypeListEntries
                .filterIsInstance<KtDelegatedSuperTypeEntry>()
                .map {
                    buildDelegateInitializer {
                        delegatedSuperType = it.typeReference!!.toAstType()
                        expression = it.delegateExpression!!.convert()
                    }
                }
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
                            delegatedConstructor = classOrObject.superTypeListEntries
                                .filterIsInstance<KtSuperTypeCallEntry>()
                                .singleOrNull()
                                ?.let { superType ->
                                    val resolvedCall = superType.getResolvedCall()!!
                                    buildCallFrom(
                                        resolvedCall = resolvedCall,
                                        create = { AstDelegatedConstructorCallBuilder(context) }
                                    ) {
                                        type = superType.typeReference!!.toAstType()
                                        callee = symbolTable.getConstructorSymbol(
                                            resolvedCall.resultingDescriptor as ConstructorDescriptor
                                        )
                                        kind = AstDelegatedConstructorCallKind.SUPER
                                    } as AstDelegatedConstructorCall
                                }
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
                symbol = symbolTable.getAnonymousFunctionSymbol(descriptor)
                extensionReceiverType = descriptor.extensionReceiverParameter?.type?.toAstType()
                type = (if (descriptor.isSuspend) context.builtIns.suspendFunction(descriptor.valueParameters.size)
                else context.builtIns.function(descriptor.valueParameters.size))
                    .owner.typeWith(
                        listOfNotNull(
                            extensionReceiverType,
                            *descriptor.valueParameters
                                .map { it.type.toAstType() }
                                .toTypedArray()
                        )
                    ) // todo extract
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
                isInline = descriptor.isInline
                typeParameters += function.typeParameters.map { it.convert() }
                dispatchReceiverType = descriptor.dispatchReceiverParameter?.type?.toAstType()
                extensionReceiverType = descriptor.extensionReceiverParameter?.type?.toAstType()
                overriddenFunctions += descriptor.overriddenDescriptors.map {
                    symbolTable.getNamedFunctionSymbol(it as SimpleFunctionDescriptor)
                }
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
            overriddenProperties += descriptor.overriddenDescriptors.map {
                symbolTable.getPropertySymbol(it as VariableDescriptor)
            }
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
            body = initializer.body?.toAstBlock() ?: buildBlock()
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
        val descriptor = parameter.descriptor<VariableDescriptor>()
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
            correspondingProperty = get(BindingContext.PRIMARY_CONSTRUCTOR_PARAMETER, parameter)
                ?.let { symbolTable.getPropertySymbol(it) }
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

    override fun visitEnumEntry(enumEntry: KtEnumEntry, data: Nothing?): AstElement {
        val descriptor = enumEntry.descriptor<ClassDescriptor>()
        return buildEnumEntry {
            symbol = symbolTable.getEnumEntrySymbol(descriptor)
            annotations += enumEntry.annotationEntries.map { it.convert() }
            declarations += enumEntry.declarations.map { it.convert() }
        }
    }

    override fun visitLambdaExpression(expression: KtLambdaExpression, data: Nothing?): AstElement {
        val label = if (labels.isNotEmpty()) labels.pop() else calleeNamesForLambda.lastOrNull()
        val target = AstFunctionTarget(label, true)
        val descriptor = expression.functionLiteral.descriptor<AnonymousFunctionDescriptor>()
        return buildAnonymousFunction {
            symbol = symbolTable.getAnonymousFunctionSymbol(descriptor)
            var destructuringBlock: AstBlock? = null
            valueParameters += expression.functionLiteral.valueParameters
                .map { valueParameter ->
                    val multiDeclaration = valueParameter.destructuringDeclaration
                    if (multiDeclaration != null) {
                        val containerParameter = buildValueParameter {
                            returnType = multiDeclaration.getTypeInferredByFrontendOrFail().toAstType()
                            symbol = AstValueParameterSymbol(Name.special("<destruct>"))
                        }
                        destructuringBlock = multiDeclaration.toAstDestructuringBlock(containerParameter)
                        containerParameter
                    } else {
                        valueParameter.convert()
                    }
                }
            if (valueParameters.isEmpty() && descriptor.valueParameters.isNotEmpty()) {
                val itParameterDescriptor = descriptor.valueParameters.single()
                valueParameters += buildValueParameter {
                    symbol = AstValueParameterSymbol(Name.identifier("it"))
                    returnType = itParameterDescriptor.type.toAstType()
                }
            }

            type = expression.getTypeInferredByFrontendOrFail().toAstType()
            this.label = label
            functionTargets.push(target)
            body = buildBlock {
                if (destructuringBlock != null) addStatementFlatten(destructuringBlock!!)
                expression.functionLiteral.bodyExpression!!.statements.forEach {
                    addStatementFlatten(it.convert())
                }
                if (statements.isEmpty()) statements += buildUnitExpression()
            }
            functionTargets.pop()
        }.also {
            target.bind(it)
        }
    }

    private fun AstBlockBuilder.addStatementFlatten(statement: AstStatement) {
        if (statement is AstBlock) statements += statement.statements
        else statements += statement
    }

    override fun visitObjectLiteralExpression(expression: KtObjectLiteralExpression, data: Nothing?): AstElement {
        val objectDeclaration = expression.objectDeclaration
        val descriptor = objectDeclaration.descriptor<ClassDescriptor>()
        return buildAnonymousObject {
            symbol = symbolTable.getAnonymousObjectSymbol(descriptor)
            type = expression.getExpressionTypeWithCoercionToUnitOrFail().toAstType()
            superTypes += descriptor.typeConstructor.supertypes.map { it.toAstType() }

            val primaryConstructorDescriptor = descriptor.unsubstitutedPrimaryConstructor!!
            declarations += buildConstructor {
                symbol = symbolTable.getConstructorSymbol(primaryConstructorDescriptor)
                isPrimary = true
                returnType = primaryConstructorDescriptor.returnType.toAstType()
                delegatedConstructor = objectDeclaration.superTypeListEntries
                    .filterIsInstance<KtSuperTypeCallEntry>()
                    .singleOrNull()
                    ?.let { superType ->
                        val resolvedCall = superType.getResolvedCall()!!
                        buildCallFrom(
                            resolvedCall = resolvedCall,
                            create = { AstDelegatedConstructorCallBuilder(context) }
                        ) {
                            type = superType.typeReference!!.toAstType()
                            callee = symbolTable.getConstructorSymbol(
                                resolvedCall.resultingDescriptor as ConstructorDescriptor
                            )
                            kind = AstDelegatedConstructorCallKind.SUPER
                        } as AstDelegatedConstructorCall
                    }
            }

            declarations += objectDeclaration.declarations.map { it.convert() }
        }
    }

    override fun visitDestructuringDeclaration(
        multiDeclaration: KtDestructuringDeclaration,
        data: Nothing?
    ): AstElement = multiDeclaration.toAstDestructuringBlock()

    override fun visitBlockExpression(expression: KtBlockExpression, data: Nothing?): AstElement =
        expression.toAstBlockBuilder().build()

    private fun KtBlockExpression.toAstBlockBuilder(): AstBlockBuilder {
        return AstBlockBuilder(this@Psi2AstBuilder.context).apply {
            type = getExpressionTypeWithCoercionToUnit()?.toAstType() ?: context.builtIns.unitType
            this@toAstBlockBuilder.statements.forEach {
                addStatementFlatten(it.convert())
            }
        }
    }

    private fun KtExpression?.toAstBlock(): AstBlock {
        return when {
            this is KtBlockExpression -> convert()
            this == null -> buildBlock()
            else -> {
                val astExpression = convert<AstExpression>()
                val type = getTypeInferredByFrontendOrFail().toAstType()
                buildBlock {
                    this.type = type
                    addStatementFlatten(
                        if (functionTargets.isNotEmpty()) {
                            buildReturn {
                                target = functionTargets.last()
                                result = astExpression
                            }
                        } else {
                            astExpression
                        }
                    )
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
        val resolvedCall = expression.getResolvedCall()

        val calleeDescriptor = resolvedCall?.resultingDescriptor
            ?: get(BindingContext.REFERENCE_TARGET, expression)

        val callee = when (calleeDescriptor) {
            is FakeCallableDescriptorForObject -> symbolTable.getClassSymbol(calleeDescriptor.classDescriptor)
            is SimpleFunctionDescriptor -> symbolTable.getNamedFunctionSymbol(calleeDescriptor)
            is ConstructorDescriptor -> symbolTable.getConstructorSymbol(calleeDescriptor)
            is ImportedFromObjectCallableDescriptor<*> -> symbolTable.getNamedFunctionSymbol(
                calleeDescriptor.callableFromObject.original as SimpleFunctionDescriptor
            )
            is LocalVariableDescriptor -> symbolTable.allSymbols[calleeDescriptor]!!
            is ValueParameterDescriptor -> symbolTable.getValueParameterSymbol(calleeDescriptor)
            is PropertyDescriptor -> symbolTable.getPropertySymbol(calleeDescriptor)
            else -> error("Unexpected callee $calleeDescriptor for ${expression.text} ${calleeDescriptor?.javaClass}")
        }
        return buildCallFrom(
            resolvedCall = resolvedCall!!,
            create = {
                if (expression is KtCallExpression) {
                    AstFunctionCallBuilder(context).apply {
                        type = expression.getExpressionTypeWithCoercionToUnitOrFail().toAstType()
                        this.callee = callee as AstFunctionSymbol<*>
                    }
                } else {
                    AstQualifiedAccessBuilder(context).apply {
                        type = expression.getExpressionTypeWithCoercionToUnitOrFail().toAstType()
                        this.callee = callee
                    }
                }
            }
        )
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

    override fun visitClassLiteralExpression(expression: KtClassLiteralExpression, data: Nothing?): AstElement {
        return buildClassReference {
            type = expression.getExpressionTypeWithCoercionToUnitOrFail().toAstType()
            val lhs = getOrFail(BindingContext.DOUBLE_COLON_LHS, expression.receiverExpression!!)
            classifier = lhs.type.toAstType().classifier
        }
    }

    override fun visitCallableReferenceExpression(expression: KtCallableReferenceExpression, data: Nothing?): AstElement {
        val resolvedCall = expression.callableReference.getResolvedCall()!!
        return buildCallableReference {
            type = expression.getExpressionTypeWithCoercionToUnitOrFail().toAstType()
            callee = when (val calleeDescriptor = resolvedCall.resultingDescriptor) {
                is FunctionImportedFromObject -> symbolTable.getNamedFunctionSymbol(
                    calleeDescriptor.callableFromObject.original as SimpleFunctionDescriptor)
                is ConstructorDescriptor -> symbolTable.getConstructorSymbol(calleeDescriptor)
                is SimpleFunctionDescriptor -> symbolTable.getNamedFunctionSymbol(calleeDescriptor)
                is PropertyDescriptor -> symbolTable.getPropertySymbol(calleeDescriptor)
                else -> error("Unexpected callee $calleeDescriptor ${calleeDescriptor.javaClass}")
            }
            fillReceiversAndTypeArgumentsFrom(resolvedCall)
        }
    }

    override fun visitArrayAccessExpression(
        expression: KtArrayAccessExpression,
        data: Nothing?
    ): AstElement {
        val resolvedCall = expression.getResolvedCall()!!
        return buildCallFrom(
            resolvedCall = resolvedCall,
            create = { AstFunctionCallBuilder(context) }
        ) {
            type = resolvedCall.getReturnType().toAstType()
            callee = symbolTable.getNamedFunctionSymbol(resolvedCall.resultingDescriptor as SimpleFunctionDescriptor)
        }
    }

    override fun visitUnaryExpression(expression: KtUnaryExpression, data: Nothing?): AstElement {
        val ktOperator = expression.operationReference.getReferencedNameElementType()
        val resolvedCall = expression.getResolvedCall()
        val argument = expression.baseExpression!!
        val prefix = expression is KtPrefixExpression
        return when (ktOperator) {
            KtTokens.EXCLEXCL -> buildFunctionCall {
                callee = builtIns.checkNotNull
                valueArguments += argument.convert<AstExpression>()
            }
            KtTokens.PLUSPLUS, KtTokens.MINUSMINUS -> {
                buildBlock {
                    val variableResolvedCall = argument.getResolvedCall()!!

                    val variableSymbol = when (val variableDescriptor = variableResolvedCall.resultingDescriptor) {
                        is SimpleFunctionDescriptor -> symbolTable.getNamedFunctionSymbol(variableDescriptor)
                        is PropertyDescriptor -> symbolTable.getPropertySymbol(variableDescriptor)
                        is LocalVariableDescriptor -> symbolTable.getPropertySymbol(variableDescriptor)
                        else -> error("Unexpected callee $variableDescriptor")
                    }

                    val getVariable = if (variableSymbol is AstFunctionSymbol<*>) {
                        buildCallFrom(
                            resolvedCall = variableResolvedCall,
                            create = { AstFunctionCallBuilder(context) }
                        ) {
                            this.type = argument.getTypeInferredByFrontendOrFail().toAstType()
                            this.callee = variableSymbol
                        }
                    } else {
                        buildQualifiedAccess {
                            this.callee = variableSymbol
                            fillReceiversAndTypeArgumentsFrom(variableResolvedCall)
                        }
                    }

                    val incrementVariable = buildVariableAssignment {
                        callee = variableSymbol.cast()
                        fillReceiversAndTypeArgumentsFrom(variableResolvedCall)
                        value = buildCallFrom(
                            resolvedCall = resolvedCall!!,
                            create = { AstFunctionCallBuilder(context) }
                        ) {
                            type = expression.getExpressionTypeWithCoercionToUnitOrFail().toAstType()
                            this.callee = symbolTable.getNamedFunctionSymbol(
                                resolvedCall.resultingDescriptor as SimpleFunctionDescriptor)
                        }
                    }

                    if (prefix) {
                        statements += incrementVariable
                        statements += getVariable
                    } else {
                        val tmp = buildTemporaryVariable(getVariable).also { statements += it }
                        statements += incrementVariable
                        statements += buildQualifiedAccess { callee = tmp.symbol }
                    }
                }
            }
            else -> error("Unexpected token $ktOperator ${expression.text}")
        }
    }

    override fun visitBinaryExpression(expression: KtBinaryExpression, data: Nothing?): AstElement {
        val ktOperator = expression.operationReference.getReferencedNameElementType()

        /*if (ktOperator == KtTokens.IDENTIFIER) {
            calleeNamesForLambda.push(expression.operationReference.getReferencedName())
        }

        val left = expression.left!!.convert<AstExpression>()
        val right = expression.right!!.convert<AstExpression>()

        if (ktOperator == KtTokens.IDENTIFIER) {
            calleeNamesForLambda.pop()
        }*/

        return when (ktOperator) {
            KtTokens.EQ, KtTokens.PLUSEQ, KtTokens.MINUSEQ, KtTokens.MULTEQ,
            KtTokens.DIVEQ, KtTokens.PERCEQ -> {
                val variableResolvedCall = expression.left!!.getResolvedCall()!!
                val assignmentDescriptor = variableResolvedCall.resultingDescriptor
                val assignmentCallee = when (assignmentDescriptor) {
                    is SimpleFunctionDescriptor -> symbolTable.getNamedFunctionSymbol(assignmentDescriptor)
                    is PropertyDescriptor -> symbolTable.getPropertySymbol(assignmentDescriptor)
                    is LocalVariableDescriptor -> symbolTable.getPropertySymbol(assignmentDescriptor)
                    else -> error("Unexpected callee $assignmentDescriptor")
                }
                val opResolvedCall = expression.getResolvedCall()
                val opDescriptor = opResolvedCall?.resultingDescriptor as? SimpleFunctionDescriptor
                val opCallee = opDescriptor?.let { symbolTable.getNamedFunctionSymbol(it) }
                val opCall = opCallee?.let {
                    buildCallFrom(
                        resolvedCall = opResolvedCall,
                        create = { AstFunctionCallBuilder(context) }
                    ) {
                        type = builtIns.unitType
                        callee = opCallee
                    }
                } ?: expression.right!!.convert()
                when (assignmentCallee) {
                    is AstVariableSymbol<*> -> buildVariableAssignment {
                        this.callee = assignmentCallee
                        fillReceiversAndTypeArgumentsFrom(variableResolvedCall)
                        value = opCall
                    }
                    is AstFunctionSymbol<*> -> buildCallFrom(
                        resolvedCall = variableResolvedCall,
                        create = { AstFunctionCallBuilder(context) }
                    ) {
                        type = builtIns.unitType
                        this.callee = assignmentCallee
                    }
                    else -> error("Unexpected callee $assignmentDescriptor")
                }
            }
            KtTokens.LT, KtTokens.LTEQ, KtTokens.GT, KtTokens.GTEQ, KtTokens.EQEQ,
                KtTokens.EXCLEQ, KtTokens.EXCLEQEQEQ, KtTokens.EQEQEQ, KtTokens.ANDAND, KtTokens.OROR -> buildFunctionCall {
                callee = when(ktOperator) {
                    KtTokens.LT -> builtIns.lessThanSymbol
                    KtTokens.LTEQ -> builtIns.lessThanEqualSymbol
                    KtTokens.GT -> builtIns.greaterThanSymbol
                    KtTokens.GTEQ -> builtIns.greaterThanEqualSymbol
                    KtTokens.EQEQ -> builtIns.structuralEqualSymbol
                    KtTokens.EXCLEQ -> builtIns.structuralNotEqualSymbol
                    KtTokens.EQEQEQ -> builtIns.identityEqualSymbol
                    KtTokens.EXCLEQEQEQ -> builtIns.identityNotEqualSymbol
                    KtTokens.ANDAND -> builtIns.lazyAndSymbol
                    KtTokens.OROR -> builtIns.lazyOrSymbol
                    else -> error("Unexpected token $ktOperator")
                }
                valueArguments += expression.left!!.convert<AstExpression>()
                valueArguments += expression.right!!.convert<AstExpression>()
            }
            KtTokens.PLUS, KtTokens.MINUS, KtTokens.MUL, KtTokens.DIV, KtTokens.PERC, KtTokens.RANGE -> {
                val resolvedCall = expression.getResolvedCall()!!
                buildCallFrom(
                    resolvedCall = resolvedCall,
                    create = { AstFunctionCallBuilder(context) }
                ) {
                    type = expression.getTypeInferredByFrontendOrFail().toAstType()
                    callee = symbolTable.getNamedFunctionSymbol(resolvedCall.resultingDescriptor as SimpleFunctionDescriptor)
                }
            }
            KtTokens.IN_KEYWORD, KtTokens.NOT_IN -> {
                val containsCall = expression.getResolvedCall()!!
                val astContainsCall = buildCallFrom(
                    resolvedCall = containsCall,
                    create = { AstFunctionCallBuilder(context) }
                ) {
                    type = context.builtIns.booleanType
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

            KtTokens.ELVIS -> buildElvisExpression(
                type = expression.getTypeInferredByFrontendOrFail().toAstType(),
                left = expression.left!!.convert(),
                right = expression.right!!.convert()
            )
            else -> {
                val resolvedCall = expression.getResolvedCall()!!
                buildCallFrom(
                    resolvedCall = resolvedCall,
                    create = { AstFunctionCallBuilder(context) }
                ) {
                    type = expression.getTypeInferredByFrontendOrFail().toAstType()
                    callee = symbolTable.getNamedFunctionSymbol(
                        resolvedCall.resultingDescriptor as SimpleFunctionDescriptor)
                }
            }
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
            operator = if (expression.isNegated) AstTypeOperator.IS_NOT else AstTypeOperator.IS
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
                    result = ktLastIf.then?.convert() ?: buildBlock()
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

    override fun visitWhenExpression(expression: KtWhenExpression, data: Nothing?): AstElement {
        val ktSubjectExpression = expression.subjectExpression
        val subjectExpression = when (ktSubjectExpression) {
            is KtVariableDeclaration -> ktSubjectExpression.initializer
            else -> ktSubjectExpression
        }?.convert<AstExpression>()
        val subjectVariable = when {
            ktSubjectExpression is KtVariableDeclaration -> buildTemporaryVariable(
                value = subjectExpression!!,
                type = ktSubjectExpression.typeReference!!.toAstType(),
                name = ktSubjectExpression.nameAsSafeName,
                symbol = symbolTable.getPropertySymbol(ktSubjectExpression.descriptor())
            )
            subjectExpression != null -> buildTemporaryVariable(value = subjectExpression)
            else -> null
        }

        val whenExpression = buildWhen {
            type = expression.getTypeInferredByFrontendOrFail().toAstType()

            branches += expression.entries
                .map { entry ->
                    val branchBody = entry.expression!!.convert<AstExpression>()
                    if (!entry.isElse) {
                        if (subjectVariable != null) {
                            buildWhenBranch {
                                condition = entry.conditions.toAstWhenCondition(
                                    buildQualifiedAccess { callee = subjectVariable.symbol }
                                )
                                result = branchBody
                            }
                        } else {
                            val ktCondition = entry.conditions.first() as? KtWhenConditionWithExpression
                            buildWhenBranch {
                                condition = ktCondition!!.expression!!.convert()
                                result = branchBody
                            }
                        }
                    } else {
                        buildElseBranch(branchBody)
                    }
                }
        }

        return if (subjectVariable == null) {
            whenExpression
        } else {
            buildBlock {
                statements += subjectVariable
                statements += whenExpression
            }
        }
    }

    private fun KtWhenCondition.toAstWhenCondition(subject: AstExpression): AstExpression {
        return when (this) {
            is KtWhenConditionWithExpression -> {
                buildFunctionCall {
                    callee = builtIns.structuralEqualSymbol
                    valueArguments += subject
                    valueArguments += expression!!.convert<AstExpression>()
                }
            }
            is KtWhenConditionInRange -> {
                val operationResolvedCall = operationReference.getResolvedCall()!!
                buildCallFrom(
                    resolvedCall = operationResolvedCall,
                    create = { AstFunctionCallBuilder(this@Psi2AstBuilder.context) }
                ) {
                    type = builtIns.booleanType
                    callee = symbolTable.getNamedFunctionSymbol(
                        operationResolvedCall.resultingDescriptor as SimpleFunctionDescriptor
                    )
                    valueArguments[0] = subject
                }
            }
            is KtWhenConditionIsPattern -> buildTypeOperation {
                operator = if (isNegated) AstTypeOperator.IS_NOT else AstTypeOperator.IS
                typeOperand = typeReference!!.toAstType()
                argument = subject
            }
            else -> error("Unexpected when condition $this")
        }
    }

    private fun Array<KtWhenCondition>.toAstWhenCondition(subject: AstExpression): AstExpression {
        var astCondition: AstExpression? = null
        for (condition in this) {
            val astConditionElement = condition.toAstWhenCondition(subject)
            astCondition = when (astCondition) {
                null -> astConditionElement
                else -> buildFunctionCall {
                    callee = builtIns.lazyOrSymbol
                    valueArguments += astCondition
                    valueArguments += astConditionElement
                }
            }
        }
        return astCondition!!
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

    override fun visitForExpression(expression: KtForExpression, data: Nothing?): AstElement {
        val ktLoopParameter = expression.loopParameter
        val ktMultiDeclaration = expression.destructuringDeclaration
        if (ktLoopParameter == null && ktMultiDeclaration == null) {
            error("Either loopParameter or destructuringParameter should be present:\n${expression.text}")
        }
        var destructuringBlock: AstBlock? = null
        return AstForLoopBuilder(context).apply {
            loopParameter = if (ktMultiDeclaration != null) {
                val containerParameter = buildValueParameter {
                    symbol = AstValueParameterSymbol(Name.special("<destruct>"))
                }
                destructuringBlock = ktMultiDeclaration.toAstDestructuringBlock(containerParameter)
                containerParameter
            } else {
                ktLoopParameter!!.convert()
            }
            loopRange = expression.loopRange!!.convert()
        }.configure {
            buildBlock {
                if (destructuringBlock != null) addStatementFlatten(destructuringBlock!!)
                addStatementFlatten(
                    expression.body?.convert()
                        ?: buildBlock()
                )
            }
        }
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
                        body = catchClause.catchBody?.convert() ?: buildBlock()
                    }
                }
            finallyBody = expression.finallyBlock?.finalExpression?.convert()
        }
    }

    override fun visitThisExpression(expression: KtThisExpression, data: Nothing?): AstElement {
        return buildThisReference {
            type = expression.getTypeInferredByFrontendOrFail().toAstType()
            labelName = expression.getLabelName()
        }
    }

    override fun visitSuperExpression(expression: KtSuperExpression, data: Nothing?): AstElement =
        buildSuperReference { superType = expression.superTypeQualifier?.toAstType() }

    override fun visitQualifiedExpression(
        expression: KtQualifiedExpression,
        data: Nothing?
    ): AstElement {
        val selector = expression.selectorExpression!!.convert<AstExpression>()

        if (selector is AstBaseQualifiedAccess) {
            val receiver = expression.receiverExpression.convert<AstExpression>()

            if (expression is KtSafeQualifiedExpression) {
                return buildBlock {
                    val tmp = buildTemporaryVariable(receiver).also { statements += it }
                    statements += buildWhen {
                        this.type = selector.type.makeNullable()
                        branches += buildWhenBranch {
                            condition = buildFunctionCall {
                                callee = context.builtIns.structuralNotEqualSymbol
                                valueArguments += buildQualifiedAccess { callee = tmp.symbol }
                                valueArguments += buildConstNull()
                            }
                            result = selector
                                .also {
                                    it.replaceExplicitReceiver(
                                        buildQualifiedAccess { callee = tmp.symbol }
                                    )
                                }
                        }
                        branches += buildElseBranch(buildConstNull())
                    }
                }
            }

            selector.replaceExplicitReceiver(receiver)
        }

        return selector
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
        }
        return result
    }

    override fun visitAnnotationEntry(annotationEntry: KtAnnotationEntry, data: Nothing?): AstElement {
        val descriptor = getOrFail(BindingContext.ANNOTATION, annotationEntry)
        return context.constantValueGenerator.generateAnnotationConstructorCall(descriptor)!!
    }

    override fun visitTypeReference(typeReference: KtTypeReference, data: Nothing?): AstElement =
        typeReference.toAstType()

    override fun visitTypeProjection(typeProjection: KtTypeProjection, data: Nothing?): AstElement {
        return when (typeProjection.projectionKind) {
            KtProjectionKind.STAR -> buildStarProjection()
            else -> buildTypeProjectionWithVariance {
                type = typeProjection.typeReference!!.convert()
                variance = when (typeProjection.projectionKind) {
                    KtProjectionKind.IN -> Variance.IN_VARIANCE
                    KtProjectionKind.OUT -> Variance.OUT_VARIANCE
                    KtProjectionKind.NONE -> Variance.INVARIANT
                    else -> error("Unexpected projection kind ${typeProjection.text}")
                }
            }
        }
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
                labelName = classDescriptor.name
                    .takeUnless { it.isSpecial }
                    ?.asString()
            }
            is ThisClassReceiver -> buildThisReference {
                this.type = this@toAstExpression.type.toAstType()
                labelName = classDescriptor.name
                    .takeUnless { it.isSpecial }
                    ?.asString()
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
            else -> error("Unexpected receiver value $this $javaClass")
        }
    }

    private fun AstBaseQualifiedAccessBuilder.fillReceiversAndTypeArgumentsFrom(resolvedCall: ResolvedCall<*>) {
        dispatchReceiver = resolvedCall.dispatchReceiver?.toAstExpression()
        extensionReceiver = resolvedCall.extensionReceiver?.toAstExpression()
        typeArguments += resolvedCall.typeArguments.map { it.value.toAstType() }
    }

    private fun <T : AstExpressionBuilder> buildCallFrom(
        resolvedCall: ResolvedCall<*>,
        create: () -> T,
        init: T.() -> Unit = {}
    ): AstExpression {
        val accessBuilder = create().apply(init)
        if (accessBuilder is AstBaseQualifiedAccessBuilder) {
            accessBuilder.fillReceiversAndTypeArgumentsFrom(resolvedCall)
        }
        return if (resolvedCall.isValueArgumentReorderingRequired()) {
            val valueArgumentsInEvaluationOrder = resolvedCall.valueArguments.values
            val valueParameters = resolvedCall.resultingDescriptor.valueParameters

            val block = AstBlockBuilder(context).apply { type = accessBuilder.type }

            val valueArgumentsToValueParameters = mutableMapOf<ResolvedValueArgument, ValueParameterDescriptor>()
            for ((index, valueArgument) in resolvedCall.valueArgumentsByIndex!!.withIndex()) {
                val valueParameter = valueParameters[index]
                valueArgumentsToValueParameters[valueArgument] = valueParameter
            }

            val astArgumentValues = mutableMapOf<ValueParameterDescriptor, AstExpression>()

            for (valueArgument in valueArgumentsInEvaluationOrder) {
                val valueParameter = valueArgumentsToValueParameters[valueArgument]!!
                val astArgument = valueArgument.toAstExpression(valueParameter) ?: continue
                val finalAstArgument = when {
                    astArgument.hasNoSideEffects() -> astArgument
                    else -> buildTemporaryVariable(astArgument)
                        .also { block.statements += it }
                        .let { buildQualifiedAccess { callee = it.symbol } }
                }
                astArgumentValues[valueParameter] = finalAstArgument
            }

            resolvedCall.valueArgumentsByIndex!!
                .forEachIndexed { index, _ ->
                    val valueParameter = valueParameters[index]
                    when (accessBuilder) {
                        is AstCallBuilder -> {
                            accessBuilder.valueArguments.add(index, astArgumentValues[valueParameter])
                        }
                        is AstVariableAssignmentBuilder -> {
                            accessBuilder.value = astArgumentValues[valueParameter]!!
                        }
                    }
                }

            block.statements.add(accessBuilder.build())
            block.build()
        } else {
            resolvedCall.valueArguments
                .map { it.value.toAstExpression(it.key) }
                .forEachIndexed { index, valueArgument ->
                    when (accessBuilder) {
                        is AstCallBuilder -> {
                            accessBuilder.valueArguments.add(index, valueArgument)
                        }
                        is AstVariableAssignmentBuilder -> {
                            accessBuilder.value = valueArgument!!
                        }
                    }
                }
            accessBuilder.build()
        }
    }

    private fun AstExpression.hasNoSideEffects() =
        this is AstAnonymousFunction ||
                (this is AstCallableReference && dispatchReceiver == null && extensionReceiver == null) ||
                this is AstClassReference ||
                this is AstConst<*> ||
                (this is AstQualifiedAccess && callee is AstVariable<*>)

    private fun ResolvedValueArgument.toAstExpression(
        valueParameter: ValueParameterDescriptor
    ): AstExpression? =
        when (this) {
            is DefaultValueArgument -> null
            is ExpressionValueArgument -> valueArgument!!.getArgumentExpression()!!.convert()
            is VarargValueArgument -> {
                if (arguments.isEmpty()) null
                else buildVararg {
                    type = valueParameter.type.toAstType()
                    elements += arguments
                        .map { argument ->
                            val argumentExpression = argument.getArgumentExpression()!!
                                .convert<AstExpression>()
                            if (argument.getSpreadElement() != null) {
                                buildSpreadElement { expression = argumentExpression }
                            } else {
                                argumentExpression
                            }
                        }
                }
            }
            else -> error("Unexpected valueArgument ${this::class.java.simpleName}")
        }

    private fun KtDestructuringDeclaration.toAstDestructuringBlock(
        container: AstVariable<*> = buildTemporaryVariable(initializer!!.convert<AstExpression>())
    ): AstBlock = buildBlock {
        statements += container
        statements += entries
            .mapNotNull { ktEntry ->
                val componentVariable = getOrFail(BindingContext.VARIABLE, ktEntry)
                // componentN for '_' SHOULD NOT be evaluated
                if (componentVariable.name.isSpecial) return@mapNotNull null
                val componentResolvedCall =
                    getOrFail(BindingContext.COMPONENT_RESOLVED_CALL, ktEntry)
                val componentCall = buildCallFrom(
                    resolvedCall = componentResolvedCall,
                    create = { AstFunctionCallBuilder(context) }
                ) {
                    type = componentVariable.type.toAstType()
                    callee = symbolTable.getNamedFunctionSymbol(
                        componentResolvedCall.resultingDescriptor as SimpleFunctionDescriptor)

                }.let { it as AstFunctionCall }
                    .also {
                        it.replaceExplicitReceiver(
                            buildQualifiedAccess { callee = container.symbol }
                        )
                    }
                buildTemporaryVariable(
                    value = componentCall,
                    symbol = symbolTable.getPropertySymbol(componentVariable)
                )
            }
    }

}
