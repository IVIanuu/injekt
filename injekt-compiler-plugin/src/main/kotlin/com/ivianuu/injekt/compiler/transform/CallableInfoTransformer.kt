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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.DeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.PersistedCallableInfo
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import java.util.Base64

class CallableInfoTransformer(
    private val declarationStore: DeclarationStore,
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    @Suppress("NewApi")
    override fun visitFunction(declaration: IrFunction): IrStatement {
        if (declaration.hasAnnotation(InjektFqNames.Given) ||
            declaration.hasAnnotation(InjektFqNames.GivenSetElement) ||
            declaration.hasAnnotation(InjektFqNames.Module) ||
            declaration.hasAnnotation(InjektFqNames.Interceptor) ||
            declaration.hasAnnotation(InjektFqNames.GivenFun)) {
            declaration.annotations += DeclarationIrBuilder(pluginContext, declaration.symbol)
                .run {
                    irCall(
                        pluginContext.referenceClass(InjektFqNames.CallableInfo)!!
                            .constructors
                            .single()
                    ).apply {
                        val info = declarationStore.callableInfoFor(declaration.descriptor)
                        val value = Base64.getEncoder()
                            .encode(declarationStore.moshi.adapter(PersistedCallableInfo::class.java)
                                .toJson(info).toByteArray())
                            .decodeToString()
                        putValueArgument(
                            0,
                            irString(value)
                        )
                    }
                }
        }
        return super.visitFunction(declaration)
    }

}
