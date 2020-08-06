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
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addFile
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.getClassFromAnnotation
import com.ivianuu.injekt.compiler.getConstantFromAnnotationOrNull
import com.ivianuu.injekt.compiler.substitute
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.runreader.RunReaderContextImplTransformer
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irAs
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irBranch
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irElseBranch
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irIs
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irWhen
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class GenericContextImplTransformer(
    pluginContext: IrPluginContext,
    private val declarationGraph: DeclarationGraph,
    private val runReaderContextImplTransformer: RunReaderContextImplTransformer
) : AbstractInjektTransformer(pluginContext) {

    override fun lower() {
        declarationGraph.genericContexts.forEach { index ->
            val delegateContext = index.getClassFromAnnotation(
                InjektFqNames.GenericContext,
                0
            )!!
            val name =
                index.getConstantFromAnnotationOrNull<String>(InjektFqNames.GenericContext, 1)!!

            val functionMap =
                index.getConstantFromAnnotationOrNull<String>(InjektFqNames.GenericContext, 2)!!
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

            val genericContextType = index.superTypes.single()

            val allParentInputs = runReaderContextImplTransformer.generatedContexts
                .values
                .flatten()
                .filter {
                    delegateContext in
                            it.superTypes.map { it.classOrNull!!.owner }
                }
                .map { runReaderContextImplTransformer.inputsByContext[it]!! }
                .toSet()

            generateGenericContextFactory(
                allParentInputs,
                delegateContext,
                genericContextType,
                name,
                functionMap
            )
        }
    }

    private fun generateGenericContextFactory(
        allParentInputs: Set<IrClass>,
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

        val factory = buildClass {
            this.name = name.asNameId()
            kind = ClassKind.OBJECT
            visibility = Visibilities.INTERNAL
        }.apply clazz@{
            createImplicitParameterDeclarationWithWrappedDescriptor()

            addConstructor {
                returnType = defaultType
                isPrimary = true
                visibility = Visibilities.PUBLIC
            }.apply {
                body = DeclarationIrBuilder(
                    pluginContext,
                    symbol
                ).irBlockBody {
                    +irDelegatingConstructorCall(context.irBuiltIns.anyClass.constructors.single().owner)
                    +IrInstanceInitializerCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        this@clazz.symbol,
                        context.irBuiltIns.unitType
                    )
                }
            }
        }

        file.addChild(factory)

        val implNameProvider = NameProvider()

        val contextImplsWithParentInputs = allParentInputs.map { parentInputs ->
            generateGenericContextImpl(
                delegateContext,
                genericContextType,
                implNameProvider.allocateForGroup("Impl"),
                factory,
                functionMap,
                parentInputs
            ) to parentInputs
        }

        contextImplsWithParentInputs.forEach { factory.addChild(it.first) }

        factory.addFunction {
            this.name = "create".asNameId()
            returnType = genericContextType
        }.apply {
            dispatchReceiverParameter = factory.thisReceiver!!.copyTo(this)

            val delegateValueParameter = addValueParameter(
                "delegate",
                delegateContext.defaultType
            )

            body = DeclarationIrBuilder(pluginContext, symbol).run {
                fun createContextImpl(contextImpl: IrClass, parent: IrClass): IrExpression {
                    return irCall(contextImpl.constructors.single()).apply {
                        putValueArgument(
                            0,
                            irGet(delegateValueParameter)
                        )
                    }
                }
                irExprBody(
                    if (contextImplsWithParentInputs.size == 1) {
                        val (contextImpl, parentInputs) = contextImplsWithParentInputs.single()
                        createContextImpl(contextImpl, parentInputs)
                    } else {
                        irWhen(
                            genericContextType,
                            contextImplsWithParentInputs.map { (contextImpl, parentInputs) ->
                                irBranch(
                                    irIs(irGet(delegateValueParameter), parentInputs.defaultType),
                                    createContextImpl(contextImpl, parentInputs)
                                )
                            } + irElseBranch(
                                irCall(
                                    pluginContext.referenceFunctions(
                                        FqName("kotlin.error")
                                    ).single()
                                ).apply {
                                    putValueArgument(
                                        0,
                                        irString("Unexpected parent")
                                    )
                                }
                            )
                        )
                    }
                )
            }
        }
    }

    private fun generateGenericContextImpl(
        delegateContext: IrClass,
        genericContextType: IrType,
        name: String,
        irParent: IrDeclarationParent,
        functionMap: Map<String, String>,
        parentInputs: IrClass
    ): IrClass {
        val contextImpl = buildClass {
            this.name = name.asNameId()
            visibility = Visibilities.INTERNAL
        }.apply clazz@{
            parent = irParent
            createImplicitParameterDeclarationWithWrappedDescriptor()
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

        (declarationGraph.getAllContextImplementations(genericContextType.classOrNull!!.owner) + parentInputs)
            .forEach { superType ->
                implementFunctions(
                    superType,
                    superType.defaultType.typeArguments.map { it.typeOrFail }
                )
            }

        return contextImpl
    }

}
