package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.transform.factory.FactoryFunctionAnnotationTransformer
import com.ivianuu.injekt.compiler.transform.factory.FactoryModuleTransformer
import com.ivianuu.injekt.compiler.transform.factory.InlineFactoryTransformer
import com.ivianuu.injekt.compiler.transform.factory.RootFactoryTransformer
import com.ivianuu.injekt.compiler.transform.module.ModuleClassTransformer
import com.ivianuu.injekt.compiler.transform.module.ModuleFunctionTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment

class InjektIrGenerationExtension : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        val declarationStore = InjektDeclarationStore(pluginContext)

        // write qualifiers of expression to the irTrace
        QualifiedMetadataTransformer(pluginContext).lower(moduleFragment)

        // generate a members injector for each annotated class
        MembersInjectorTransformer(pluginContext)
            .also { declarationStore.membersInjectorTransformer = it }
            .lower(moduleFragment)

        // generate a provider for each annotated class
        ClassFactoryTransformer(pluginContext)
            .also { declarationStore.classFactoryTransformer = it }
            .lower(moduleFragment)

        // add @InstanceFactory or @ImplFactory annotations
        FactoryFunctionAnnotationTransformer(pluginContext)
            .lower(moduleFragment)

        val moduleFunctionTransformer = ModuleFunctionTransformer(pluginContext)

        val moduleClassTransformer = ModuleClassTransformer(
            pluginContext,
            declarationStore,
            moduleFunctionTransformer
        ).also { declarationStore.moduleClassTransformer = it }

        val factoryModuleTransformer = FactoryModuleTransformer(
            pluginContext, moduleFunctionTransformer
        )
            .also { declarationStore.factoryModuleTransformer = it }
        val factoryTransformer = RootFactoryTransformer(
            pluginContext,
            declarationStore
        ).also { declarationStore.factoryTransformer = it }

        // add a local @Factory fun at each call side of a inline factory
        InlineFactoryTransformer(pluginContext, declarationStore)
            .lower(moduleFragment)

        // transform typed modules
        moduleFunctionTransformer.lower(moduleFragment)

        // move the module block of @Factory createImpl { ... } to a @Module function
        factoryModuleTransformer.lower(moduleFragment)

        // transform @Module functions to their ast representation
        moduleClassTransformer.lower(moduleFragment)

        // create implementations for factories
        factoryTransformer.lower(moduleFragment)

        // patch file metadata
        FileMetadataPatcher(pluginContext).lower(moduleFragment)
    }

}
