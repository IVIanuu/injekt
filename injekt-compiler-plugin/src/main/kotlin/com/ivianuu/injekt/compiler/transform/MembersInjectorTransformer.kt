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
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.getParameterName
import com.ivianuu.injekt.compiler.withNoArgAnnotations
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.lower.irIfThen
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addSetter
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildField
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
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames

class MembersInjectorTransformer(context: IrPluginContext) : AbstractInjektTransformer(context) {

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

    fun getMembersInjectorForClass(clazz: IrClass): IrClass? {
        clazz
            .declarations
            .filterIsInstance<IrClass>()
            .singleOrNull { it.name == InjektNameConventions.getMembersInjectorNameForClass(clazz.name) }
            ?.let { return it }
        val membersInjector = DeclarationIrBuilder(pluginContext, clazz.symbol)
            .membersInjector(clazz) ?: return null
        clazz.addChild(membersInjector)
        return membersInjector
    }

    private fun IrBuilderWithScope.membersInjector(clazz: IrClass): IrClass? {
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

        return buildClass {
            kind = if (injectProperties.isNotEmpty()) ClassKind.CLASS else ClassKind.OBJECT
            name = InjektNameConventions.getMembersInjectorNameForClass(clazz.descriptor.name)
            visibility = clazz.visibility
        }.apply clazz@{
            parent = clazz
            superTypes += irBuiltIns.function(1)
                .typeWith(clazz.defaultType, irBuiltIns.unitType)

            createImplicitParameterDeclarationWithWrappedDescriptor()

            addMetadataIfNotLocal()

            val nameProvider = NameProvider()

            val fieldsByInjectProperty = injectProperties.associateWith { property ->
                addField {
                    name = nameProvider.allocateForGroup(property.name)
                    type = irBuiltIns.function(0)
                        .typeWith(property.getter!!.returnType)
                        .withNoArgAnnotations(pluginContext, listOf(InjektFqNames.Provider))
                }
            }

            val fieldsByInjectFunction = injectFunctions.associateWith { function ->
                function.valueParameters
                    .map { valueParameter ->
                        addField {
                            name = Name.identifier(
                                nameProvider.allocateForGroup(
                                    "${function.name}\$${valueParameter.name}"
                                )
                            )
                            type = irBuiltIns.function(0)
                                .typeWith(valueParameter.type)
                                .withNoArgAnnotations(pluginContext, listOf(InjektFqNames.Provider))
                        }
                    }
            }

            addConstructor {
                this.returnType = defaultType
                isPrimary = true
                visibility = Visibilities.PUBLIC
            }.apply {
                val valueParametersByField = (fieldsByInjectProperty.values +
                        fieldsByInjectFunction.values.flatten()).associateWith {
                    addValueParameter(
                        it.name.asString(),
                        it.type
                    )
                }

                body = irBlockBody {
                    with(
                        InjektDeclarationIrBuilder(
                            pluginContext,
                            symbol
                        )
                    ) {
                        initializeClassWithAnySuperClass(this@clazz.symbol)
                    }
                    valueParametersByField.forEach { (field, valueParameter) ->
                        +irSetField(
                            irGet(thisReceiver!!),
                            field,
                            irGet(valueParameter)
                        )
                    }
                }
            }

            val companion = buildClass {
                kind = ClassKind.OBJECT
                name = SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
                isCompanion = true
                this.visibility = Visibilities.PUBLIC
            }.apply clazz@{
                createImplicitParameterDeclarationWithWrappedDescriptor()

                addMetadataIfNotLocal()

                addConstructor {
                    this.returnType = defaultType
                    isPrimary = true
                    this.visibility = Visibilities.PUBLIC
                }.apply {
                    body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                        InjektDeclarationIrBuilder(pluginContext, symbol).run {
                            initializeClassWithAnySuperClass(this@clazz.symbol)
                        }
                    }
                }
            }.also { addChild(it) }

            val companionInjectFunctionsByProperty = injectProperties.associateWith { property ->
                companion.addFunction {
                    name =
                        Name.identifier(nameProvider.allocateForGroup("inject\$${property.name}"))
                    returnType = irBuiltIns.unitType
                }.apply {
                    dispatchReceiverParameter = thisReceiver!!.copyTo(this)

                    addMetadataIfNotLocal()

                    val instanceValueParameter = addValueParameter(
                        "\$instance",
                        clazz.defaultType
                    )
                    val valueValueParameter = addValueParameter(
                        property.name.asString(),
                        property.getter!!.returnType
                    )

                    body = irBlockBody {
                        +irCall(property.setter!!).apply {
                            dispatchReceiver = irGet(instanceValueParameter)
                            putValueArgument(0, irGet(valueValueParameter))
                        }
                    }
                }
            }

            val componentInjectFunctionsByFunctions = injectFunctions.associateWith { function ->
                companion.addFunction {
                    name =
                        Name.identifier(nameProvider.allocateForGroup("inject\$${function.name}"))
                    returnType = irBuiltIns.unitType
                }.apply {
                    dispatchReceiverParameter = thisReceiver!!.copyTo(this)

                    addMetadataIfNotLocal()

                    val instanceValueParameter = addValueParameter(
                        "\$instance",
                        clazz.defaultType
                    )
                    val valueValueParameters = function.valueParameters.map { valueParameter ->
                        addValueParameter(
                            valueParameter.name.asString(),
                            valueParameter.type
                        )
                    }

                    body = irBlockBody {
                        +irCall(function).apply {
                            dispatchReceiver = irGet(instanceValueParameter)
                            valueValueParameters.forEach {
                                putValueArgument(it.index - 1, irGet(it))
                            }
                        }
                    }
                }
            }

            addFunction {
                name = Name.identifier("invoke")
                returnType = irBuiltIns.unitType
            }.apply {
                dispatchReceiverParameter = thisReceiver!!.copyTo(this)

                overriddenSymbols += superTypes.single()
                    .getClass()!!
                    .functions
                    .single { it.name.asString() == "invoke" }
                    .symbol

                val instanceValueParameter = addValueParameter(
                    "\$instance",
                    clazz.defaultType
                )

                body = irBlockBody {
                    fieldsByInjectProperty.forEach { (property, field) ->
                        +irCall(companionInjectFunctionsByProperty.getValue(property)).apply {
                            dispatchReceiver = irGetObject(companion.symbol)
                            putValueArgument(0, irGet(instanceValueParameter))
                            putValueArgument(
                                1,
                                if (property.backingField!!.type.isFunction() &&
                                    property.backingField!!.type.hasAnnotation(InjektFqNames.Provider)
                                ) {
                                    irGetField(irGet(dispatchReceiverParameter!!), field)
                                } else {
                                    irCall(
                                        irBuiltIns.function(0)
                                            .functions
                                            .single { it.owner.name.asString() == "invoke" }
                                    ).apply {
                                        dispatchReceiver =
                                            irGetField(irGet(dispatchReceiverParameter!!), field)
                                    }
                                }
                            )
                        }
                    }

                    fieldsByInjectFunction.forEach { (function, fields) ->
                        +irCall(componentInjectFunctionsByFunctions.getValue(function)).apply {
                            dispatchReceiver = irGetObject(companion.symbol)
                            putValueArgument(0, irGet(instanceValueParameter))

                            fields.forEachIndexed { index, field ->
                                putValueArgument(
                                    index + 1,
                                    if (function.valueParameters[index].type.isFunction() &&
                                        function.valueParameters[index].type.hasAnnotation(
                                            InjektFqNames.Provider
                                        )
                                    ) {
                                        irGetField(irGet(dispatchReceiverParameter!!), field)
                                    } else {
                                        irCall(
                                            irBuiltIns.function(0)
                                                .functions
                                                .single { it.owner.name.asString() == "invoke" }
                                        ).apply {
                                            dispatchReceiver =
                                                irGetField(
                                                    irGet(dispatchReceiverParameter!!),
                                                    field
                                                )
                                        }
                                    }
                                )
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
            name = Name.identifier("inject\$${function.name}")
            returnType = irBuiltIns.unitType
        }.apply {
            dispatchReceiverParameter = function.dispatchReceiverParameter!!.copyTo(this)
            val valueParameters = function.valueParameters.map {
                addValueParameter(
                    it.getParameterName(),
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
                name = Name.identifier("inject\$${property.name}")
                returnType = irBuiltIns.unitType
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
