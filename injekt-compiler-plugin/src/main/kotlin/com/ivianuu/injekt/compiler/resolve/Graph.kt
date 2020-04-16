package com.ivianuu.injekt.compiler.resolve

import com.ivianuu.injekt.compiler.InjektClassNames
import com.ivianuu.injekt.compiler.getTopLevelClass
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class Graph(
    private val context: IrPluginContext,
    private val bindingTrace: BindingTrace,
    private val modules: List<ModuleWithAccessor>
) {

    private val moduleMetadata =
        context.moduleDescriptor.getTopLevelClass(InjektClassNames.ModuleMetadata)
    private val provider = context.moduleDescriptor.getTopLevelClass(InjektClassNames.Provider)

    val bindings = mutableMapOf<Key, Binding>()

    init {
        modules.forEach { moduleWithAccessor ->
            val module = moduleWithAccessor.module
            val metadata = module.descriptor.annotations.single {
                it.fqName == InjektClassNames.ModuleMetadata
            }.allValueArguments

            val scopes = metadata[Name.identifier("scopes")]?.value.safeAs<ArrayValue>()?.value
                ?.map { it.value }
                ?.filterIsInstance<String>()
                ?: emptyList()

            val parents = metadata[Name.identifier("parents")]?.safeAs<ArrayValue>()?.value
                ?.map { it.value }
                ?.filterIsInstance<String>()
                ?: emptyList()

            val bindingKeys = metadata[Name.identifier("bindingKeys")]?.safeAs<ArrayValue>()?.value
                ?.map { it.value }
                ?.filterIsInstance<String>()
                ?: emptyList()

            val bindingProviders =
                metadata[Name.identifier("bindingProviders")]?.safeAs<ArrayValue>()?.value
                    ?.map { it.value }
                    ?.filterIsInstance<String>()
                    ?.map { providerName ->
                        module.declarations
                            .filterIsInstance<IrClass>()
                            .first { it.name.asString() == providerName }
                    }
                    ?: emptyList()

            check(bindingKeys.size == bindingProviders.size) {
                "Invalid metadata keys $bindingKeys providers $bindingProviders"
            }

            val moduleBindings = bindingKeys.zip(bindingProviders) { key, provider ->
                Binding(
                    Key(provider.typeParameters.single().superTypes.single().toKotlinType()),
                    Binding.BindingType.ModuleProvider(provider, moduleWithAccessor),
                    provider.constructors.single().valueParameters
                        .filter { it.name.asString() != "module" }
                        .map { Key(it.type.toKotlinType().arguments.single().type) }
                )
            }

            moduleBindings.forEach { binding ->
                addBinding(binding)
            }

            val includes = metadata[Name.identifier("includes")]?.safeAs<ArrayValue>()?.value
                ?.map { it.value }
                ?.filterIsInstance<String>()
                ?: emptyList()
        }

        // check
        bindings.forEach { (_, binding) ->
            binding.dependencies.forEach { dependency ->
                check(bindings.contains(dependency)) {
                    "Missing binding for $dependency"
                }
            }
        }
    }

    private fun addBinding(binding: Binding) {
        check(!bindings.contains(binding.key)) {
            "Duplicated binding ${binding.key}"
        }

        bindings[binding.key] = binding
    }
}
