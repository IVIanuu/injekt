package com.ivianuu.injekt.compiler.resolve

import com.ivianuu.injekt.compiler.InjektClassNames
import com.ivianuu.injekt.compiler.ModuleStore
import com.ivianuu.injekt.compiler.getTopLevelClass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.builders.irGet
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class Graph(
    private val context: IrPluginContext,
    private val bindingTrace: BindingTrace,
    private val modules: List<ModuleWithAccessor>,
    private val moduleStore: ModuleStore
) {

    private val moduleMetadata =
        context.moduleDescriptor.getTopLevelClass(InjektClassNames.ModuleMetadata)
    private val provider = context.moduleDescriptor.getTopLevelClass(InjektClassNames.Provider)

    val componentBindings = mutableMapOf<Key, Binding>()

    private val allModules = mutableListOf<ModuleWithAccessor>()

    init {
        collectModules()
        collectBindings()
        resolveBindings()
    }

    private fun resolveBindings() {
        // check
        componentBindings.forEach { (_, binding) ->
            binding.dependencies.forEach { dependency ->
                check(componentBindings.contains(dependency)) {
                    "Missing binding for $dependency"
                }
            }
        }
    }

    private fun collectBindings() {
        allModules.forEach { moduleWithAccessor ->
            val module = moduleWithAccessor.module
            val metadata = module.descriptor.annotations.single {
                it.fqName == InjektClassNames.ModuleMetadata
            }

            val scopes = metadata.getStringList("scopes")
            val parents = metadata.getStringList("parents")

            val bindingKeys = metadata.getStringList("bindingKeys")

            val bindingProviders = metadata.getStringList("bindingProviders")
                .map { providerName ->
                    module.declarations
                        .filterIsInstance<IrClass>()
                        .first { it.name.asString() == providerName }
                }

            check(bindingKeys.size == bindingProviders.size) {
                "Invalid metadata keys $bindingKeys providers $bindingProviders"
            }

            val moduleBindings = bindingKeys.zip(bindingProviders) { key, provider ->
                Binding(
                    Key(provider.typeParameters.single().superTypes.single().toKotlinType()),
                    Binding.BindingType.ModuleProvider(provider, moduleWithAccessor),
                    moduleWithAccessor.module,
                    provider.constructors.single().valueParameters
                        .filter { it.name.asString() != "module" }
                        .map { Key(it.type.toKotlinType().arguments.single().type) }
                )
            }

            moduleBindings.forEach { binding ->
                addBinding(binding)
            }
        }
    }

    private fun addScope(scope: FqName) {

    }

    private fun addParent(parent: IrClass) {

    }

    private fun addBinding(binding: Binding) {
        check(!componentBindings.contains(binding.key)) {
            "Duplicated binding ${binding.key}"
        }

        componentBindings[binding.key] = binding
    }

    private fun collectModules() {
        modules.forEach { collectModules(it) }
    }

    private fun collectModules(moduleWithAccessor: ModuleWithAccessor) {
        allModules += moduleWithAccessor

        val module = moduleWithAccessor.module
        val metadata = module.descriptor.annotations.single {
            it.fqName == InjektClassNames.ModuleMetadata
        }

        metadata.getStringList("includedModuleTypes").zip(
            metadata.getStringList("includedModuleNames")
        ).forEach { (includedModuleType, includedModuleFieldName) ->
            val includedModule = moduleStore.getModule(FqName(includedModuleType))
            val field = module.fields.single { it.name.asString() == includedModuleFieldName }
            ModuleWithAccessor(
                includedModule
            ) {
                DeclarationIrBuilder(this@Graph.context, module.symbol).run {
                    irGetField(
                        irGet(module.thisReceiver!!),
                        field
                    )
                }
            }.also { collectModules(it) }
        }
    }

    private fun AnnotationDescriptor.getStringList(name: String): List<String> {
        return allValueArguments[Name.identifier(name)]?.safeAs<ArrayValue>()?.value
            ?.map { it.value }
            ?.filterIsInstance<String>()
            ?: emptyList()
    }
}
