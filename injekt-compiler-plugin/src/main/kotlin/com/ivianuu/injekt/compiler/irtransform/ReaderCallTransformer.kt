/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.irtransform

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektTrace
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.filePositionOf
import com.ivianuu.injekt.compiler.generator.toTypeRef
import com.ivianuu.injekt.compiler.generator.uniqueKey
import com.ivianuu.injekt.compiler.generator.uniqueTypeName
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.ScopeWithIr
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.impl.EmptyPackageFragmentDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrTypeParameter
import org.jetbrains.kotlin.ir.declarations.IrTypeParametersContainer
import org.jetbrains.kotlin.ir.declarations.impl.IrExternalPackageFragmentImpl
import org.jetbrains.kotlin.ir.declarations.path
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrDelegatingConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionAccessExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrConstructorSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.isAny
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given
class ReaderCallTransformer : IrLowering {

    private val injektTrace = given<InjektTrace>()

    private val transformedDeclarations = mutableListOf<IrDeclaration>()
    private val capturedTypeParameters = mutableListOf<IrTypeParameter>()

    private inline fun <R> withCapturedTypeParametersIfNeeded(
        owner: IrDeclaration,
        typeParameters: List<IrTypeParameter>,
        block: () -> R
    ): R {
        val isReader = owner.isTransformedReader()
        if (isReader) capturedTypeParameters += typeParameters
        val result = block()
        if (isReader) capturedTypeParameters -= typeParameters
        return result
    }

    private fun IrDeclaration.isTransformedReader(): Boolean =
        (this is IrSimpleFunction && injektTrace[InjektWritableSlices.IS_TRANSFORMED_READER, attributeOwnerId] == true) ||
                (this is IrClass && injektTrace[InjektWritableSlices.IS_TRANSFORMED_READER, attributeOwnerId] == true) ||
                (this is IrConstructor && constructedClass.isTransformedReader())

    override fun lower() {
        irModule.transformChildrenVoid(
            object : IrElementTransformerVoidWithContext() {
                override fun visitClassNew(declaration: IrClass): IrStatement {
                    return withCapturedTypeParametersIfNeeded(
                        declaration,
                        declaration.typeParameters
                    ) {
                        transformClassIfNeeded(declaration)
                        super.visitClassNew(declaration)
                    }
                }

                override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                    return withCapturedTypeParametersIfNeeded(
                        declaration,
                        declaration.typeParameters
                    ) {
                        transformFunctionIfNeeded(declaration)
                        super.visitFunctionNew(declaration)
                    }
                }

                override fun visitCall(expression: IrCall): IrExpression {
                    val result = super.visitCall(expression) as IrCall
                    return when {
                        expression.symbol.descriptor.fqNameSafe.asString() ==
                                "com.ivianuu.injekt.rootContext" -> {
                            transformContextCall(
                                null,
                                result,
                                currentFile,
                                null
                            )
                        }
                        expression.symbol.descriptor.fqNameSafe.asString() ==
                                "com.ivianuu.injekt.runReader" || expression.symbol.descriptor.fqNameSafe.asString() ==
                                "com.ivianuu.injekt.runChildReader" -> {
                            transformRunReaderCall(result, currentFile, null, null)
                        }
                        else -> {
                            result
                        }
                    }
                }
            }
        )
    }

    inner class ReaderScope(
        val declaration: IrDeclaration,
        val context: IrClass,
    ) {

        private val substitutionMap: Map<FqName, IrType> = run {
            val contextTypeParametersWithOrigin = injektTrace[
                    InjektWritableSlices.CONTEXT_TYPE_PARAMETERS_WITH_ORIGIN,
                    declaration.descriptor.uniqueKey()
            ]?.mapKeys { (key, _) ->
                context.typeParameters.singleOrNull {
                    it.descriptor.fqNameSafe == key.fqName
                }!!.symbol
            }
                ?.mapValues { it.value.fqNameSafe }
                ?.map { it.value to it.key }
                ?.toMap()

            capturedTypeParameters
                .map { it.descriptor.fqNameSafe }
                .zip(context.typeParameters.map { it.defaultType })
                .toMap() + (contextTypeParametersWithOrigin
                ?.mapValues { it.value.defaultType } ?: emptyMap())
        }

        fun givenExpressionForType(
            type: IrType,
            contextExpression: () -> IrExpression
        ): IrExpression {
            val finalType = type.substituteByFqName(substitutionMap)
            return if (!finalType.isAny() && finalType in context.superTypes) {
                contextExpression()
            } else {
                val typeName = finalType
                    .toKotlinType()
                    .toTypeRef()
                    .uniqueTypeName()
                    .asString()
                val function =
                    context.functions.singleOrNull {
                        it.name.asString() == typeName
                    } ?: error("Nothing found for $typeName in ${context.dump()}")
                function.irBuilder().run {
                    irCall(function).apply {
                        dispatchReceiver = contextExpression()
                    }
                }
            }
        }
    }

    private fun transformClassIfNeeded(
        declaration: IrClass
    ) {
        if (!declaration.isTransformedReader() &&
            !declaration.hasAnnotation(InjektFqNames.ContextImplMarker)
        ) return

        if (declaration.hasAnnotation(InjektFqNames.ContextImplMarker)) {
            transformDeclarationIfNeeded(
                declaration = declaration,
                declarationFunction = null,
                context = declaration,
                contextExpression = { scopes ->
                    declaration.irBuilder()
                        .irGet(scopes.thisOfClass(declaration)!!)
                }
            )
        } else {
            val readerConstructor = declaration.getReaderConstructor()!!

            val context = readerConstructor.getContext()!!

            transformDeclarationIfNeeded(
                declaration = declaration,
                declarationFunction = readerConstructor,
                context = context,
                contextExpression = { scopes ->
                    if (scopes.none { it.irElement == readerConstructor }) {
                        declaration.irBuilder().run {
                            irGetField(
                                irGet(scopes.thisOfClass(declaration)!!),
                                declaration.fields.single { it.name.asString() == "_context" }
                            )
                        }
                    } else {
                        readerConstructor.irBuilder()
                            .irGet(readerConstructor.getContextValueParameter()!!)
                    }
                }
            )
        }
    }

    private fun transformFunctionIfNeeded(declaration: IrFunction) {
        if (declaration !is IrSimpleFunction ||
            injektTrace[InjektWritableSlices.IS_TRANSFORMED_READER,
                    declaration.attributeOwnerId] != true
        ) return

        transformDeclarationIfNeeded(
            declaration = declaration,
            declarationFunction = declaration,
            context = declaration.getContext()!!,
            contextExpression = {
                declaration.irBuilder()
                    .irGet(declaration.getContextValueParameter()!!)
            }
        )
    }

    private fun transformDeclarationIfNeeded(
        declaration: IrDeclarationWithName,
        declarationFunction: IrFunction?,
        context: IrClass,
        contextExpression: (List<ScopeWithIr>) -> IrExpression
    ) {
        if (declaration in transformedDeclarations) return
        transformedDeclarations += declaration

        val scope = ReaderScope(declaration, context)

        declaration.transform(object : IrElementTransformerVoidWithContext() {
            override fun visitFunctionAccess(expression: IrFunctionAccessExpression): IrExpression {
                expression.transformChildrenVoid(this)
                if (expression !is IrCall &&
                    expression !is IrConstructorCall &&
                    expression !is IrDelegatingConstructorCall
                ) return expression

                if (allScopes
                        .mapNotNull { it.irElement as? IrDeclarationWithName }
                        .last {
                            it.isTransformedReader() ||
                                    (it is IrClass && it.hasAnnotation(InjektFqNames.ContextImplMarker))
                        }
                        .let { it != declaration && it != declarationFunction }
                ) {
                    return expression
                }

                return when {
                    expression is IrCall &&
                            expression.symbol.owner.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.given" ->
                        transformGivenCall(scope, expression) {
                            contextExpression(allScopes)
                        }
                    expression is IrCall &&
                            (expression.symbol.owner.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.rootContext" ||
                                    expression.symbol.owner.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.childContext") ->
                        transformContextCall(scope, expression, scope.declaration.file) {
                            contextExpression(allScopes)
                        }
                    expression is IrCall &&
                            (expression.symbol.owner.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.runReader" ||
                                    expression.symbol.owner.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.runChildReader") ->
                        transformRunReaderCall(expression, scope.declaration.file, scope) {
                            contextExpression(allScopes)
                        }
                    expression.symbol.owner.isTransformedReader() ->
                        transformReaderCall(scope, expression) {
                            contextExpression(allScopes)
                        }
                    else -> expression
                }
            }
        }, null)
    }

    private fun transformContextCall(
        scope: ReaderScope?,
        call: IrCall,
        file: IrFile,
        contextExpression: (() -> IrExpression)?
    ): IrExpression {
        val inputs = (call.getValueArgument(0) as? IrVarargImpl)
            ?.elements
            ?.map { it as IrExpression } ?: emptyList()

        val isChild = call.symbol.descriptor.fqNameSafe.asString() ==
                "com.ivianuu.injekt.childContext"

        val contextFactory = pluginContext.referenceClass(
            injektTrace[InjektWritableSlices.CONTEXT_FACTORY, filePositionOf(
                file.path, call.startOffset
            )]!!.factoryType.classifier.fqName
        )!!.owner

        return call.symbol.irBuilder().run {
            irCall(contextFactory.functions.single { it.name.asString() == "create" }).apply {
                dispatchReceiver = if (isChild) {
                    scope!!.givenExpressionForType(
                        if (scope.declaration is IrTypeParametersContainer)
                            contextFactory.typeWith(
                                scope.declaration.typeParameters
                                    .map { it.defaultType }
                            )
                        else contextFactory.defaultType,
                        contextExpression!!
                    )
                } else {
                    val contextFactoryImplStub = buildClass {
                        this.name = (contextFactory.name.asString() + "Impl").asNameId()
                        origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
                        kind = ClassKind.OBJECT
                        visibility = Visibilities.INTERNAL
                    }.apply clazz@{
                        createImplicitParameterDeclarationWithWrappedDescriptor()
                        parent = IrExternalPackageFragmentImpl(
                            IrExternalPackageFragmentSymbolImpl(
                                EmptyPackageFragmentDescriptor(
                                    pluginContext.moduleDescriptor,
                                    file.fqName
                                )
                            ),
                            file.fqName
                        )
                    }
                    irGetObject(contextFactoryImplStub.symbol)
                }

                inputs.forEachIndexed { index, input ->
                    putValueArgument(index, input)
                }
            }
        }
    }

    private fun transformRunReaderCall(
        call: IrCall,
        file: IrFile,
        scope: ReaderScope?,
        contextExpression: (() -> IrExpression)?
    ): IrExpression {
        val runReaderCallContextExpression = call.extensionReceiver ?: run {
            val inputs = (call.getValueArgument(0) as? IrVarargImpl)
                ?.elements
                ?.map { it as IrExpression } ?: emptyList()

            val isChild = call.symbol.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.runChildReader"

            val contextFactory = pluginContext.referenceClass(
                injektTrace[InjektWritableSlices.CONTEXT_FACTORY, filePositionOf(
                    file.path, call.startOffset
                )]!!.factoryType.classifier.fqName
            )!!.owner

            call.symbol.irBuilder().run {
                irCall(contextFactory.functions.single { it.name.asString() == "create" }).apply {
                    dispatchReceiver = if (isChild) {
                        scope!!.givenExpressionForType(
                            if (scope.declaration is IrTypeParametersContainer)
                                contextFactory.typeWith(
                                    scope.declaration.typeParameters
                                        .map { it.defaultType }
                                )
                            else contextFactory.defaultType,
                            contextExpression!!
                        )
                    } else {
                        val contextFactoryImplStub = buildClass {
                            this.name = (contextFactory.name.asString() + "Impl").asNameId()
                            origin = IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB
                            kind = ClassKind.OBJECT
                            visibility = Visibilities.INTERNAL
                        }.apply clazz@{
                            createImplicitParameterDeclarationWithWrappedDescriptor()
                            parent = IrExternalPackageFragmentImpl(
                                IrExternalPackageFragmentSymbolImpl(
                                    EmptyPackageFragmentDescriptor(
                                        pluginContext.moduleDescriptor,
                                        file.fqName
                                    )
                                ),
                                file.fqName
                            )
                        }
                        irGetObject(contextFactoryImplStub.symbol)
                    }

                    inputs.forEachIndexed { index, input ->
                        putValueArgument(index, input)
                    }
                }
            }
        }
        val lambdaExpression = call.getValueArgument(
            if (call.symbol.owner.extensionReceiverParameter != null) 0
            else 1
        )!! as IrFunctionExpression
        return call.symbol.irBuilder().run {
            irCall(
                pluginContext.referenceFunctions(
                    FqName("com.ivianuu.injekt.internal.runReaderDummy")
                ).single()
            ).apply {
                putTypeArgument(0, runReaderCallContextExpression.type)
                putTypeArgument(1, lambdaExpression.type.typeArguments.last().typeOrFail)
                putValueArgument(0, runReaderCallContextExpression)
                putValueArgument(1, lambdaExpression)
            }
        }
    }

    private fun transformGivenCall(
        scope: ReaderScope,
        call: IrCall,
        contextExpression: () -> IrExpression
    ): IrExpression {
        val arguments = (call.getValueArgument(0) as? IrVarargImpl)
            ?.elements
            ?.map { it as IrExpression } ?: emptyList()
        val realType = when {
            arguments.isNotEmpty() -> pluginContext.tmpFunction(arguments.size)
                .owner
                .typeWith(arguments.map { it.type } + call.getTypeArgument(0)!!)
            else -> call.getTypeArgument(0)!!
        }
        val rawExpression = scope.givenExpressionForType(
            realType,
            contextExpression
        )
        return call.symbol.irBuilder().run {
            when {
                arguments.isNotEmpty() -> call.symbol.irBuilder().irCall(
                    rawExpression.type.classOrNull!!
                        .owner
                        .functions
                        .first { it.name.asString() == "invoke" }
                ).apply {
                    dispatchReceiver = rawExpression
                    arguments.forEachIndexed { index, argument ->
                        putValueArgument(index, argument)
                    }
                }
                else -> rawExpression
            }
        }
    }

    private fun transformReaderCall(
        scope: ReaderScope,
        call: IrFunctionAccessExpression,
        contextExpression: () -> IrExpression
    ): IrExpression {
        val callee = call.symbol.owner

        // todo remove once kotlin compiler fixed IrConstructorCallImpl constructor
        val transformedCall = when (call) {
            is IrConstructorCall -> {
                IrConstructorCallImpl(
                    call.startOffset,
                    call.endOffset,
                    callee.returnType,
                    callee.symbol as IrConstructorSymbol,
                    call.typeArgumentsCount,
                    callee.typeParameters.size,
                    callee.valueParameters.size,
                    call.origin
                ).apply {
                    copyTypeAndValueArgumentsFrom(call)
                }
            }
            is IrDelegatingConstructorCall -> {
                IrDelegatingConstructorCallImpl(
                    call.startOffset,
                    call.endOffset,
                    call.type,
                    callee.symbol as IrConstructorSymbol,
                    call.typeArgumentsCount,
                    callee.valueParameters.size
                ).apply {
                    copyTypeAndValueArgumentsFrom(call)
                }
            }
            else -> {
                call as IrCall
                IrCallImpl(
                    call.startOffset,
                    call.endOffset,
                    callee.returnType,
                    callee.symbol,
                    call.origin,
                    call.superQualifierSymbol
                ).apply {
                    copyTypeAndValueArgumentsFrom(call)
                }
            }
        }

        val contextArgument = scope.givenExpressionForType(
            transformedCall.symbol.owner
                .getContext()!!
                .typeWith(transformedCall.typeArguments),
            contextExpression
        )

        transformedCall.putValueArgument(transformedCall.valueArgumentsCount - 1, contextArgument)

        return transformedCall
    }

}
