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
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrClassReferenceImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
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

        val intoComponentFunctions = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.origin == InjektOrigin ||
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

    private fun autoServiceAnnotation(): IrConstructorCallImpl {
        val autoServiceAnnotation =
            pluginContext.moduleDescriptor.findClassAcrossModuleDependencies(
                ClassId.topLevel(FqName("com.google.auto.service.AutoService"))
            )!!
        return IrConstructorCallImpl(
            UNDEFINED_OFFSET, UNDEFINED_OFFSET,
            autoServiceAnnotation.defaultType.toIrType(),
            symbolTable.referenceConstructor(autoServiceAnnotation.constructors.single()!!),
            0,
            0,
            1,
            null
        ).apply {
            putValueArgument(
                0,
                IrClassReferenceImpl(
                    startOffset, endOffset,
                    pluginContext.irBuiltIns.kClassClass.descriptor.defaultType.toIrType(),
                    pluginContext.irBuiltIns.kClassClass,
                    componentBuilderContributor.defaultType.toIrType()
                )
            )
        }
    }

    private fun componentBuilderContributor(
        file: IrFile,
        function: IrFunction
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

            annotations += autoServiceAnnotation()

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
                        function.descriptor.getAnnotatedAnnotations(InjektClassNames.ScopeMarker)
                            .singleOrNull()

                    if (scopeAnnotation != null) {
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
                                    dispatchReceiver = irGet(this@func.valueParameters[0])
                                }

                                putValueArgument(
                                    0,
                                    irGetObject(
                                        symbolTable.referenceClass(
                                            getClass(scopeAnnotation.fqName!!).companionObjectDescriptor!!
                                        )
                                    )
                                )
                            },
                            thenPart = functionCall
                        )
                    } else {
                        +functionCall
                    }
                }
            }
        }
    }
}
