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
import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irInt
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.addMember
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrTypeParameterImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrValueParameterImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.putValueArgument
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrTypeParameterSymbolImpl
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.endOffset
import org.jetbrains.kotlin.ir.util.irConstructorCall
import org.jetbrains.kotlin.ir.util.module
import org.jetbrains.kotlin.ir.util.patchDeclarationParents
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.util.startOffset
import org.jetbrains.kotlin.ir.visitors.IrElementVisitorVoid
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.psi2ir.findSingleFunction
import org.jetbrains.kotlin.resolve.constants.KClassValue
import org.jetbrains.kotlin.resolve.descriptorUtil.annotationClass
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import org.jetbrains.kotlin.utils.addToStdlib.cast

class InjektBindingGenerator(private val context: IrPluginContext) : IrElementVisitorVoid {

    private val symbolTable = context.symbolTable
    private val typeTranslator = context.typeTranslator
    private fun KotlinType.toIrType() = typeTranslator.translateType(this)

    private fun getClass(fqName: FqName) =
        context.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(fqName))!!

    private val abstractBindingFactory = getClass(InjektClassNames.AbstractBindingFactory)
    private val abstractProvider = getClass(InjektClassNames.AbstractBindingProvider)
    private val binding = getClass(InjektClassNames.Binding)
    private val component = getClass(InjektClassNames.Component)
    private val key = getClass(InjektClassNames.Key)
    private val parameters = getClass(InjektClassNames.Parameters)
    private val scoped = getClass(InjektClassNames.Scoping)
        .sealedSubclasses
        .single { it.name.asString() == "Scoped" }

    override fun visitElement(element: IrElement) {
    }

    override fun visitClass(declaration: IrClass) {
        val descriptor = declaration.descriptor

        if (!descriptor.hasAnnotatedAnnotations(KindMarkerAnnotation)) return

        declaration.addMember(bindingFactory(declaration))
        declaration.patchDeclarationParents(declaration.parent)

        //error("declaration ${declaration.dump()}")
    }

    private fun bindingFactory(declaration: IrClass): IrClass {
        val descriptor = declaration.descriptor

        val bindingDescriptor = ClassDescriptorImpl(
            descriptor,
            Name.identifier("BindingFactory"),
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

            val bindingFactoryWithType = KotlinTypeFactory.simpleType(
                baseType = abstractBindingFactory.defaultType,
                arguments = listOf(descriptor.defaultType.asTypeProjection())
            ).toIrType()

            superTypes = superTypes + bindingFactoryWithType

            addConstructor {
                origin = InjektOrigin
                isPrimary = true
                visibility = Visibilities.PRIVATE
            }.apply {
                body = DeclarationIrBuilder(context, symbol).irBlockBody {
                    +IrDelegatingConstructorCallImpl(
                        startOffset, endOffset,
                        context.irBuiltIns.unitType,
                        symbolTable.referenceConstructor(abstractBindingFactory.unsubstitutedPrimaryConstructor!!)
                    ).apply {
                        putTypeArgument(0, descriptor.defaultType.toIrType())
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, this@clazz.symbol, context.irBuiltIns.unitType)
                }
            }

            val bindingProvider = bindingProvider(declaration)

            val create = abstractBindingFactory.unsubstitutedMemberScope
                .findSingleFunction(Name.identifier("create"))

            val bindingWithType = KotlinTypeFactory.simpleType(
                baseType = binding.defaultType,
                arguments = listOf(descriptor.defaultType.asTypeProjection())
            ).toIrType()

            addFunction(
                name = "create",
                returnType = bindingWithType,
                modality = Modality.FINAL,
                isStatic = false,
                isSuspend = false,
                origin = InjektOrigin
            ).apply {
                overriddenSymbols = overriddenSymbols + symbolTable.referenceSimpleFunction(create)
                createParameterDeclarations(create)
                body = DeclarationIrBuilder(context, symbol).irBlockBody {
                    val constructor = symbolTable.referenceConstructor(binding.constructors.first())
                    val newBindingCall = irCall(
                        callee = constructor,
                        type = bindingWithType
                    ).apply {
                        putTypeArgument(0, descriptor.defaultType.toIrType())

                        fun parameter(name: String) = binding.constructors
                            .first().valueParameters.single { it.name.asString() == name }

                        val keyOf = this@InjektBindingGenerator.context.moduleDescriptor.getPackage(InjektClassNames.InjektPackage)
                            .memberScope.findFirstFunction("keyOf") {
                            it.valueParameters.size == 1
                        }

                        // key
                        putValueArgument(
                            parameter("key"),
                            irCall(
                                callee = symbolTable.referenceFunction(keyOf),
                                type = key.defaultType.toIrType()
                            ).apply {
                                putTypeArgument(0, descriptor.defaultType.toIrType())
                            }
                        )

                        // kind
                        val kind = descriptor.getAnnotatedAnnotations(KindMarkerAnnotation)
                            .single()
                            .annotationClass!!
                            .annotations
                            .findAnnotation(KindMarkerAnnotation)!!
                            .allValueArguments[Name.identifier("type")]
                            .cast<KClassValue>()
                            .getArgumentType(module)
                            .constructor
                            .declarationDescriptor
                            .cast<ClassDescriptor>()

                        putValueArgument(
                            parameter("kind"),
                            irGetObject(symbolTable.referenceClass(kind))
                        )

                        // scoping
                        val scopeAnnotation = descriptor.getAnnotatedAnnotations(ScopeAnnotation).singleOrNull()
                        if (scopeAnnotation != null) {
                            val scopeObject =
                                getClass(scopeAnnotation.fqName!!).companionObjectDescriptor!!
                            val scopedConstructor =
                                symbolTable.referenceConstructor(scoped.constructors.first())
                            putValueArgument(
                                parameter("scoping"),
                                irCall(
                                    callee = scopedConstructor,
                                    type = scoped.defaultType.toIrType()
                                ).apply {
                                    putValueArgument(
                                        0,
                                        irGetObject(symbolTable.referenceClass(scopeObject))
                                    )
                                }
                            )
                        }

                        // provider
                        putValueArgument(
                            parameter("provider"),
                            irGetObject(bindingProvider.symbol)
                        )
                    }

                    +irReturn(newBindingCall)
                }
            }

            addMember(bindingProvider)
        }
    }

    private fun bindingProvider(declaration: IrClass): IrClass {
        val descriptor = declaration.descriptor

        val providerDescriptor = ClassDescriptorImpl(
            descriptor,
            Name.identifier("BindingProvider"),
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
            IrClassSymbolImpl(providerDescriptor)
        ).apply clazz@ {
            createImplicitParameterDeclarationWithWrappedDescriptor()

            val abstractProviderWithType = KotlinTypeFactory.simpleType(
                baseType = abstractProvider.defaultType,
                arguments = listOf(descriptor.defaultType.asTypeProjection())
            ).toIrType()

            addConstructor {
                origin = InjektOrigin
                isPrimary = true
                visibility = Visibilities.PRIVATE
            }.apply {
                body = DeclarationIrBuilder(context, symbol).irBlockBody {
                    +IrDelegatingConstructorCallImpl(
                        startOffset, endOffset,
                        context.irBuiltIns.unitType,
                        symbolTable.referenceConstructor(abstractProvider.unsubstitutedPrimaryConstructor!!)
                    ).apply {
                        putTypeArgument(0, descriptor.defaultType.toIrType())
                    }
                    +IrInstanceInitializerCallImpl(startOffset, endOffset, this@clazz.symbol, context.irBuiltIns.unitType)
                }
            }

            superTypes = superTypes + abstractProviderWithType

            val injektConstructor = descriptor.findInjektConstructor()

            val resolve = abstractProvider.unsubstitutedMemberScope
                .findSingleFunction(Name.identifier("resolve"))

            val componentGet = component.unsubstitutedMemberScope
                .findFirstFunction("get") {
                    it.valueParameters.first().name.asString() == "name"
                }

            val parametersGet = parameters.unsubstitutedMemberScope
                .findSingleFunction(Name.identifier("get"))

            addFunction(
                name = "resolve",
                returnType = declaration.defaultType,
                modality = Modality.FINAL,
                isStatic = false,
                isSuspend = false,
                origin = InjektOrigin
            ).apply {
                overriddenSymbols = overriddenSymbols + symbolTable.referenceSimpleFunction(resolve)
                createParameterDeclarations(resolve)
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
                                val paramType = injektConstructor.valueParameters[index].type.toIrType()
                                val expr = if (param.annotations.hasAnnotation(ParamAnnotation)) {
                                    DeclarationIrBuilder(context, symbol).irBlock {
                                        +irCall(
                                            callee = symbolTable.referenceSimpleFunction(parametersGet),
                                            type = paramType
                                        ).apply {
                                            dispatchReceiver = irGet(valueParameters[1])
                                            putTypeArgument(0, paramType)
                                            putValueArgument(0, irInt(paramIndex))
                                        }
                                        ++paramIndex
                                    }
                                } else {
                                    DeclarationIrBuilder(context, symbol).irBlock {
                                        +irCall(
                                            callee = symbolTable.referenceSimpleFunction(componentGet),
                                            type = paramType
                                        ).apply {
                                            dispatchReceiver = irGet(valueParameters[0])
                                            putTypeArgument(0, paramType)
                                            val nameObject = param.getAnnotatedAnnotations(NameAnnotation)
                                                .singleOrNull()
                                                ?.let { nameAnnotation -> getClass(nameAnnotation.fqName!!) }
                                                ?.companionObjectDescriptor

                                            if (nameObject != null) {
                                                putValueArgument(
                                                    0,
                                                    irGetObject(symbolTable.referenceClass(nameObject))
                                                )
                                            }
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
        valueParameters = descriptor.valueParameters.map { it.irValueParameter() }

        assert(typeParameters.isEmpty()) { "types ${typeParameters.map { it.name }}" }
        typeParameters + descriptor.typeParameters.map { it.irTypeParameter() }
    }
}

private object InjektOrigin : IrDeclarationOrigin

private object InjektClassNames {
    val AbstractBindingFactory = FqName("com.ivianuu.injekt.AbstractBindingFactory")
    val AbstractBindingProvider = FqName("com.ivianuu.injekt.AbstractBindingProvider")
    val Binding = FqName("com.ivianuu.injekt.Binding")
    val Component = FqName("com.ivianuu.injekt.Component")
    val InjektPackage = FqName("com.ivianuu.injekt")
    val Key = FqName("com.ivianuu.injekt.Key")
    val Parameters = FqName("com.ivianuu.injekt.Parameters")
    val Scoping = FqName("com.ivianuu.injekt.Scoping")
}
