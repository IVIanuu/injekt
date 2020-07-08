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
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.child
import com.ivianuu.injekt.compiler.getFunctionType
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.transform.reader.ReaderTransformer
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.withNoArgAnnotations
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBlockBodyBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionExpressionImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrClassifierSymbol
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.deepCopyWithSymbols
import org.jetbrains.kotlin.ir.util.isSuspendFunction
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorUtils

class InjektDeclarationIrBuilder(
    val pluginContext: IrPluginContext,
    val symbol: IrSymbol
) {

    val builder = DeclarationIrBuilder(pluginContext, symbol)

    val symbols = InjektSymbols(pluginContext)

    val irBuiltIns = pluginContext.irBuiltIns

    fun irInjektIntrinsicUnit(): IrExpression {
        return builder.irCall(
            pluginContext.referenceFunctions(
                InjektFqNames.InternalPackage.child("injektIntrinsic")
            ).single()
        )
    }

    fun noArgSingleConstructorCall(clazz: IrClassSymbol): IrConstructorCall =
        builder.irCall(clazz.constructors.single())

    fun singleClassArgConstructorCall(
        clazz: IrClassSymbol,
        value: IrClassifierSymbol
    ): IrConstructorCall =
        builder.irCall(clazz.constructors.single()).apply {
            putValueArgument(
                0,
                IrClassReferenceImpl(
                    startOffset, endOffset,
                    irBuiltIns.kClassClass.typeWith(value.defaultType),
                    value,
                    value.defaultType
                )
            )
        }

    fun IrBlockBodyBuilder.initializeClassWithAnySuperClass(symbol: IrClassSymbol) {
        +irDelegatingConstructorCall(context.irBuiltIns.anyClass.owner.constructors.single())
        +IrInstanceInitializerCallImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            symbol,
            irBuiltIns.unitType
        )
    }

    fun irLambda(
        type: IrType,
        startOffset: Int = UNDEFINED_OFFSET,
        endOffset: Int = UNDEFINED_OFFSET,
        body: IrBlockBodyBuilder.(IrFunction) -> Unit
    ): IrExpression {
        val returnType = type.typeArguments.last().typeOrNull!!

        val lambda = buildFun {
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            name = Name.special("<anonymous>")
            this.returnType = returnType
            visibility = Visibilities.LOCAL
            isSuspend = type.isSuspendFunction()
        }.apply {
            parent = builder.scope.getLocalDeclarationParent()
            type.typeArguments.dropLast(1).forEachIndexed { index, typeArgument ->
                addValueParameter(
                    "p$index",
                    typeArgument.typeOrNull!!
                )
            }
            annotations += type.annotations.map {
                it.deepCopyWithSymbols()
            }
            this.body =
                DeclarationIrBuilder(pluginContext, symbol).irBlockBody { body(this, this@apply) }
        }

        return IrFunctionExpressionImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            type = type,
            function = lambda,
            origin = IrStatementOrigin.LAMBDA
        )
    }

    data class FactoryParameter(
        val name: String,
        val type: IrType,
        val assisted: Boolean
    )

    fun factoryLambda(
        readerTransformer: ReaderTransformer,
        assistedParameters: List<FactoryParameter>,
        returnType: IrType,
        startOffset: Int,
        endOffset: Int,
        createExpr: IrBuilderWithScope.(
            IrFunction,
            Map<FactoryParameter, IrValueParameter>
        ) -> IrExpression
    ): IrExpression {
        val lambdaType = pluginContext.tmpFunction(assistedParameters.size)
            .typeWith(assistedParameters.map { it.type } + returnType)
            .withNoArgAnnotations(pluginContext, listOf(InjektFqNames.Reader))

        return irReaderLambda(
            readerTransformer,
            lambdaType,
            startOffset,
            endOffset
        ) { lambda ->
            var parameterIndex = 0
            val parametersMap = assistedParameters.associateWith {
                lambda.valueParameters[parameterIndex++]
            }

            +irReturn(createExpr(this, lambda, parametersMap))
        }
    }

    fun irReaderLambda(
        readerTransformer: ReaderTransformer,
        type: IrType,
        startOffset: Int,
        endOffset: Int,
        body: IrBlockBodyBuilder.(IrFunction) -> Unit
    ): IrExpression {
        val returnType = type.typeArguments.last().typeOrNull!!

        val lambda = buildFun {
            origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
            name = Name.special("<anonymous>")
            this.returnType = returnType
            visibility = Visibilities.LOCAL
            this.startOffset = startOffset
            this.endOffset = endOffset
        }.apply {
            annotations += noArgSingleConstructorCall(symbols.reader)
            type.typeArguments.dropLast(1).forEachIndexed { index, typeArgument ->
                addValueParameter(
                    "p$index",
                    typeArgument.typeOrNull!!
                )
            }
            parent = builder.scope.getLocalDeclarationParent()
            this.body =
                DeclarationIrBuilder(pluginContext, symbol).irBlockBody { body(this, this@apply) }
        }.let { readerTransformer.getTransformedFunction(it) } as IrSimpleFunction

        return IrFunctionExpressionImpl(
            startOffset = startOffset,
            endOffset = endOffset,
            type = lambda.getFunctionType(pluginContext),
            function = lambda,
            origin = IrStatementOrigin.LAMBDA
        )
    }

    fun jvmNameAnnotation(name: String): IrConstructorCall {
        val jvmName = pluginContext.referenceClass(DescriptorUtils.JVM_NAME)!!
        return builder.run {
            irCall(jvmName.constructors.single()).apply {
                putValueArgument(0, irString(name))
            }
        }
    }

}
