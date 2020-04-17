package com.ivianuu.injekt.compiler.resolve

import com.ivianuu.injekt.compiler.InjektClassNames
import com.ivianuu.injekt.compiler.InjektDeclarationStore
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

data class ComponentWithAccessor(
    val component: IrClass,
    val accessor: () -> IrExpression
)

class Graph(
    private val context: IrPluginContext,
    private val bindingTrace: BindingTrace,
    private val component: IrClass,
    private val modules: List<ModuleWithAccessor>,
    private val declarationStore: InjektDeclarationStore
) {

    val allBindings = mutableMapOf<Key, Binding>()

    private val allModules = mutableListOf<ModuleWithAccessor>()
    private val allParents = mutableListOf<ComponentWithAccessor>()

    init {
        collectModules()
        collectParents()
        collectBindings()
        validate()
    }

    private fun collectModules() {
        modules.forEach { collectModules(it) }
    }

    private fun collectModules(moduleWithAccessor: ModuleWithAccessor) {
        check(moduleWithAccessor !in allModules) {
            "Duplicated module ${moduleWithAccessor.module}"
        }
        allModules += moduleWithAccessor

        val module = moduleWithAccessor.module
        val metadata = module.descriptor.annotations.single {
            it.fqName == InjektClassNames.ModuleMetadata
        }

        metadata.getStringList("includedModuleTypes").zip(
            metadata.getStringList("includedModuleNames")
        ).forEach { (includedModuleType, includedModuleFieldName) ->
            val includedModule = declarationStore.getModule(FqName(includedModuleType))
            val field = module.fields.single { it.name.asString() == includedModuleFieldName }
            ModuleWithAccessor(includedModule) {
                DeclarationIrBuilder(this@Graph.context, module.symbol).run {
                    irGetField(
                        moduleWithAccessor.accessor(),
                        field
                    )
                }
            }.also { collectModules(it) }
        }
    }

    private fun collectParents() {
        allModules.forEach { moduleWithAccessor ->
            val module = moduleWithAccessor.module
            val metadata = module.descriptor.annotations.single {
                it.fqName == InjektClassNames.ModuleMetadata
            }

            val parentsKeys = metadata.getStringList("parents")
            val parentNames = metadata.getStringList("parentNames")

            val parentsForModule = parentsKeys.zip(parentNames) { key, fieldName ->
                val component = declarationStore.getComponent(key)
                ComponentWithAccessor(component) {
                    DeclarationIrBuilder(context, component.symbol).run {
                        irGetField(
                            moduleWithAccessor.accessor(),
                            module.fields.single { it.name.asString() == fieldName }
                        )
                    }
                }
            }

            parentsForModule.forEach { addParent(it) }
        }
    }

    private fun addParent(parent: ComponentWithAccessor) {
        check(!allParents.contains(parent)) {
            "Duplicated parent $parent"
        }

        allParents += parent
    }

    private fun addBinding(binding: Binding) {
        check(!allBindings.contains(binding.key)) {
            "Duplicated binding ${binding.key}"
        }

        allBindings[binding.key] = binding
    }

    private fun validate() {
        // check
        allBindings.forEach { (_, binding) ->
            binding.dependencies.forEach { dependency ->
                check(allBindings.contains(dependency)) {
                    "Missing binding for $dependency"
                }
            }
        }
    }

    private fun collectBindings() {
        allParents.forEach { componentWithAccessor ->
            val component = componentWithAccessor.component
            val metadata = component.descriptor.annotations.singleOrNull {
                it.fqName == InjektClassNames.ComponentMetadata
            }
                ?: error("Wtf for ${componentWithAccessor.component.dump()} this component is ${this.component.dump()}")

            val bindingKeys = metadata.getStringList("bindingKeys")

            val bindingProviders = metadata.getStringList("bindingNames")
                .map { providerName ->
                    component.declarations
                        .filterIsInstance<IrField>()
                        .first { it.name.asString() == providerName }
                }

            check(bindingKeys.size == bindingProviders.size) {
                "Invalid metadata keys $bindingKeys providers $bindingProviders"
            }

            val componentBindings = bindingKeys.zip(bindingProviders) { key, provider ->
                Binding(
                    Key((provider.type as IrSimpleType).arguments.single().typeOrNull!!.toKotlinType()),
                    Binding.BindingType.ComponentProvider(provider, componentWithAccessor),
                    componentWithAccessor.component,
                    emptyList()
                )
            }

            componentBindings.forEach { binding ->
                addBinding(binding)
            }
        }

        allModules.forEach { moduleWithAccessor ->
            val module = moduleWithAccessor.module
            val metadata = module.descriptor.annotations.single {
                it.fqName == InjektClassNames.ModuleMetadata
            }

            val bindingKeys = metadata.getStringList("bindingKeys")

            val bindingProviders = metadata.getStringList("bindingNames")
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

    private fun AnnotationDescriptor.getStringList(name: String): List<String> {
        return allValueArguments[Name.identifier(name)]?.safeAs<ArrayValue>()?.value
            ?.map { it.value }
            ?.filterIsInstance<String>()
            ?: emptyList()
    }
}
