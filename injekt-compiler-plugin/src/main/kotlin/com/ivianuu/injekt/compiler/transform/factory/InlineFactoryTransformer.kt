package com.ivianuu.injekt.compiler.transform.factory

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.ir.builders.declarations.buildFun
import org.jetbrains.kotlin.ir.builders.irBlock
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.getArgumentsWithIr
import org.jetbrains.kotlin.ir.util.referenceFunction
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi2ir.findSingleFunction

class InlineFactoryTransformer(
    pluginContext: IrPluginContext,
    private val declarationStore: InjektDeclarationStore
) : AbstractInjektTransformer(pluginContext) {

    override fun visitFile(declaration: IrFile): IrFile {
        super.visitFile(declaration)
        val inlineFactoryCalls = mutableListOf<IrCall>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                if (expression.symbol.owner.hasAnnotation(InjektFqNames.Factory) &&
                    expression.symbol.owner.isInline
                ) {
                    inlineFactoryCalls += expression
                }
                return super.visitCall(expression)
            }
        })

        val newCallByOldCall = inlineFactoryCalls.associateWith { inlineFactoryCall ->
            val factoryFunction = inlineFactoryCall.symbol.owner
            DeclarationIrBuilder(pluginContext, inlineFactoryCall.symbol).run {
                irBlock {
                    val inlinedFactory = buildFun {
                        name = InjektNameConventions.getImplNameForFactoryCall(
                            declaration, inlineFactoryCall
                        )
                        returnType = inlineFactoryCall.type
                        visibility = Visibilities.LOCAL
                    }.apply {
                        parent = scope.getLocalDeclarationParent()

                        annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
                            .noArgSingleConstructorCall(symbols.factory)

                        // todo this should be transformed by the FactoryFunctionAnnotationTransformer
                        if (factoryFunction.hasAnnotation(InjektFqNames.AstInstanceFactory)) {
                            annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
                                .noArgSingleConstructorCall(symbols.astInstanceFactory)
                        } else if (factoryFunction.hasAnnotation(InjektFqNames.AstImplFactory)) {
                            annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
                                .noArgSingleConstructorCall(symbols.astImplFactory)
                        }

                        body = irBlockBody {
                            val factoryModule = declarationStore
                                .getModuleFunctionForFactory(inlineFactoryCall.symbol.owner)
                            +irCall(factoryModule).apply {
                                (0 until inlineFactoryCall.typeArgumentsCount).forEach {
                                    putTypeArgument(it, inlineFactoryCall.getTypeArgument(it)!!)
                                }
                                inlineFactoryCall.getArgumentsWithIr()
                                    .forEachIndexed { index, (_, valueArgument) ->
                                        putValueArgument(index, valueArgument)
                                    }
                            }
                            val createFunctionName =
                                if (factoryFunction.hasAnnotation(InjektFqNames.AstInstanceFactory)) {
                                    "createInstance"
                                } else if (factoryFunction.hasAnnotation(InjektFqNames.AstImplFactory)) {
                                    "createImpl"
                                } else {
                                    error("Unexpected factory function ${factoryFunction.dump()}")
                                }

                            +irCall(
                                symbolTable.referenceFunction(
                                    symbols.getPackage(InjektFqNames.InjektPackage)
                                        .memberScope
                                        .findSingleFunction(Name.identifier(createFunctionName))
                                ),
                                inlineFactoryCall.type
                            )
                        }
                    }

                    +inlinedFactory
                    +irCall(inlinedFactory)
                }
            }
        }

        declaration.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitCall(expression: IrCall): IrExpression {
                return newCallByOldCall[expression] ?: super.visitCall(expression)
            }
        })

        return declaration
    }

}