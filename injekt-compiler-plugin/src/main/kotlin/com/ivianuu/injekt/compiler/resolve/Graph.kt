package com.ivianuu.injekt.compiler.resolve

import com.ivianuu.injekt.compiler.InjektDeclarationStore
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.getStringList
import com.ivianuu.injekt.compiler.substituteByName
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.ir.builders.irGetField
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.ir.types.IrSimpleType
import org.jetbrains.kotlin.ir.types.toKotlinType
import org.jetbrains.kotlin.ir.types.typeOrNull
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.dump
import org.jetbrains.kotlin.ir.util.fields
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.annotations.argumentValue
import org.jetbrains.kotlin.types.typeUtil.isTypeParameter
import org.jetbrains.kotlin.utils.addToStdlib.cast

class Graph(
    private val context: IrPluginContext,
    thisComponentModule: ModuleWithAccessor,
    private val declarationStore: InjektDeclarationStore
) {

    val allScopes = mutableSetOf<FqName>()
    private val parentScopes = mutableSetOf<FqName>()

    val allBindings = mutableMapOf<Key, Binding>()

    private val allModules = mutableListOf<ModuleWithAccessor>()
    private val allParents = mutableListOf<ComponentWithAccessor>()

    init {
        addModule(thisComponentModule)
        validate()
    }

    private fun addScope(scope: FqName) {
        check(scope !in allScopes && scope !in parentScopes) {
            "Duplicated scope $scope"
        }

        allScopes += scope
    }

    private fun addParentScope(scope: FqName) {
        check(scope !in allScopes && scope !in parentScopes) {
            "Duplicated scope $scope"
        }

        parentScopes += scope
    }

    private fun addParent(parent: ComponentWithAccessor) {
        check(allParents.none { it.key == parent.key }) {
            "Duplicated parent $parent"
        }

        allParents += parent

        val metadata = parent.component.descriptor.annotations.single {
            it.fqName == InjektFqNames.ComponentMetadata
        }

        val scopes = metadata.getStringList("scopes")
        scopes.forEach { addParentScope(FqName(it)) }

        val bindingKeys = metadata.getStringList("bindingKeys")
        val bindingProviders = metadata.getStringList("bindingNames")
            .map { providerName ->
                parent.component.declarations
                    .filterIsInstance<IrField>()
                    .first { it.name.asString() == providerName }
            }

        check(bindingKeys.size == bindingProviders.size) {
            "Invalid metadata keys $bindingKeys providers $bindingProviders"
        }

        bindingKeys.zip(bindingProviders) { key, provider ->
            ParentComponentBinding(
                Key((provider.type as IrSimpleType).arguments.single().typeOrNull!!.toKotlinType()),
                parent.component,
                emptyList(),
                provider,
                parent
            )
        }.forEach { addBinding(it) }
    }

    private fun addModule(moduleWithAccessor: ModuleWithAccessor) {
        allModules += moduleWithAccessor

        val module = moduleWithAccessor.module
        val metadata = module.descriptor.annotations.single {
            it.fqName == InjektFqNames.ModuleMetadata
        }

        val scopes = metadata.getStringList("scopes")
        scopes.forEach { addScope(FqName(it)) }

        val parentsKeys = metadata.getStringList("parents")
        val parentNames = metadata.getStringList("parentNames")

        parentsKeys.zip(parentNames) { key, fieldName ->
            val component = declarationStore.getComponent(key)
            ComponentWithAccessor(key, component) {
                DeclarationIrBuilder(context, component.symbol).run {
                    irGetField(
                        moduleWithAccessor.accessor(),
                        module.fields.single { it.name.asString() == fieldName }
                    )
                }
            }
        }.forEach { addParent(it) }

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

        bindingKeys.zip(bindingProviders) { key, provider ->
            val providerMetadata = provider.descriptor.annotations.single {
                it.fqName == InjektFqNames.ProviderMetadata
            }

            val isSingle =
                providerMetadata.argumentValue("isSingle")?.value as? Boolean ?: false

            val returnType = provider.functions
                .single { it.name.asString() == "invoke" && it.valueParameters.isEmpty() }
                .returnType
                .substituteByName(moduleWithAccessor.typeParametersMap)

            ModuleBinding(
                key = Key(returnType.toKotlinType()),
                containingDeclaration = moduleWithAccessor.module,
                dependencies = provider.constructors.single().valueParameters
                    .filter { it.name.asString() != "module" }
                    .map {
                        Key(
                            it.type.substituteByName(moduleWithAccessor.typeParametersMap)
                                .cast<IrSimpleType>()
                                .arguments
                                .single()
                                .typeOrNull!!
                                .toKotlinType()
                        )
                    },
                provider = provider,
                module = moduleWithAccessor,
                isSingle = isSingle
            )
        }.forEach { addBinding(it) }

        metadata.getStringList("includedModuleTypes").zip(
            metadata.getStringList("includedModuleNames")
        ).forEach { (includedModuleType, includedModuleFieldName) ->
            val includedModule = declarationStore.getModule(FqName(includedModuleType))
            val field = if (includedModuleFieldName == "null") null else
                module.fields.single { it.name.asString() == includedModuleFieldName }
            val typeParametersMap = includedModule.typeParameters
                .map { it.symbol to (field!!.type as IrSimpleType).arguments[it.index].typeOrNull!! }
                .toMap()
            ModuleWithAccessor(includedModule, typeParametersMap) {
                DeclarationIrBuilder(this@Graph.context, module.symbol).run {
                    irGetField(
                        moduleWithAccessor.accessor(),
                        field
                            ?: error("No field for ${includedModule.dump()} in ${moduleWithAccessor.module.dump()}")
                    )
                }
            }.also { addModule(it) }
        }
    }

    private fun addBinding(binding: Binding) {
        check(!binding.key.type.isTypeParameter()) {
            "Binding type not refined ${binding.key}"
        }

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
}
