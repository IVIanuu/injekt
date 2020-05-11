package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import com.ivianuu.injekt.compiler.transform.getNearestDeclarationContainer
import org.jetbrains.kotlin.backend.common.IrElementTransformerVoidWithContext
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer
import org.jetbrains.kotlin.ir.declarations.IrDeclarationParent
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.symbols.IrTypeParameterSymbol
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name

class RootFactoryTransformer(
    context: IrPluginContext,
    private val declarationStore: InjektDeclarationStore
) : AbstractInjektTransformer(context) {

    private data class CallWithFile(val call: IrCall, val file: IrFile)

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val nonInlineFactoryFunctions = mutableListOf<IrFunction>()
        val inlineFactoryCalls = mutableListOf<CallWithFile>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Factory) && !declaration.isInline) {
                    nonInlineFactoryFunctions += declaration
                }
                return super.visitFunctionNew(declaration)
            }

            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.owner.hasAnnotation(InjektFqNames.Factory) &&
                    expression.symbol.owner.isInline
                ) {
                    inlineFactoryCalls += CallWithFile(expression, currentFile)
                }
                return super.visitCall(expression)
            }
        })

        transformNoInlineFactoryFunctions(nonInlineFactoryFunctions)

        val factoryCallsByCall = transformInlineFactoryCalls(inlineFactoryCalls)
        declaration.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitCall(expression: IrCall): IrExpression {
                return factoryCallsByCall[expression] ?: super.visitCall(expression)
            }
        })

        (nonInlineFactoryFunctions
            .map { it.file } + inlineFactoryCalls.map { it.file })
            .distinct()
            .forEach {
                (it as IrFileImpl).metadata =
                    MetadataSource.File(it.declarations.map { it.descriptor })
            }

        return super.visitModuleFragment(declaration)
    }

    private fun transformNoInlineFactoryFunctions(
        nonInlineFactoryFunctions: List<IrFunction>
    ) {
        nonInlineFactoryFunctions.forEach { function ->
            DeclarationIrBuilder(pluginContext, function.symbol).run {
                val valueArguments = mutableListOf<IrExpression>()

                if (function.dispatchReceiverParameter != null) {
                    valueArguments += irGet(function.dispatchReceiverParameter!!)
                }

                if (function.extensionReceiverParameter != null) {
                    valueArguments += irGet(function.extensionReceiverParameter!!)
                }

                function.valueParameters.forEach { valueArguments += irGet(it) }

                function.body = irExprBody(
                    when {
                        function.hasAnnotation(InjektFqNames.AstImplFactory) -> {
                            createImplFactoryAndGetInitCall(
                                function = function,
                                name = InjektNameConventions.getImplNameForFactoryFunction(function),
                                parent = function.parent,
                                declarationContainer = function.getNearestDeclarationContainer(),
                                typeParameterMap = emptyMap(),
                                valueArguments = valueArguments
                            )
                        }
                        function.hasAnnotation(InjektFqNames.AstInstanceFactory) -> {
                            createInstanceFactoryAndGetInstanceExpression(
                                function = function,
                                typeParameterMap = emptyMap(),
                                valueArguments = valueArguments
                            )
                        }
                        else -> error("Unexpected factory body ${function.dump()}")
                    }
                )
            }
        }
    }

    private fun transformInlineFactoryCalls(
        inlineFactoryCalls: List<CallWithFile>
    ): Map<IrCall, IrExpression> {
        return inlineFactoryCalls.associateWith { (call, file) ->
            val function = call.symbol.owner
            val valueArguments = mutableListOf<IrExpression>()

            if (call.dispatchReceiver != null) {
                valueArguments += call.dispatchReceiver!!
            }

            if (call.extensionReceiver != null) {
                valueArguments += call.extensionReceiver!!
            }

            (0 until call.valueArgumentsCount).forEach {
                valueArguments += call.getValueArgument(it)!!
            }

            val typeParametersMap = function.typeParameters.associateWith {
                call.getTypeArgument(it.index)!!
            }.mapKeys { it.key.symbol }

            when {
                function.hasAnnotation(InjektFqNames.AstImplFactory) -> {
                    createImplFactoryAndGetInitCall(
                        function = function,
                        name = InjektNameConventions.getImplNameForFactoryCall(file, call),
                        parent = function.parent,
                        declarationContainer = file,
                        typeParameterMap = typeParametersMap,
                        valueArguments = valueArguments
                    )
                }
                function.hasAnnotation(InjektFqNames.AstInstanceFactory) -> {
                    createInstanceFactoryAndGetInstanceExpression(
                        function = function,
                        typeParameterMap = typeParametersMap,
                        valueArguments = valueArguments
                    )
                }
                else -> error("Unexpected factory body ${function.dump()}")
            }
        }.mapKeys { it.key.call }
    }

    private fun createImplFactoryAndGetInitCall(
        function: IrFunction,
        name: Name,
        parent: IrDeclarationParent,
        declarationContainer: IrDeclarationContainer,
        typeParameterMap: Map<IrTypeParameterSymbol, IrType>,
        valueArguments: List<IrExpression>
    ): IrExpression {
        val moduleClass = declarationStore.getModuleClass(
            declarationStore.getModuleFunctionForFactory(function)
        )

        val implFactory = ImplFactory(
            parent = null,
            irDeclarationParent = parent,
            name = name,
            superType = function.returnType,
            moduleClass = moduleClass,
            typeParameterMap = typeParameterMap,
            pluginContext = pluginContext,
            symbols = symbols,
            declarationStore = declarationStore
        )

        declarationContainer.addChild(implFactory.clazz)

        return implFactory.getInitExpression(valueArguments)
    }

    private fun createInstanceFactoryAndGetInstanceExpression(
        function: IrFunction,
        typeParameterMap: Map<IrTypeParameterSymbol, IrType>,
        valueArguments: List<IrExpression>
    ): IrExpression {
        val moduleClass = declarationStore.getModuleClass(
            declarationStore.getModuleFunctionForFactory(function)
        )

        val instanceFactory = InstanceFactory(
            factoryFunction = function,
            typeParameterMap = typeParameterMap,
            moduleClass = moduleClass,
            pluginContext = pluginContext,
            symbols = symbols,
            declarationStore = declarationStore
        )

        return instanceFactory.getInstanceExpression(valueArguments)
    }

}
