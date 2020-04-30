package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.InjektSymbols
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.fqNameForIrSerialization
import org.jetbrains.kotlin.name.FqName

class InjektDeclarationStore(
    private val context: IrPluginContext,
    private val moduleFragment: IrModuleFragment,
    private val symbols: InjektSymbols
) {

    lateinit var moduleTransformer: ModuleTransformer

    /*fun getComponent(key: String): IrClass {
        return try {
            componentTransformer.getProcessedComponent(key)
                ?: throw DeclarationNotFound("Couldn't find for $key")
        } catch (e: DeclarationNotFound) {
            try {
                moduleFragment.files
                    .flatMap { it.declarations }
                    .filterIsInstance<IrClass>()
                    .singleOrNull { it.name.asString().endsWith("$key\$Impl") }
                    ?.name
                    ?.asString()
                    ?.replace("_", ".")
                    ?.let { fqName ->
                        moduleFragment.files
                            .flatMap { it.declarations }
                            .filterIsInstance<IrClass>()
                            .singleOrNull {
                                it.fqNameForIrSerialization.asString() == fqName
                            }
                    }
                    ?: throw DeclarationNotFound("Decls ${moduleFragment.files.flatMap { it.declarations }
                        .map { it.descriptor.name }}")
            } catch (e: DeclarationNotFound) {
                try {
                    val componentPackage = context.moduleDescriptor
                        .getPackage(InjektFqNames.InjektModulesPackage)

                    return componentPackage.memberScope
                        .getContributedDescriptors()
                        .filterIsInstance<ClassDescriptor>()
                        .singleOrNull { it.name.asString().endsWith("$key\$Impl") }
                        ?.name
                        ?.asString()
                        ?.replace("_", ".")
                        ?.let { symbols.getTopLevelClass(FqName(it)).owner }
                        ?: throw DeclarationNotFound("Found ${componentPackage.memberScope.getClassifierNames()}")
                } catch (e: DeclarationNotFound) {
                    throw DeclarationNotFound("Couldn't find component for $key")
                }
            }
        }
    }*/

    fun getModule(fqName: FqName): IrClass {
        return try {
            moduleTransformer.getProcessedModule(fqName)
                ?: throw DeclarationNotFound()
        } catch (e: DeclarationNotFound) {
            try {
                moduleFragment.files
                    .flatMap { it.declarations }
                    .filterIsInstance<IrClass>()
                    .firstOrNull { it.fqNameForIrSerialization == fqName }
                    ?: throw DeclarationNotFound()
            } catch (e: DeclarationNotFound) {
                try {
                    symbols.getTopLevelClass(fqName).owner
                } catch (e: DeclarationNotFound) {
                    throw DeclarationNotFound("Couldn't find module for $fqName")
                }
            }
        }
    }

}

class DeclarationNotFound(message: String? = null) : RuntimeException(message)
