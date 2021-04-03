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
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.resolution.toCallableRef
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.toPersistedCallableInfo
import com.ivianuu.injekt.compiler.toPersistedClassifierInfo
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.annotations.AnnotatedImpl
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptorImpl
import org.jetbrains.kotlin.descriptors.annotations.Annotations
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.resolve.constants.StringValue
import java.lang.reflect.Field
import java.lang.reflect.Modifier
import java.util.Base64

class InfoTransformer(
    private val context: InjektContext,
    private val pluginContext: IrPluginContext
) : IrElementTransformerVoid() {

    @Suppress("NewApi")
    override fun visitClass(declaration: IrClass): IrStatement {
        declaration.annotations += DeclarationIrBuilder(pluginContext, declaration.symbol)
            .run {
                val info = declaration.descriptor.toClassifierRef(this@InfoTransformer.context,
                    null)
                    .toPersistedClassifierInfo(this@InfoTransformer.context)
                val serializedValue = Base64.getEncoder()
                    .encode(this@InfoTransformer.context.moshi.adapter(
                        PersistedClassifierInfo::class.java)
                        .toJson(info).toByteArray())
                    .decodeToString()
                irCall(
                    pluginContext.referenceClass(InjektFqNames.ClassifierInfo)!!
                        .constructors
                        .single()
                ).apply {

                    putValueArgument(0, irString(serializedValue))
                }
            }
        return super.visitClass(declaration)
    }

    @Suppress("NewApi")
    override fun visitFunction(declaration: IrFunction): IrStatement {
        val descriptor = declaration.descriptor
        if (descriptor is AnnotatedImpl) {
            val info = declaration.descriptor.toCallableRef(this@InfoTransformer.context, null)
                .toPersistedCallableInfo(this@InfoTransformer.context)
            val serializedInfo = Base64.getEncoder()
                .encode(this@InfoTransformer.context.moshi.adapter(PersistedCallableInfo::class.java)
                    .toJson(info).toByteArray())
                .decodeToString()

            val field = AnnotatedImpl::class.java.declaredFields
                .single { it.name == "annotations" }
            field.isAccessible = true
            val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
            field.set(
                descriptor,
                Annotations.create(
                    descriptor.annotations.toList() + AnnotationDescriptorImpl(
                        pluginContext.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(
                            InjektFqNames.CallableInfo
                        ))!!.defaultType,
                        mapOf(
                            "value".asNameId() to StringValue(serializedInfo)
                        ),
                        SourceElement.NO_SOURCE
                    )
                )
            )

            declaration.annotations += DeclarationIrBuilder(pluginContext, declaration.symbol)
                .run {
                    irCall(
                        pluginContext.referenceClass(InjektFqNames.CallableInfo)!!
                            .constructors
                            .single()
                    ).apply {

                        putValueArgument(0, irString(serializedInfo))
                    }
                }
        }
        return super.visitFunction(declaration)
    }

    @Suppress("NewApi")
    override fun visitProperty(declaration: IrProperty): IrStatement {
        val descriptor = declaration.descriptor
        if (descriptor is AnnotatedImpl) {
            val info = declaration.descriptor.toCallableRef(this@InfoTransformer.context, null)
                .toPersistedCallableInfo(this@InfoTransformer.context)
            val serializedInfo = Base64.getEncoder()
                .encode(this@InfoTransformer.context.moshi.adapter(PersistedCallableInfo::class.java)
                    .toJson(info).toByteArray())
                .decodeToString()

            val field = AnnotatedImpl::class.java.declaredFields
                .single { it.name == "annotations" }
            field.isAccessible = true
            val modifiersField: Field = Field::class.java.getDeclaredField("modifiers")
            modifiersField.isAccessible = true
            modifiersField.setInt(field, field.modifiers and Modifier.FINAL.inv())
            field.set(
                descriptor,
                Annotations.create(
                    descriptor.annotations.toList() + AnnotationDescriptorImpl(
                        pluginContext.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(
                            InjektFqNames.CallableInfo
                        ))!!.defaultType,
                        mapOf(
                            "value".asNameId() to StringValue(serializedInfo)
                        ),
                        SourceElement.NO_SOURCE
                    )
                )
            )

            declaration.annotations += DeclarationIrBuilder(pluginContext, declaration.symbol)
                .run {
                    irCall(
                        pluginContext.referenceClass(InjektFqNames.CallableInfo)!!
                            .constructors
                            .single()
                    ).apply {

                        putValueArgument(0, irString(serializedInfo))
                    }
                }
        }
        return super.visitProperty(declaration)
    }

}
