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

import com.ivianuu.injekt.compiler.CompositionSymbols
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.child
import com.ivianuu.injekt.compiler.getClassesFromSingleArrayValueAnnotation
import com.ivianuu.injekt.compiler.getFunctionType
import com.ivianuu.injekt.compiler.tmpFunction
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.withNoArgAnnotations
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.PackageViewDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrFunctionReferenceImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.isExternalDeclaration

class GenerateCompositionsTransformer(
    pluginContext: IrPluginContext,
    private val declarationStore: InjektDeclarationStore
) : AbstractInjektTransformer(pluginContext) {

    private val compositionSymbols = CompositionSymbols(pluginContext)

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val generateCompositionsCalls = mutableListOf<Pair<IrCall, IrFile>>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.descriptor.fqNameSafe.asString() ==
                    "com.ivianuu.injekt.composition.initializeCompositions"
                ) {
                    generateCompositionsCalls += expression to currentFile
                }
                return super.visitCall(expression)
            }
        })

        if (generateCompositionsCalls.isEmpty()) return super.visitModuleFragment(declaration)

        val allModules = mutableMapOf<IrClassSymbol, MutableList<IrFunctionSymbol>>()
        val allFactories = mutableMapOf<IrClassSymbol, MutableList<IrFunctionSymbol>>()

        fun handleFunction(function: IrFunction) {
            if (function.hasAnnotation(InjektFqNames.CompositionFactory)) {
                allFactories.getOrPut(function.returnType.classOrNull!!) { mutableListOf() } += function.symbol
            } else if (function.hasAnnotation(InjektFqNames.Module)) {
                val metadata = declarationStore.getCompositionModuleMetadata(function)
                    ?: return
                val compositionTypes = metadata.getClassesFromSingleArrayValueAnnotation(
                    InjektFqNames.AstCompositionTypes, pluginContext
                )

                compositionTypes.forEach { compositionType ->
                    allModules.getOrPut(compositionType.symbol) { mutableListOf() } += function.symbol
                }
            }
        }

        val moduleDescriptor = moduleFragment.descriptor

        fun forEachPackageRecursive(
            packageViewDescriptor: PackageViewDescriptor,
            block: (PackageViewDescriptor) -> Unit
        ) {
            block(packageViewDescriptor)
            moduleDescriptor.getSubPackagesOf(packageViewDescriptor.fqName) { true }
                .map { moduleDescriptor.getPackage(it) }
                .forEach { forEachPackageRecursive(it, block) }
        }

        forEachPackageRecursive(moduleFragment.descriptor.getPackage(FqName.ROOT)) { packageViewDescriptor ->
            packageViewDescriptor.fragments
                .map { it.getMemberScope() }
                .flatMap { memberScope ->
                    memberScope.getFunctionNames()
                        .flatMap {
                            memberScope.getContributedFunctions(
                                it,
                                NoLookupLocation.FROM_BACKEND
                            )
                        }
                }
                .filter {
                    (it.hasAnnotation(InjektFqNames.Module) && it.valueParameters.lastOrNull()?.name?.asString() == "moduleMarker") ||
                            it.hasAnnotation(InjektFqNames.CompositionFactory)
                }
                .flatMap { functionDescriptor ->
                    pluginContext.referenceFunctions(functionDescriptor.fqNameSafe)
                        .filter { it.descriptor == functionDescriptor }
                }
                .map { it.owner }
                .filter { it.isExternalDeclaration() }
                .distinct()
                .forEach { handleFunction(it) }
        }

        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                handleFunction(declaration)
                return super.visitFunction(declaration)
            }
        })

        val graph = CompositionFactoryGraph(
            pluginContext,
            allFactories,
            allModules
        )

        val factoryImpls = mutableMapOf<IrClassSymbol, IrFunctionSymbol>()

        generateCompositionsCalls.forEach { (call, file) ->
            val nameProvider = NameProvider()

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
                        .flatMap {
                            declarationStore.getCompositionModuleMetadata(it.owner)!!
                                .getClassesFromSingleArrayValueAnnotation(
                                    InjektFqNames.AstEntryPoints,
                                    pluginContext
                                ).map { it.defaultType }
                        }
                        .distinct()

                    val factoryType = compositionFactoryType(
                        nameProvider.allocateForGroup(
                            InjektNameConventions.getCompositionFactoryTypeNameForCall(
                                file,
                                call,
                                factory.factoryFunction
                            )
                        ),
                        factory.compositionType.defaultType,
                        entryPoints
                    )
                    file.addChild(factoryType)

                    val factoryFunctionImpl = compositionFactoryImpl(
                        nameProvider.allocateForGroup(
                            InjektNameConventions.getCompositionFactoryImplNameForCall(
                                file,
                                call,
                                factory.factoryFunction,
                                factory.parents.isNotEmpty()
                            )
                        ),
                        factory.parents.isNotEmpty(),
                        factoryType.symbol,
                        factory.compositionType.defaultType,
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
                                compositionSymbols.compositionFactories
                                    .functions
                                    .single { it.owner.name.asString() == "register" }
                            ).apply {
                                dispatchReceiver =
                                    irGetObject(compositionSymbols.compositionFactories)

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
                                        factoryFunctionImpl.owner.getFunctionType(pluginContext),
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
        addMetadataIfNotLocal()

        superTypes += compositionType
        entryPoints.forEach { superTypes += it }
    }

    private fun compositionFactoryImpl(
        name: Name,
        childFactory: Boolean,
        factoryType: IrClassSymbol,
        compositionType: IrType,
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

        addMetadataIfNotLocal()

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
                    if (factoryModule.valueParameters.size > valueParameters.size) {
                        putValueArgument(valueParameters.size, irNull())
                    }
                }

                +irCall(
                    pluginContext.referenceFunctions(
                        InjektFqNames.InjektPackage
                            .child("alias")
                    ).single()
                ).apply {
                    putTypeArgument(0, factoryType.defaultType)
                    putTypeArgument(1, compositionType)
                }

                childFactories.forEach { (compositionType, childFactory) ->
                    +irCall(
                        pluginContext.referenceFunctions(
                            InjektFqNames.InjektPackage
                                .child("childFactory")
                        ).single()
                    ).apply {
                        putValueArgument(
                            0,
                            IrFunctionReferenceImpl(
                                UNDEFINED_OFFSET,
                                UNDEFINED_OFFSET,
                                childFactory.owner.getFunctionType(pluginContext),
                                childFactory,
                                0,
                                null
                            )
                        )
                    }

                    +irCall(
                        pluginContext.referenceFunctions(
                            InjektFqNames.InjektPackage
                                .child("alias")
                        ).single()
                    ).apply {
                        val functionType = childFactory.owner.getFunctionType(pluginContext)
                            .withNoArgAnnotations(
                                pluginContext,
                                listOf(InjektFqNames.ChildFactory)
                            )
                        val aliasFunctionType = pluginContext.tmpFunction(
                            childFactory.owner
                                .valueParameters
                                .size
                        ).typeWith(childFactory.owner
                            .valueParameters
                            .map { it.type } + compositionType.defaultType
                        ).withNoArgAnnotations(
                            pluginContext,
                            listOf(InjektFqNames.ChildFactory)
                        )
                        putTypeArgument(0, functionType)
                        putTypeArgument(1, aliasFunctionType)
                    }
                }

                modules.forEach {
                    +irCall(it).apply {
                        if (it.owner.valueParameters.isNotEmpty()) {
                            putValueArgument(0, irNull())
                        }
                    }
                }

                +irReturn(
                    irCall(
                        pluginContext.referenceFunctions(
                            InjektFqNames.InjektPackage
                                .child("create")
                        ).single()
                    ).apply {
                        putTypeArgument(0, factoryType.defaultType)
                    }
                )
            }
        }
    }

}
