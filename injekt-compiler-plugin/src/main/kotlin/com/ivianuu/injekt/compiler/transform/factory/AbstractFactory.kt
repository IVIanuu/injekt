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
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrVariable
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.FqName

abstract class AbstractFactory(
    val origin: FqName,
    val moduleClass: IrClass,
    val moduleVariable: IrVariable,
    val factoryFunction: IrFunction,
    val pluginContext: IrPluginContext,
    val symbols: InjektSymbols,
    val declarationStore: InjektDeclarationStore
) {

    val factoryMembers = FactoryMembers(pluginContext)

    lateinit var graph: Graph
        private set

    lateinit var factoryExpressions: FactoryExpressions
        private set

    protected fun init(
        parent: AbstractFactory?,
        dependencyRequests: List<BindingRequest>
    ) {
        graph = Graph(
            parent = parent?.graph,
            factory = this,
            context = pluginContext,
            factoryModule = ModuleNode(
                key = moduleClass.defaultType
                    .asKey(),
                module = moduleClass,
                accessor = { irGet(moduleVariable) },
                typeParametersMap = emptyMap(),
                moduleLambdaMap = emptyMap()
            ),
            declarationStore = declarationStore,
            symbols = symbols
        )

        factoryExpressions = FactoryExpressions(
            graph = graph,
            pluginContext = pluginContext,
            symbols = symbols,
            members = factoryMembers,
            parent = parent?.factoryExpressions,
            factory = this
        )

        dependencyRequests.forEach { graph.validate(it) }
    }
}
