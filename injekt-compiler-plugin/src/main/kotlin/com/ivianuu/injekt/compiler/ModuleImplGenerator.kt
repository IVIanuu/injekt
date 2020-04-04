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
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
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
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBoolean
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irNull
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.typeUtil.makeNullable

class ModuleImplGenerator(
    pluginContext: IrPluginContext,
    private val modules: MutableList<IrClass>
) : AbstractInjektTransformer(pluginContext) {

    private val moduleImpl = getClass(InjektClassNames.Module)
        .unsubstitutedMemberScope.getContributedClassifier(Name.identifier("Impl"), NoLookupLocation.FROM_BACKEND)!! as ClassDescriptor
    private val scope = getClass(InjektClassNames.Scope)

    override fun visitSimpleFunction(declaration: IrSimpleFunction): IrStatement {
        super.visitSimpleFunction(declaration)
        if (pluginContext.irTrace[InjektWritableSlices.IS_INTO_COMPONENT, declaration] == null &&
            !declaration.descriptor.annotations.hasAnnotation(InjektClassNames.Module)
        ) return declaration

        return moduleImpl(declaration)
    }

    private fun moduleImpl(function: IrSimpleFunction): IrClass {
        val moduleDescriptor = ClassDescriptorImpl(
            function.file.packageFragmentDescriptor,
            function.name,
            Modality.FINAL,
            ClassKind.OBJECT,
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
            IrClassSymbolImpl(moduleDescriptor)
        ).apply clazz@{
            modules += this

            createImplicitParameterDeclarationWithWrappedDescriptor()

            metadata = MetadataSource.Class(moduleDescriptor)

            superTypes = superTypes + moduleImpl.defaultType.toIrType()

            checkNotNull(thisReceiver)

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

            val scopeProperty = moduleImpl.unsubstitutedMemberScope
                .getContributedVariables(
                    Name.identifier("scope"),
                    NoLookupLocation.FROM_BACKEND
                )
                .single()

            addProperty {
                name = Name.identifier("scope")
            }.apply {
                addGetter {
                    returnType = scope.defaultType.makeNullable().toIrType()
                }.apply {
                    overriddenSymbols =
                        overriddenSymbols + symbolTable.referenceSimpleFunction(scopeProperty.getter!!)
                    createParameterDeclarations(scopeProperty.getter!!)
                    dispatchReceiverParameter = thisReceiver!!.deepCopyWithVariables()

                    body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                        val scopeAnnotation =
                            pluginContext.irTrace[InjektWritableSlices.SCOPE, function]
                                ?: function.descriptor.getAnnotatedAnnotations(InjektClassNames.ScopeMarker)
                                    .singleOrNull()?.annotationClass

                        +irReturn(
                            if (scopeAnnotation != null) {
                                irGetObject(
                                    symbolTable.referenceClass(
                                        scopeAnnotation.companionObjectDescriptor!!
                                    )
                                )
                            } else {
                                irNull()
                            }
                        )
                    }
                }
            }

            val invokeOnInit =
                function.descriptor.annotations.findAnnotation(InjektClassNames.Module)
                    ?.let { annotation ->
                        (annotation.argumentValue("invokeOnInit") as? BooleanValue)?.value
                    } == true

            val invokeOnInitProperty = moduleImpl.unsubstitutedMemberScope
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
                val applyFunc = moduleImpl.unsubstitutedMemberScope
                    .findSingleFunction(Name.identifier("apply"))

                overriddenSymbols =
                    overriddenSymbols + symbolTable.referenceSimpleFunction(applyFunc)
                createParameterDeclarations(applyFunc)
                dispatchReceiverParameter = thisReceiver!!.deepCopyWithVariables()

                body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                    val functionBlock = irBlock {
                        function.body?.statements?.forEach { statement ->
                            +statement.deepCopyWithVariables()
                        }
                    }

                    functionBlock.transformChildrenVoid(object : IrElementTransformerVoid() {
                        override fun visitGetValue(expression: IrGetValue): IrExpression {
                            try {
                                if (expression.symbol == function.extensionReceiverParameter!!.symbol) {
                                    return irGet(valueParameters.single())
                                }
                            } catch (e: Exception) {
                            }
                            return super.visitGetValue(expression)
                        }
                    })

                    +functionBlock
                }
            }
        }
    }
}
