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

package com.ivianuu.injekt.compiler.transform.composition

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.dumpSrc
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isExternalDeclaration
import com.ivianuu.injekt.compiler.remapTypeParameters
import com.ivianuu.injekt.compiler.transform.AbstractFunctionTransformer
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeWith
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.ir.copyBodyTo
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.createTmpVariable
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irIfThenElse
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.symbols.IrValueSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ReadableFunctionTransformer(
    pluginContext: IrPluginContext
) : AbstractFunctionTransformer(pluginContext, TransformOrder.BottomUp) {

    fun getContextForFunction(readable: IrFunction): IrClass =
        transformFunctionIfNeeded(readable).valueParameters.last().type.classOrNull!!.owner

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        super.visitModuleFragment(declaration)
        transformedFunctions
            .filterNot { it.key.isExternalDeclaration() }
            .forEach {
                it.value.file.addChild(
                    it.value.valueParameters.last().type.classOrNull!!.owner
                )
            }
        return declaration
    }

    override fun needsTransform(function: IrFunction): Boolean =
        function.isReadable(pluginContext.bindingContext)

    override fun transform(function: IrFunction, callback: (IrFunction) -> Unit) {
        val transformedFunction = buildFun {
            name = InjektNameConventions.getTransformedReadableFunctionNameForReadable(
                function.getPackageFragment()!!.fqName,
                function
            )
            isInline = function.isInline
            if (function.visibility == Visibilities.LOCAL) visibility = Visibilities.LOCAL
            else Visibilities.PUBLIC
        }.apply {
            if (function.visibility == Visibilities.LOCAL) parent = function.parent
            else function.file
            addMetadataIfNotLocal()

            annotations = function.annotations.map { it.deepCopyWithSymbols() }
            if (function.visibility != Visibilities.LOCAL) {
                annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                    irCall(symbols.astName.constructors.single()).apply {
                        putValueArgument(0, irString(function.descriptor.fqNameSafe.asString()))
                    }
                }
            }

            copyTypeParametersFrom(function)

            valueParameters = function.allParameters.mapIndexed { index, valueParameter ->
                val defaultExpr = valueParameter.defaultValue?.expression
                val isGiven = defaultExpr is IrCall && defaultExpr.symbol.descriptor
                    .fqNameSafe.asString() == "com.ivianuu.injekt.composition.given"
                valueParameter.copyTo(
                    this,
                    index = index,
                    type = if (isGiven) irBuiltIns.anyNType else valueParameter.type
                        .remapTypeParameters(function, this)
                )
            }

            body = function.copyBodyTo(this)
        }
        transformedFunction.returnType = function.returnType
            .remapTypeParameters(function, transformedFunction)
        callback(transformedFunction)

        val thisParameters = transformedFunction.valueParameters
            .filter { valueParameter ->
                val defaultExpr = valueParameter.defaultValue?.expression
                defaultExpr is IrCall && defaultExpr.symbol.descriptor
                    .fqNameSafe.asString() == "com.ivianuu.injekt.composition.given"
            }
        thisParameters
            .forEach {
                it.defaultValue = DeclarationIrBuilder(pluginContext, it.symbol).run {
                    irExprBody(irGetObject(symbols.uninitialized))
                }
            }

        val contextClass = buildClass {
            kind = ClassKind.INTERFACE
            name = InjektNameConventions.getContextClassNameForReadableFunction(
                function.getPackageFragment()!!.fqName,
                function
            )
        }.apply {
            parent = function.file
            createImplicitParameterDeclarationWithWrappedDescriptor()
            addMetadataIfNotLocal()
            copyTypeParametersFrom(transformedFunction)
        }

        val originalTypeByValueParameter = thisParameters.associateWith {
            function.allParameters[it.index].type
                .remapTypeParameters(function, transformedFunction)
        }

        val providerFunctionByParameter = thisParameters.associateWith { valueParameter ->
            contextClass.addFunction {
                name = InjektNameConventions.getReadableContextParamNameForValueParameter(
                    function.file, valueParameter
                )
                returnType = originalTypeByValueParameter[valueParameter]!!
                    .remapTypeParameters(transformedFunction, contextClass)
                modality = Modality.ABSTRACT
            }.apply {
                dispatchReceiverParameter = contextClass.thisReceiver?.copyTo(this)
            }
        }.mapKeys { it.key.symbol }

        val readableCalls = mutableListOf<IrCall>()

        transformedFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.owner.isReadable(pluginContext.bindingContext)) {
                    readableCalls += expression
                }
                return super.visitCall(expression)
            }
        })

        // todo add contexts of passed lambdas

        readableCalls
            .map { call ->
                val context = getContextForFunction(call.symbol.owner)
                context.defaultType.typeWith(*call.typeArguments.toTypedArray())
            }
            .forEach { contextClass.superTypes += it }

        val contextValueParameter = transformedFunction.addValueParameter(
            "readable_context",
            contextClass.typeWith(transformedFunction.typeParameters.map { it.defaultType })
        )

        val variableByValueParameter = mutableMapOf<IrValueSymbol, IrVariable>()
        val ignoredGetValues = mutableSetOf<IrGetValue>()

        transformedFunction.body =
            with(DeclarationIrBuilder(pluginContext, transformedFunction.symbol)) {
                irBlockBody {
                    thisParameters.forEach { valueParameter ->
                        variableByValueParameter[valueParameter.symbol] = createTmpVariable(
                            irIfThenElse(
                                valueParameter.type,
                                irEqeqeq(
                                    irGet(valueParameter)
                                        .also { ignoredGetValues += it },
                                    irGetObject(symbols.uninitialized)
                                ),
                                irCall(providerFunctionByParameter.getValue(valueParameter.symbol)).apply {
                                    dispatchReceiver = irGet(contextValueParameter)
                                },
                                irImplicitCast(
                                    irGet(valueParameter)
                                        .also { ignoredGetValues += it },
                                    originalTypeByValueParameter[valueParameter]!!
                                )
                            )
                        )
                    }

                    transformedFunction.body?.statements?.forEach { +it }
                }
            }

        transformedFunction.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitGetValue(expression: IrGetValue): IrExpression {
                return if (expression in ignoredGetValues) super.visitGetValue(expression)
                else variableByValueParameter[expression.symbol]
                    ?.let { DeclarationIrBuilder(pluginContext, it.symbol).irGet(it) }
                    ?: super.visitGetValue(expression)
            }
        })
    }

    override fun transformExternal(function: IrFunction, callback: (IrFunction) -> Unit) {
        callback(
            moduleFragment.descriptor.getPackage(function.getPackageFragment()!!.fqName)
                .memberScope
                .getContributedDescriptors()
                .filterIsInstance<FunctionDescriptor>()
                .filter { it.hasAnnotation(InjektFqNames.Readable) }
                .filter { it.hasAnnotation(InjektFqNames.AstName) }
                .single {
                    it.annotations.findAnnotation(InjektFqNames.AstName)!!
                        .argumentValue("name")
                        .let { it as StringValue }
                        .value == function.descriptor.fqNameSafe.asString()
                }
                .let {
                    pluginContext.referenceFunctions(it.fqNameSafe)
                        .single()
                        .owner
                }
        )
    }

    override fun transformCall(
        callingFunction: IrFunction?,
        transformedCallee: IrFunction,
        expression: IrCall
    ): IrCall {
        return super.transformCall(callingFunction, transformedCallee, expression).apply {
            putValueArgument(
                valueArgumentsCount - 1,
                DeclarationIrBuilder(pluginContext, symbol).run {
                    println("calling $callingFunction transformed parent ${transformedCallee.parent}")
                    irGet(callingFunction!!.valueParameters.last())
                }
            )
        }.also {
            println("transformed call ${expression.dumpSrc()} to ${it.dumpSrc()}")
        }
    }

}
