package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class InjektDeclarationStore(
    private val pluginContext: IrPluginContext,
    private val moduleFragment: IrModuleFragment
) {

    fun getComponentFqName(key: String): FqName {
        return try {
            val componentPackage = pluginContext.moduleDescriptor
                .getPackage(InjektClassNames.InjektComponentsPackage)
            return componentPackage.memberScope
                .getContributedDescriptors()
                .filterIsInstance<ClassDescriptor>()
                .single { it.name.asString().startsWith("$key\$") }
                .fqNameSafe
        } catch (e: Exception) {
            try {
                moduleFragment.files
                    .flatMap { it.declarations }
                    .filterIsInstance<IrClass>()
                    .singleOrNull { it.name.asString().startsWith("$key\$") }
                    ?.fqNameForIrSerialization
                    ?: throw IllegalStateException(e)
            } catch (e: Exception) {
                throw IllegalStateException("Couldn't find component for $key", e)
            }
        }
    }

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
                throw IllegalStateException("Couldn't find module for $fqName", e)
            }
        }
    }

}
