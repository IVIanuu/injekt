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
import org.jetbrains.kotlin.descriptors.ClassConstructorDescriptor
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.ParameterDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.endOffset
import org.jetbrains.kotlin.ir.util.irConstructorCall
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.startOffset
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

class InjektBindingGenerator(private val context: IrPluginContext) : IrElementVisitorVoid {

    private val symbolTable = context.symbolTable
    private val typeTranslator = context.typeTranslator
    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    private fun getClass(fqName: FqName) =
        context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(fqName))!!

    private val component = getClass(InjektClassNames.Component)
    private val hasScope = getClass(InjektClassNames.HasScope)
    private val isSingle = getClass(InjektClassNames.IsSingle)
    private val key = getClass(InjektClassNames.Key)
    private val linkedBinding = getClass(InjektClassNames.LinkedBinding)
    private val parameters = getClass(InjektClassNames.Parameters)
    private val provider = getClass(InjektClassNames.Provider)
    private val unlinkedBinding = getClass(InjektClassNames.UnlinkedBinding)

    override fun visitElement(element: IrElement) {
    }

    override fun visitClass(declaration: IrClass) {
        val descriptor = declaration.descriptor

        if (!descriptor.annotations.hasAnnotation(InjektClassNames.Factory) &&
            !descriptor.annotations.hasAnnotation(InjektClassNames.Single)
        ) return

        val injektConstructor = descriptor.findInjektConstructor()

        if (injektConstructor.valueParameters.isEmpty()) {
            declaration.addMember(
                linkedBinding(
                    declaration,
                    Name.identifier("Binding"),
                    ClassKind.OBJECT
                )
            )
        } else {
            declaration.addMember(unlinkedBinding(declaration))
        }

        declaration.patchDeclarationParents(declaration.parent)
    }

    private fun unlinkedBinding(declaration: IrClass): IrClass {
        val descriptor = declaration.descriptor

        val bindingDescriptor = ClassDescriptorImpl(
            descriptor,
            Name.identifier("Binding"),
            Modality.FINAL,
            ClassKind.OBJECT,
            emptyList(),
            descriptor.source,
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
            IrClassSymbolImpl(bindingDescriptor)
        ).apply clazz@ {
            createImplicitParameterDeclarationWithWrappedDescriptor()

            val unlinkedBindingWithType = KotlinTypeFactory.simpleType(
                baseType = unlinkedBinding.defaultType,
                arguments = listOf(descriptor.defaultType.asTypeProjection())
            ).toIrType()

            val linkedBindingWithType = KotlinTypeFactory.simpleType(
                baseType = linkedBinding.defaultType,
                arguments = listOf(descriptor.defaultType.asTypeProjection())
            ).toIrType()

            superTypes = superTypes + unlinkedBindingWithType

            if (descriptor.annotations.hasAnnotation(InjektClassNames.Single)) {
                superTypes = superTypes + isSingle.defaultType.toIrType()
            }

            val scopeAnnotation =
                descriptor.getAnnotatedAnnotations(InjektClassNames.Scope).singleOrNull()
            if (scopeAnnotation != null) {
                superTypes = superTypes + hasScope.defaultType.toIrType()

                val scopeCompanion = getClass(scopeAnnotation.fqName!!).companionObjectDescriptor!!

                addProperty {
                    name = Name.identifier("scope")
                }.apply {
                    getter = buildFun {
                        visibility = Visibilities.PUBLIC
                        modality = Modality.FINAL
                        name = Name.identifier("getScope")
                        returnType = scopeCompanion.defaultType.toIrType()
                    }.apply {
                        overriddenSymbols = overriddenSymbols + symbolTable.referenceSimpleFunction(
                            hasScope.unsubstitutedMemberScope
                                .getContributedVariables(
                                    Name.identifier("scope"),
                                    NoLookupLocation.FROM_BACKEND
                                )
                                .single()
                                .getter!!
                        )
                        dispatchReceiverParameter = thisReceiver!!.deepCopyWithVariables()
                        body = DeclarationIrBuilder(context, symbol).irBlockBody {
                            +irReturn(irGetObject(symbolTable.referenceClass(scopeCompanion)))
                        }
                    }
                }
            }

            addConstructor {
                origin = InjektOrigin
                isPrimary = true
                visibility = Visibilities.PRIVATE
            }.apply {
                body = DeclarationIrBuilder(context, symbol).irBlockBody {
                    +IrDelegatingConstructorCallImpl(
                        startOffset, endOffset,
                        context.irBuiltIns.unitType,
                        symbolTable.referenceConstructor(
                            unlinkedBinding.unsubstitutedPrimaryConstructor!!
                        )
                    ).apply {
                        putTypeArgument(0, descriptor.defaultType.toIrType())
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, this@clazz.symbol, context.irBuiltIns.unitType)
                }
            }

            val injektConstructor = descriptor.findInjektConstructor()

            val keyOf = context.moduleDescriptor.getPackage(InjektClassNames.InjektPackage)
                .memberScope.findFirstFunction("keyOf") {
                it.valueParameters.size == 1
            }

            val paramKeyFields = injektConstructor.valueParameters
                .filter { !it.annotations.hasAnnotation(InjektClassNames.Param) }
                .map { param ->
                    val nameClass = param.getAnnotatedAnnotations(InjektClassNames.Name)
                        .singleOrNull()
                        ?.let { nameAnnotation -> getClass(nameAnnotation.fqName!!) }
                        ?.companionObjectDescriptor

                    val fieldName = Name.identifier(param.name.asString() + "Key")

                    val field = addField {
                        name = fieldName
                        type = key.defaultType.toIrType()
                        visibility = Visibilities.PRIVATE
                    }.apply {
                        initializer = DeclarationIrBuilder(context, symbol).irExprBody(
                            DeclarationIrBuilder(context, symbol).irBlock {
                                +irCall(
                                    callee = symbolTable.referenceSimpleFunction(keyOf),
                                    type = key.defaultType.toIrType()
                                ).apply {
                                    putTypeArgument(0, param.type.toIrType())
                                    if (nameClass != null) {
                                        putValueArgument(
                                            0,
                                            irGetObject(symbolTable.referenceClass(nameClass))
                                        )
                                    }
                                }
                            }
                        )
                    }
                    field to param
                }

            val link = unlinkedBinding.unsubstitutedMemberScope
                .findSingleFunction(Name.identifier("link"))

            val getBinding = component.unsubstitutedMemberScope
                .findFirstFunction("getBinding") {
                    it.valueParameters.first().type == key.defaultType
                }

            val linkedBindingClass =
                linkedBinding(declaration, Name.identifier("Linked"), ClassKind.CLASS)

            addFunction(
                name = "link",
                returnType = linkedBindingWithType,
                modality = Modality.FINAL,
                isStatic = false,
                isSuspend = false,
                origin = InjektOrigin
            ).apply {
                overriddenSymbols = overriddenSymbols + symbolTable.referenceSimpleFunction(link)
                createParameterDeclarations(link)
                dispatchReceiverParameter = thisReceiver!!.deepCopyWithVariables()
                body = DeclarationIrBuilder(context, symbol).irBlockBody {
                    val constructor = linkedBindingClass.constructors.first()
                    val newLinkedBindingCall = irConstructorCall(
                        call = irCall(constructor),
                        newFunction = constructor
                    ).apply {
                        paramKeyFields.forEachIndexed { index, (field, param) ->
                            putValueArgument(
                                index,
                                DeclarationIrBuilder(context, symbol).irBlock {
                                    +irCall(
                                        callee = symbolTable.referenceSimpleFunction(getBinding),
                                        type = KotlinTypeFactory.simpleType(
                                            baseType = linkedBinding.defaultType,
                                            arguments = listOf(param.type.asTypeProjection()),
                                            nullable = param.type.isMarkedNullable
                                        ).toIrType()
                                    ).apply {
                                        dispatchReceiver = irGet(valueParameters.first())
                                        putTypeArgument(0, param.type.toIrType())
                                        putValueArgument(0,
                                            irGetField(irGet(dispatchReceiverParameter!!), field)
                                        )
                                    }
                                }
                            )
                        }
                    }

                    +irReturn(newLinkedBindingCall)
                }
            }

            addMember(linkedBindingClass)
        }
    }

    private fun linkedBinding(
        declaration: IrClass,
        name: Name,
        classKind: ClassKind
    ): IrClass {
        val descriptor = declaration.descriptor

        val bindingDescriptor = ClassDescriptorImpl(
            descriptor,
            name,
            Modality.FINAL,
            classKind,
            emptyList(),
            descriptor.source,
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
            IrClassSymbolImpl(bindingDescriptor)
        ).apply clazz@ {
            createImplicitParameterDeclarationWithWrappedDescriptor()

            val linkedBindingWithType = KotlinTypeFactory.simpleType(
                baseType = linkedBinding.defaultType,
                arguments = listOf(descriptor.defaultType.asTypeProjection())
            ).toIrType()

            superTypes = superTypes + linkedBindingWithType

            val injektConstructor = descriptor.findInjektConstructor()

            val paramBindingFields = injektConstructor.valueParameters
                .filter { !it.annotations.hasAnnotation(InjektClassNames.Param) }
                .map { param ->
                    val fieldName = Name.identifier(param.name.asString() + "Provider")
                    addField {
                        this.name = fieldName
                        type = KotlinTypeFactory.simpleType(
                            baseType = provider.defaultType,
                            arguments = listOf(param.type.asTypeProjection()),
                            nullable = param.type.isMarkedNullable
                        ).toIrType()
                        visibility = Visibilities.PRIVATE
                    }
                }

            addConstructor {
                origin = InjektOrigin
                isPrimary = true
                visibility = Visibilities.PRIVATE

            }.apply {
                paramBindingFields.forEachIndexed { index, field ->
                    addValueParameter {
                        this.index = index
                        this.name = field.name
                        type = field.type
                    }
                }

                body = DeclarationIrBuilder(context, symbol).irBlockBody {
                    +IrDelegatingConstructorCallImpl(
                        startOffset, endOffset,
                        context.irBuiltIns.unitType,
                        symbolTable.referenceConstructor(linkedBinding.unsubstitutedPrimaryConstructor!!)
                    ).apply {
                        putTypeArgument(0, descriptor.defaultType.toIrType())
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, this@clazz.symbol, context.irBuiltIns.unitType)
                    paramBindingFields.forEachIndexed { index, field ->
                        +irSetField(irGet(thisReceiver!!), field, irGet(valueParameters[index]))
                    }
                }
            }

            val invoke = linkedBinding.unsubstitutedMemberScope
                .findSingleFunction(Name.identifier("invoke"))

            val parametersGet = parameters.unsubstitutedMemberScope
                .findSingleFunction(Name.identifier("get"))

            addFunction(
                name = "invoke",
                returnType = declaration.defaultType,
                modality = Modality.FINAL,
                isStatic = false,
                isSuspend = false,
                origin = InjektOrigin
            ).apply {
                overriddenSymbols = overriddenSymbols + symbolTable.referenceSimpleFunction(invoke)
                createParameterDeclarations(invoke)
                dispatchReceiverParameter = thisReceiver!!.deepCopyWithVariables()
                body = DeclarationIrBuilder(context, symbol).irBlockBody {
                    val getInstanceCall = if (descriptor.kind == ClassKind.OBJECT) {
                        irGetObject(symbolTable.referenceClass(descriptor))
                    } else {
                        irCall(
                            callee = symbolTable.referenceConstructor(injektConstructor),
                            type = injektConstructor.returnType.toIrType()
                        ).apply {
                            var paramIndex = 0
                            injektConstructor.valueParameters.forEachIndexed { index, param ->
                                val paramType =
                                    injektConstructor.valueParameters[index].type.toIrType()
                                val expr =
                                    if (param.annotations.hasAnnotation(InjektClassNames.Param)) {
                                        DeclarationIrBuilder(context, symbol).irBlock {
                                            +irCall(
                                                callee = symbolTable.referenceSimpleFunction(
                                                    parametersGet
                                                ),
                                                type = paramType
                                            ).apply {
                                                dispatchReceiver = irGet(valueParameters.first())
                                                putTypeArgument(0, paramType)
                                                putValueArgument(0, irInt(paramIndex))
                                            }
                                            ++paramIndex
                                    }
                                } else {
                                    DeclarationIrBuilder(context, symbol).irBlock {
                                        +irCall(
                                            callee = symbolTable.referenceSimpleFunction(invoke),
                                            type = paramType
                                        ).apply {
                                            dispatchReceiver = irGetField(
                                                irGet(dispatchReceiverParameter!!),
                                                paramBindingFields.single { it.name.asString().startsWith(param.name.asString()) }
                                            )
                                        }
                                    }
                                }

                                putValueArgument(
                                    index = index,
                                    valueArgument = expr
                                )
                            }
                        }
                    }

                    +irReturn(getInstanceCall)
                }
            }
        }
    }

    private fun ClassDescriptor.findInjektConstructor(): ClassConstructorDescriptor {
        return constructors.singleOrNull { it.annotations.hasAnnotation(InjektClassNames.InjektConstructor) }
            ?: unsubstitutedPrimaryConstructor!!
    }

    private fun IrFunction.createParameterDeclarations(descriptor: FunctionDescriptor) {
        fun ParameterDescriptor.irValueParameter() = IrValueParameterImpl(
            this.startOffset ?: UNDEFINED_OFFSET,
            this.endOffset ?: UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            this,
            type.toIrType(),
            (this as? ValueParameterDescriptor)?.varargElementType?.toIrType()
        ).also {
            it.parent = this@createParameterDeclarations
        }

        fun TypeParameterDescriptor.irTypeParameter() = IrTypeParameterImpl(
            this.startOffset ?: UNDEFINED_OFFSET,
            this.endOffset ?: UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            IrTypeParameterSymbolImpl(this)
        ).also {
            it.parent = this@createParameterDeclarations
        }

        dispatchReceiverParameter = descriptor.dispatchReceiverParameter?.irValueParameter()
        extensionReceiverParameter = descriptor.extensionReceiverParameter?.irValueParameter()

        assert(valueParameters.isEmpty()) { "params ${valueParameters.map { it.name }}" }
        valueParameters = descriptor.valueParameters.map { it.irValueParameter() }

        assert(typeParameters.isEmpty()) { "types ${typeParameters.map { it.name }}" }
        typeParameters + descriptor.typeParameters.map { it.irTypeParameter() }
    }
}

object InjektOrigin : IrDeclarationOrigin
