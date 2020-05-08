package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.typeArguments
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addField
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irSetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.isFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class ModuleImplementation(
    val function: IrFunction,
    val pluginContext: IrPluginContext,
    val symbols: InjektSymbols,
    val declarationStore: InjektDeclarationStore
) {

    val nameProvider = NameProvider()
    val providerFactory = ModuleProviderFactory(this, pluginContext)

    val declarationFactory = ModuleDeclarationFactory(
        this, pluginContext,
        symbols, nameProvider, declarationStore, providerFactory
    )

    val moduleDescriptor = ModuleDescriptorImplementation(
        this@ModuleImplementation,
        pluginContext,
        symbols
    )

    val initializerBlocks =
        mutableListOf<IrBuilderWithScope.(() -> IrExpression) -> IrExpression>()

    val parameterMap = mutableMapOf<IrValueParameter, IrValueParameter>()
    val fieldsByParameters = mutableMapOf<IrValueParameter, IrField>()

    val clazz: IrClass = buildClass {
        name = InjektNameConventions.getModuleClassNameForModuleFunction(function.name)
        visibility = function.visibility
    }.apply {
        parent = function.parent
        createImplicitParameterDeclarationWithWrappedDescriptor()
        (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)
        copyTypeParametersFrom(function)
    }

    fun build() {
        clazz.apply clazz@{
            val constructor = addConstructor {
                returnType = defaultType
                isPrimary = true
                visibility = Visibilities.PUBLIC
            }.apply {
                copyTypeParametersFrom(this@clazz)

                function.valueParameters
                    .filter {
                        !it.type.isFunction() ||
                                it.type.typeArguments.firstOrNull()?.classOrNull != symbols.providerDsl
                    }
                    .forEach { p ->
                        addValueParameter(
                            name = p.name.asString(),
                            type = p.type
                        ).also { parameterMap[p] = it }
                    }

                valueParameters.forEach { p ->
                    addField(
                        "p_${p.name.asString()}",
                        p.type
                    ).also { fieldsByParameters[p] = it }
                }
            }
            val declarations = parseModuleDefinition()
            moduleDescriptor.addDeclarations(declarations)

            addChild(moduleDescriptor.clazz)

            constructor.body = InjektDeclarationIrBuilder(pluginContext, symbol).run {
                builder.irBlockBody {
                    initializeClassWithAnySuperClass(this@clazz.symbol)

                    fieldsByParameters.forEach { (parameter, field) ->
                        +irSetField(
                            irGet(thisReceiver!!),
                            field,
                            irGet(parameter)
                        )
                    }

                    initializerBlocks.forEach {
                        +it(this) { irGet(thisReceiver!!) }
                    }
                }
            }

            transformChildrenVoid(object : IrElementTransformerVoid() {
                override fun visitGetValue(expression: IrGetValue): IrExpression {
                    return if (parameterMap.keys.none { it.symbol == expression.symbol }) {
                        super.visitGetValue(expression)
                    } else {
                        val newParameter = parameterMap[expression.symbol.owner]!!
                        val field = fieldsByParameters[newParameter]!!
                        return DeclarationIrBuilder(pluginContext, symbol).run {
                            irGetField(
                                irGet(thisReceiver!!),
                                field
                            )
                        }
                    }
                }
            })
        }

        function.file.addChild(clazz)
        function.body = InjektDeclarationIrBuilder(pluginContext, clazz.symbol).run {
            builder.irExprBody(irInjektIntrinsicUnit())
        }
    }

    private fun parseModuleDefinition(): List<ModuleDeclaration> {
        val declarations = mutableListOf<ModuleDeclaration>()
        function.body?.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                super.visitCall(expression)
                declarations += declarationFactory.create(expression)
                return expression
            }
        })

        return declarations
    }

}
