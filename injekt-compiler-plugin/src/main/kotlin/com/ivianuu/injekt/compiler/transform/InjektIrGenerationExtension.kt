package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.generateSymbols
import com.ivianuu.injekt.compiler.transform.key.KeyCachingTransformer
import com.ivianuu.injekt.compiler.transform.key.KeyIntrinsicTransformer
import com.ivianuu.injekt.compiler.transform.key.KeyOfTransformer
import com.ivianuu.injekt.compiler.transform.module.ComponentDslIntrinsicTransformer
import com.ivianuu.injekt.compiler.transform.module.ComponentDslParamTransformer
import com.ivianuu.injekt.compiler.transform.module.ModuleAggregateGenerator
import com.ivianuu.injekt.compiler.transform.module.RegisterModuleFunctionGenerator
import org.jetbrains.kotlin.backend.common.extensions.IrGenerationExtension
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.util.DeepCopySymbolRemapper
import org.jetbrains.kotlin.resolve.DelegatingBindingTrace

class InjektIrGenerationExtension(private val project: Project) : IrGenerationExtension {

    override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
        fun ModuleLoweringPass.visitModuleAndGenerateSymbols() {
            generateSymbols(pluginContext)
            lower(moduleFragment)
            generateSymbols(pluginContext)
        }

        val bindingTrace = DelegatingBindingTrace(
            pluginContext.bindingContext, "trace in " +
                    "InjektIrGenerationExtension"
        )
        val symbolRemapper = DeepCopySymbolRemapper()

        // generate bindings for annotated classes
        ClassBindingGenerator(pluginContext, symbolRemapper, bindingTrace, project)
            .visitModuleAndGenerateSymbols()

        // generate bindings from binding definitions
        BindingIntrinsicTransformer(pluginContext, symbolRemapper, bindingTrace)
            .visitModuleAndGenerateSymbols()

        // rewrite calls like component.get<String>() -> component.get(keyOf<String>())
        KeyIntrinsicTransformer(
            pluginContext, symbolRemapper, bindingTrace
        ).visitModuleAndGenerateSymbols()

        RegisterModuleFunctionGenerator(pluginContext, symbolRemapper, bindingTrace)
            .visitModuleAndGenerateSymbols()

        ModuleAggregateGenerator(
            pluginContext,
            symbolRemapper,
            bindingTrace,
            project
        ).visitModuleAndGenerateSymbols()

        InjektInitTransformer(pluginContext, symbolRemapper, bindingTrace)
            .visitModuleAndGenerateSymbols()

        // cache static keyOf calls
        KeyCachingTransformer(
            pluginContext, symbolRemapper, bindingTrace
        )
            .visitModuleAndGenerateSymbols()

        // rewrite keyOf<String>() -> SimpleKey(String::class)
        KeyOfTransformer(pluginContext, symbolRemapper, bindingTrace)
            .visitModuleAndGenerateSymbols()

        ComponentDslParamTransformer(pluginContext, symbolRemapper, bindingTrace)
            .visitModuleAndGenerateSymbols()

        ComponentDslIntrinsicTransformer(pluginContext, symbolRemapper, bindingTrace)
            .visitModuleAndGenerateSymbols()
    }

}
