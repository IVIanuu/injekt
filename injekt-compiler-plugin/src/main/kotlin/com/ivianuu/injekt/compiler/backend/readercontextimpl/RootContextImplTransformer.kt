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
import com.ivianuu.injekt.compiler.InjektAttributes
import com.ivianuu.injekt.compiler.backend.IrLowering
import com.ivianuu.injekt.compiler.backend.irModule
import com.ivianuu.injekt.compiler.backend.pluginContext
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given
class RootContextImplTransformer : IrLowering {

    override fun lower() {
        irModule.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                val isRootFactoryImpl = given<InjektAttributes>()[
                        InjektAttributes.IsRootFactory(declaration.descriptor.fqNameSafe)]
                return if (isRootFactoryImpl != null) {
                    val factoryInterface = FqName(
                        declaration.descriptor.fqNameSafe.asString()
                            .removeSuffix("Impl")
                    )
                        .let { pluginContext.referenceClass(it) }!!.owner
                    ReaderContextFactoryImplGenerator(
                        name = declaration.name,
                        factoryInterface = factoryInterface,
                        factoryType = factoryInterface.defaultType,
                        irParent = declaration.parent,
                        parentContext = null,
                        parentGraph = null,
                        parentExpressions = null
                    ).generateFactory()
                } else {
                    super.visitClass(declaration)
                }
            }
        })
    }

}
