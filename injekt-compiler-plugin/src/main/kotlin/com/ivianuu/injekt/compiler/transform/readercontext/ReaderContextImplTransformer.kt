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

package com.ivianuu.injekt.compiler.transform.readercontext

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.addFile
import com.ivianuu.injekt.compiler.getConstantFromAnnotationOrNull
import com.ivianuu.injekt.compiler.recordLookup
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.transform.implicit.ImplicitContextParamTransformer
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.name.FqName

class ReaderContextImplTransformer(
    injektContext: InjektContext,
    private val declarationGraph: DeclarationGraph,
    private val implicitContextParamTransformer: ImplicitContextParamTransformer,
    private val initFile: IrFile
) : AbstractInjektTransformer(injektContext) {

    override fun lower() {
        declarationGraph.rootContexts
            .forEach { index ->
                val factoryFqName = FqName(
                    index.getConstantFromAnnotationOrNull<String>(
                        InjektFqNames.RootContext, 0
                    )!!
                )

                val factoryInterface = index.superTypes[0].classOrNull!!.owner

                val file = injektContext.module.addFile(injektContext, factoryFqName)

                val factoryImpl = ReaderContextFactoryImplGenerator(
                    injektContext,
                    factoryFqName.shortName(),
                    factoryInterface,
                    file,
                    declarationGraph,
                    implicitContextParamTransformer
                ).generateFactory()

                file.addChild(factoryImpl)

                recordLookup(initFile, factoryImpl)
            }
    }

}

typealias ContextExpression = IrBuilderWithScope.(() -> IrExpression) -> IrExpression
