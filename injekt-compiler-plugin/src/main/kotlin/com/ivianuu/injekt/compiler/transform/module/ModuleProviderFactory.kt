package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Visibility
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.at
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.declarations.IrValueDeclaration
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrGetValue
import org.jetbrains.kotlin.ir.expressions.IrReturn
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class ModuleProviderFactory(
    private val module: ModuleImpl,
    private val pluginContext: IrPluginContext
) {

    fun providerForClass(
        name: Name,
        clazz: IrClass,
        visibility: Visibility
    ): IrClass {
        val constructor = clazz.constructors.singleOrNull()
        return InjektDeclarationIrBuilder(pluginContext, module.clazz.symbol).factory(
            name = name,
            visibility = visibility,
            parameters = constructor?.valueParameters
                ?.mapIndexed { index, valueParameter ->
                    InjektDeclarationIrBuilder.FactoryParameter(
                        name = "p$index",
                        type = valueParameter.type,
                        assisted = valueParameter.hasAnnotation(InjektFqNames.Assisted),
                        requirement = false
                    )
                } ?: emptyList(),
            returnType = clazz.defaultType,
            createBody = { createFunction ->
                irExprBody(
                    if (clazz.kind == ClassKind.OBJECT) {
                        irGetObject(clazz.symbol)
                    } else {
                        irCall(constructor!!).apply {
                            createFunction.valueParameters.forEach { valueParameter ->
                                putValueArgument(
                                    valueParameter.index,
                                    irGet(valueParameter)
                                )
                            }
                        }
                    }
                )
            }
        )
    }

    fun providerForDefinition(
        name: Name,
        definition: IrFunctionExpression,
        visibility: Visibility,
        moduleFieldsByParameter: Map<IrValueDeclaration, IrField>
    ): IrClass {
        val definitionFunction = definition.function

        val type = definition.function.returnType

        val assistedParameterCalls = mutableListOf<IrCall>()
        val dependencyCalls = mutableListOf<IrCall>()
        val capturedModuleValueParameters = mutableListOf<IrValueDeclaration>()

        definitionFunction.body?.transformChildrenVoid(object :
            IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                when ((expression.dispatchReceiver as? IrGetValue)?.type) {
                    definitionFunction.extensionReceiverParameter!!.type -> {
                        dependencyCalls += expression
                    }
                    definitionFunction.valueParameters.single().type -> {
                        assistedParameterCalls += expression
                    }
                }
                return super.visitCall(expression)
            }

            override fun visitGetValue(expression: IrGetValue): IrExpression {
                super.visitGetValue(expression)

                if (moduleFieldsByParameter.keys.any { it.symbol == expression.symbol }) {
                    capturedModuleValueParameters += expression.symbol.owner
                }

                return expression
            }
        })

        val parameters = mutableListOf<InjektDeclarationIrBuilder.FactoryParameter>()

        if (capturedModuleValueParameters.isNotEmpty()) {
            parameters += InjektDeclarationIrBuilder.FactoryParameter(
                name = "module",
                type = module.clazz.defaultType,
                assisted = false,
                requirement = true
            )
        }

        val parametersByCall = mutableMapOf<IrCall, InjektDeclarationIrBuilder.FactoryParameter>()
        (assistedParameterCalls + dependencyCalls).forEachIndexed { i, call ->
            parameters += InjektDeclarationIrBuilder.FactoryParameter(
                name = "p$i",
                type = call.type,
                assisted = call in assistedParameterCalls,
                requirement = false
            ).also { parametersByCall[call] = it }
        }

        return InjektDeclarationIrBuilder(pluginContext, module.clazz.symbol).factory(
            name = name,
            visibility = visibility,
            parameters = parameters,
            returnType = type,
            createBody = { createFunction ->
                val body = definitionFunction.body!!
                body.transformChildrenVoid(object : IrElementTransformerVoid() {
                    override fun visitReturn(expression: IrReturn): IrExpression {
                        return if (expression.returnTargetSymbol != definitionFunction.symbol) {
                            super.visitReturn(expression)
                        } else {
                            at(expression.startOffset, expression.endOffset)
                            DeclarationIrBuilder(
                                pluginContext,
                                createFunction.symbol
                            ).irReturn(expression.value.transform(this, null))
                        }
                    }

                    override fun visitDeclaration(declaration: IrDeclaration): IrStatement {
                        if (declaration.parent == definitionFunction)
                            declaration.parent = createFunction
                        return super.visitDeclaration(declaration)
                    }

                    override fun visitCall(expression: IrCall): IrExpression {
                        super.visitCall(expression)
                        return when (expression) {
                            in assistedParameterCalls, in dependencyCalls -> {
                                irGet(createFunction.valueParameters.single {
                                    it.name.asString() == parametersByCall.getValue(expression).name
                                })
                            }
                            else -> expression
                        }
                    }

                    override fun visitGetValue(expression: IrGetValue): IrExpression {
                        return if (moduleFieldsByParameter.keys.none { it.symbol == expression.symbol }) {
                            super.visitGetValue(expression)
                        } else {
                            val field = moduleFieldsByParameter[expression.symbol.owner]!!
                            return irGetField(
                                irGet(createFunction.valueParameters.first()),
                                field
                            )
                        }
                    }

                })

                body
            }
        )
    }

}
