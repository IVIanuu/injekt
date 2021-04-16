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

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.descriptors.annotations.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.constants.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.lang.reflect.*

class InfoTransformer(
    private val context: InjektContext,
    private val pluginContext: IrPluginContext,
    private val trace: BindingTrace
) : IrElementTransformerVoid() {
    @Suppress("NewApi")
    override fun visitClass(declaration: IrClass): IrStatement {
        val classifierRef = declaration.descriptor.toClassifierRef(context, trace)
        if (declaration.hasAnnotation(InjektFqNames.Given) ||
            classifierRef.typeParameters.any { it.isForTypeKey || it.isGivenConstraint } ||
            declaration.declarations.any { it.descriptor.isGiven(context, trace) }) {
            declaration.annotations += DeclarationIrBuilder(pluginContext, declaration.symbol)
                .run {
                    val info = declaration.descriptor.toClassifierRef(this@InfoTransformer.context,
                        null)
                        .toPersistedClassifierInfo(this@InfoTransformer.context, trace)
                    val serializedValue = info.encode()
                    irCall(
                        pluginContext.referenceClass(InjektFqNames.ClassifierInfo)!!
                            .constructors
                            .single()
                    ).apply {
                        putValueArgument(0, irString(serializedValue))
                    }
                }
        }
        return super.visitClass(declaration)
    }

    @Suppress("NewApi")
    override fun visitFunction(declaration: IrFunction): IrStatement {
        if (declaration.descriptor is PropertyAccessorDescriptor) return super.visitFunction(declaration)
        addInfoToCallableIfNeeded(declaration, declaration.symbol)
        return super.visitFunction(declaration)
    }

    @Suppress("NewApi")
    override fun visitProperty(declaration: IrProperty): IrStatement {
        addInfoToCallableIfNeeded(declaration, declaration.symbol)
        return super.visitProperty(declaration)
    }

    private fun addInfoToCallableIfNeeded(
        declaration: IrDeclarationWithName,
        symbol: IrSymbol
    ) {
        val descriptor = declaration.descriptor as CallableDescriptor
        if (descriptor !is AnnotatedImpl) return
        var needsInfo = declaration.hasAnnotation(InjektFqNames.Given) ||
                (declaration is IrConstructor &&
                        declaration.constructedClass.hasAnnotation(InjektFqNames.Given))
        if (!needsInfo) {
            needsInfo = declaration
                .safeAs<IrFunction>()
                ?.valueParameters
                ?.any { it.descriptor.isGiven(context, trace) } == true
        }
        val callableRef = descriptor.toCallableRef(context, trace)
        if (!needsInfo) {
            callableRef.type.anyType {
                it.qualifiers.isNotEmpty()
            } || callableRef.parameterTypes.any { (_, parameterType) ->
                parameterType.anyType {
                    it.qualifiers.isNotEmpty() ||
                            (it.classifier.isTypeAlias &&
                                    it.fullyExpandedType.isSuspendFunctionType)
                }
            }
        }
        if (!needsInfo) return
        val info = callableRef.toPersistedCallableInfo(this@InfoTransformer.context, trace)
        val serializedValue = info.encode()

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
                    mapOf("value".asNameId() to StringValue(serializedValue)),
                    SourceElement.NO_SOURCE
                )
            )
        )

        declaration.annotations += DeclarationIrBuilder(pluginContext, symbol)
            .run {
                irCall(
                    pluginContext.referenceClass(InjektFqNames.CallableInfo)!!
                        .constructors
                        .single()
                ).apply {
                    putValueArgument(0, irString(serializedValue))
                }
            }
    }
}
