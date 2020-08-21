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
import com.ivianuu.injekt.compiler.SimpleUniqueNameProvider
import com.ivianuu.injekt.compiler.addFile
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.getAllFunctions
import com.ivianuu.injekt.compiler.getClassFromAnnotation
import com.ivianuu.injekt.compiler.getConstantFromAnnotationOrNull
import com.ivianuu.injekt.compiler.recordLookup
import com.ivianuu.injekt.compiler.substitute
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.DeclarationGraph
import com.ivianuu.injekt.compiler.transform.InjektContext
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
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
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
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
import org.jetbrains.kotlin.name.Name

class GenericContextImplTransformer(
    injektContext: InjektContext,
    private val declarationGraph: DeclarationGraph,
    private val initFile: IrFile
) : AbstractInjektTransformer(injektContext) {

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

            generateGenericContextFactory(
                delegateContext,
                genericContextType,
                name,
                functionMap
            )
        }
    }

    private fun generateGenericContextFactory(
        delegateContext: IrClass,
        genericContextType: IrType,
        name: String,
        functionMap: Map<String, String>
    ) {
        val file = injektContext.module.addFile(
            injektContext,
            delegateContext.getPackageFragment()!!
                .fqName
                .child(name.asNameId())
        )

        val factory = buildClass {
            this.name = name.asNameId()
            kind = ClassKind.OBJECT
            visibility = Visibilities.INTERNAL
        }.apply clazz@{
            parent = file
            createImplicitParameterDeclarationWithWrappedDescriptor()

            addConstructor {
                returnType = defaultType
                isPrimary = true
                visibility = Visibilities.PUBLIC
            }.apply {
                body = DeclarationIrBuilder(
                    injektContext,
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

        recordLookup(initFile, factory)

        file.addChild(factory)

        val implNameProvider = SimpleUniqueNameProvider()

        val contextImpl = generateGenericContextImpl(
            delegateContext,
            genericContextType,
            implNameProvider("ContextImpl".asNameId()),
            factory,
            functionMap
        )

        factory.addChild(contextImpl)

        factory.addFunction {
            this.name = "create".asNameId()
            returnType = genericContextType
        }.apply {
            dispatchReceiverParameter = factory.thisReceiver!!.copyTo(this)

            val delegateValueParameter = addValueParameter(
                "delegate",
                delegateContext.defaultType
            )

            body = DeclarationIrBuilder(injektContext, symbol).run {
                irExprBody(
                    irCall(contextImpl.constructors.single()).apply {
                        putValueArgument(
                            0,
                            irGet(delegateValueParameter)
                        )
                    }
                )
            }
        }
    }

    private fun generateGenericContextImpl(
        delegateContext: IrClass,
        genericContextType: IrType,
        name: Name,
        irParent: IrDeclarationParent,
        functionMap: Map<String, String>
    ): IrClass {
        val contextImpl = buildClass {
            this.name = name
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
                injektContext,
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

        fun implement(
            superType: IrClass,
            typeArguments: List<IrType>
        ) {
            if (superType in contextImpl.superTypes.map { it.classOrNull!!.owner }) return
            contextImpl.superTypes += superType.typeWith(typeArguments)
            recordLookup(contextImpl, superType)

            for (declaration in superType.declarations.toList()) {
                if (declaration !is IrFunction) continue
                if (declaration is IrConstructor) continue
                if (declaration.dispatchReceiverParameter?.type == injektContext.irBuiltIns.anyType) continue
                if (declaration.isFakeOverride) continue
                val existingDeclaration = contextImpl.functions.firstOrNull {
                    it.name == declaration.name
                }
                if (existingDeclaration != null) {
                    existingDeclaration.overriddenSymbols += declaration.symbol as IrSimpleFunctionSymbol
                    continue
                }
                contextImpl.addFunction {
                    this.name = declaration.name
                    returnType = declaration.returnType.substitute(
                        superType.typeParameters
                            .map { it.symbol }
                            .zip(typeArguments)
                            .toMap()
                    )
                }.apply {
                    dispatchReceiverParameter = contextImpl.thisReceiver!!.copyTo(this)
                    overriddenSymbols += declaration.symbol as IrSimpleFunctionSymbol
                    addMetadataIfNotLocal()
                    body = DeclarationIrBuilder(
                        injektContext,
                        symbol
                    ).run {
                        val finalCallee = delegateContext.getAllFunctions()
                            .firstOrNull { it.name.asString() == functionMap[declaration.name.asString()] }
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

            superType.superTypes
                .map { it to it.classOrNull?.owner }
                .forEach { (superType, clazz) ->
                    if (clazz != null)
                        implement(
                            clazz,
                            superType.typeArguments.map { it.typeOrFail }
                        )
                }
        }

        // It's important to implement the functions of the generic context type
        // with the correct type arguments to prevent abstract method errors
        implement(
            genericContextType.classOrNull!!.owner,
            genericContextType.typeArguments.map { it.typeOrFail }
        )

        declarationGraph.getAllContextImplementations(genericContextType.classOrNull!!.owner)
            .forEach { superType ->
                implement(
                    superType,
                    superType.defaultType.typeArguments.map { it.typeOrFail }
                )
            }

        return contextImpl
    }

}
