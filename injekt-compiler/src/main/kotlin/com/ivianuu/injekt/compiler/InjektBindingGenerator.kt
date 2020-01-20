/*
 * Copyright 2019 Manuel Wrage
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
import org.jetbrains.kotlin.descriptors.computeConstructorTypeParameters
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.findTypeAliasAcrossModuleDependencies
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
import org.jetbrains.kotlin.ir.builders.irTemporary
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
import org.jetbrains.kotlin.types.typeUtil.makeNullable

class InjektBindingGenerator(private val context: IrPluginContext) : IrElementVisitorVoid {

    private val symbolTable = context.symbolTable
    private val typeTranslator = context.typeTranslator
    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    override fun visitElement(element: IrElement) {
    }

    override fun visitClass(declaration: IrClass) {
        val descriptor = declaration.descriptor

        if (!descriptor.annotations.hasAnnotation(FactoryAnnotation) &&
            !descriptor.annotations.hasAnnotation(SingleAnnotation)) return

        declaration.addMember(unlinkedBinding(declaration))
        declaration.patchDeclarationParents(declaration.parent)

        //error("declaration ${declaration.dump()}")
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

            val unlinkedBinding = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(InjektClassNames.UnlinkedBinding))!!
            val unlinkedBindingWithType = KotlinTypeFactory.simpleType(
                baseType = unlinkedBinding.defaultType,
                arguments = listOf(descriptor.defaultType.asTypeProjection())
            ).toIrType()

            val linkedBinding = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(InjektClassNames.LinkedBinding))!!
            linkedBinding.computeConstructorTypeParameters()

            val linkedBindingWithType = KotlinTypeFactory.simpleType(
                baseType = linkedBinding.defaultType,
                arguments = listOf(descriptor.defaultType.asTypeProjection())
            ).toIrType()

            superTypes += unlinkedBindingWithType

            if (descriptor.annotations.hasAnnotation(SingleAnnotation)) {
                superTypes += context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(InjektClassNames.IsSingle))!!
                    .defaultType.toIrType()
            }

            val scopeAnnotation = descriptor.getAnnotatedAnnotations(ScopeAnnotation).singleOrNull()
            if (scopeAnnotation != null) {
                val hasScope = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(InjektClassNames.HasScope))!!
                superTypes += hasScope.defaultType.toIrType()

                val scopeCompanion = context.moduleDescriptor.findClassAcrossModuleDependencies(
                    ClassId.topLevel(scopeAnnotation.fqName!!))!!.companionObjectDescriptor!!

                addProperty {
                    name = Name.identifier("scope")
                }.apply {
                    getter = buildFun {
                        visibility = Visibilities.PUBLIC
                        modality = Modality.FINAL
                        name = Name.identifier("getScope")
                        returnType = scopeCompanion.defaultType.toIrType()
                    }.apply {
                        overriddenSymbols += symbolTable.referenceSimpleFunction(
                            hasScope.unsubstitutedMemberScope
                                .getContributedVariables(Name.identifier("scope"), NoLookupLocation.FROM_BACKEND)
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

            val key = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(InjektClassNames.Key))!!

            val keyOf = context.moduleDescriptor.getPackage(InjektClassNames.InjektPackage)
                .memberScope.findFirstFunction("keyOf") {
                it.valueParameters.size == 1
            }

            val paramKeyFields = injektConstructor.valueParameters
                .filter { !it.annotations.hasAnnotation(ParamAnnotation) }
                .map { param ->
                    val nameClass = param.getAnnotatedAnnotations(NameAnnotation)
                        .singleOrNull()
                        ?.let { nameAnnotation ->
                            context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(nameAnnotation.fqName!!))
                        }?.companionObjectDescriptor

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

            val linker = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(InjektClassNames.Linker))!!
            val get = linker.unsubstitutedMemberScope
                .findFirstFunction("get") {
                    it.valueParameters.first().type == key.defaultType
                }

            val getOrNull = linker.unsubstitutedMemberScope
                .findFirstFunction("getOrNull") {
                    it.valueParameters.first().type == key.defaultType
                }

            val linkedBindingClass = linkedBinding(declaration)

            addFunction(
                name = "link",
                returnType = linkedBindingWithType,
                modality = Modality.FINAL,
                isStatic = false,
                isSuspend = false,
                origin = InjektOrigin
            ).apply {
                overriddenSymbols += symbolTable.referenceSimpleFunction(link)
                createParameterDeclarations(link)
                body = DeclarationIrBuilder(context, symbol).irBlockBody {
                    val constructor = linkedBindingClass.constructors.first()
                    val newLinkedBindingCall = irConstructorCall(
                        call = irCall(constructor),
                        newFunction = constructor
                    ).apply {
                        paramKeyFields.forEachIndexed { index, (field, param) ->
                            val optional = param.annotations.hasAnnotation(OptionalAnnotation)
                            putValueArgument(
                                index,
                                DeclarationIrBuilder(context, symbol).irBlock {
                                    +irCall(
                                        callee = symbolTable.referenceSimpleFunction(if (optional) getOrNull else get),
                                        type = KotlinTypeFactory.simpleType(
                                            baseType = linkedBinding.defaultType,
                                            arguments = listOf(param.type.asTypeProjection()),
                                            nullable = optional
                                        ).toIrType()
                                    ).apply {
                                        dispatchReceiver = irGet(valueParameters.first())
                                        putTypeArgument(0, param.type.toIrType())
                                        putValueArgument(0, irGetField(irGet(dispatchReceiverParameter!!), field))
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

    private fun linkedBinding(declaration: IrClass): IrClass {
        val descriptor = declaration.descriptor

        val bindingDescriptor = ClassDescriptorImpl(
            descriptor,
            Name.identifier("Linked"),
            Modality.FINAL,
            ClassKind.CLASS,
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

            val linkedBinding = context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(InjektClassNames.LinkedBinding))!!

            val linkedBindingWithType = KotlinTypeFactory.simpleType(
                baseType = linkedBinding.defaultType,
                arguments = listOf(descriptor.defaultType.asTypeProjection())
            ).toIrType()

            superTypes += linkedBindingWithType

            val injektConstructor = descriptor.findInjektConstructor()

            val paramBindingFields = injektConstructor.valueParameters
                .filter { !it.annotations.hasAnnotation(ParamAnnotation) }
                .map { param ->
                    val fieldName = Name.identifier(param.name.asString() + "Binding")
                    addField {
                        name = fieldName
                        type = KotlinTypeFactory.simpleType(
                            baseType = linkedBinding.defaultType,
                            arguments = listOf(param.type.asTypeProjection()),
                            nullable = param.annotations.hasAnnotation(OptionalAnnotation)
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
                        name = field.name
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

            val parametersDefinition =
                context.moduleDescriptor.findTypeAliasAcrossModuleDependencies(ClassId.topLevel(InjektClassNames.ParametersDefinition))!!
            val parametersDefinitionInvoke = parametersDefinition.classDescriptor!!.unsubstitutedMemberScope
                .findSingleFunction(Name.identifier("invoke"))
            val parameters =
                context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(InjektClassNames.Parameters))!!
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
                overriddenSymbols += symbolTable.referenceSimpleFunction(invoke)
                createParameterDeclarations(invoke)
                body = DeclarationIrBuilder(context, symbol).irBlockBody {
                    val parametersVar = if (injektConstructor.valueParameters.any {
                            it.annotations.hasAnnotation(ParamAnnotation)
                        }) {
                        irTemporary(
                            irCall(
                                callee = context.irBuiltIns.checkNotNullSymbol,
                                type = parameters.defaultType.toIrType()
                            ).apply {
                                putValueArgument(
                                    0,
                                    irCall(
                                        callee = symbolTable.referenceSimpleFunction(parametersDefinitionInvoke),
                                        type = parameters.defaultType.makeNullable().toIrType()
                                    ).apply {
                                        dispatchReceiver = irGet(valueParameters.first())
                                    }
                                )
                            }
                        )
                    } else null

                    val newInstanceCall = irCall(
                        callee = symbolTable.referenceConstructor(injektConstructor),
                        type = injektConstructor.returnType.toIrType()
                    ).apply {
                        var paramIndex = 0
                        injektConstructor.valueParameters.forEachIndexed { index, param ->
                            val paramType = injektConstructor.valueParameters[index].type.toIrType()
                            val expr = if (param.annotations.hasAnnotation(ParamAnnotation)) {
                                DeclarationIrBuilder(context, symbol).irBlock {
                                    +irCall(
                                        callee = symbolTable.referenceSimpleFunction(parametersGet),
                                        type = paramType
                                    ).apply {
                                        dispatchReceiver = irGet(parametersVar!!)
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
                                        dispatchReceiver = irGetField(irGet(dispatchReceiverParameter!!),
                                            paramBindingFields.single { it.name.asString().startsWith(param.name.asString()) })
                                    }
                                }
                            }

                            putValueArgument(
                                index = index,
                                valueArgument = expr
                            )
                        }
                    }

                    +irReturn(newInstanceCall)
                }
            }
        }
    }

    private fun ClassDescriptor.findInjektConstructor(): ClassConstructorDescriptor {
        return constructors.singleOrNull { it.annotations.hasAnnotation(InjektConstructorAnnotation) }
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
        descriptor.valueParameters.mapTo(valueParameters) { it.irValueParameter() }

        assert(typeParameters.isEmpty()) { "types ${typeParameters.map { it.name }}" }
        descriptor.typeParameters.mapTo(typeParameters) { it.irTypeParameter() }
    }
}

private object InjektOrigin : IrDeclarationOrigin

private object InjektClassNames {
    val InjektPackage = FqName("com.ivianuu.injekt")
    val HasScope = FqName("com.ivianuu.injekt.HasScope")
    val IsSingle = FqName("com.ivianuu.injekt.IsSingle")
    val Key = FqName("com.ivianuu.injekt.Key")
    val LinkedBinding = FqName("com.ivianuu.injekt.LinkedBinding")
    val Linker = FqName("com.ivianuu.injekt.Linker")
    val Parameters = FqName("com.ivianuu.injekt.Parameters")
    val ParametersDefinition = FqName("com.ivianuu.injekt.ParametersDefinition")
    val UnlinkedBinding = FqName("com.ivianuu.injekt.UnlinkedBinding")
}
