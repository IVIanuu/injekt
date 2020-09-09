package com.ivianuu.ast.deepcopy

import com.ivianuu.ast.AstElement
import com.ivianuu.ast.AstAnnotationContainer
import com.ivianuu.ast.types.AstType
import com.ivianuu.ast.AstSymbolOwner
import com.ivianuu.ast.AstVarargElement
import com.ivianuu.ast.AstTargetElement
import com.ivianuu.ast.expressions.AstStatement
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.declarations.AstDeclaration
import com.ivianuu.ast.declarations.AstDeclarationContainer
import com.ivianuu.ast.declarations.AstNamedDeclaration
import com.ivianuu.ast.declarations.AstMemberDeclaration
import com.ivianuu.ast.declarations.AstAnonymousInitializer
import com.ivianuu.ast.declarations.AstCallableDeclaration
import com.ivianuu.ast.declarations.AstTypeParameter
import com.ivianuu.ast.declarations.AstTypeParametersOwner
import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstClassLikeDeclaration
import com.ivianuu.ast.declarations.AstClass
import com.ivianuu.ast.declarations.AstRegularClass
import com.ivianuu.ast.declarations.AstTypeAlias
import com.ivianuu.ast.declarations.AstEnumEntry
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.declarations.AstNamedFunction
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstModuleFragment
import com.ivianuu.ast.declarations.AstPackageFragment
import com.ivianuu.ast.declarations.AstFile
import com.ivianuu.ast.declarations.AstAnonymousFunction
import com.ivianuu.ast.declarations.AstAnonymousObject
import com.ivianuu.ast.expressions.AstJump
import com.ivianuu.ast.expressions.AstLoop
import com.ivianuu.ast.expressions.AstDoWhileLoop
import com.ivianuu.ast.expressions.AstWhileLoop
import com.ivianuu.ast.expressions.AstForLoop
import com.ivianuu.ast.expressions.AstBlock
import com.ivianuu.ast.expressions.AstLoopJump
import com.ivianuu.ast.expressions.AstBreak
import com.ivianuu.ast.expressions.AstContinue
import com.ivianuu.ast.expressions.AstCatch
import com.ivianuu.ast.expressions.AstTry
import com.ivianuu.ast.expressions.AstConst
import com.ivianuu.ast.types.AstTypeProjection
import com.ivianuu.ast.types.AstStarProjection
import com.ivianuu.ast.types.AstTypeProjectionWithVariance
import com.ivianuu.ast.expressions.AstCalleeReference
import com.ivianuu.ast.expressions.AstCall
import com.ivianuu.ast.expressions.AstWhen
import com.ivianuu.ast.expressions.AstWhenBranch
import com.ivianuu.ast.expressions.AstClassReference
import com.ivianuu.ast.expressions.AstBaseQualifiedAccess
import com.ivianuu.ast.expressions.AstQualifiedAccess
import com.ivianuu.ast.expressions.AstFunctionCall
import com.ivianuu.ast.expressions.AstDelegatedConstructorCall
import com.ivianuu.ast.expressions.AstDelegateInitializer
import com.ivianuu.ast.expressions.AstCallableReference
import com.ivianuu.ast.expressions.AstVararg
import com.ivianuu.ast.AstSpreadElement
import com.ivianuu.ast.expressions.AstReturn
import com.ivianuu.ast.expressions.AstThrow
import com.ivianuu.ast.expressions.AstVariableAssignment
import com.ivianuu.ast.expressions.AstSuperReference
import com.ivianuu.ast.expressions.AstThisReference
import com.ivianuu.ast.expressions.AstPropertyBackingFieldReference
import com.ivianuu.ast.expressions.AstTypeOperation
import com.ivianuu.ast.types.builder.AstTypeBuilder
import com.ivianuu.ast.declarations.builder.AstAnonymousInitializerBuilder
import com.ivianuu.ast.declarations.builder.AstTypeParameterBuilder
import com.ivianuu.ast.declarations.builder.AstValueParameterBuilder
import com.ivianuu.ast.declarations.builder.AstPropertyBuilder
import com.ivianuu.ast.declarations.builder.AstRegularClassBuilder
import com.ivianuu.ast.declarations.builder.AstTypeAliasBuilder
import com.ivianuu.ast.declarations.builder.AstEnumEntryBuilder
import com.ivianuu.ast.declarations.builder.AstNamedFunctionBuilder
import com.ivianuu.ast.declarations.builder.AstPropertyAccessorBuilder
import com.ivianuu.ast.declarations.builder.AstConstructorBuilder
import com.ivianuu.ast.declarations.builder.AstModuleFragmentBuilder
import com.ivianuu.ast.declarations.builder.AstFileBuilder
import com.ivianuu.ast.declarations.builder.AstAnonymousFunctionBuilder
import com.ivianuu.ast.declarations.builder.AstAnonymousObjectBuilder
import com.ivianuu.ast.expressions.builder.AstDoWhileLoopBuilder
import com.ivianuu.ast.expressions.builder.AstWhileLoopBuilder
import com.ivianuu.ast.expressions.builder.AstForLoopBuilder
import com.ivianuu.ast.expressions.builder.AstBlockBuilder
import com.ivianuu.ast.expressions.builder.AstBreakBuilder
import com.ivianuu.ast.expressions.builder.AstContinueBuilder
import com.ivianuu.ast.expressions.builder.AstCatchBuilder
import com.ivianuu.ast.expressions.builder.AstTryBuilder
import com.ivianuu.ast.types.builder.AstStarProjectionBuilder
import com.ivianuu.ast.types.builder.AstTypeProjectionWithVarianceBuilder
import com.ivianuu.ast.expressions.builder.AstWhenBuilder
import com.ivianuu.ast.expressions.builder.AstWhenBranchBuilder
import com.ivianuu.ast.expressions.builder.AstClassReferenceBuilder
import com.ivianuu.ast.expressions.builder.AstQualifiedAccessBuilder
import com.ivianuu.ast.expressions.builder.AstFunctionCallBuilder
import com.ivianuu.ast.expressions.builder.AstDelegatedConstructorCallBuilder
import com.ivianuu.ast.expressions.builder.AstDelegateInitializerBuilder
import com.ivianuu.ast.expressions.builder.AstCallableReferenceBuilder
import com.ivianuu.ast.expressions.builder.AstVarargBuilder
import com.ivianuu.ast.builder.AstSpreadElementBuilder
import com.ivianuu.ast.expressions.builder.AstReturnBuilder
import com.ivianuu.ast.expressions.builder.AstThrowBuilder
import com.ivianuu.ast.expressions.builder.AstVariableAssignmentBuilder
import com.ivianuu.ast.expressions.builder.AstSuperReferenceBuilder
import com.ivianuu.ast.expressions.builder.AstThisReferenceBuilder
import com.ivianuu.ast.expressions.builder.AstPropertyBackingFieldReferenceBuilder
import com.ivianuu.ast.expressions.builder.AstTypeOperationBuilder
import com.ivianuu.ast.visitors.compose
import com.ivianuu.ast.visitors.CompositeTransformResult
import com.ivianuu.ast.expressions.buildConst

/*
 * This file was generated automatically
 * DO NOT MODIFY IT MANUALLY
 */

class DeepCopyTransformerImpl(symbolRemapper: SymbolRemapper) : DeepCopyTransformer(symbolRemapper) {

    override fun transformType(type: AstType): CompositeTransformResult<AstType> {
        val copyBuilder = AstTypeBuilder(type.context)
        copyBuilder.annotations.addAll(type.annotations.map { it.transform() })
        copyBuilder.classifier = type.classifier.let { symbolRemapper.getSymbol(it) }
        copyBuilder.arguments.addAll(type.arguments.map { it.transform() })
        copyBuilder.isMarkedNullable = type.isMarkedNullable
        return copyBuilder.build().compose()
    }

    override fun transformAnonymousInitializer(anonymousInitializer: AstAnonymousInitializer): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstAnonymousInitializerBuilder(anonymousInitializer.context)
        copyBuilder.annotations.addAll(anonymousInitializer.annotations.map { it.transform() })
        copyBuilder.origin = anonymousInitializer.origin
        copyBuilder.attributes = anonymousInitializer.attributes
        copyBuilder.body = anonymousInitializer.body.transform()
        copyBuilder.symbol = anonymousInitializer.symbol.let { symbolRemapper.getSymbol(it) }
        return copyBuilder.build().compose()
    }

    override fun transformTypeParameter(typeParameter: AstTypeParameter): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstTypeParameterBuilder(typeParameter.context)
        copyBuilder.annotations.addAll(typeParameter.annotations.map { it.transform() })
        copyBuilder.origin = typeParameter.origin
        copyBuilder.attributes = typeParameter.attributes
        copyBuilder.name = typeParameter.name
        copyBuilder.symbol = typeParameter.symbol.let { symbolRemapper.getSymbol(it) }
        copyBuilder.variance = typeParameter.variance
        copyBuilder.isReified = typeParameter.isReified
        copyBuilder.bounds.addAll(typeParameter.bounds.map { it.transform() })
        return copyBuilder.build().compose()
    }

    override fun transformValueParameter(valueParameter: AstValueParameter): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstValueParameterBuilder(valueParameter.context)
        copyBuilder.annotations.addAll(valueParameter.annotations.map { it.transform() })
        copyBuilder.origin = valueParameter.origin
        copyBuilder.attributes = valueParameter.attributes
        copyBuilder.returnType = valueParameter.returnType.transform()
        copyBuilder.name = valueParameter.name
        copyBuilder.symbol = valueParameter.symbol.let { symbolRemapper.getSymbol(it) }
        copyBuilder.defaultValue = valueParameter.defaultValue?.transform()
        copyBuilder.isCrossinline = valueParameter.isCrossinline
        copyBuilder.isNoinline = valueParameter.isNoinline
        copyBuilder.isVararg = valueParameter.isVararg
        copyBuilder.correspondingProperty = valueParameter.correspondingProperty?.let { symbolRemapper.getSymbol(it) }
        copyBuilder.varargElementType = valueParameter.varargElementType?.transform()
        return copyBuilder.build().compose()
    }

    override fun transformProperty(property: AstProperty): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstPropertyBuilder(property.context)
        copyBuilder.annotations.addAll(property.annotations.map { it.transform() })
        copyBuilder.origin = property.origin
        copyBuilder.attributes = property.attributes
        copyBuilder.dispatchReceiverType = property.dispatchReceiverType?.transform()
        copyBuilder.extensionReceiverType = property.extensionReceiverType?.transform()
        copyBuilder.returnType = property.returnType.transform()
        copyBuilder.name = property.name
        copyBuilder.initializer = property.initializer?.transform()
        copyBuilder.delegate = property.delegate?.transform()
        copyBuilder.isVar = property.isVar
        copyBuilder.getter = property.getter?.transform()
        copyBuilder.setter = property.setter?.transform()
        copyBuilder.typeParameters.addAll(property.typeParameters.map { it.transform() })
        copyBuilder.visibility = property.visibility
        copyBuilder.modality = property.modality
        copyBuilder.platformStatus = property.platformStatus
        copyBuilder.symbol = property.symbol.let { symbolRemapper.getSymbol(it) }
        copyBuilder.isLocal = property.isLocal
        copyBuilder.isInline = property.isInline
        copyBuilder.isConst = property.isConst
        copyBuilder.isLateinit = property.isLateinit
        copyBuilder.isExternal = property.isExternal
        copyBuilder.overriddenProperties.addAll(property.overriddenProperties.map { it.let { symbolRemapper.getSymbol(it) } })
        return copyBuilder.build().compose()
    }

    override fun transformRegularClass(regularClass: AstRegularClass): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstRegularClassBuilder(regularClass.context)
        copyBuilder.annotations.addAll(regularClass.annotations.map { it.transform() })
        copyBuilder.origin = regularClass.origin
        copyBuilder.attributes = regularClass.attributes
        copyBuilder.name = regularClass.name
        copyBuilder.visibility = regularClass.visibility
        copyBuilder.modality = regularClass.modality
        copyBuilder.platformStatus = regularClass.platformStatus
        copyBuilder.typeParameters.addAll(regularClass.typeParameters.map { it.transform() })
        copyBuilder.declarations.addAll(regularClass.declarations.map { it.transform() })
        copyBuilder.classKind = regularClass.classKind
        copyBuilder.delegateInitializers.addAll(regularClass.delegateInitializers.map { it.transform() })
        copyBuilder.symbol = regularClass.symbol.let { symbolRemapper.getSymbol(it) }
        copyBuilder.superTypes.addAll(regularClass.superTypes.map { it.transform() })
        copyBuilder.isInline = regularClass.isInline
        copyBuilder.isCompanion = regularClass.isCompanion
        copyBuilder.isFun = regularClass.isFun
        copyBuilder.isData = regularClass.isData
        copyBuilder.isInner = regularClass.isInner
        copyBuilder.isExternal = regularClass.isExternal
        return copyBuilder.build().compose()
    }

    override fun transformTypeAlias(typeAlias: AstTypeAlias): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstTypeAliasBuilder(typeAlias.context)
        copyBuilder.annotations.addAll(typeAlias.annotations.map { it.transform() })
        copyBuilder.origin = typeAlias.origin
        copyBuilder.attributes = typeAlias.attributes
        copyBuilder.name = typeAlias.name
        copyBuilder.visibility = typeAlias.visibility
        copyBuilder.modality = typeAlias.modality
        copyBuilder.platformStatus = typeAlias.platformStatus
        copyBuilder.typeParameters.addAll(typeAlias.typeParameters.map { it.transform() })
        copyBuilder.symbol = typeAlias.symbol.let { symbolRemapper.getSymbol(it) }
        copyBuilder.expandedType = typeAlias.expandedType.transform()
        return copyBuilder.build().compose()
    }

    override fun transformEnumEntry(enumEntry: AstEnumEntry): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstEnumEntryBuilder(enumEntry.context)
        copyBuilder.annotations.addAll(enumEntry.annotations.map { it.transform() })
        copyBuilder.origin = enumEntry.origin
        copyBuilder.attributes = enumEntry.attributes
        copyBuilder.declarations.addAll(enumEntry.declarations.map { it.transform() })
        copyBuilder.delegateInitializers.addAll(enumEntry.delegateInitializers.map { it.transform() })
        copyBuilder.name = enumEntry.name
        copyBuilder.symbol = enumEntry.symbol.let { symbolRemapper.getSymbol(it) }
        copyBuilder.initializer = enumEntry.initializer?.transform()
        return copyBuilder.build().compose()
    }

    override fun transformNamedFunction(namedFunction: AstNamedFunction): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstNamedFunctionBuilder(namedFunction.context)
        copyBuilder.annotations.addAll(namedFunction.annotations.map { it.transform() })
        copyBuilder.origin = namedFunction.origin
        copyBuilder.attributes = namedFunction.attributes
        copyBuilder.dispatchReceiverType = namedFunction.dispatchReceiverType?.transform()
        copyBuilder.extensionReceiverType = namedFunction.extensionReceiverType?.transform()
        copyBuilder.returnType = namedFunction.returnType.transform()
        copyBuilder.valueParameters.addAll(namedFunction.valueParameters.map { it.transform() })
        copyBuilder.body = namedFunction.body?.transform()
        copyBuilder.name = namedFunction.name
        copyBuilder.visibility = namedFunction.visibility
        copyBuilder.modality = namedFunction.modality
        copyBuilder.platformStatus = namedFunction.platformStatus
        copyBuilder.typeParameters.addAll(namedFunction.typeParameters.map { it.transform() })
        copyBuilder.isExternal = namedFunction.isExternal
        copyBuilder.isSuspend = namedFunction.isSuspend
        copyBuilder.isOperator = namedFunction.isOperator
        copyBuilder.isInfix = namedFunction.isInfix
        copyBuilder.isInline = namedFunction.isInline
        copyBuilder.isTailrec = namedFunction.isTailrec
        copyBuilder.overriddenFunctions.addAll(namedFunction.overriddenFunctions.map { it.let { symbolRemapper.getSymbol(it) } })
        copyBuilder.symbol = namedFunction.symbol.let { symbolRemapper.getSymbol(it) }
        return copyBuilder.build().compose()
    }

    override fun transformPropertyAccessor(propertyAccessor: AstPropertyAccessor): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstPropertyAccessorBuilder(propertyAccessor.context)
        copyBuilder.annotations.addAll(propertyAccessor.annotations.map { it.transform() })
        copyBuilder.origin = propertyAccessor.origin
        copyBuilder.attributes = propertyAccessor.attributes
        copyBuilder.returnType = propertyAccessor.returnType.transform()
        copyBuilder.valueParameters.addAll(propertyAccessor.valueParameters.map { it.transform() })
        copyBuilder.body = propertyAccessor.body?.transform()
        copyBuilder.name = propertyAccessor.name
        copyBuilder.visibility = propertyAccessor.visibility
        copyBuilder.modality = propertyAccessor.modality
        copyBuilder.symbol = propertyAccessor.symbol.let { symbolRemapper.getSymbol(it) }
        copyBuilder.isSetter = propertyAccessor.isSetter
        return copyBuilder.build().compose()
    }

    override fun transformConstructor(constructor: AstConstructor): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstConstructorBuilder(constructor.context)
        copyBuilder.annotations.addAll(constructor.annotations.map { it.transform() })
        copyBuilder.origin = constructor.origin
        copyBuilder.attributes = constructor.attributes
        copyBuilder.dispatchReceiverType = constructor.dispatchReceiverType?.transform()
        copyBuilder.extensionReceiverType = constructor.extensionReceiverType?.transform()
        copyBuilder.returnType = constructor.returnType.transform()
        copyBuilder.valueParameters.addAll(constructor.valueParameters.map { it.transform() })
        copyBuilder.symbol = constructor.symbol.let { symbolRemapper.getSymbol(it) }
        copyBuilder.delegatedConstructor = constructor.delegatedConstructor?.transform()
        copyBuilder.body = constructor.body?.transform()
        copyBuilder.visibility = constructor.visibility
        copyBuilder.isPrimary = constructor.isPrimary
        return copyBuilder.build().compose()
    }

    override fun transformModuleFragment(moduleFragment: AstModuleFragment): CompositeTransformResult<AstModuleFragment> {
        val copyBuilder = AstModuleFragmentBuilder(moduleFragment.context)
        copyBuilder.name = moduleFragment.name
        copyBuilder.files.addAll(moduleFragment.files.map { it.transform() })
        return copyBuilder.build().compose()
    }

    override fun transformFile(file: AstFile): CompositeTransformResult<AstElement> {
        val copyBuilder = AstFileBuilder(file.context)
        copyBuilder.annotations.addAll(file.annotations.map { it.transform() })
        copyBuilder.declarations.addAll(file.declarations.map { it.transform() })
        copyBuilder.name = file.name
        copyBuilder.packageFqName = file.packageFqName
        return copyBuilder.build().compose()
    }

    override fun transformAnonymousFunction(anonymousFunction: AstAnonymousFunction): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstAnonymousFunctionBuilder(anonymousFunction.context)
        copyBuilder.annotations.addAll(anonymousFunction.annotations.map { it.transform() })
        copyBuilder.origin = anonymousFunction.origin
        copyBuilder.attributes = anonymousFunction.attributes
        copyBuilder.extensionReceiverType = anonymousFunction.extensionReceiverType?.transform()
        copyBuilder.returnType = anonymousFunction.returnType.transform()
        copyBuilder.valueParameters.addAll(anonymousFunction.valueParameters.map { it.transform() })
        copyBuilder.body = anonymousFunction.body?.transform()
        copyBuilder.type = anonymousFunction.type.transform()
        copyBuilder.label = anonymousFunction.label
        copyBuilder.symbol = anonymousFunction.symbol.let { symbolRemapper.getSymbol(it) }
        return copyBuilder.build().compose()
    }

    override fun transformAnonymousObject(anonymousObject: AstAnonymousObject): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstAnonymousObjectBuilder(anonymousObject.context)
        copyBuilder.annotations.addAll(anonymousObject.annotations.map { it.transform() })
        copyBuilder.origin = anonymousObject.origin
        copyBuilder.attributes = anonymousObject.attributes
        copyBuilder.declarations.addAll(anonymousObject.declarations.map { it.transform() })
        copyBuilder.superTypes.addAll(anonymousObject.superTypes.map { it.transform() })
        copyBuilder.delegateInitializers.addAll(anonymousObject.delegateInitializers.map { it.transform() })
        copyBuilder.type = anonymousObject.type.transform()
        copyBuilder.symbol = anonymousObject.symbol.let { symbolRemapper.getSymbol(it) }
        return copyBuilder.build().compose()
    }

    override fun transformDoWhileLoop(doWhileLoop: AstDoWhileLoop): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstDoWhileLoopBuilder(doWhileLoop.context)
        copyBuilder.annotations.addAll(doWhileLoop.annotations.map { it.transform() })
        copyBuilder.label = doWhileLoop.label
        copyBuilder.condition = doWhileLoop.condition.transform()
        copyBuilder.body = doWhileLoop.body.transform()
        return copyBuilder.build().compose()
    }

    override fun transformWhileLoop(whileLoop: AstWhileLoop): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstWhileLoopBuilder(whileLoop.context)
        copyBuilder.annotations.addAll(whileLoop.annotations.map { it.transform() })
        copyBuilder.label = whileLoop.label
        copyBuilder.condition = whileLoop.condition.transform()
        copyBuilder.body = whileLoop.body.transform()
        return copyBuilder.build().compose()
    }

    override fun transformForLoop(forLoop: AstForLoop): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstForLoopBuilder(forLoop.context)
        copyBuilder.annotations.addAll(forLoop.annotations.map { it.transform() })
        copyBuilder.label = forLoop.label
        copyBuilder.body = forLoop.body.transform()
        copyBuilder.loopRange = forLoop.loopRange.transform()
        copyBuilder.loopParameter = forLoop.loopParameter.transform()
        return copyBuilder.build().compose()
    }

    override fun transformBlock(block: AstBlock): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstBlockBuilder(block.context)
        copyBuilder.annotations.addAll(block.annotations.map { it.transform() })
        copyBuilder.statements.addAll(block.statements.map { it.transform() })
        copyBuilder.type = block.type.transform()
        return copyBuilder.build().compose()
    }

    override fun transformBreak(breakExpression: AstBreak): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstBreakBuilder(breakExpression.context)
        copyBuilder.annotations.addAll(breakExpression.annotations.map { it.transform() })
        copyBuilder.target = breakExpression.target
        return copyBuilder.build().compose()
    }

    override fun transformContinue(continueExpression: AstContinue): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstContinueBuilder(continueExpression.context)
        copyBuilder.annotations.addAll(continueExpression.annotations.map { it.transform() })
        copyBuilder.target = continueExpression.target
        return copyBuilder.build().compose()
    }

    override fun transformCatch(catch: AstCatch): CompositeTransformResult<AstCatch> {
        val copyBuilder = AstCatchBuilder(catch.context)
        copyBuilder.parameter = catch.parameter.transform()
        copyBuilder.body = catch.body.transform()
        return copyBuilder.build().compose()
    }

    override fun transformTry(tryExpression: AstTry): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstTryBuilder(tryExpression.context)
        copyBuilder.annotations.addAll(tryExpression.annotations.map { it.transform() })
        copyBuilder.type = tryExpression.type.transform()
        copyBuilder.tryBody = tryExpression.tryBody.transform()
        copyBuilder.catches.addAll(tryExpression.catches.map { it.transform() })
        copyBuilder.finallyBody = tryExpression.finallyBody?.transform()
        return copyBuilder.build().compose()
    }

    override fun <T> transformConst(const: AstConst<T>): CompositeTransformResult<AstStatement> {
        return const.context.buildConst(const.value, const.kind, const.annotations.mapTo(mutableListOf()) { it.transform() }).compose()
    }

    override fun transformStarProjection(starProjection: AstStarProjection): CompositeTransformResult<AstTypeProjection> {
        val copyBuilder = AstStarProjectionBuilder(starProjection.context)
        return copyBuilder.build().compose()
    }

    override fun transformTypeProjectionWithVariance(typeProjectionWithVariance: AstTypeProjectionWithVariance): CompositeTransformResult<AstTypeProjection> {
        val copyBuilder = AstTypeProjectionWithVarianceBuilder(typeProjectionWithVariance.context)
        copyBuilder.type = typeProjectionWithVariance.type.transform()
        copyBuilder.variance = typeProjectionWithVariance.variance
        return copyBuilder.build().compose()
    }

    override fun transformWhen(whenExpression: AstWhen): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstWhenBuilder(whenExpression.context)
        copyBuilder.annotations.addAll(whenExpression.annotations.map { it.transform() })
        copyBuilder.type = whenExpression.type.transform()
        copyBuilder.branches.addAll(whenExpression.branches.map { it.transform() })
        return copyBuilder.build().compose()
    }

    override fun transformWhenBranch(whenBranch: AstWhenBranch): CompositeTransformResult<AstWhenBranch> {
        val copyBuilder = AstWhenBranchBuilder(whenBranch.context)
        copyBuilder.condition = whenBranch.condition.transform()
        copyBuilder.result = whenBranch.result.transform()
        return copyBuilder.build().compose()
    }

    override fun transformClassReference(classReference: AstClassReference): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstClassReferenceBuilder(classReference.context)
        copyBuilder.annotations.addAll(classReference.annotations.map { it.transform() })
        copyBuilder.type = classReference.type.transform()
        copyBuilder.classifier = classReference.classifier.let { symbolRemapper.getSymbol(it) }
        return copyBuilder.build().compose()
    }

    override fun transformQualifiedAccess(qualifiedAccess: AstQualifiedAccess): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstQualifiedAccessBuilder(qualifiedAccess.context)
        copyBuilder.annotations.addAll(qualifiedAccess.annotations.map { it.transform() })
        copyBuilder.type = qualifiedAccess.type.transform()
        copyBuilder.callee = qualifiedAccess.callee.let { symbolRemapper.getSymbol(it) }
        copyBuilder.typeArguments.addAll(qualifiedAccess.typeArguments.map { it.transform() })
        copyBuilder.dispatchReceiver = qualifiedAccess.dispatchReceiver?.transform()
        copyBuilder.extensionReceiver = qualifiedAccess.extensionReceiver?.transform()
        return copyBuilder.build().compose()
    }

    override fun transformFunctionCall(functionCall: AstFunctionCall): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstFunctionCallBuilder(functionCall.context)
        copyBuilder.annotations.addAll(functionCall.annotations.map { it.transform() })
        copyBuilder.type = functionCall.type.transform()
        copyBuilder.typeArguments.addAll(functionCall.typeArguments.map { it.transform() })
        copyBuilder.dispatchReceiver = functionCall.dispatchReceiver?.transform()
        copyBuilder.extensionReceiver = functionCall.extensionReceiver?.transform()
        copyBuilder.callee = functionCall.callee.let { symbolRemapper.getSymbol(it) }
        copyBuilder.valueArguments.addAll(functionCall.valueArguments.map { it?.transform() })
        return copyBuilder.build().compose()
    }

    override fun transformDelegatedConstructorCall(delegatedConstructorCall: AstDelegatedConstructorCall): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstDelegatedConstructorCallBuilder(delegatedConstructorCall.context)
        copyBuilder.annotations.addAll(delegatedConstructorCall.annotations.map { it.transform() })
        copyBuilder.type = delegatedConstructorCall.type.transform()
        copyBuilder.valueArguments.addAll(delegatedConstructorCall.valueArguments.map { it?.transform() })
        copyBuilder.callee = delegatedConstructorCall.callee.let { symbolRemapper.getSymbol(it) }
        copyBuilder.dispatchReceiver = delegatedConstructorCall.dispatchReceiver?.transform()
        copyBuilder.kind = delegatedConstructorCall.kind
        return copyBuilder.build().compose()
    }

    override fun transformDelegateInitializer(delegateInitializer: AstDelegateInitializer): CompositeTransformResult<AstElement> {
        val copyBuilder = AstDelegateInitializerBuilder(delegateInitializer.context)
        copyBuilder.delegatedSuperType = delegateInitializer.delegatedSuperType.transform()
        copyBuilder.expression = delegateInitializer.expression.transform()
        return copyBuilder.build().compose()
    }

    override fun transformCallableReference(callableReference: AstCallableReference): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstCallableReferenceBuilder(callableReference.context)
        copyBuilder.annotations.addAll(callableReference.annotations.map { it.transform() })
        copyBuilder.type = callableReference.type.transform()
        copyBuilder.typeArguments.addAll(callableReference.typeArguments.map { it.transform() })
        copyBuilder.dispatchReceiver = callableReference.dispatchReceiver?.transform()
        copyBuilder.extensionReceiver = callableReference.extensionReceiver?.transform()
        copyBuilder.callee = callableReference.callee.let { symbolRemapper.getSymbol(it) }
        copyBuilder.hasQuestionMarkAtLHS = callableReference.hasQuestionMarkAtLHS
        return copyBuilder.build().compose()
    }

    override fun transformVararg(vararg: AstVararg): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstVarargBuilder(vararg.context)
        copyBuilder.annotations.addAll(vararg.annotations.map { it.transform() })
        copyBuilder.type = vararg.type.transform()
        copyBuilder.elements.addAll(vararg.elements.map { it.transform() })
        return copyBuilder.build().compose()
    }

    override fun transformSpreadElement(spreadElement: AstSpreadElement): CompositeTransformResult<AstVarargElement> {
        val copyBuilder = AstSpreadElementBuilder(spreadElement.context)
        copyBuilder.expression = spreadElement.expression.transform()
        return copyBuilder.build().compose()
    }

    override fun transformReturn(returnExpression: AstReturn): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstReturnBuilder(returnExpression.context)
        copyBuilder.annotations.addAll(returnExpression.annotations.map { it.transform() })
        copyBuilder.target = returnExpression.target
        copyBuilder.result = returnExpression.result.transform()
        return copyBuilder.build().compose()
    }

    override fun transformThrow(throwExpression: AstThrow): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstThrowBuilder(throwExpression.context)
        copyBuilder.annotations.addAll(throwExpression.annotations.map { it.transform() })
        copyBuilder.exception = throwExpression.exception.transform()
        return copyBuilder.build().compose()
    }

    override fun transformVariableAssignment(variableAssignment: AstVariableAssignment): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstVariableAssignmentBuilder(variableAssignment.context)
        copyBuilder.annotations.addAll(variableAssignment.annotations.map { it.transform() })
        copyBuilder.typeArguments.addAll(variableAssignment.typeArguments.map { it.transform() })
        copyBuilder.dispatchReceiver = variableAssignment.dispatchReceiver?.transform()
        copyBuilder.extensionReceiver = variableAssignment.extensionReceiver?.transform()
        copyBuilder.callee = variableAssignment.callee.let { symbolRemapper.getSymbol(it) }
        copyBuilder.value = variableAssignment.value.transform()
        return copyBuilder.build().compose()
    }

    override fun transformSuperReference(superReference: AstSuperReference): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstSuperReferenceBuilder(superReference.context)
        copyBuilder.annotations.addAll(superReference.annotations.map { it.transform() })
        copyBuilder.type = superReference.type.transform()
        copyBuilder.superType = superReference.superType.let { symbolRemapper.getSymbol(it) }
        return copyBuilder.build().compose()
    }

    override fun transformThisReference(thisReference: AstThisReference): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstThisReferenceBuilder(thisReference.context)
        copyBuilder.annotations.addAll(thisReference.annotations.map { it.transform() })
        copyBuilder.type = thisReference.type.transform()
        copyBuilder.labelName = thisReference.labelName
        return copyBuilder.build().compose()
    }

    override fun transformPropertyBackingFieldReference(propertyBackingFieldReference: AstPropertyBackingFieldReference): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstPropertyBackingFieldReferenceBuilder(propertyBackingFieldReference.context)
        copyBuilder.annotations.addAll(propertyBackingFieldReference.annotations.map { it.transform() })
        copyBuilder.type = propertyBackingFieldReference.type.transform()
        copyBuilder.property = propertyBackingFieldReference.property.let { symbolRemapper.getSymbol(it) }
        return copyBuilder.build().compose()
    }

    override fun transformTypeOperation(typeOperation: AstTypeOperation): CompositeTransformResult<AstStatement> {
        val copyBuilder = AstTypeOperationBuilder(typeOperation.context)
        copyBuilder.annotations.addAll(typeOperation.annotations.map { it.transform() })
        copyBuilder.type = typeOperation.type.transform()
        copyBuilder.operator = typeOperation.operator
        copyBuilder.argument = typeOperation.argument.transform()
        copyBuilder.typeOperand = typeOperation.typeOperand.transform()
        return copyBuilder.build().compose()
    }

}
