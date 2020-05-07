package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.builders.irTemporaryVar
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.copyTypeArgumentsFrom
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.statements

class FactoryInstance(
    val factoryFunction: IrFunction,
    moduleClass: IrClass,
    context: IrPluginContext,
    symbols: InjektSymbols,
    factoryTransformer: TopLevelFactoryTransformer,
    declarationStore: InjektDeclarationStore
) : AbstractFactoryProduct(moduleClass, context, symbols, factoryTransformer, declarationStore) {

    override val factoryMembers = FunctionFactoryMembers(context, factoryFunction)

    private lateinit var moduleAccessor: IrBuilderWithScope.() -> IrExpression

    init {
        init(
            null,
            listOf(DependencyRequest(factoryFunction.returnType.asKey(context)))
        ) { moduleAccessor() }
        DeclarationIrBuilder(context, factoryFunction.symbol).writeBody()
    }

    private fun IrBuilderWithScope.writeBody() {
        factoryFunction.body = irBlockBody {
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
            moduleAccessor = { irGet(moduleVar) }

            val bindingExpression = factoryExpressions.getBindingExpression(
                BindingRequest(Key(factoryFunction.returnType))
            )(this, EmptyFactoryExpressionContext)

            var lastRoundFields: Map<Key, FactoryField>? = null
            while (true) {
                val fieldsToInitialize = factoryMembers.fields
                    .filterKeys { it !in factoryMembers.initializedFields }
                if (fieldsToInitialize.isEmpty()) {
                    break
                } else if (lastRoundFields == fieldsToInitialize) {
                    error("Initializing error ${lastRoundFields.keys}")
                }
                lastRoundFields = fieldsToInitialize

                fieldsToInitialize.forEach { (key, field) ->
                    val initExpr = field.initializer(this, EmptyFactoryExpressionContext)
                    if (initExpr != null) {
                        val fieldVar = irTemporaryVar(initExpr)
                        field.initialize { irGet(fieldVar) }
                        factoryMembers.initializedFields += key
                    }
                }
            }

            factoryMembers.getFunctions.forEach {
                it.parent = scope.getLocalDeclarationParent()
                +it
            }

            +irReturn(bindingExpression)
        }
    }
}
