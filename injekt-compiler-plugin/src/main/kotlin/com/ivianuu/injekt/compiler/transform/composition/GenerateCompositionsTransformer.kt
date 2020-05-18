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
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.withNoArgQualifiers
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class GenerateCompositionsTransformer(
    pluginContext: IrPluginContext,
    private val declarationStore: InjektDeclarationStore,
    private val compositionAggregateGenerator: CompositionAggregateGenerator
) : AbstractInjektTransformer(pluginContext) {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val generateCompositionsCalls = mutableListOf<Pair<IrCall, IrFile>>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.composition.generateCompositions"
                ) {
                    generateCompositionsCalls += expression to currentFile
                }
                return super.visitCall(expression)
            }
        })

        if (generateCompositionsCalls.isEmpty()) return super.visitModuleFragment(declaration)

        val compositionsPackage =
            pluginContext.moduleDescriptor.getPackage(InjektFqNames.CompositionsPackage)

        val allModules = mutableMapOf<IrClassSymbol, MutableList<IrFunctionSymbol>>()
        val allFactories = mutableMapOf<IrClassSymbol, MutableList<IrFunctionSymbol>>()

        compositionAggregateGenerator.compositionElements
            .forEach { (compositionType, elements) ->
                elements.forEach {
                    if (it.owner.hasAnnotation(InjektFqNames.CompositionFactory)) {
                        allFactories.getOrPut(compositionType) { mutableListOf() } += it
                    } else if (it.owner.hasAnnotation(InjektFqNames.Module)) {
                        allModules.getOrPut(compositionType) { mutableListOf() } += it
                    }
                }
            }

        compositionsPackage
            .memberScope
            .getContributedDescriptors()
            .filterIsInstance<ClassDescriptor>()
            .map {
                val x = it.name.asString().split("___")
                FqName(x[0].replace("__", ".")) to FqName(x[1].replace("__", "."))
            }
            .groupBy { it.first }
            .mapValues { it.value.map { it.second } }
            .mapKeys { pluginContext.referenceClass(it.key)!! }
            .mapValues {
                it.value.map {
                    pluginContext.referenceFunctions(it).first()
                }
            }
            .forEach { (compositionType, elements) ->
                elements.forEach {
                    if (it.owner.hasAnnotation(InjektFqNames.CompositionFactory)) {
                        allFactories.getOrPut(compositionType) { mutableListOf() } += it
                    } else if (it.owner.hasAnnotation(InjektFqNames.Module)) {
                        allModules.getOrPut(compositionType) { mutableListOf() } += it
                    }
                }
            }

        val graph = CompositionFactoryGraph(
            pluginContext,
            allFactories,
            allModules
        )

        val factoryImpls = mutableMapOf<IrClassSymbol, IrFunctionSymbol>()

        generateCompositionsCalls.forEach { (call, file) ->
            val processedFactories = mutableSetOf<CompositionFactory>()

            while (true) {
                val factoriesToProcess = graph.compositionFactories
                    .filter { it !in processedFactories }
                    .filter { factory ->
                        factory.children.all { it in processedFactories }
                    }

                if (factoriesToProcess.isEmpty()) {
                    break
                }

                factoriesToProcess.forEach { factory ->
                    val modules = factory.modules

                    val entryPoints = modules
                        .map { declarationStore.getModuleClassForFunction(it.owner) }
                        .map {
                            it.declarations
                                .single { it is IrClass && it.name.asString() == "Descriptor" }
                                .let { it as IrClass }
                        }
                        .flatMap { it.functions.toList() }
                        .filter { it.hasAnnotation(InjektFqNames.AstEntryPoint) }
                        .map { it.returnType }
                        .distinct()

                    val factoryType = compositionFactoryType(
                        InjektNameConventions.getCompositionFactoryTypeNameForCall(
                            file,
                            call,
                            factory.factoryFunction
                        ),
                        factory.compositionType.defaultType,
                        entryPoints
                    )
                    file.addChild(factoryType)

                    val factoryFunctionImpl = compositionFactoryImpl(
                        InjektNameConventions.getCompositionFactoryImplNameForCall(
                            file,
                            call,
                            factory.factoryFunction
                        ),
                        factory.parents.isNotEmpty(),
                        factoryType.symbol,
                        factory.factoryFunction,
                        factory.children.map {
                            it.compositionType to factoryImpls.getValue(it.compositionType)
                        },
                        factory.modules
                    )

                    processedFactories += factory

                    factoryImpls[factory.compositionType] = factoryFunctionImpl.symbol

                    file.addChild(factoryFunctionImpl)
                }
            }
        }

        declaration.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (generateCompositionsCalls.none { it.first == expression }) return super.visitCall(
                    expression
                )
                return DeclarationIrBuilder(pluginContext, expression.symbol).run {
                    irBlock {
                        factoryImpls.forEach { (compositionType, factoryFunctionImpl) ->
                            if (factoryFunctionImpl.owner.hasAnnotation(InjektFqNames.ChildFactory)) return@forEach
                            +irCall(
                                symbols.compositionFactories
                                    .functions
                                    .single { it.owner.name.asString() == "register" }
                            ).apply {
                                dispatchReceiver = irGetObject(symbols.compositionFactories)

                                putValueArgument(
                                    0,
                                    IrClassReferenceImpl(
                                        UNDEFINED_OFFSET,
                                        UNDEFINED_OFFSET,
                                        irBuiltIns.kClassClass.typeWith(compositionType.defaultType),
                                        compositionType,
                                        compositionType.defaultType
                                    )
                                )

                                putValueArgument(
                                    1,
                                    IrFunctionReferenceImpl(
                                        UNDEFINED_OFFSET,
                                        UNDEFINED_OFFSET,
                                        irBuiltIns.function(factoryFunctionImpl.owner.valueParameters.size)
                                            .typeWith(
                                                factoryFunctionImpl.owner.valueParameters
                                                    .map { it.type } + factoryFunctionImpl.owner.returnType
                                            ),
                                        factoryFunctionImpl,
                                        0,
                                        null
                                    )
                                )
                            }
                        }
                    }
                }
            }
        })

        return super.visitModuleFragment(declaration)
    }

    private fun compositionFactoryType(
        name: Name,
        compositionType: IrType,
        entryPoints: List<IrType>
    ) = buildClass {
        this.name = name
        kind = ClassKind.INTERFACE
    }.apply {
        createImplicitParameterDeclarationWithWrappedDescriptor()
        (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)

        superTypes += compositionType
        entryPoints.forEach { superTypes += it }
    }

    private fun compositionFactoryImpl(
        name: Name,
        childFactory: Boolean,
        factoryType: IrClassSymbol,
        factory: IrFunctionSymbol,
        childFactories: List<Pair<IrClassSymbol, IrFunctionSymbol>>,
        modules: Set<IrFunctionSymbol>
    ) = buildFun {
        this.name = name
        returnType = factoryType.owner.defaultType
    }.apply {
        annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
            .noArgSingleConstructorCall(
                if (childFactory) symbols.childFactory else symbols.factory
            )

        metadata = MetadataSource.Function(descriptor)

        factory.owner.valueParameters.forEach {
            addValueParameter(
                it.name.asString(),
                it.type
            )
        }

        body = DeclarationIrBuilder(pluginContext, symbol).run {
            irBlockBody {
                val factoryModule = declarationStore.getModuleFunctionForFactory(factory.owner)

                +irCall(factoryModule).apply {
                    valueParameters.forEach {
                        putValueArgument(it.index, irGet(it))
                    }
                }

                childFactories.forEach { (compositionType, childFactory) ->
                    +irCall(
                        pluginContext.referenceFunctions(
                            InjektFqNames.InjektPackage
                                .child(Name.identifier("childFactory"))
                        ).single()
                    ).apply {
                        putValueArgument(
                            0,
                            IrFunctionReferenceImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                irBuiltIns.function(childFactory.owner.valueParameters.size)
                                    .typeWith(
                                        childFactory.owner.valueParameters
                                            .map { it.type } + childFactory.owner.returnType
                                    ),
                                childFactory,
                                0,
                                null
                            )
                        )
                    }

                    +irCall(
                        pluginContext.referenceFunctions(
                            InjektFqNames.InjektPackage
                                .child(Name.identifier("alias"))
                        ).single()
                    ).apply {
                        val functionType = irBuiltIns.function(
                            childFactory.owner
                                .valueParameters
                                .size
                        ).typeWith(childFactory.owner
                            .valueParameters
                            .map { it.type } + childFactory.owner.returnType
                        ).withNoArgQualifiers(
                            pluginContext,
                            listOf(InjektFqNames.ChildFactory)
                        )
                        val aliasFunctionType = irBuiltIns.function(
                            childFactory.owner
                                .valueParameters
                                .size
                        ).typeWith(childFactory.owner
                            .valueParameters
                            .map { it.type } + compositionType.defaultType
                        ).withNoArgQualifiers(
                            pluginContext,
                            listOf(InjektFqNames.ChildFactory)
                        )
                        putTypeArgument(0, functionType)
                        putTypeArgument(1, aliasFunctionType)
                    }
                }

                modules.forEach { +irCall(it) }

                +irReturn(
                    irCall(
                        pluginContext.referenceFunctions(
                            InjektFqNames.InjektPackage
                                .child(Name.identifier("create"))
                        ).single()
                    ).apply {
                        putTypeArgument(0, factoryType.defaultType)
                    }
                )
            }
        }
    }

}
