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

package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.substituteAndKeepQualifiers
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.typeWith
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType

abstract class AbstractFactory(
    val moduleClass: IrClass,
    val typeParametersMap: Map<IrTypeParameterSymbol, IrType>,
    val pluginContext: IrPluginContext,
    val symbols: InjektSymbols,
    val declarationStore: InjektDeclarationStore
) {

    lateinit var graph: Graph
        private set

    abstract val factoryMembers: FactoryMembers

    lateinit var factoryExpressions: FactoryExpressions
        private set

    protected fun IrType.substituteWithFactoryTypeArguments(): IrType {
        (classifierOrFail as? IrTypeParameterSymbol)?.let {
            this@AbstractFactory.typeParametersMap[it]?.let { return it }
        }
        return substituteAndKeepQualifiers(typeParametersMap)
    }

    protected fun init(
        parent: AbstractFactory?,
        dependencyRequests: List<BindingRequest>,
        moduleAccessor: InitializerAccessor
    ) {
        factoryExpressions = FactoryExpressions(
            pluginContext = pluginContext,
            symbols = symbols,
            members = factoryMembers,
            parent = parent?.factoryExpressions,
            factory = this
        )
        graph = Graph(
            parent = parent?.graph,
            factory = this,
            context = pluginContext,
            factoryModule = ModuleNode(
                key = moduleClass.defaultType
                    .typeWith(*typeParametersMap.values.toTypedArray())
                    .asKey(),
                module = moduleClass,
                initializerAccessor = moduleAccessor,
                typeParametersMap = emptyMap()
            ),
            declarationStore = declarationStore,
            symbols = symbols,
            factoryMembers = factoryMembers
        ).also { factoryExpressions.graph = it }

        dependencyRequests.forEach { graph.validate(it) }
    }

    protected fun getModuleInitExpression(
        valueArguments: List<IrExpression>
    ): IrExpression {
        return DeclarationIrBuilder(pluginContext, moduleClass.symbol)
            .irCall(moduleClass.constructors.single()).apply {
                typeParametersMap.values.forEachIndexed { index, typeArgument ->
                    putTypeArgument(index, typeArgument)
                }
                valueArguments.forEachIndexed { index, valueArgument ->
                    putValueArgument(index, valueArgument)
                }
            }
    }
}
