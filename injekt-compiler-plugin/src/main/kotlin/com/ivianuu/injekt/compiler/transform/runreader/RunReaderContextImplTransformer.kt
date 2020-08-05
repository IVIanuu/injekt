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
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addFile
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
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
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irAs
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irIs
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
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
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isObject
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class RunReaderContextImplTransformer(
    pluginContext: IrPluginContext,
    private val declarationGraph: DeclarationGraph,
    private val implicitContextParamTransformer: ImplicitContextParamTransformer,
    private val initFile: IrFile
) : AbstractInjektTransformer(pluginContext) {

    private val generatedContexts = mutableMapOf<IrClass, Set<IrClass>>()

    override fun lower() {
        while (true) {
            val roundContexts = declarationGraph.runReaderContexts
                .filter { it.superTypes.first().classOrNull!!.owner !in generatedContexts }

            if (roundContexts.isEmpty()) break

            for (index in roundContexts) {
                val context = index.superTypes.first().classOrNull!!.owner
                if (index in generatedContexts) continue

                val parents = declarationGraph.getParentRunReaderContexts(context)

                val unimplementedParents =
                    parents
                        .filter { it !in generatedContexts }

                if (unimplementedParents.isNotEmpty()) continue

                generatedContexts[context] = generateRunReaderContextWithFactory(
                    index,
                    parents
                        .flatMapFix { generatedContexts[it]!! }
                        .toSet()
                )
            }
        }
    }

    private fun generateRunReaderContextWithFactory(
        index: IrClass,
        parents: Set<IrClass>
    ): Set<IrClass> {
        val thisInputs = index.functions
            .single { it.name.asString() == "inputs" }
            .valueParameters
            .map { it.type }

        val fqName = index.getAnnotation(InjektFqNames.RunReaderContext)!!
            .getValueArgument(0)
            .let { it as IrConst<String> }
            .value
            .let { FqName(it) }
        val isChild = index.getAnnotation(InjektFqNames.RunReaderContext)!!
            .getValueArgument(1)
            .let { it as IrConst<Boolean> }
            .value

        val file = module.addFile(pluginContext, fqName)

        val thisContext = index.superTypes[0].classOrNull!!.owner
        val callingContext = if (isChild) index.superTypes[1].classOrNull!!.owner else null

        val implNameProvider = NameProvider()

        val contextImplsWithParents: List<Pair<IrClass, IrClass?>> = parents
            .takeIf { it.isNotEmpty() }
            .let { it ?: listOf(null) }
            .map { parent ->
                val parentInputs = parent?.functions
                    ?.filter { it.hasAnnotation(InjektFqNames.Input) }
                    ?.map { it.returnType }
                    ?.toSet() ?: emptySet()

                val combinedInputs = parentInputs.toMutableSet()
                combinedInputs += thisInputs

                val contextImpl = generateRunReaderContext(
                    implNameProvider.allocateForGroup("Impl").asNameId(),
                    thisContext,
                    combinedInputs,
                    file
                )

                if (parent != null) recordLookup(contextImpl, parent)

                contextImpl to parent
            }

        val factory = buildClass {
            this.name = fqName.shortName()
            kind = ClassKind.OBJECT
            visibility = Visibilities.INTERNAL
        }.apply clazz@{
            createImplicitParameterDeclarationWithWrappedDescriptor()

            addConstructor {
                returnType = defaultType
                isPrimary = true
                visibility = Visibilities.PUBLIC
            }.apply {
                body = DeclarationIrBuilder(
                    pluginContext,
                    symbol
                ).irBlockBody {
                    +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                    +IrInstanceInitializerCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        this@clazz.symbol,
                        context.irBuiltIns.unitType
                    )
                }
            }

            addFunction {
                name = "create".asNameId()
                returnType = thisContext.defaultType
            }.apply {
                dispatchReceiverParameter = thisReceiver!!.copyTo(this)

                val parentValueParameter = if (isChild) {
                    addValueParameter(
                        "parent",
                        callingContext!!.defaultType
                    )
                } else null

                val inputNameProvider = NameProvider()
                thisInputs.forEach {
                    addValueParameter(
                        inputNameProvider.allocateForGroup(it.uniqueTypeName()).asString(),
                        it
                    )
                }

                body = DeclarationIrBuilder(pluginContext, symbol).run {
                    fun createContextImpl(contextImpl: IrClass, parent: IrClass?): IrExpression {
                        return if (contextImpl.isObject) {
                            irGetObject(contextImpl.symbol)
                        } else {
                            val contextImplInputs = contextImpl.constructors.single()
                                .valueParameters
                                .map { it.type }
                            irCall(contextImpl.constructors.single()).apply {
                                contextImplInputs.forEachIndexed { index, type ->
                                    putValueArgument(
                                        index,
                                        valueParameters.firstOrNull {
                                            it.type == type
                                        }?.let { irGet(it) }
                                            ?: parent!!.functions
                                                .filter { it.hasAnnotation(InjektFqNames.Input) }
                                                .single { it.returnType == type }
                                                .let { parentInput ->
                                                    irCall(parentInput).apply {
                                                        dispatchReceiver = irAs(
                                                            irGet(parentValueParameter!!),
                                                            parent.defaultType
                                                        )
                                                    }
                                                }
                                    )
                                }
                            }
                        }
                    }
                    irExprBody(
                        if (contextImplsWithParents.size == 1) {
                            val (contextImpl, parent) = contextImplsWithParents.single()
                            createContextImpl(contextImpl, parent)
                        } else {
                            irWhen(
                                thisContext.defaultType,
                                contextImplsWithParents.map { (contextImpl, parent) ->
                                    parent!!
                                    irBranch(
                                        irIs(irGet(parentValueParameter!!), parent.defaultType),
                                        createContextImpl(contextImpl, parent)
                                    )
                                } + irElseBranch(
                                    irCall(
                                        pluginContext.referenceFunctions(
                                            FqName("kotlin.error")
                                        ).single()
                                    ).apply {
                                        putValueArgument(
                                            0,
                                            irString("Unexpected parent")
                                        )
                                    }
                                )
                            )
                        }
                    )
                }
            }

            contextImplsWithParents.forEach { addChild(it.first) }
        }

        file.addChild(factory)

        return contextImplsWithParents
            .map { it.first }
            .toSet()
    }

    private fun generateRunReaderContext(
        name: Name,
        thisContext: IrClass,
        inputs: Set<IrType>,
        irParent: IrDeclarationParent
    ): IrClass {
        val contextImpl = buildClass {
            this.name = name
            visibility = Visibilities.INTERNAL
            if (inputs.isEmpty()) kind = ClassKind.OBJECT
        }.apply clazz@{
            this.parent = irParent
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes += thisContext.defaultType
            recordLookup(this, thisContext)
        }

        recordLookup(initFile, contextImpl)

        val inputFieldNameProvider = NameProvider()
        val inputFields = inputs.associateWith {
            contextImpl.addField(
                inputFieldNameProvider.allocateForGroup(it.uniqueTypeName()),
                it
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
                (if (firstRound) listOf(thisContext)
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

        // include expressions for inputs so that we can access them from children
        // todo maybe only do this if we have children
        inputs.forEach {
            bindingExpressions.getOrPut(it.asKey()) {
                createBindingExpression(
                    contextImpl, graph, BindingRequest(
                        it.asKey(),
                        null,
                        null
                    )
                )
            }
        }

        return contextImpl
    }

    private fun createBindingExpression(
        context: IrClass,
        graph: BindingGraph,
        request: BindingRequest
    ): ContextBindingExpression {
        val binding = graph.getBinding(request)
        val rawExpression = when (binding) {
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
            addMetadataIfNotLocal()
            dispatchReceiverParameter = context.thisReceiver!!.copyTo(this)
            this.parent = context
            context.addChild(this)
            this.body = DeclarationIrBuilder(pluginContext, symbol).run {
                irExprBody(rawExpression(this) { irGet(dispatchReceiverParameter!!) })
            }
            if (binding is InputBindingNode) {
                annotations += DeclarationIrBuilder(pluginContext, symbol)
                    .irCall(symbols.input.constructors.single())
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
