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

package com.ivianuu.injekt.compiler.backend.readercontextimpl

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.backend.DeclarationGraph
import com.ivianuu.injekt.compiler.backend.IrLowering
import com.ivianuu.injekt.compiler.backend.getConstantFromAnnotationOrNull
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.name.FqName

@Given
class ReaderContextImplTransformer : IrLowering {

    override fun lower() {
        val declarationGraph = given<DeclarationGraph>()
        declarationGraph.rootContextFactories
            .forEach { rootFactory ->
                val factoryFqName = FqName(
                    rootFactory.getConstantFromAnnotationOrNull<String>(
                        InjektFqNames.RootContextFactory, 0
                    )!!
                )

                /*val fileStore = given<IrFileStore>()
                val filePath = fileStore.get(factoryFqName.asString())
                    ?: error("Null for $factoryFqName ${fileStore.map}")
                val file = irModule.files.single { it.path == filePath }

                val factoryImpl = ReaderContextFactoryImplGenerator(
                    name = factoryFqName.shortName(),
                    factoryInterface = rootFactory,
                    factoryType = rootFactory.defaultType,
                    irParent = file,
                    parentContext = null,
                    parentGraph = null,
                    parentExpressions = null
                ).generateFactory()

                file.addChildAndUpdateMetadata(factoryImpl)*/
            }
    }

}
