package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.getNearestDeclarationContainer
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.irExprBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid

class ModuleClassTransformer(
    context: IrPluginContext,
    private val declarationStore: InjektDeclarationStore,
    private val moduleFunctionTransformer: ModuleFunctionTransformer
) : AbstractInjektTransformer(context) {

    private val transformedModules = mutableMapOf<IrFunction, IrClass>()
    private val transformingModules = mutableSetOf<IrFunction>()

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        val moduleFunctions = mutableListOf<IrFunction>()

        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.isModule(pluginContext.bindingContext)
                    && !moduleFunctionTransformer.isDecoy(declaration)
                ) {
                    moduleFunctions += declaration
                }

                return super.visitFunction(declaration)
            }
        })

        moduleFunctions.forEach { function ->
            getModuleClassForFunction(function)
        }

        return super.visitModuleFragment(declaration)
    }

    fun getModuleClassForFunction(function: IrFunction): IrClass {
        transformedModules[function]?.let { return it }
        val finalFunction = moduleFunctionTransformer.getTransformedModule(function)

        return DeclarationIrBuilder(pluginContext, finalFunction.file.symbol).run {
            check(finalFunction !in transformingModules) {
                "Circular dependency for module ${finalFunction.dump()}"
            }
            transformingModules += finalFunction
            val moduleImpl = ModuleImpl(
                finalFunction,
                pluginContext,
                symbols,
                declarationStore
            )
            finalFunction.getNearestDeclarationContainer().addChild(moduleImpl.clazz)
            finalFunction.body =
                InjektDeclarationIrBuilder(pluginContext, finalFunction.symbol).run {
                    builder.irExprBody(irInjektIntrinsicUnit())
                }
            transformedModules[finalFunction] = moduleImpl.clazz
            transformedModules[function] = moduleImpl.clazz
            transformingModules -= finalFunction
            moduleImpl.clazz
        }
    }

}
