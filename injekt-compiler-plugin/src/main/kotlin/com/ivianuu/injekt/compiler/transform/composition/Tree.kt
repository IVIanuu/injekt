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
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.expressions.IrClassReference
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.IrClassSymbol
import org.jetbrains.kotlin.ir.symbols.IrFunctionSymbol
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.util.ReferenceSymbolTable
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.render
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.module

class CompositionFactoryGraph(
    private val symbolTable: ReferenceSymbolTable,
    private val allFactories: Map<IrClassSymbol, List<IrFunctionSymbol>>,
    private val allModules: Map<IrClassSymbol, List<IrFunctionSymbol>>
) {

    val compositionFactories = mutableListOf<CompositionFactory>()

    private val chain = mutableSetOf<IrFunctionSymbol>()

    init {
        allFactories.forEach { (compositionType, factoryFunctions) ->
            factoryFunctions.forEach { getOrCreateFactory(it, compositionType) }
        }
    }

    private fun getFactoriesForCompositionType(type: IrClassSymbol): List<CompositionFactory> {
        return allFactories.getValue(type)
            .map { getOrCreateFactory(it, type) }
    }

    private fun getOrCreateFactory(
        factoryFunction: IrFunctionSymbol,
        compositionType: IrClassSymbol
    ): CompositionFactory {
        check(factoryFunction !in chain) {
            "Circular dependency for ${factoryFunction.owner.render()} ${compositionType.owner.render()} " +
                    "${chain.map { it.owner.render() }}"
        }
        val existing = compositionFactories.firstOrNull {
            it.factoryFunction == factoryFunction
        }
        if (existing != null) return existing

        chain += factoryFunction

        val factory = CompositionFactory(compositionType, factoryFunction, symbolTable)

        factory.parentsCompositionTypes.forEach { parentCompositionType ->
            getFactoriesForCompositionType(parentCompositionType)
                .forEach { parentFactory ->
                    parentFactory.children += factory
                    factory.parents += parentFactory
                }
        }

        allModules[compositionType]?.forEach { factory.modules += it }

        compositionFactories += factory

        chain -= factoryFunction

        return factory
    }
}

class CompositionFactory(
    val compositionType: IrClassSymbol,
    val factoryFunction: IrFunctionSymbol,
    symbolTable: ReferenceSymbolTable
) {

    val parentsCompositionTypes = factoryFunction.owner.getAnnotation(InjektFqNames.AstParents)
        ?.getValueArgument(0)
        ?.let { it as IrVarargImpl }
        ?.elements
        ?.map { it as IrClassReference }
        ?.map { it.classType.classOrNull!! }
        ?: factoryFunction.owner.descriptor
            .annotations
            .findAnnotation(InjektFqNames.AstParents)
            ?.allValueArguments
            ?.values
            ?.single()
            ?.let { it as ArrayValue }
            ?.value
            ?.filterIsInstance<KClassValue>()
            ?.map { it.getArgumentType(factoryFunction.descriptor.module) }
            ?.map { symbolTable.referenceClass(it.constructor.declarationDescriptor as ClassDescriptor) }
            .let { it ?: emptyList() }

    val parents = mutableSetOf<CompositionFactory>()
    val modules = mutableSetOf<IrFunctionSymbol>()
    val children = mutableSetOf<CompositionFactory>()

    override fun toString(): String {
        return "CompositionFactory(compositionType=${compositionType.defaultType.render()}, " +
                "parents=$parents, " +
                "modules=${modules.map { it.owner.render() }}, " +
                "children=${children.map { it.factoryFunction.owner.render() }})"
    }

}
