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

package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager

class ComponentBuilderContributorGenerator(
    pluginContext: IrPluginContext,
    private val serviceLoaderFileWriter: ServiceLoaderFileWriter
) : AbstractInjektTransformer(pluginContext) {

    private val componentBuilderContributor = getClass(InjektClassNames.ComponentBuilderContributor)
    private val componentBuilder = getClass(InjektClassNames.ComponentBuilder)

    override fun visitFile(declaration: IrFile): IrFile {
        super.visitFile(declaration)

        val intoComponentFunctions = mutableListOf<IrSimpleFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
                if (pluginContext.irTrace[InjektWritableSlices.IS_INTO_COMPONENT, declaration] != null ||
                    declaration.descriptor.annotations.hasAnnotation(InjektClassNames.IntoComponent)
                ) {
                    intoComponentFunctions += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        intoComponentFunctions.forEach {
            val contributor = componentBuilderContributor(declaration, it)
            declaration.addChild(contributor)
            serviceLoaderFileWriter.add(contributor.fqNameForIrSerialization)
        }

        return declaration
    }

    private fun componentBuilderContributor(
        file: IrFile,
        function: IrSimpleFunction
    ): IrClass {
        val componentBuilderContributorDescriptor = ClassDescriptorImpl(
            file.packageFragmentDescriptor,
            Name.identifier(
                function.fqNameForIrSerialization.asString().replace(
                    ".",
                    "_"
                )
            ), // todo fix name
            Modality.FINAL,
            ClassKind.CLASS,
            emptyList(),
            SourceElement.NO_SOURCE,
            false,
            LockBasedStorageManager.NO_LOCKS
        ).apply {
            initialize(
                MemberScope.Empty,
                emptySet(),
                null
            )
        }

        return IrClassImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            InjektOrigin,
            IrClassSymbolImpl(componentBuilderContributorDescriptor)
        ).apply clazz@{
            createImplicitParameterDeclarationWithWrappedDescriptor()

            superTypes = superTypes + componentBuilderContributor.defaultType.toIrType()

            addConstructor {
                origin = InjektOrigin
                isPrimary = true
                visibility = Visibilities.PUBLIC
            }.apply {
                body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                    +IrDelegatingConstructorCallImpl(
                        startOffset, endOffset,
                        context.irBuiltIns.unitType,
                        symbolTable.referenceConstructor(
                            context.builtIns.any.unsubstitutedPrimaryConstructor!!
                        )
                    )
                    +IrInstanceInitializerCallImpl(
                        startOffset,
                        endOffset,
                        this@clazz.symbol,
                        context.irBuiltIns.unitType
                    )
                }
            }

            val invokeOnInit =
                function.descriptor.annotations.findAnnotation(InjektClassNames.IntoComponent)?.let { annotation ->
                    (annotation.argumentValue("invokeOnInit") as? BooleanValue)?.value
                } == true

            val invokeOnInitProperty = componentBuilderContributor.unsubstitutedMemberScope
                .getContributedVariables(
                    Name.identifier("invokeOnInit"),
                    NoLookupLocation.FROM_BACKEND
                )
                .single()

            addProperty {
                name = Name.identifier("invokeOnInit")
            }.apply {
                addGetter {
                    returnType = pluginContext.irBuiltIns.booleanType
                }.apply {
                    overriddenSymbols =
                        overriddenSymbols + symbolTable.referenceSimpleFunction(invokeOnInitProperty.getter!!)
                    createParameterDeclarations(invokeOnInitProperty.getter!!)
                    dispatchReceiverParameter = thisReceiver!!.deepCopyWithVariables()

                    body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                        +irReturn(irBoolean(invokeOnInit))
                    }
                }
            }

            addFunction(
                name = "apply",
                returnType = pluginContext.irBuiltIns.unitType,
                modality = Modality.FINAL,
                isStatic = false,
                isSuspend = false,
                origin = InjektOrigin
            ).apply func@{
                val applyFunc = componentBuilderContributor.unsubstitutedMemberScope
                    .findSingleFunction(Name.identifier("apply"))

                overriddenSymbols =
                    overriddenSymbols + symbolTable.referenceSimpleFunction(applyFunc)
                createParameterDeclarations(applyFunc)
                dispatchReceiverParameter = thisReceiver!!.deepCopyWithVariables()

                body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                    val functionCall = irCall(function).apply {
                        if (function.dispatchReceiverParameter != null) {
                            dispatchReceiver = irGetObject(
                                symbolTable.referenceClass(
                                    function.dispatchReceiverParameter!!.descriptor
                                        .type.constructor.declarationDescriptor as ClassDescriptor
                                )
                            )
                        }

                        if (function.extensionReceiverParameter != null) {
                            extensionReceiver = irGet(this@func.valueParameters[0])
                        } else {
                            putValueArgument(0, irGet(this@func.valueParameters[0]))
                        }
                    }

                    val scopeAnnotation =
                        pluginContext.irTrace[InjektWritableSlices.SCOPE, function]
                            ?: function.descriptor.getAnnotatedAnnotations(InjektClassNames.ScopeMarker)
                                .singleOrNull()?.annotationClass

                    if (scopeAnnotation == null) {
                        +functionCall
                    } else {
                        val onPreBuildFunction = componentBuilder.unsubstitutedMemberScope
                            .findSingleFunction(Name.identifier("onPreBuild"))
                        +irCall(
                            callee = symbolTable.referenceSimpleFunction(onPreBuildFunction),
                            type = pluginContext.irBuiltIns.unitType
                        ).apply {
                            dispatchReceiver = irGet(this@func.valueParameters[0])
                            putValueArgument(
                                0,
                                irLambdaExpression(
                                    descriptor = createFunctionDescriptor(onPreBuildFunction.valueParameters.single().type),
                                    type = onPreBuildFunction.valueParameters.single().type.toIrType()
                                ) {
                                    +DeclarationIrBuilder(context, symbol).irIfThen(
                                        condition = irCall(
                                            callee = symbolTable.referenceSimpleFunction(
                                                pluginContext.builtIns.list.unsubstitutedMemberScope
                                                    .findSingleFunction(Name.identifier("contains"))
                                            ),
                                            type = pluginContext.irBuiltIns.booleanType
                                        ).apply {
                                            dispatchReceiver = irCall(
                                                callee = symbolTable.referenceSimpleFunction(
                                                    componentBuilder.unsubstitutedMemberScope
                                                        .getContributedVariables(
                                                            Name.identifier("scopes"),
                                                            NoLookupLocation.FROM_BACKEND
                                                        )
                                                        .single()
                                                        .getter!!
                                                ),
                                                type = context.builtIns.list.defaultType.toIrType()
                                            ).apply {
                                                dispatchReceiver =
                                                    irGet(this@func.valueParameters[0])
                                            }

                                            putValueArgument(
                                                0,
                                                irGetObject(
                                                    symbolTable.referenceClass(
                                                        scopeAnnotation.companionObjectDescriptor!!
                                                    )
                                                )
                                            )
                                        },
                                        thenPart = functionCall
                                    )

                                    +irReturn(irBoolean(false))
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
