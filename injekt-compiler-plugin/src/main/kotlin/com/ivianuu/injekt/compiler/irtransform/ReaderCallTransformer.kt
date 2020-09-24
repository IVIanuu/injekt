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
import com.ivianuu.injekt.compiler.InjektAttributes
import com.ivianuu.injekt.compiler.InjektAttributes.ContextFactoryKey
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektWritableSlices
import com.ivianuu.injekt.compiler.generator.toTypeRef
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
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.symbols.impl.IrExternalPackageFragmentSymbolImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.copyTypeAndValueArgumentsFrom
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given
class ReaderCallTransformer : IrLowering {

    private val transformedDeclarations = mutableListOf<IrDeclaration>()
    private val readerContextParamTransformer = given<ReaderContextParamTransformer>()

    private fun IrDeclaration.isTransformedReader(): Boolean =
        (this is IrSimpleFunction && given<BindingTrace>()[InjektWritableSlices.IS_TRANSFORMED_READER, attributeOwnerId] == true) ||
                (this is IrClass && given<BindingTrace>()[InjektWritableSlices.IS_TRANSFORMED_READER, attributeOwnerId] == true) ||
                (this is IrConstructor && constructedClass.isTransformedReader())

    override fun lower() {
        irModule.transformChildrenVoid(
            object : IrElementTransformerVoidWithContext() {
                override fun visitClassNew(declaration: IrClass): IrStatement {
                    transformClassIfNeeded(declaration)
                    return super.visitClassNew(declaration)
                }

                override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                    transformFunctionIfNeeded(declaration)
                    return super.visitFunctionNew(declaration)
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
                                "com.ivianuu.injekt.runReader" -> {
                            transformRunReaderCall(result)
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
        val originalToDeclarationSubstitutionMap: Map<IrTypeParameterSymbol, IrTypeParameterSymbol>,
        val context: IrClass,
    ) {

        private val declarationToContextSubstitutionMap = (declaration as IrTypeParametersContainer)
            .typeParameters
            .map { it.symbol }
            .zip(context.typeParameters.map { it.defaultType })
            .toMap() + declaration.typeParameters
            .map { originalToDeclarationSubstitutionMap[it.symbol] ?: it.symbol }
            .zip(context.typeParameters.map { it.defaultType })
            .toMap()

        fun givenExpressionForType(
            type: IrType,
            contextExpression: () -> IrExpression
        ): IrExpression {
            val finalType = type
                .substitute(declarationToContextSubstitutionMap)
            return if (finalType in context.superTypes) {
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

        fun givenExpressionForName(
            name: Name,
            contextExpression: () -> IrExpression
        ): IrExpression {
            val function =
                context.functions.singleOrNull { it.name == name }
                    ?: error("Nothing found for $name in ${context.dump()}")
            return function.irBuilder().run {
                irCall(function).apply {
                    dispatchReceiver = contextExpression()
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

    private fun transformFunctionIfNeeded(
        declaration: IrFunction
    ) {
        if (declaration !is IrSimpleFunction ||
            given<BindingTrace>()[InjektWritableSlices.IS_TRANSFORMED_READER,
                    declaration.attributeOwnerId] != true
        ) return

        transformDeclarationIfNeeded(
            declaration = declaration,
            declarationFunction = declaration,
            context = declaration.getContext() ?: error("Wtf ${declaration.render()}"),
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

        val scope = ReaderScope(
            declaration,
            readerContextParamTransformer.transformedTypeParametersToOriginalTypeParameters,
            context
        )

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
                            expression.symbol.owner.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.runReader" ->
                        transformRunReaderCall(expression)
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
            given<InjektAttributes>()[ContextFactoryKey(file.path, call.startOffset)]!!
                .factoryType.classifier.fqName
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

    private fun transformRunReaderCall(call: IrCall): IrExpression {
        val runReaderCallContextExpression = call.extensionReceiver!!
        val lambdaExpression = call.getValueArgument(0)!! as IrFunctionExpression
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
        val rawExpression = scope.givenExpressionForName(
            given<InjektAttributes>()[InjektAttributes.GivenFunctionName(
                scope.declaration.file.path, call.startOffset
            )]!!,
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
        transformFunctionIfNeeded(callee)

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
