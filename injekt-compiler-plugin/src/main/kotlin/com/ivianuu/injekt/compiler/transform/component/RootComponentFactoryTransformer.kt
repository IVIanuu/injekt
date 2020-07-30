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

package com.ivianuu.injekt.compiler.transform.component

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.addFile
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.getClassFromAnnotation
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPackageFragment

class RootComponentFactoryTransformer(
    pluginContext: IrPluginContext,
    private val declarationGraph: DeclarationGraph
) : AbstractInjektTransformer(pluginContext) {

    override fun lower() {
        val rootComponentFactories = declarationGraph.rootComponentFactories.groupBy {
            it.functions
                .filterNot { it.isFakeOverride }
                .single()
                .returnType
        }

        val entryPoints = declarationGraph.entryPoints.groupBy {
            it.getClassFromAnnotation(
                InjektFqNames.EntryPoint,
                0,
                pluginContext
            )
        }

        rootComponentFactories.values
            .flatten()
            .forEach { componentFactory ->
                val component = componentFactory.functions
                    .filterNot { it.isFakeOverride }
                    .single()
                    .returnType
                    .classOrNull!!
                    .owner

                val file = module.addFile(
                    pluginContext,
                    componentFactory.getPackageFragment()!!.fqName
                        .child((componentFactory.name.asString() + "Impl").asNameId())
                )

                val componentFactoryImpl = ComponentFactoryImpl(
                    file,
                    componentFactory,
                    entryPoints.getOrElse(component) { emptyList() },
                    null,
                    pluginContext,
                    declarationGraph,
                    symbols
                )

                componentFactoryImpl.init()

                file.addChild(componentFactoryImpl.clazz)
            }
    }

}
