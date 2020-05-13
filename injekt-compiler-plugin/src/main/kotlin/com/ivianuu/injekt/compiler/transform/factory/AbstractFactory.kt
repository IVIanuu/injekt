package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.typeWith
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.substitute

abstract class AbstractFactory(
    val moduleClass: IrClass,
    val typeParametersMap: Map<IrTypeParameterSymbol, IrType>,
    val pluginContext: IrPluginContext,
    val symbols: InjektSymbols,
    val declarationStore: InjektDeclarationStore
) {

    lateinit var graph: Graph
        private set

    abstract val factoryMembers: FactoryMembers

    lateinit var factoryExpressions: FactoryExpressions
        private set

    protected fun IrType.substituteWithFactoryTypeArguments(): IrType {
        (classifierOrFail as? IrTypeParameterSymbol)?.let {
            this@AbstractFactory.typeParametersMap[it]?.let { return it }
        }
        return substitute(typeParametersMap)
    }

    protected fun init(
        parent: AbstractFactory?,
        dependencyRequests: List<BindingRequest>,
        moduleAccessor: InitializerAccessor
    ) {
        factoryExpressions = FactoryExpressions(
            pluginContext = pluginContext,
            symbols = symbols,
            members = factoryMembers,
            parent = parent?.factoryExpressions,
            factory = this
        )
        graph = Graph(
            parent = parent?.graph,
            factory = this,
            context = pluginContext,
            factoryModule = ModuleNode(
                key = moduleClass.defaultType
                    .typeWith(*typeParametersMap.values.toTypedArray())
                    .asKey(),
                module = moduleClass,
                initializerAccessor = moduleAccessor,
                typeParametersMap = emptyMap()
            ),
            declarationStore = declarationStore,
            symbols = symbols,
            factoryMembers = factoryMembers
        ).also { factoryExpressions.graph = it }

        dependencyRequests.forEach { graph.validate(it) }
    }

    protected fun getModuleInitExpression(
        valueArguments: List<IrExpression>
    ): IrExpression {
        return DeclarationIrBuilder(pluginContext, moduleClass.symbol)
            .irCall(moduleClass.constructors.single()).apply {
                typeParametersMap.values.forEachIndexed { index, typeArgument ->
                    putTypeArgument(index, typeArgument)
                }
                valueArguments.forEachIndexed { index, valueArgument ->
                    putValueArgument(index, valueArgument)
                }
            }
    }
}
