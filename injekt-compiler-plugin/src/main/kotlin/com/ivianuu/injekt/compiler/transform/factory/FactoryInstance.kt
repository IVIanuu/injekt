package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.transform.getNearestDeclarationContainer
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irTemporaryVar
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.statements
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class FactoryInstance(
    val factoryFunction: IrFunction,
    moduleClass: IrClass,
    context: IrPluginContext,
    symbols: InjektSymbols,
    declarationStore: InjektDeclarationStore
) : AbstractFactoryProduct(moduleClass, context, symbols, declarationStore) {

    override val factoryMembers =
        FunctionFactoryMembers(context, factoryFunction.getNearestDeclarationContainer()!!)

    init {
        with(DeclarationIrBuilder(context, factoryFunction.symbol)) {
            factoryFunction.body = irBlockBody {
                factoryMembers.blockBodyBuilder = this

                val moduleVar by lazy {
                    val moduleCall = factoryFunction.body!!.statements[0] as IrCall
                    irTemporaryVar(
                        irCall(moduleClass.constructors.single()).apply {
                            copyTypeArgumentsFrom(moduleCall)
                            (0 until moduleCall.valueArgumentsCount).forEach {
                                putValueArgument(it, moduleCall.getValueArgument(it))
                            }
                        }
                    )
                }

                init(
                    null,
                    listOf(
                        BindingRequest(
                            factoryFunction.returnType.asKey(context),
                            factoryFunction.descriptor.fqNameSafe
                        )
                    )
                ) { irGet(moduleVar) }

                val bindingExpression = factoryExpressions.getBindingExpression(
                    BindingRequest(
                        Key(factoryFunction.returnType),
                        factoryFunction.descriptor.fqNameSafe
                    )
                )(this, EmptyFactoryExpressionContext)

                +irReturn(bindingExpression)
            }
        }
    }

}
