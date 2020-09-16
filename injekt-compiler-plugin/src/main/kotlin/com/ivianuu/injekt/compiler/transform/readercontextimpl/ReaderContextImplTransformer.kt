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

package com.ivianuu.injekt.compiler.transform.readercontextimpl

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.addChildAndUpdateMetadata
import com.ivianuu.injekt.compiler.addFile
import com.ivianuu.injekt.compiler.getConstantFromAnnotationOrNull
import com.ivianuu.injekt.compiler.recordLookup
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.ReaderContextParamTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrDeclarationWithName
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.FqName

class ReaderContextImplTransformer(
    pluginContext: IrPluginContext,
    private val declarationGraph: DeclarationGraph,
    private val readerContextParamTransformer: ReaderContextParamTransformer,
    private val initTrigger: IrDeclarationWithName
) : AbstractInjektTransformer(pluginContext) {

    override fun lower() {
        declarationGraph.rootContextFactories
            .forEach { rootFactory ->
                val factoryFqName = FqName(
                    rootFactory.getConstantFromAnnotationOrNull<String>(
                        InjektFqNames.RootContextFactory, 0
                    )!!
                )

                val file = module.addFile(pluginContext, factoryFqName)

                val factoryImpl = ReaderContextFactoryImplGenerator(
                    pluginContext = pluginContext,
                    name = factoryFqName.shortName(),
                    factoryInterface = rootFactory,
                    factoryType = rootFactory.defaultType,
                    irParent = file,
                    declarationGraph = declarationGraph,
                    readerContextParamTransformer = readerContextParamTransformer,
                    parentContext = null,
                    initTrigger = initTrigger,
                    parentGraph = null,
                    parentExpressions = null
                ).generateFactory()

                file.addChildAndUpdateMetadata(factoryImpl)

                recordLookup(initTrigger, factoryImpl)
            }
    }

}
