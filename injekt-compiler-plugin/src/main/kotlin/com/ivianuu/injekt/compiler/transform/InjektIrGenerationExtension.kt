package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.generateSymbols
import com.ivianuu.injekt.compiler.transform.factory.FactoryFunctionAnnotationTransformer
import com.ivianuu.injekt.compiler.transform.factory.FactoryModuleTransformer
import com.ivianuu.injekt.compiler.transform.factory.RootFactoryTransformer
import com.ivianuu.injekt.compiler.transform.module.ModuleTransformer
import com.ivianuu.injekt.compiler.transform.module.TypedModuleTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class InjektIrGenerationExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        fun ModuleLoweringPass.visitModuleAndGenerateSymbols() {
            generateSymbols(pluginContext)
            lower(moduleFragment)
            generateSymbols(pluginContext)
        }

        val declarationStore = InjektDeclarationStore(pluginContext)

        // write qualifiers of expression to the irTrace
        QualifiedMetadataTransformer(pluginContext).visitModuleAndGenerateSymbols()

        // generate a members injector for each annotated class
        MembersInjectorTransformer(pluginContext)
            .also { declarationStore.membersInjectorTransformer = it }
            .visitModuleAndGenerateSymbols()

        // generate a provider for each annotated class
        ClassProviderTransformer(pluginContext)
            .also { declarationStore.classProviderTransformer = it }
            .visitModuleAndGenerateSymbols()

        FactoryFunctionAnnotationTransformer(pluginContext)
            .visitModuleAndGenerateSymbols()

        // move the module block of @Factory createImpl { ... } to a @Module function
        FactoryModuleTransformer(pluginContext)
            .also { declarationStore.factoryModuleTransformer = it }
            .visitModuleAndGenerateSymbols()

        val moduleTransformer = ModuleTransformer(
            pluginContext,
            declarationStore
        ).also { declarationStore.moduleTransformer = it }
        val factoryTransformer = RootFactoryTransformer(
            pluginContext,
            declarationStore
        ).also { declarationStore.factoryTransformer = it }

        TypedModuleTransformer(pluginContext).visitModuleAndGenerateSymbols()

        // transform @Module functions to their ast representation
        moduleTransformer.visitModuleAndGenerateSymbols()

        // create implementations for factories
        factoryTransformer.visitModuleAndGenerateSymbols()
    }

}
