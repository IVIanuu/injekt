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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.isTypeParameter
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ClassOfFunctionTransformer(pluginContext: IrPluginContext) :
    AbstractInjektTransformer(pluginContext) {

    private val transformedFunctions = mutableMapOf<IrFunction, IrFunction>()
    private val decoys = mutableMapOf<IrFunction, IrFunction>()

    override fun visitFile(declaration: IrFile): IrFile {
        val originalFunctions = declaration.declarations.filterIsInstance<IrFunction>()
        val result = super.visitFile(declaration)
        result.patchWithDecoys(originalFunctions)
        return result
    }

    override fun visitClass(declaration: IrClass): IrStatement {
        val originalFunctions = declaration.declarations.filterIsInstance<IrFunction>()
        val result = super.visitClass(declaration) as IrClass
        result.patchWithDecoys(originalFunctions)
        return result
    }

    private fun IrDeclarationContainer.patchWithDecoys(originalFunctions: List<IrFunction>) {
        for (original in originalFunctions) {
            val transformed = transformedFunctions[original]
            if (transformed != null && transformed != original) {
                declarations.add(
                    original.deepCopyWithPreservingQualifiers()
                        .also { decoy ->
                            decoys[original] = decoy
                            InjektDeclarationIrBuilder(pluginContext, decoy.symbol).run {
                                if (transformed.valueParameters.any {
                                        it.name.asString().startsWith("class\$")
                                    }) {
                                    decoy.annotations += noArgSingleConstructorCall(symbols.astProviderDslFunction)
                                }
                                decoy.body = builder.irExprBody(irInjektIntrinsicUnit())
                            }
                        }
                )
            }
        }
    }

    override fun visitFunction(declaration: IrFunction): IrStatement =
        transformFunctionIfNeeded(super.visitFunction(declaration) as IrFunction)

    private fun transformFunctionIfNeeded(function: IrFunction): IrFunction {
        if (function.visibility == Visibilities.LOCAL &&
            function.origin == IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
        ) return function
        if (function.origin == IrDeclarationOrigin.IR_EXTERNAL_DECLARATION_STUB ||
            function.origin == IrDeclarationOrigin.IR_EXTERNAL_JAVA_DECLARATION_STUB
        ) {
            return if (function.hasAnnotation(InjektFqNames.AstTyped)) {
                pluginContext.referenceFunctions(function.descriptor.fqNameSafe)
                    .map { it.owner }
                    .single { other ->
                        other.name == function.name &&
                                other.valueParameters.any {
                                    "class\$" in it.name.asString()
                                }
                    }
            } else function
        }

        val originalClassOfCalls = mutableListOf<IrCall>()
        val originalTypedModuleCalls = mutableListOf<IrCall>()
        var hasUnresolvedClassOfCalls = false

        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(expression.symbol.owner)
                if (callee.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.classOf") {
                    originalClassOfCalls += expression
                    if (expression.getTypeArgument(0)!!.isTypeParameter()) {
                        hasUnresolvedClassOfCalls = true
                    }
                } else if (callee.hasAnnotation(InjektFqNames.AstTyped)) {
                    originalTypedModuleCalls += expression
                    if (expression.typeArguments.any { it.isTypeParameter() }) {
                        hasUnresolvedClassOfCalls = true
                    }
                }
                return super.visitCall(expression)
            }
        })

        if (!hasUnresolvedClassOfCalls) {
            transformedFunctions[function] = function
            rewriteTypedFunctionCalls(
                function,
                originalClassOfCalls,
                originalTypedModuleCalls,
                emptyMap()
            )
            return function
        }

        val transformedFunction = function.deepCopyWithPreservingQualifiers()
        transformedFunctions[function] = transformedFunction

        val classOfCalls = mutableListOf<IrCall>()
        val typedModuleCalls = mutableListOf<IrCall>()

        transformedFunction.body?.transformChildrenVoid(object :
            IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                val callee = transformFunctionIfNeeded(expression.symbol.owner)
                if (callee.descriptor.fqNameSafe.asString() == "com.ivianuu.injekt.classOf") {
                    classOfCalls += expression
                } else if (callee.hasAnnotation(InjektFqNames.AstTyped)) {
                    typedModuleCalls += expression
                }
                return super.visitCall(expression)
            }
        })

        transformedFunction.annotations +=
            InjektDeclarationIrBuilder(pluginContext, transformedFunction.symbol)
                .noArgSingleConstructorCall(symbols.astTyped)

        val valueParametersByUnresolvedType =
            mutableMapOf<IrTypeParameterSymbol, IrValueParameter>()

        (classOfCalls
            .map { it.getTypeArgument(0)!! } + typedModuleCalls.flatMap { it.typeArguments })
            .filter { it.isTypeParameter() }
            .map { it.classifierOrFail as IrTypeParameterSymbol }
            .distinct()
            .forEach { typeParameter ->
                valueParametersByUnresolvedType[typeParameter] =
                    transformedFunction.addValueParameter(
                        InjektNameConventions.classParameterNameForTypeParameter(
                            typeParameter.owner
                        )
                            .asString(),
                        irBuiltIns.kClassClass.typeWith(typeParameter.defaultType)
                    )
            }

        rewriteTypedFunctionCalls(
            transformedFunction,
            classOfCalls,
            typedModuleCalls,
            valueParametersByUnresolvedType
        )

        return transformedFunction
    }

    private fun rewriteTypedFunctionCalls(
        function: IrFunction,
        classOfCalls: List<IrCall>,
        typedModuleCalls: List<IrCall>,
        valueParametersByUnresolvedType: Map<IrTypeParameterSymbol, IrValueParameter>
    ) {
        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                return when (expression) {
                    in classOfCalls -> {
                        val typeArgument = expression.getTypeArgument(0)!!
                        if (typeArgument.isTypeParameter()) {
                            val symbol = typeArgument.classifierOrFail as IrTypeParameterSymbol
                            DeclarationIrBuilder(pluginContext, expression.symbol)
                                .irGet(valueParametersByUnresolvedType.getValue(symbol))
                        } else {
                            IrClassReferenceImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                irBuiltIns.kClassClass.typeWith(typeArgument),
                                typeArgument.classifierOrFail,
                                typeArgument
                            )
                        }
                    }
                    in typedModuleCalls -> {
                        val transformedFunction = transformFunctionIfNeeded(expression.symbol.owner)
                        DeclarationIrBuilder(pluginContext, expression.symbol)
                            .irCall(transformedFunction).apply {
                                expression.typeArguments.forEachIndexed { index, typeArgument ->
                                    putTypeArgument(index, typeArgument)
                                }

                                dispatchReceiver = expression.dispatchReceiver
                                extensionReceiver = expression.extensionReceiver

                                transformedFunction.valueParameters.forEach { valueParameter ->
                                    var valueArgument = try {
                                        expression.getValueArgument(valueParameter.index)
                                    } catch (e: Throwable) {
                                        null
                                    }

                                    if (valueArgument == null &&
                                        valueParameter.name.asString().startsWith("class\$")
                                    ) {
                                        val typeParameterName = InjektNameConventions
                                            .typeParameterNameForClassParameterName(valueParameter.name)
                                        val typeParameter = transformedFunction.typeParameters
                                            .single { it.name == typeParameterName }
                                        val typeArgument = getTypeArgument(typeParameter.index)!!
                                        valueArgument = if (typeArgument.isTypeParameter()) {
                                            val symbol =
                                                typeArgument.classifierOrFail as IrTypeParameterSymbol
                                            DeclarationIrBuilder(pluginContext, expression.symbol)
                                                .irGet(
                                                    valueParametersByUnresolvedType.getValue(
                                                        symbol
                                                    )
                                                )
                                        } else {
                                            IrClassReferenceImpl(
                                                UNDEFINED_OFFSET,
                                                UNDEFINED_OFFSET,
                                                irBuiltIns.kClassClass.typeWith(typeArgument),
                                                typeArgument.classifierOrFail,
                                                typeArgument
                                            )
                                        }
                                    }

                                    putValueArgument(
                                        valueParameter.index,
                                        valueArgument
                                    )
                                }
                            }
                    }
                    else -> super.visitCall(expression)
                }
            }
        })
    }

}
