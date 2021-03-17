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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.PersistedCallableInfo
import com.ivianuu.injekt.compiler.PersistedClassifierInfo
import com.ivianuu.injekt.compiler.resolution.toCallableRef
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.toPersistedCallableInfo
import com.ivianuu.injekt.compiler.toPersistedClassifierInfo
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.DescriptorVisibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.primaryConstructor
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import java.util.Base64

class InfoTransformer(
    private val context: InjektContext,
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    @Suppress("NewApi")
    override fun visitClass(declaration: IrClass): IrStatement {
        if (declaration.visibility == DescriptorVisibilities.PUBLIC &&
            (declaration.hasAnnotation(InjektFqNames.Given) ||
                    declaration.descriptor.toClassifierRef(context, null)
                        .forTypeKeyTypeParameters.isNotEmpty())) {
            declaration.annotations += DeclarationIrBuilder(pluginContext, declaration.symbol)
                .run {
                    irCall(
                        pluginContext.referenceClass(InjektFqNames.ClassifierInfo)!!
                            .constructors
                            .single()
                    ).apply {
                        val info = declaration.descriptor.toClassifierRef(this@InfoTransformer.context, null)
                            .toPersistedClassifierInfo(this@InfoTransformer.context)
                        val value = Base64.getEncoder()
                            .encode(this@InfoTransformer.context.moshi.adapter(PersistedClassifierInfo::class.java)
                                .toJson(info).toByteArray())
                            .decodeToString()
                        putValueArgument(0, irString(value))
                    }
                }
        }
        return super.visitClass(declaration)
    }

    @Suppress("NewApi")
    override fun visitFunction(declaration: IrFunction): IrStatement {
        if ((declaration.hasAnnotation(InjektFqNames.Given) &&
                    declaration.visibility == DescriptorVisibilities.PUBLIC) || (
                    (declaration is IrConstructor &&
                            declaration.constructedClass.hasAnnotation(InjektFqNames.Given) &&
                            declaration.constructedClass.visibility == DescriptorVisibilities.PUBLIC))) {
                val annotation = DeclarationIrBuilder(pluginContext, declaration.symbol)
                    .run {
                        irCall(
                            pluginContext.referenceClass(InjektFqNames.CallableInfo)!!
                                .constructors
                                .single()
                        ).apply {
                            val info = declaration.descriptor.toCallableRef(this@InfoTransformer.context, null)
                                .toPersistedCallableInfo(this@InfoTransformer.context)
                            val value = Base64.getEncoder()
                                .encode(this@InfoTransformer.context.moshi.adapter(PersistedCallableInfo::class.java)
                                    .toJson(info).toByteArray())
                                .decodeToString()
                            putValueArgument(0, irString(value))
                        }
                    }

                if (declaration is IrConstructor &&
                        declaration.constructedClass.primaryConstructor == declaration) {
                    declaration.constructedClass.annotations += annotation
                } else {
                    declaration.annotations += annotation
                }
        }
        return super.visitFunction(declaration)
    }

    @Suppress("NewApi")
    override fun visitProperty(declaration: IrProperty): IrStatement {
        if (declaration.hasAnnotation(InjektFqNames.Given) &&
                declaration.visibility == DescriptorVisibilities.PUBLIC) {
            declaration.annotations += DeclarationIrBuilder(pluginContext, declaration.symbol)
                .run {
                    irCall(
                        pluginContext.referenceClass(InjektFqNames.CallableInfo)!!
                            .constructors
                            .single()
                    ).apply {
                        val info = declaration.descriptor.toCallableRef(this@InfoTransformer.context, null)
                            .toPersistedCallableInfo(this@InfoTransformer.context)
                        val value = Base64.getEncoder()
                            .encode(this@InfoTransformer.context.moshi.adapter(PersistedCallableInfo::class.java)
                                .toJson(info).toByteArray())
                            .decodeToString()
                        putValueArgument(0, irString(value))
                    }
                }
        }
        return super.visitProperty(declaration)
    }

}
