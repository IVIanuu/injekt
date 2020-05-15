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
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.getNearestDeclarationContainer
import com.ivianuu.injekt.compiler.withNoArgQualifiers
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
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
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
import org.jetbrains.kotlin.ir.builders.irTemporaryVar
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

class MembersInjectorTransformer(context: IrPluginContext) : AbstractInjektTransformer(context) {

    private val membersInjectorByClass = mutableMapOf<IrClass, IrClass>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val classes = mutableSetOf<IrClass>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitPropertyNew(declaration: IrProperty): IrStatement {
                val field = declaration.backingField
                if (field != null && field.origin == IrDeclarationOrigin.PROPERTY_DELEGATE &&
                    field.initializer?.expression?.type?.classOrNull == symbols.injectProperty
                ) {
                    classes += currentClass?.irElement as? IrClass ?: return super.visitPropertyNew(
                        declaration
                    )

                    DeclarationIrBuilder(pluginContext, declaration.symbol).run {
                        declaration.backingField = buildField {
                            name = Name.identifier("injected\$${declaration.name}")
                            type = irBuiltIns.anyNType
                        }.apply {
                            initializer = irExprBody(irGetObject(symbols.uninitialized))
                        }

                        val oldGetter = declaration.getter!!

                        declaration.getter = declaration.addGetter {
                            name = oldGetter.name
                            returnType = oldGetter.returnType
                        }.apply {
                            dispatchReceiverParameter =
                                oldGetter.dispatchReceiverParameter!!.copyTo(this)

                            body = irBlockBody {
                                val tmp = irTemporaryVar(
                                    irGetField(
                                        irGet(dispatchReceiverParameter!!),
                                        declaration.backingField!!
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

                                +DeclarationIrBuilder(pluginContext, symbol)
                                    .irReturn(irImplicitCast(irGet(tmp), returnType))
                            }
                        }

                        declaration.setter = declaration.addSetter {
                            name = Name.identifier("inject\$${declaration.name}")
                            returnType = irBuiltIns.unitType
                        }.apply {
                            dispatchReceiverParameter =
                                declaration.getter!!.dispatchReceiverParameter!!.copyTo(this)

                            val valueParameter =
                                addValueParameter("value", declaration.getter!!.returnType)

                            body = irExprBody(
                                irSetField(
                                    irGet(dispatchReceiverParameter!!),
                                    declaration.backingField!!,
                                    irGet(valueParameter)
                                )
                            )
                        }
                    }
                }

                return super.visitPropertyNew(declaration)
            }
        })

        classes.forEach { clazz -> getMembersInjectorForClass(clazz) }

        return super.visitModuleFragment(declaration)
    }

    fun getMembersInjectorForClass(clazz: IrClass): IrClass {
        membersInjectorByClass[clazz]?.let { return it }
        val membersInjector = DeclarationIrBuilder(pluginContext, clazz.symbol)
            .membersInjector(clazz)
        clazz.getNearestDeclarationContainer(includeThis = false).addChild(membersInjector)
        membersInjectorByClass[clazz] = membersInjector
        return membersInjector
    }

    private fun IrBuilderWithScope.membersInjector(clazz: IrClass): IrClass {
        val injectProperties = mutableListOf<IrProperty>()

        fun IrClass.collectInjectorProperties() {
            injectProperties += properties
                .filter {
                    it.backingField?.name?.asString()?.startsWith("injected\$") == true
                }
            superTypes
                .forEach { it.classOrNull?.owner?.collectInjectorProperties() }
        }

        clazz.collectInjectorProperties()

        return buildClass {
            kind = if (injectProperties.isNotEmpty()) ClassKind.CLASS else ClassKind.OBJECT
            name = InjektNameConventions.getMembersInjectorNameForClass(clazz.name)
            visibility = clazz.visibility
        }.apply clazz@{
            superTypes += irBuiltIns.function(1)
                .typeWith(clazz.defaultType, irBuiltIns.unitType)

            createImplicitParameterDeclarationWithWrappedDescriptor()

            (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)

            var fieldIndex = 0
            val fieldsByInjectProperty = injectProperties.associateWith { property ->
                addField {
                    name = Name.identifier("p${fieldIndex++}")
                    type = irBuiltIns.function(0)
                        .typeWith(property.getter!!.returnType)
                        .withNoArgQualifiers(pluginContext, listOf(InjektFqNames.Provider))
                }
            }

            addConstructor {
                this.returnType = defaultType
                isPrimary = true
                visibility = Visibilities.PUBLIC
            }.apply {
                val valueParametersByField = fieldsByInjectProperty.values.associateWith {
                    addValueParameter(
                        it.name.asString(),
                        it.type
                    )
                }

                body = irBlockBody {
                    with(InjektDeclarationIrBuilder(pluginContext, symbol)) {
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
                    "instance",
                    clazz.defaultType
                )

                body = irBlockBody {
                    fieldsByInjectProperty.forEach { (property, field) ->
                        +irCall(property.setter!!).apply {
                            dispatchReceiver = irGet(instanceValueParameter)
                            putValueArgument(
                                0,
                                irCall(
                                    irBuiltIns.function(0)
                                        .functions
                                        .single { it.owner.name.asString() == "invoke" }
                                ).apply {
                                    dispatchReceiver =
                                        irGetField(irGet(dispatchReceiverParameter!!), field)
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}
