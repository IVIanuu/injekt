package com.ivianuu.injekt.compiler.resolve

import com.ivianuu.injekt.compiler.InjektDeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.resolve.constants.ArrayValue
import org.jetbrains.kotlin.utils.addToStdlib.safeAs

class Graph(
    private val context: IrPluginContext,
    private val bindingTrace: BindingTrace,
    private val component: IrClass,
    private val modules: List<ModuleWithAccessor>,
    private val declarationStore: InjektDeclarationStore
) {

    val allScopes = mutableListOf<FqName>()

    val allBindings = mutableMapOf<Key, Binding>()

    private val allModules = mutableListOf<ModuleWithAccessor>()
    private val allParents = mutableListOf<ComponentWithAccessor>()

    init {
        collectModules()
        collectScopes()
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
            it.fqName == InjektFqNames.ModuleMetadata
        }

        metadata.getStringList("includedModuleTypes").zip(
            metadata.getStringList("includedModuleNames")
        ).forEach { (includedModuleType, includedModuleFieldName) ->
            val includedModule = declarationStore.getModule(FqName(includedModuleType))
            val field = if (includedModuleFieldName == "null") null else
                module.fields.single { it.name.asString() == includedModuleFieldName }
            ModuleWithAccessor(includedModule) {
                DeclarationIrBuilder(this@Graph.context, module.symbol).run {
                    irGetField(
                        moduleWithAccessor.accessor(),
                        field
                            ?: error("No field for ${includedModule.dump()} in ${moduleWithAccessor.module.dump()}")
                    )
                }
            }.also { collectModules(it) }
        }
    }

    private fun collectScopes() {
        allModules.forEach { moduleWithAccessor ->
            val module = moduleWithAccessor.module
            val metadata = module.descriptor.annotations.single {
                it.fqName == InjektFqNames.ModuleMetadata
            }

            val scopes = metadata.getStringList("scopes")
            scopes.forEach { addScope(FqName(it)) }
        }
    }

    private fun collectParents() {
        allModules.forEach { moduleWithAccessor ->
            val module = moduleWithAccessor.module
            val metadata = module.descriptor.annotations.single {
                it.fqName == InjektFqNames.ModuleMetadata
            }

            val parentsKeys = metadata.getStringList("parents")
            val parentNames = metadata.getStringList("parentNames")

            val parentsForModule = parentsKeys.zip(parentNames) { key, fieldName ->
                val component = declarationStore.getComponent(key)
                ComponentWithAccessor(key, component) {
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

    private fun addScope(scope: FqName) {
        check(scope !in allScopes) {
            "Duplicated scope $scope"
        }

        allScopes += scope
    }

    private fun addParent(parent: ComponentWithAccessor) {
        check(allParents.none { it.key == parent.key }) {
            "Duplicated parent $parent"
        }

        allParents += parent
    }

    private fun addBinding(binding: Binding) {
        check(binding.key !in allBindings) {
            "Duplicated binding ${binding.key}"
        }

        allBindings[binding.key] = binding
    }

    private fun validate() {
        // check
        allBindings.forEach { (_, binding) ->
            binding.dependencies.forEach { dependency ->
                check(dependency in allBindings) {
                    "Missing binding for $dependency"
                }
            }
        }
    }

    private fun collectBindings() {
        allParents.forEach { componentWithAccessor ->
            val component = componentWithAccessor.component
            val componentMetadata = component.descriptor.annotations.singleOrNull {
                it.fqName == InjektFqNames.ComponentMetadata
            }
                ?: error("Wtf for ${componentWithAccessor.component.dump()} this component is ${this.component.dump()}")

            val bindingKeys = componentMetadata.getStringList("bindingKeys")

            val bindingProviders = componentMetadata.getStringList("bindingNames")
                .map { providerName ->
                    component.declarations
                        .filterIsInstance<IrField>()
                        .first { it.name.asString() == providerName }
                }

            check(bindingKeys.size == bindingProviders.size) {
                "Invalid metadata keys $bindingKeys providers $bindingProviders"
            }

            val componentBindings = bindingKeys.zip(bindingProviders) { key, provider ->
                ParentComponentBinding(
                    Key((provider.type as IrSimpleType).arguments.single().typeOrNull!!.toKotlinType()),
                    componentWithAccessor.component,
                    emptyList(),
                    provider,
                    componentWithAccessor
                )
            }

            componentBindings.forEach { binding ->
                addBinding(binding)
            }
        }

        allModules.forEach { moduleWithAccessor ->
            val module = moduleWithAccessor.module
            val moduleMetadata = module.descriptor.annotations.single {
                it.fqName == InjektFqNames.ModuleMetadata
            }

            val bindingKeys = moduleMetadata.getStringList("bindingKeys")

            val bindingProviders = moduleMetadata.getStringList("bindingNames")
                .map { providerName ->
                    module.declarations
                        .filterIsInstance<IrClass>()
                        .first { it.name.asString() == providerName }
                }

            check(bindingKeys.size == bindingProviders.size) {
                "Invalid metadata keys $bindingKeys providers $bindingProviders"
            }

            val moduleBindings = bindingKeys.zip(bindingProviders) { key, provider ->
                val providerMetadata = provider.descriptor.annotations.single {
                    it.fqName == InjektFqNames.ProviderMetadata
                }

                val isSingle =
                    providerMetadata.argumentValue("isSingle")?.value as? Boolean ?: false

                ModuleBinding(
                    key = Key(provider.typeParameters.single().superTypes.single().toKotlinType()),
                    containingDeclaration = moduleWithAccessor.module,
                    dependencies = provider.constructors.single().valueParameters
                        .filter { it.name.asString() != "module" }
                        .map { Key(it.type.toKotlinType().arguments.single().type) },
                    provider = provider,
                    module = moduleWithAccessor,
                    isSingle = isSingle
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
