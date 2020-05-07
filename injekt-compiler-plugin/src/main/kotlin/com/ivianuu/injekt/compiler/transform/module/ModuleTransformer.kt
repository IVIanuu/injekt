package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.InjektNameConventions
import com.ivianuu.injekt.compiler.hasAnnotation
import com.ivianuu.injekt.compiler.transform.AbstractInjektTransformer
import com.ivianuu.injekt.compiler.transform.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.util.file
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.ir.visitors.transformChildrenVoid
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ModuleTransformer(
    context: IrPluginContext,
    private val declarationStore: InjektDeclarationStore
) : AbstractInjektTransformer(context) {

    private val moduleFunctions = mutableListOf<IrFunction>()
    private val transformedModules = mutableMapOf<IrFunction, IrClass>()
    private val transformingModules = mutableSetOf<FqName>()
    private var computedModuleFunctions = false

    override fun visitModuleFragment(declaration: IrModuleFragment): IrModuleFragment {
        computeModuleFunctionsIfNeeded()

        moduleFunctions.forEach { function ->
            DeclarationIrBuilder(pluginContext, function.file.symbol).run {
                getGeneratedModuleClass(function)
            }
        }

        moduleFunctions
            .map { it.file }
            .distinct()
            .forEach {
                (it as IrFileImpl).metadata =
                    MetadataSource.File(it.declarations.map { it.descriptor })
            }

        return super.visitModuleFragment(declaration)
    }

    fun getGeneratedModuleClass(function: IrFunction): IrClass? {
        computeModuleFunctionsIfNeeded()

        if (function !in moduleFunctions) return null

        transformedModules[function]?.let { return it }

        return DeclarationIrBuilder(pluginContext, function.file.symbol).run {
            val packageName = function.fqNameForIrSerialization.parent()
            val moduleName =
                InjektNameConventions.getModuleClassNameForModuleFunction(function.name)
            val moduleFqName = packageName.child(moduleName)
            check(moduleFqName !in transformingModules) {
                "Circular dependency for module $moduleFqName"
            }
            transformingModules += moduleFqName
            val moduleImplementation = ModuleImplementation(
                function,
                pluginContext,
                symbols,
                declarationStore
            ).also { it.build() }
            transformedModules[function] = moduleImplementation.clazz
            transformingModules -= moduleFqName
            moduleImplementation.clazz
        }
    }

    private fun computeModuleFunctionsIfNeeded() {
        if (computedModuleFunctions) return
        computedModuleFunctions = true
        moduleFragment.transformChildrenVoid(object : IrElementTransformerVoid() {
            override fun visitFunction(declaration: IrFunction): IrStatement {
                if (declaration.hasAnnotation(InjektFqNames.Module)
                    && (declaration.parent as? IrFunction)?.descriptor?.fqNameSafe?.asString() != "com.ivianuu.injekt.createImpl"
                    && (declaration.parent as? IrFunction)?.descriptor?.fqNameSafe?.asString() != "com.ivianuu.injekt.createInstance"
                ) {
                    moduleFunctions += declaration
                }

                return super.visitFunction(declaration)
            }
        })
    }

}
