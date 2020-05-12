package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.transform.getNearestDeclarationContainer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irTemporaryVar
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class InstanceFactory(
    val factoryFunction: IrFunction,
    typeParameterMap: Map<IrTypeParameterSymbol, IrType>,
    moduleClass: IrClass,
    pluginContext: IrPluginContext,
    symbols: InjektSymbols,
    declarationStore: InjektDeclarationStore
) : AbstractFactory(moduleClass, typeParameterMap, pluginContext, symbols, declarationStore) {

    override val factoryMembers =
        FunctionFactoryMembers(pluginContext, factoryFunction.getNearestDeclarationContainer())

    fun getInstanceExpression(
        valueArguments: List<IrExpression>
    ): IrExpression {
        return DeclarationIrBuilder(pluginContext, factoryFunction.symbol).run {
            irBlock {
                factoryMembers.blockBuilder = this

                val moduleVar by lazy {
                    irTemporaryVar(getModuleInitExpression(valueArguments))
                }

                val instanceRequest = BindingRequest(
                    factoryFunction.returnType
                        .substituteWithFactoryTypeArguments()
                        .asKey(),
                    factoryFunction.descriptor.fqNameSafe
                )

                init(
                    null,
                    listOf(instanceRequest)
                ) { irGet(moduleVar) }

                +factoryExpressions.getBindingExpression(instanceRequest)(
                    this,
                    EmptyFactoryExpressionContext
                )
            }
        }
    }
}
