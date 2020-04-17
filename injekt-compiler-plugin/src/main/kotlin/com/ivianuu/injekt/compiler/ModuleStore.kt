package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.name.FqName

class ModuleStore(
    private val pluginContext: IrPluginContext,
    private val moduleFragment: IrModuleFragment
) {

    fun getModule(fqName: FqName): IrClass {
        return try {
            pluginContext.symbolTable.referenceClass(
                pluginContext.moduleDescriptor.getTopLevelClass(fqName)
            ).ensureBound(pluginContext.irProviders).owner
        } catch (e: Exception) {
            try {
                moduleFragment.files
                    .flatMap { it.declarations }
                    .filterIsInstance<IrClass>()
                    .firstOrNull { it.fqNameForIrSerialization == fqName }
                    ?: throw IllegalStateException(e)
            } catch (e: Exception) {
                throw IllegalStateException(
                    "Couldn't find module for $fqName",
                    e
                )
            }
        }
    }

}