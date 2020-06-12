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

package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.substituteAndKeepQualifiers
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.typeArguments
import com.ivianuu.injekt.compiler.typeOrFail
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irDelegatingConstructorCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.IrSimpleFunctionSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ImplFactory(
    origin: FqName,
    superType: IrType,
    val parent: ImplFactory?,
    factoryFunction: IrFunction,
    moduleClass: IrClass,
    factoryModuleAccessor: FactoryExpression,
    pluginContext: IrPluginContext,
    symbols: InjektSymbols,
    declarationStore: InjektDeclarationStore
) : AbstractFactory(
    origin,
    moduleClass,
    factoryModuleAccessor,
    factoryFunction,
    pluginContext,
    symbols,
    declarationStore
) {

    val clazz = buildClass {
        name = Name.special("<impl>")
        visibility = Visibilities.LOCAL
    }.apply clazz@{
        parent = factoryFunction
        createImplicitParameterDeclarationWithWrappedDescriptor()
        superTypes += superType
    }

    val factoryImplementationNode = FactoryImplementationNode(
        key = clazz.defaultType.asKey(),
        implFactory = this,
        accessor = { error("") }
    )

    private val dependencyRequests =
        mutableMapOf<IrDeclaration, BindingRequest>()
    private val implementedRequests = mutableMapOf<IrDeclaration, IrDeclaration>()

    var factoryLateinitProvider: FactoryExpression? = null

    fun getImplExpression(): IrExpression {
        return DeclarationIrBuilder(pluginContext, factoryFunction.symbol).run {
            irBlock {
                factoryMembers.blockBuilder = this

                collectDependencyRequests()
                init(parent, dependencyRequests.values.toList())

                DeclarationIrBuilder(pluginContext, clazz.symbol).run {
                    implementDependencyRequests()
                }

                +clazz

                val constructor = clazz.addConstructor {
                    returnType = clazz.defaultType
                    isPrimary = true
                    visibility = Visibilities.PUBLIC
                }.apply {
                    body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                        val superType = clazz.superTypes.single()
                        +irDelegatingConstructorCall(
                            if (superType.classOrNull!!.owner.kind == ClassKind.CLASS)
                                superType.classOrNull!!.owner.constructors.single { it.valueParameters.isEmpty() }
                            else context.irBuiltIns.anyClass.constructors.single().owner
                        )
                        +IrInstanceInitializerCallImpl(
                            UNDEFINED_OFFSET,
                            UNDEFINED_OFFSET,
                            clazz.symbol,
                            context.irBuiltIns.unitType
                        )

                        factoryLateinitProvider?.let { factoryLateinitProvider ->
                            +irCall(
                                symbols.lateinitFactory
                                    .functions
                                    .single { it.owner.name.asString() == "init" }
                            ).apply {
                                dispatchReceiver = factoryLateinitProvider()
                                putValueArgument(0, irGet(clazz.thisReceiver!!))
                            }
                        }
                    }
                }

                +irCall(constructor)
            }
        }
    }

    private fun IrBuilderWithScope.implementDependencyRequests(): Unit = clazz.run clazz@{
        dependencyRequests.forEach { factoryExpressions.getBindingExpression(it.value) }

        dependencyRequests
            .filter { it.key !in implementedRequests }
            .forEach { (declaration, request) ->
                val binding = graph.getBinding(request)
                implementedRequests[declaration] =
                    addDependencyRequestImplementation(declaration) { function ->
                        val bindingExpression = factoryExpressions.getBindingExpression(
                            BindingRequest(
                                binding.key,
                                request.requestOrigin,
                                false, // todo add default support
                                RequestType.Instance
                            )
                        )

                        bindingExpression(this@implementDependencyRequests)!!
                    }
            }
    }

    private fun collectDependencyRequests() {
        fun IrClass.collectDependencyRequests(
            typeArguments: List<IrType>
        ) {
            for (declaration in declarations) {
                fun reqisterRequest(type: IrType) {
                    dependencyRequests[declaration] = BindingRequest(
                        type
                            .substituteAndKeepQualifiers(
                                typeParameters.map { it.symbol }.associateWith {
                                    typeArguments[it.owner.index]
                                }
                            )
                            .asKey(),
                        declaration.descriptor.fqNameSafe,
                        false // todo add default value support
                    )
                }

                when (declaration) {
                    is IrFunction -> {
                        if (declaration !is IrConstructor &&
                            declaration.dispatchReceiverParameter?.type != pluginContext.irBuiltIns.anyType &&
                            !declaration.isFakeOverride
                        ) reqisterRequest(declaration.returnType)
                    }
                    is IrProperty -> {
                        if (!declaration.isFakeOverride)
                            reqisterRequest(declaration.getter!!.returnType)
                    }
                }
            }

            superTypes
                .map { it to it.classOrNull?.owner }
                .forEach { (superType, clazz) ->
                    clazz?.collectDependencyRequests(
                        superType.typeArguments.map { it.typeOrFail }
                    )
                }
        }

        val superType = clazz.superTypes.single()
        val superTypeClass = superType.getClass()!!
        superTypeClass.collectDependencyRequests(superType.typeArguments.map { it.typeOrFail })
    }

    private fun addDependencyRequestImplementation(
        declaration: IrDeclaration,
        body: IrBuilderWithScope.(IrFunction) -> IrExpression
    ): IrDeclaration {
        fun IrFunctionImpl.implement(symbol: IrSimpleFunctionSymbol) {
            overriddenSymbols += symbol
            dispatchReceiverParameter = clazz.thisReceiver!!.copyTo(this)
            DeclarationIrBuilder(pluginContext, symbol).apply {
                this@implement.body = irExprBody(body(this, this@implement))
            }
        }

        return when (declaration) {
            is IrFunction -> {
                clazz.addFunction {
                    name = declaration.name
                    returnType = declaration.returnType
                    visibility = declaration.visibility
                }.apply { implement(declaration.symbol as IrSimpleFunctionSymbol) }
            }
            is IrProperty -> {
                clazz.addProperty {
                    name = declaration.name
                    visibility = declaration.visibility
                }.apply {
                    addGetter {
                        returnType = declaration.getter!!.returnType
                    }.apply { (this as IrFunctionImpl).implement(declaration.getter!!.symbol) }
                }
            }
            else -> error("Unexpected declaration ${declaration.dump()}")
        }
    }
}
