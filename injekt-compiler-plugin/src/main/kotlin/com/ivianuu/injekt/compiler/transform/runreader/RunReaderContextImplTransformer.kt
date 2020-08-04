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

package com.ivianuu.injekt.compiler.transform.runreader

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.addFile
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getAllClasses
import com.ivianuu.injekt.compiler.irCallAndRecordLookup
import com.ivianuu.injekt.compiler.irLambda
import com.ivianuu.injekt.compiler.recordLookup
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitContextParamTransformer
import com.ivianuu.injekt.compiler.uniqueTypeName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class RunReaderContextImplTransformer(
    pluginContext: IrPluginContext,
    private val declarationGraph: DeclarationGraph,
    private val implicitContextParamTransformer: ImplicitContextParamTransformer,
    private val initFile: IrFile
) : AbstractInjektTransformer(pluginContext) {

    override fun lower() {
        declarationGraph.runReaderContexts.forEach { index ->
            generateRunReaderContext(index)
        }
    }

    private fun generateRunReaderContext(index: IrClass) {
        val fqName = index.getAnnotation(InjektFqNames.RunReaderContext)!!
            .getValueArgument(0)
            .let { it as IrConst<String> }
            .value
            .let { FqName(it) }

        val file = module.addFile(
            pluginContext,
            fqName
        )

        val inputs = index.functions
            .single { it.name.asString() == "inputs" }
            .valueParameters

        val contextImpl = buildClass {
            this.name = fqName.shortName()
            visibility = Visibilities.INTERNAL
            if (inputs.isEmpty()) kind = ClassKind.OBJECT
        }.apply clazz@{
            parent = file
            file.addChild(this)
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes += index.superTypes.single()
            recordLookup(
                this,
                index.superTypes.single().classOrNull!!.owner
            )
        }

        recordLookup(
            initFile,
            contextImpl
        )

        val inputFields = inputs.associateWith {
            contextImpl.addField(
                it.name,
                it.type
            )
        }

        contextImpl.addConstructor {
            returnType = contextImpl.defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            val inputValueParameters = inputFields.values.associateWith {
                addValueParameter(
                    it.name.asString(),
                    it.type
                )
            }

            body = DeclarationIrBuilder(
                pluginContext,
                symbol
            ).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    contextImpl.symbol,
                    context.irBuiltIns.unitType
                )
                inputValueParameters.forEach { (field, valueParameter) ->
                    +irSetField(
                        irGet(contextImpl.thisReceiver!!),
                        field,
                        irGet(valueParameter)
                    )
                }
            }
        }

        val graph = BindingGraph(
            pluginContext = pluginContext,
            declarationGraph = declarationGraph,
            symbols = symbols,
            inputs = inputFields.values.toList(),
            implicitContextParamTransformer = implicitContextParamTransformer
        )

        val processedSuperTypes = mutableSetOf<IrType>()
        val declarationNames = mutableSetOf<Name>()
        var firstRound = true
        val bindingExpressions = mutableMapOf<Key, ContextBindingExpression>()

        while (true) {
            val entryPoints =
                (if (firstRound) listOf(index.superTypes.single().classOrNull!!.owner)
                else graph.resolvedBindings.values
                    .flatMapFix { it.contexts }
                    .flatMapFix { it.getAllClasses() })
                    .flatMapFix { declarationGraph.getAllContextImplementations(it) }
                    .distinct()
                    .filter { it.defaultType !in processedSuperTypes }

            if (entryPoints.isEmpty()) break

            fun collect(superClass: IrClass) {
                if (superClass.defaultType in processedSuperTypes) return
                processedSuperTypes += superClass.defaultType

                for (declaration in superClass.declarations.toList()) {
                    if (declaration !is IrFunction) continue
                    if (declaration is IrConstructor) continue
                    if (declaration.dispatchReceiverParameter?.type ==
                        pluginContext.irBuiltIns.anyType
                    ) continue
                    declarationNames += declaration.name
                    val request = BindingRequest(
                        declaration.returnType.asKey(),
                        null,
                        declaration.descriptor.fqNameSafe
                    )
                    bindingExpressions.getOrPut(request.key) {
                        createBindingExpression(contextImpl, graph, request)
                    }
                }

                superClass.superTypes
                    .map { it.classOrNull!!.owner }
                    .forEach { collect(it) }
            }

            entryPoints.forEach { entryPoint ->
                contextImpl.superTypes += entryPoint.defaultType
                recordLookup(contextImpl, entryPoint)
                collect(entryPoint)
            }

            firstRound = false
        }
    }

    private fun createBindingExpression(
        context: IrClass,
        graph: BindingGraph,
        request: BindingRequest
    ): ContextBindingExpression {
        val rawExpression = when (val binding = graph.getBinding(request)) {
            is GivenBindingNode -> givenExpression(context, binding)
            is InputBindingNode -> inputExpression(binding)
            is MapBindingNode -> mapBindingExpression(context, binding)
            is NullBindingNode -> nullExpression(binding)
            is SetBindingNode -> setBindingExpression(context, binding)
        }

        val function = buildFun {
            this.name = request.key.type.uniqueTypeName()
            returnType = request.key.type
        }.apply {
            dispatchReceiverParameter = context.thisReceiver!!.copyTo(this)
            this.parent = context
            context.addChild(this)
            this.body = DeclarationIrBuilder(pluginContext, symbol).run {
                irExprBody(rawExpression(this) { irGet(dispatchReceiverParameter!!) })
            }
        }

        return {
            irCall(function).apply {
                dispatchReceiver = it()
            }
        }
    }

    private fun inputExpression(
        binding: InputBindingNode
    ): ContextBindingExpression = { irGetField(it(), binding.inputField) }

    private fun mapBindingExpression(
        context: IrClass,
        bindingNode: MapBindingNode
    ): ContextBindingExpression {
        return { c ->
            irBlock {
                val tmpMap = irTemporary(
                    irCall(pluginContext.referenceFunctions(
                        FqName("kotlin.collections.mutableMapOf")
                    ).first { it.owner.valueParameters.isEmpty() })
                )
                val mapType = pluginContext.referenceClass(
                    FqName("kotlin.collections.Map")
                )!!
                bindingNode.contexts.forEach { recordLookup(context, it) }
                bindingNode.functions.forEach { function ->
                    +irCall(
                        tmpMap.type.classOrNull!!
                            .functions
                            .map { it.owner }
                            .single {
                                it.name.asString() == "putAll" &&
                                        it.valueParameters.singleOrNull()?.type?.classOrNull == mapType
                            }
                    ).apply {
                        dispatchReceiver = irGet(tmpMap)
                        putValueArgument(
                            0,
                            irCallAndRecordLookup(context, function.symbol).apply {
                                if (function.dispatchReceiverParameter != null)
                                    dispatchReceiver =
                                        irGetObject(function.dispatchReceiverParameter!!.type.classOrNull!!)
                                putValueArgument(valueArgumentsCount - 1, c())
                            }
                        )
                    }
                }

                +irGet(tmpMap)
            }
        }
    }

    private fun setBindingExpression(
        context: IrClass,
        bindingNode: SetBindingNode
    ): ContextBindingExpression {
        return { c ->
            irBlock {
                val tmpSet = irTemporary(
                    irCall(pluginContext.referenceFunctions(
                        FqName("kotlin.collections.mutableSetOf")
                    ).first { it.owner.valueParameters.isEmpty() })
                )
                val collectionType = pluginContext.referenceClass(
                    FqName("kotlin.collections.Collection")
                )
                bindingNode.contexts.forEach { recordLookup(context, it) }
                bindingNode.functions.forEach { function ->
                    +irCall(
                        tmpSet.type.classOrNull!!
                            .functions
                            .map { it.owner }
                            .single {
                                it.name.asString() == "addAll" &&
                                        it.valueParameters.singleOrNull()?.type?.classOrNull == collectionType
                            }
                    ).apply {
                        dispatchReceiver = irGet(tmpSet)
                        putValueArgument(
                            0,
                            irCallAndRecordLookup(context, function.symbol).apply {
                                if (function.dispatchReceiverParameter != null)
                                    dispatchReceiver =
                                        irGetObject(function.dispatchReceiverParameter!!.type.classOrNull!!)
                                putValueArgument(valueArgumentsCount - 1, c())
                            }
                        )
                    }
                }

                +irGet(tmpSet)
            }
        }
    }

    private fun nullExpression(binding: NullBindingNode): ContextBindingExpression =
        { irNull() }

    private fun givenExpression(
        context: IrClass,
        binding: GivenBindingNode
    ): ContextBindingExpression {
        recordLookup(context, binding.function)
        return { c ->
            if (binding.explicitParameters.isNotEmpty()) {
                irLambda(binding.key.type) { function ->
                    binding.createExpression(
                        this,
                        binding.explicitParameters
                            .associateWith { parameter ->
                                {
                                    irGet(
                                        function.valueParameters[parameter.index]
                                    )
                                }
                            },
                        c
                    )
                }
            } else {
                binding.createExpression(this, emptyMap(), c)
            }
        }
    }

}

typealias ContextBindingExpression = IrBuilderWithScope.(() -> IrExpression) -> IrExpression