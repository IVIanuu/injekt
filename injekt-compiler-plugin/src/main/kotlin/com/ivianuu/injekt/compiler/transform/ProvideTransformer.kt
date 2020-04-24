package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektSymbols
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTo
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.ir.passTypeArgumentsFrom
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrConstructor
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.ir.util.SymbolTable
import org.jetbrains.kotlin.ir.util.constructedClass
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.hasAnnotation
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.name.SpecialNames
import org.jetbrains.kotlin.psi2ir.findSingleFunction

class ProvideTransformer(context: IrPluginContext) : AbstractInjektTransformer(context) {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val provideFunctions = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitClass(declaration: IrClass): IrStatement {
                super.visitClass(declaration)

                if (declaration.annotations.hasAnnotation(InjektFqNames.Provide)) {
                    provideFunctions += declaration.constructors.single()
                }

                return declaration
            }

            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.annotations.hasAnnotation(InjektFqNames.Provide)) {
                    provideFunctions += declaration
                }
                return super.visitFunction(declaration)
            }
        })

        provideFunctions.forEach { provideFunction ->
            DeclarationIrBuilder(context, provideFunction.symbol).run {
                if (provideFunction is IrConstructor) {
                    val clazz = provideFunction.constructedClass
                    clazz.addChild(
                        provideFunctionProvider(
                            module = null,
                            provideFunction = provideFunction,
                            name = Name.identifier("${clazz.name.asString()}\$Provider")
                        )
                    )
                } else {
                    val module = provideFunction.parent as IrClass
                    module.addChild(
                        provideFunctionProvider(
                            module = module,
                            provideFunction = provideFunction,
                            name = Name.identifier("${provideFunction.name.asString()}\$Provider")
                        )
                    )
                }
            }
        }
        return super.visitModuleFragment(declaration)
    }

    fun IrBuilderWithScope.provideFunctionProvider(
        module: IrClass?,
        provideFunction: IrFunction,
        name: Name
    ) = buildClass {
        kind = if (provideFunction.valueParameters.isEmpty() && module?.kind == ClassKind.OBJECT)
            ClassKind.OBJECT else ClassKind.CLASS
        this.name = name
    }.apply clazz@ {
        superTypes += symbols.provider.typeWith(provideFunction.returnType)

        module?.let { copyTypeParametersFrom(it) }

        createImplicitParameterDeclarationWithWrappedDescriptor()

        val requiresModule = module?.kind == ClassKind.CLASS

        val moduleField = if (requiresModule) {
            addField(
                "module",
                module!!.defaultType
            )
        } else null

        val providerFields = provideFunction.valueParameters
            .map { valueParameter ->
                addField(
                    "${valueParameter.name}Provider",
                    when (valueParameter.type.classifierOrNull) {
                        symbols.lazy -> valueParameter.type
                        symbols.provider -> valueParameter.type
                        else -> symbols.provider.typeWith(valueParameter.type)
                    }
                )
            }

        addConstructor {
            returnType = defaultType
            isPrimary = true
        }.apply {
            copyTypeParametersFrom(this@clazz)

            if (requiresModule) {
                addValueParameter(
                    "module",
                    module!!.defaultType
                )
            }

            providerFields.forEach {
                addValueParameter(
                    it.name.asString(),
                    it.type
                )
            }

            body = irBlockBody {
                initializeClassWithAnySuperClass(this@clazz.symbol)

                if (moduleField != null) {
                    +irSetField(
                        irGet(thisReceiver!!),
                        moduleField,
                        irGet(valueParameters.first())
                    )
                }

                valueParameters
                    .drop(if (moduleField != null) 1 else 0)
                    .forEach {
                        +irSetField(
                            irGet(thisReceiver!!),
                            providerFields[it.index - (if (moduleField != null) 1 else 0)],
                            irGet(it)
                        )
                    }
            }
        }

        val companion = if (kind != ClassKind.OBJECT) {
            providerCompanion(module, provideFunction)
                .also { addChild(it) }
        } else null

        val createFunction = if (kind == ClassKind.OBJECT) {
            createFunction(this, module, provideFunction)
                .also { addChild(it) }
        } else {
            null
        }

        addFunction {
            this.name = Name.identifier("invoke")
            returnType = provideFunction.returnType
        }.apply func@{
            dispatchReceiverParameter = thisReceiver?.copyTo(this, type = defaultType)

            overriddenSymbols += symbolTable.referenceSimpleFunction(
                symbols.provider.descriptor.unsubstitutedMemberScope
                    .findSingleFunction(Name.identifier("invoke"))
            )

            body = irExprBody(
                irCall(companion?.functions?.single() ?: createFunction!!).apply {
                    dispatchReceiver =
                        if (companion != null) irGetObject(companion.symbol) else irGet(
                            dispatchReceiverParameter!!
                        )

                    passTypeArgumentsFrom(this@clazz)

                    if (moduleField != null) {
                        putValueArgument(
                            0,
                            irGetField(
                                irGet(dispatchReceiverParameter!!),
                                moduleField
                            )
                        )
                    }

                    providerFields.forEachIndexed { index, field ->
                        putValueArgument(
                            if (moduleField != null) index + 1 else index,
                            irCall(
                                symbolTable.referenceFunction(
                                    symbols.provider.descriptor.unsubstitutedMemberScope.findSingleFunction(
                                        Name.identifier(
                                            "invoke"
                                        )
                                    )
                                ),
                                (field.type as IrSimpleType).arguments.single().typeOrNull!!
                            ).apply {
                                dispatchReceiver = irGetField(
                                    irGet(dispatchReceiverParameter!!),
                                    field
                                )
                            }
                        )
                    }
                }
            )
        }
    }

    private fun IrBuilderWithScope.providerCompanion(
        module: IrClass?,
        provideFunction: IrFunction
    ) = buildClass {
        kind = ClassKind.OBJECT
        name = SpecialNames.DEFAULT_NAME_FOR_COMPANION_OBJECT
        isCompanion = true
    }.apply clazz@{
        createImplicitParameterDeclarationWithWrappedDescriptor()

        addConstructor {
            returnType = defaultType
            isPrimary = true
        }.apply {
            body = irBlockBody {
                initializeClassWithAnySuperClass(this@clazz.symbol)
            }
        }

        addChild(createFunction(this, module, provideFunction))
    }

    private fun IrBuilderWithScope.createFunction(
        owner: IrClass,
        module: IrClass?,
        provideFunction: IrFunction
    ) = buildFun {
        name = Name.identifier("create")
        returnType = provideFunction.returnType
    }.apply {
        copyTypeParametersFrom(provideFunction)
        dispatchReceiverParameter = owner.thisReceiver?.copyTo(this, type = owner.defaultType)

        val moduleParameter = if (module?.kind == ClassKind.CLASS) {
            addValueParameter(
                "module",
                module.defaultType
            )
        } else null

        provideFunction.valueParameters.forEach {
            valueParameters += it.copyTo(this, index = it.index + (if (moduleParameter != null) 1 else 0))
        }

        body = irExprBody(
            irCall(provideFunction).apply {
                dispatchReceiver = when {
                    moduleParameter != null -> irGet(moduleParameter)
                    module != null -> irGetObject(module.symbol)
                    else -> null
                }

                valueParameters
                    .drop(if (moduleParameter != null) 1 else 0)
                    .forEach {
                        putValueArgument(
                            it.index - (if (moduleParameter != null) 1 else 0),
                            irGet(it)
                        )
                    }
            }
        )
    }

}
