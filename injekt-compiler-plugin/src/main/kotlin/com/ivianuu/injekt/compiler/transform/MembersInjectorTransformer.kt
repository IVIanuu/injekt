package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.withNoArgQualifiers
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.getClass
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.properties
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class MembersInjectorTransformer(context: IrPluginContext) : AbstractInjektTransformer(context) {

    private val membersInjectorByClass = mutableMapOf<IrClass, IrClass>()

    private val injectSettersByProperty = mutableMapOf<IrProperty, IrFunction>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val classes = mutableSetOf<IrClass>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitPropertyNew(declaration: IrProperty): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Inject)) {
                    classes += currentClass?.irElement as? IrClass ?: return super.visitPropertyNew(
                        declaration
                    )
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

    private fun getInjectSetter(property: IrProperty): IrFunction {
        return injectSettersByProperty.getOrPut(property) {
            val clazz = property.parent as IrClass
            fun IrClass.findInjectSetter(): IrFunction? {
                functions.singleOrNull {
                    it.name == Name.identifier("inject\$${property.name}")
                }?.let { return it }
                for (superType in superTypes) {
                    superType.classOrNull?.owner?.findInjectSetter()
                        ?.let { return it }
                }
                return null
            }

            clazz.findInjectSetter() ?: clazz.addFunction {
                this.name = Name.identifier("inject\$${property.name}")
                this.returnType = irBuiltIns.unitType
                visibility = Visibilities.PUBLIC
            }.apply func@{
                dispatchReceiverParameter = clazz.thisReceiver?.copyTo(this)

                val instanceValueParameter = addValueParameter(
                    name = "instance",
                    property.backingField!!.type
                )

                body = DeclarationIrBuilder(pluginContext, symbol).run {
                    irExprBody(
                        irSetField(
                            irGet(dispatchReceiverParameter!!),
                            property.backingField!!,
                            irGet(instanceValueParameter)
                        )
                    )
                }
            }
        }
    }

    private fun IrBuilderWithScope.membersInjector(clazz: IrClass): IrClass {
        val injectProperties = mutableListOf<IrProperty>()

        fun IrClass.collectInjectorProperties() {
            injectProperties += properties
                .filter { it.hasAnnotation(InjektFqNames.Inject) }
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
                        .typeWith(property.backingField!!.type)
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
                        +irCall(getInjectSetter(property)).apply {
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
