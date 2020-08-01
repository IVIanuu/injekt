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

package com.ivianuu.injekt.compiler.transform.implicit

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.addFile
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.flatMapFix
import com.ivianuu.injekt.compiler.getClassFromAnnotation
import com.ivianuu.injekt.compiler.readableName
import com.ivianuu.injekt.compiler.substitute
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import com.ivianuu.injekt.compiler.typeWith
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.ir.remapTypeParameters
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irAs
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrConst
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.defaultType
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getAnnotation
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.name.Name

class SpecialContextTransformer(
    pluginContext: IrPluginContext,
    private val declarationGraph: DeclarationGraph
) : AbstractInjektTransformer(pluginContext) {

    override fun lower() {
        declarationGraph.genericContexts.forEach { index ->
            val delegateContext = index.getClassFromAnnotation(
                InjektFqNames.GenericContext,
                0,
                pluginContext
            )!!
            val name = index.getAnnotation(InjektFqNames.GenericContext)!!
                .getValueArgument(1)
                .let { it as IrConst<String> }
                .value

            val functionMap = index.getAnnotation(InjektFqNames.GenericContext)!!
                .getValueArgument(2)
                .let { it as IrConst<String> }
                .value
                .let {
                    if (it.isNotEmpty()) {
                        it.split("=:=")
                            .filter { it.isNotEmpty() }
                            .map {
                                it.split("===")
                                    .filter { it.isNotEmpty() }
                                    .let { it[0] to it[1] }
                            }
                    } else emptyList()
                }
                .toMap()

            generateGenericContext(
                delegateContext,
                index.superTypes.single(),
                name,
                functionMap
            )
        }

        declarationGraph.withInstancesContexts.forEach { index ->
            val implementingContext = index.getClassFromAnnotation(
                InjektFqNames.WithInstancesContext,
                0,
                pluginContext
            )!!
            val name = index.getAnnotation(InjektFqNames.WithInstancesContext)!!
                .getValueArgument(1)
                .let { it as IrConst<String> }
                .value

            generateWithInstancesContext(index, implementingContext, name)
        }
    }

    private fun generateGenericContext(
        delegateContext: IrClass,
        genericContextType: IrType,
        name: String,
        functionMap: Map<String, String>
    ) {
        val file = module.addFile(
            pluginContext,
            delegateContext.getPackageFragment()!!
                .fqName
                .child(name.asNameId())
        )
        val contextImpl = buildClass {
            this.name = name.asNameId()
        }.apply clazz@{
            parent = file
            file.addChild(this)
            createImplicitParameterDeclarationWithWrappedDescriptor()
            superTypes += genericContextType
        }

        val delegateField = contextImpl.addField(
            "delegate",
            delegateContext.defaultType
        )

        contextImpl.addConstructor {
            returnType = contextImpl.defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            val delegateValueParameter = addValueParameter(
                "delegate",
                delegateContext.defaultType
            )

            body = DeclarationIrBuilder(
                pluginContext,
                symbol
            ).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    contextImpl.symbol,
                    context.irBuiltIns.unitType
                )
                +irSetField(
                    irGet(contextImpl.thisReceiver!!),
                    delegateField,
                    irGet(delegateValueParameter)
                )
            }
        }

        val implementedSuperTypes = mutableSetOf<IrType>()
        val declarationNames = mutableSetOf<Name>()

        fun implementFunctions(
            superClass: IrClass,
            typeArguments: List<IrType>
        ) {
            if (superClass.defaultType in implementedSuperTypes) return
            implementedSuperTypes += superClass
                .typeWith(typeArguments)
            contextImpl.superTypes += superClass
                .typeWith(typeArguments)
            for (declaration in superClass.declarations.toList()) {
                if (declaration !is IrFunction) continue
                if (declaration is IrConstructor) continue
                if (declaration.isFakeOverride) continue
                if (declaration.dispatchReceiverParameter?.type == irBuiltIns.anyType) continue
                if (declaration.name in declarationNames) continue
                declarationNames += declaration.name
                contextImpl.addFunction {
                    this.name = declaration.name
                    returnType = declaration.returnType.substitute(
                        superClass.typeParameters
                            .map { it.symbol }
                            .zip(typeArguments)
                            .toMap()
                    )
                }.apply {
                    dispatchReceiverParameter = contextImpl.thisReceiver!!.copyTo(this)
                    overriddenSymbols += declaration.symbol as IrSimpleFunctionSymbol
                    addMetadataIfNotLocal()
                    body = DeclarationIrBuilder(
                        pluginContext,
                        symbol
                    ).run {
                        val finalCallee = delegateContext.functions
                            .singleOrNull { it.name.asString() == functionMap[declaration.name.asString()] }
                            ?: declaration
                        irExprBody(
                            irCall(
                                finalCallee
                            ).apply {
                                dispatchReceiver = if (finalCallee == declaration) {
                                    irAs(
                                        irGetField(
                                            irGet(dispatchReceiverParameter!!),
                                            delegateField
                                        ),
                                        finalCallee.dispatchReceiverParameter!!.type
                                    )
                                } else {
                                    irGetField(
                                        irGet(dispatchReceiverParameter!!),
                                        delegateField
                                    )
                                }
                            }
                        )
                    }
                }
            }

            superClass.superTypes
                .map { it to it.classOrNull?.owner }
                .forEach { (superType, clazz) ->
                    if (clazz != null)
                        implementFunctions(
                            clazz,
                            superType.typeArguments.map { it.typeOrFail }
                        )
                }
        }

        implementFunctions(
            genericContextType.classOrNull!!.owner,
            genericContextType.typeArguments.map { it.typeOrFail }
        )

        declarationGraph.getAllContextImplementations(genericContextType.classOrNull!!.owner)
            .forEach { superType ->
                implementFunctions(
                    superType,
                    superType.defaultType.typeArguments.map { it.typeOrFail }
                )
            }
    }

    private fun generateWithInstancesContext(
        index: IrClass,
        implementingContext: IrClass,
        name: String
    ) {
        val rawDelegateContextType = index.superTypes.first()
        val delegateContext = rawDelegateContextType.classOrNull!!.owner

        val file = module.addFile(
            pluginContext,
            delegateContext.getPackageFragment()!!
                .fqName
                .child(name.asNameId())
        )
        val contextImpl = buildClass {
            this.name = name.asNameId()
        }.apply clazz@{
            parent = file
            file.addChild(this)
            createImplicitParameterDeclarationWithWrappedDescriptor()
            copyTypeParametersFrom(delegateContext)
            superTypes += index.superTypes
                .map {
                    it.remapTypeParameters(index, this)
                        .typeWith(
                            *it.typeArguments.map { it.typeOrFail }.toTypedArray()
                        )
                }
        }

        val delegateField = contextImpl.addField(
            "delegate",
            delegateContext.typeWith(contextImpl.typeParameters.map { it.defaultType })
        )

        val instances = index.functions
            .single { it.name.asString() == "instances" }
            .valueParameters
            .map {
                it.type
                    .remapTypeParameters(index, contextImpl)
            }

        val instanceFields = instances.associateWith {
            contextImpl.addField(
                it.readableName(),
                it
            )
        }

        contextImpl.addConstructor {
            returnType = contextImpl.defaultType
            isPrimary = true
            visibility = Visibilities.PUBLIC
        }.apply {
            val delegateValueParameter = addValueParameter(
                "delegate",
                delegateContext.typeWith(contextImpl.typeParameters.map { it.defaultType })
            )

            val instanceValueParameters = instanceFields.values.associateWith {
                addValueParameter(
                    it.name.asString(),
                    it.type
                )
            }

            body = DeclarationIrBuilder(
                pluginContext,
                symbol
            ).irBlockBody {
                +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                +IrInstanceInitializerCallImpl(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    contextImpl.symbol,
                    context.irBuiltIns.unitType
                )
                +irSetField(
                    irGet(contextImpl.thisReceiver!!),
                    delegateField,
                    irGet(delegateValueParameter)
                )
                instanceValueParameters.forEach { (field, valueParameter) ->
                    +irSetField(
                        irGet(contextImpl.thisReceiver!!),
                        field,
                        irGet(valueParameter)
                    )
                }
            }
        }

        val implementedSuperTypes = mutableSetOf<IrType>()
        val declarationNames = mutableSetOf<Name>()

        fun implementFunctions(
            superClass: IrClass,
            typeArguments: List<IrType>
        ) {
            if (superClass.defaultType in implementedSuperTypes) return
            implementedSuperTypes += superClass
                .typeWith(typeArguments)
            contextImpl.superTypes += superClass
                .typeWith(typeArguments)
            for (declaration in superClass.declarations.toList()) {
                if (declaration !is IrFunction) continue
                if (declaration is IrConstructor) continue
                if (declaration.isFakeOverride) continue
                if (declaration.dispatchReceiverParameter?.type == irBuiltIns.anyType) continue
                if (declaration.name in declarationNames) continue
                declarationNames += declaration.name
                contextImpl.addFunction {
                    this.name = declaration.name
                    returnType = declaration.returnType
                }.apply {
                    dispatchReceiverParameter = contextImpl.thisReceiver!!.copyTo(this)
                    overriddenSymbols += declaration.symbol as IrSimpleFunctionSymbol
                    addMetadataIfNotLocal()
                    body = DeclarationIrBuilder(
                        pluginContext,
                        symbol
                    ).run {
                        irExprBody(
                            instanceFields[returnType]?.let {
                                irGetField(
                                    irGet(dispatchReceiverParameter!!),
                                    it
                                )
                            } ?: irCall(declaration).apply {
                                dispatchReceiver = irAs(
                                    irGetField(
                                        irGet(dispatchReceiverParameter!!),
                                        delegateField
                                    ),
                                    declaration.dispatchReceiverParameter!!.type
                                )
                            }
                        )
                    }
                }
            }

            superClass.superTypes
                .map { it to it.classOrNull?.owner }
                .forEach { (superType, clazz) ->
                    if (clazz != null)
                        implementFunctions(
                            clazz,
                            superType.typeArguments.map { it.typeOrFail }
                        )
                }
        }

        (contextImpl.superTypes
            .map { it.classOrNull!!.owner } + implementingContext)
            .flatMapFix { declarationGraph.getAllContextImplementations(it) }
            .forEach { superType ->
                implementFunctions(
                    superType,
                    superType.defaultType.typeArguments.map { it.typeOrFail }
                )
            }
    }

}
