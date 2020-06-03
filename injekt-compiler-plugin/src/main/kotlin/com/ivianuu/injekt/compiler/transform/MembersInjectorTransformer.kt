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

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addSetter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irEqeqeq
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irImplicitCast
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.builders.irTemporary
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.getPackageFragment
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class MembersInjectorTransformer(context: IrPluginContext) : AbstractInjektTransformer(context) {

    private val injectFunctionsByClass = mutableMapOf<IrClass, IrFunction>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val classes = mutableSetOf<IrClass>()
        val injectFunctionsByClass = mutableMapOf<IrClass, MutableList<IrFunction>>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitPropertyNew(declaration: IrProperty): IrStatement {
                val field = declaration.backingField
                if (field != null && field.origin == IrDeclarationOrigin.PROPERTY_DELEGATE &&
                    field.initializer?.expression?.type?.classOrNull == symbols.injectProperty
                ) {
                    makePropertyInjectable(declaration)
                    classes += currentClass?.irElement as IrClass
                }

                return super.visitPropertyNew(declaration)
            }

            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Inject)) {
                    classes += declaration.parent as IrClass
                    injectFunctionsByClass.getOrPut(declaration.parent as IrClass) {
                        mutableListOf()
                    } + declaration
                }
                return super.visitFunctionNew(declaration)
            }
        })

        injectFunctionsByClass.forEach { (clazz, injectFunctions) ->
            injectFunctions.forEach { injectFunction ->
                addInjectFunctionAccessor(clazz, injectFunction)
            }
        }

        classes.forEach { clazz -> getMembersInjectorForClass(clazz) }

        return super.visitModuleFragment(declaration)
    }

    fun getMembersInjectorForClass(clazz: IrClass): IrFunction? {
        injectFunctionsByClass[clazz]?.let { return it }
        val membersInjector = DeclarationIrBuilder(pluginContext, clazz.symbol)
            .membersInjector(clazz) ?: return null
        clazz.addChild(membersInjector)
        injectFunctionsByClass[clazz] = membersInjector
        return membersInjector
    }

    private fun IrBuilderWithScope.membersInjector(clazz: IrClass): IrFunction? {
        val injectProperties = mutableListOf<IrProperty>()
        val injectFunctions = mutableListOf<IrFunction>()

        fun IrClass.collectInjectMembers() {
            injectProperties += properties
                .filter {
                    it.backingField?.name?.asString()?.startsWith("injected\$") == true
                }
            injectFunctions += functions
                .filter { it.hasAnnotation(InjektFqNames.Inject) }
            superTypes
                .forEach { it.classOrNull?.owner?.collectInjectMembers() }
        }

        clazz.collectInjectMembers()

        if (injectProperties.isEmpty() && injectFunctions.isEmpty()) return null

        return buildFun {
            name = InjektNameConventions.getMembersInjectorNameForClass(
                clazz.getPackageFragment()!!.fqName,
                clazz.descriptor.fqNameSafe
            )
            visibility = clazz.visibility
            returnType = irBuiltIns.unitType
        }.apply {
            addMetadataIfNotLocal()

            val parameters = mutableListOf<IrType>()

            parameters += clazz.defaultType

            val parameterNameProvider = NameProvider()

            val instanceParameter = addValueParameter(
                parameterNameProvider.allocateForGroup("instance"),
                clazz.defaultType
            )

            val parametersByInjectProperties = injectProperties.associateWith {
                addValueParameter(
                    parameterNameProvider.allocateForType(it.getter!!.returnType).asString(),
                    it.getter!!.returnType
                )
            }

            val parametersByInjectFunctions = injectFunctions.associateWith { function ->
                function.valueParameters.map {
                    addValueParameter(
                        parameterNameProvider.allocateForType(it.type).asString(),
                        it.type
                    )
                }
            }

            body = DeclarationIrBuilder(pluginContext, symbol).run {
                irBlockBody {
                    parametersByInjectProperties.forEach { (property, valueParameter) ->
                        +irCall(property.setter!!).apply {
                            dispatchReceiver = irGet(instanceParameter)
                            putValueArgument(0, irGet(valueParameter))
                        }
                    }

                    parametersByInjectFunctions.forEach { (function, valueParameters) ->
                        +irCall(function).apply {
                            dispatchReceiver = irGet(instanceParameter)
                            valueParameters.forEachIndexed { index, valueParameter ->
                                putValueArgument(index, irGet(valueParameter))
                            }
                        }
                    }
                }
            }
        }
    }

    private fun addInjectFunctionAccessor(
        clazz: IrClass,
        function: IrFunction
    ) {
        clazz.addFunction {
            name = Name.identifier(
                "injectFun\$${function.descriptor.fqNameSafe.pathSegments().joinToString("_")}"
            )
            returnType = irBuiltIns.unitType
            visibility = Visibilities.PUBLIC
        }.apply {
            dispatchReceiverParameter = function.dispatchReceiverParameter!!.copyTo(this)
            val valueParameters = function.valueParameters.map {
                addValueParameter(
                    it.name.asString(),
                    it.type
                )
            }

            body = DeclarationIrBuilder(pluginContext, symbol).run {
                irExprBody(
                    irCall(function).apply {
                        dispatchReceiver = irGet(dispatchReceiverParameter!!)
                        valueParameters.forEach {
                            putValueArgument(
                                it.index,
                                irGet(it)
                            )
                        }
                    }
                )
            }
        }
    }

    private fun makePropertyInjectable(property: IrProperty) {
        DeclarationIrBuilder(pluginContext, property.symbol).run {
            property.backingField = buildField {
                name = Name.identifier("injected\$${property.name}")
                type = irBuiltIns.anyNType
                visibility = Visibilities.PRIVATE
            }.apply {
                initializer = irExprBody(irGetObject(symbols.uninitialized))
            }

            property.getter!!.body = DeclarationIrBuilder(
                pluginContext,
                property.getter!!.symbol
            ).irBlockBody {
                val tmp = irTemporary(
                    irGetField(
                        irGet(property.getter!!.dispatchReceiverParameter!!),
                        property.backingField!!
                    )
                )
                +irIfThen(
                    irEqeqeq(
                        irGet(tmp),
                        irGetObject(symbols.uninitialized)
                    ),
                    irCall(
                        pluginContext.referenceFunctions(FqName("kotlin.error"))
                            .single { it.owner.valueParameters.size == 1 }
                    ).apply {
                        putValueArgument(
                            0, irString("Not injected")
                        )
                    }
                )

                +DeclarationIrBuilder(pluginContext, property.getter!!.symbol)
                    .irReturn(
                        irImplicitCast(
                            irGet(tmp),
                            property.getter!!.returnType
                        )
                    )
            }

            property.addSetter {
                name = Name.identifier(
                    "injectProperty\$${property.descriptor.fqNameSafe.pathSegments()
                        .joinToString("_")}"
                )
                returnType = irBuiltIns.unitType
                visibility = Visibilities.PUBLIC
            }.apply {
                dispatchReceiverParameter =
                    property.getter!!.dispatchReceiverParameter!!.copyTo(this)

                val valueParameter =
                    addValueParameter("value", property.getter!!.returnType)

                body = irExprBody(
                    irSetField(
                        irGet(dispatchReceiverParameter!!),
                        property.backingField!!,
                        irGet(valueParameter)
                    )
                )
            }
        }
    }

}
