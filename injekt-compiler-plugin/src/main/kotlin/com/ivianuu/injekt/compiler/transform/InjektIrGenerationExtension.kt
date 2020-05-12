package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.transform.factory.FactoryFunctionAnnotationTransformer
import com.ivianuu.injekt.compiler.transform.factory.FactoryModuleTransformer
import com.ivianuu.injekt.compiler.transform.factory.InlineFactoryTransformer
import com.ivianuu.injekt.compiler.transform.factory.RootFactoryTransformer
import com.ivianuu.injekt.compiler.transform.module.ModuleTransformer
import com.ivianuu.injekt.compiler.transform.module.TypedModuleTransformer
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.dump

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

        FactoryFunctionAnnotationTransformer(pluginContext)
            .lower(moduleFragment)

        val typedModuleTransformer = TypedModuleTransformer(pluginContext)

        // add @InstanceFactory or @ImplFactory annotations
        val factoryModuleTransformer = FactoryModuleTransformer(
            pluginContext, typedModuleTransformer
        )
            .also { declarationStore.factoryModuleTransformer = it }
        val moduleTransformer = ModuleTransformer(
            pluginContext,
            declarationStore,
            typedModuleTransformer
        ).also { declarationStore.moduleTransformer = it }
        val factoryTransformer = RootFactoryTransformer(
            pluginContext,
            declarationStore
        ).also { declarationStore.factoryTransformer = it }

        // add a local @Factory fun at each call side of a inline factory
        InlineFactoryTransformer(pluginContext, declarationStore)
            .lower(moduleFragment)

        // transform typed modules
        typedModuleTransformer.lower(moduleFragment)

        // move the module block of @Factory createImpl { ... } to a @Module function
        factoryModuleTransformer.lower(moduleFragment)

        // transform @Module functions to their ast representation
        moduleTransformer.lower(moduleFragment)

        // create implementations for factories
        factoryTransformer.lower(moduleFragment)

        moduleFragment.files.forEach {
            println("file: ${it.dump()}")
        }
    }

}
