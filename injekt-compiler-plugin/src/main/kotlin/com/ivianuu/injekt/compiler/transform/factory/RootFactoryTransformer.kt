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
import org.jetbrains.kotlin.backend.common.ir.allParameters
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class RootFactoryTransformer(
    context: IrPluginContext,
    private val declarationStore: InjektDeclarationStore
) : AbstractInjektTransformer(context) {

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val factoryFunctions = mutableListOf<IrFunction>()

        declaration.transformChildrenVoid(object : IrElementTransformerVoidWithContext() {
            override fun visitFunctionNew(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Factory) && !declaration.isInline) {
                    factoryFunctions += declaration
                }
                return super.visitFunctionNew(declaration)
            }
        })

        factoryFunctions.forEach { function ->
            DeclarationIrBuilder(pluginContext, function.symbol).run {
                val valueArguments = function.allParameters
                    .map { irGet(it) }
                val moduleClass = declarationStore.getModuleClassForFunction(
                    declarationStore.getModuleFunctionForFactory(function)
                )
                function.body = irExprBody(
                    when {
                        function.hasAnnotation(InjektFqNames.AstImplFactory) -> {
                            val implFactory = ImplFactory(
                                parent = null,
                                irDeclarationParent = function.parent,
                                name = InjektNameConventions.getImplNameForFactoryFunction(function),
                                superType = function.returnType,
                                moduleClass = moduleClass,
                                typeParameterMap = emptyMap(),
                                pluginContext = pluginContext,
                                symbols = symbols,
                                declarationStore = declarationStore
                            )

                            function.getNearestDeclarationContainer()
                                .addChild(implFactory.clazz)

                            implFactory.getInitExpression(valueArguments)
                        }
                        function.hasAnnotation(InjektFqNames.AstInstanceFactory) -> {
                            val instanceFactory = InstanceFactory(
                                factoryFunction = function,
                                typeParameterMap = emptyMap(),
                                moduleClass = moduleClass,
                                pluginContext = pluginContext,
                                symbols = symbols,
                                declarationStore = declarationStore
                            )

                            instanceFactory.getInstanceExpression(valueArguments)
                        }
                        else -> error("Unexpected factory ${function.dump()}")
                    }
                )
            }
        }

        factoryFunctions
            .map { it.file }
            .distinct()
            .forEach {
                (it as IrFileImpl).metadata =
                    MetadataSource.File(it.declarations.map { it.descriptor })
            }

        return super.visitModuleFragment(declaration)
    }

}
