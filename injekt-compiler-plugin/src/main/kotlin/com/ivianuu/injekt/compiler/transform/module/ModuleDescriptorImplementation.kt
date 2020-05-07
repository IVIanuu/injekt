package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.ClassPath
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.name.Name

class ModuleDescriptorImplementation(
    private val module: ModuleImplementation,
    private val pluginContext: IrPluginContext,
    private val symbols: InjektSymbols
) {

    private val nameProvider = module.nameProvider

    val clazz = buildClass {
        kind = ClassKind.INTERFACE
        name = Name.identifier("Descriptor")
    }.apply {
        createImplicitParameterDeclarationWithWrappedDescriptor()
        (this as IrClassImpl).metadata = MetadataSource.Class(descriptor)
        copyTypeParametersFrom(module.function)
        annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
            .noArgSingleConstructorCall(symbols.astModule)
    }

    fun addDeclarations(
        moduleDeclarations: List<ModuleDeclaration>
    ) {
        moduleDeclarations.forEach { declaration ->
            when (declaration) {
                is ScopeDeclaration -> addScopeFunction(declaration)
                is DependencyDeclaration -> addDependencyFunction(declaration)
                is ChildFactoryDeclaration -> addChildFactoryFunction(declaration)
                is AliasDeclaration -> addAliasFunction(declaration)
                is BindingDeclaration -> addBindingFunction(declaration)
                is IncludedModuleDeclaration -> addIncludedModuleFunction(declaration)
                is MapDeclaration -> addMapFunction(declaration)
                is MapEntryDeclaration -> addMapEntryFunction(declaration)
                is SetDeclaration -> addSetFunction(declaration)
                is SetElementDeclaration -> addSetElementFunction(declaration)
            }.let { }
        }
    }

    private fun addScopeFunction(declaration: ScopeDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate("scope"),
            returnType = declaration.scopeType,
            modality = Modality.ABSTRACT
        ).apply {
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(module.symbols.astScope)
        }
    }

    private fun addDependencyFunction(declaration: DependencyDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate("dependency"),
            returnType = declaration.dependencyType,
            modality = Modality.ABSTRACT
        ).apply {
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(module.symbols.astDependency)
            annotations += declaration.path
                .asAnnotation(DeclarationIrBuilder(pluginContext, symbol), symbols)
        }
    }

    private fun addChildFactoryFunction(declaration: ChildFactoryDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate("child_factory"),
            returnType = declaration.factoryRef.symbol.owner.returnType,
            modality = Modality.ABSTRACT
        ).apply {
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(symbols.astChildFactory)
            if (declaration.factoryModuleClass != null) {
                annotations += ClassPath(declaration.factoryModuleClass)
                    .asAnnotation(DeclarationIrBuilder(pluginContext, symbol), symbols)
            }

            declaration.factoryRef.symbol.owner.valueParameters.forEachIndexed { index, valueParameter ->
                addValueParameter(
                    "p${index}",
                    valueParameter.type
                )
            }
        }
    }

    private fun addAliasFunction(declaration: AliasDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate("alias"),
            returnType = declaration.aliasType,
            modality = Modality.ABSTRACT
        ).apply {
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(symbols.astAlias)
            addValueParameter(
                name = "original",
                type = declaration.originalType
            )
        }
    }

    private fun addBindingFunction(declaration: BindingDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate("binding"),
            returnType = declaration.bindingType,
            modality = Modality.ABSTRACT
        ).apply {
            val builder = InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
            annotations += builder.noArgSingleConstructorCall(symbols.astBinding)
            if (declaration.scoped) {
                annotations += builder.noArgSingleConstructorCall(symbols.astScoped)
            }
            annotations += declaration.path.asAnnotation(
                DeclarationIrBuilder(pluginContext, symbol),
                symbols
            )

            declaration.parameters.forEach { parameter ->
                addValueParameter(
                    name = parameter.name,
                    type = parameter.type
                ).apply {
                    if (parameter.assisted) {
                        annotations += builder.noArgSingleConstructorCall(symbols.astAssisted)
                    }
                }
            }
        }
    }

    private fun addIncludedModuleFunction(declaration: IncludedModuleDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate("module"),
            returnType = declaration.includedType,
            modality = Modality.ABSTRACT
        ).apply {
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(symbols.astModule)
            annotations += declaration.path.asAnnotation(
                DeclarationIrBuilder(pluginContext, symbol),
                symbols
            )
        }
    }

    private fun addMapFunction(declaration: MapDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate("map"),
            returnType = declaration.mapType,
            modality = Modality.ABSTRACT
        ).apply {
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(symbols.astMap)
        }
    }

    private fun addMapEntryFunction(declaration: MapEntryDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate("map_entry"),
            returnType = pluginContext.irBuiltIns.unitType,
            modality = Modality.ABSTRACT
        ).apply {
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(symbols.astMapEntry)
            addValueParameter(
                name = "map",
                type = declaration.mapType
            )
            addValueParameter(
                name = "entry",
                type = declaration.entryValueType
            ).apply {
                annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
                    .irMapKeyConstructorForKey(declaration.entryKey)
            }
        }
    }

    private fun addSetFunction(declaration: SetDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate("set"),
            returnType = declaration.setType,
            modality = Modality.ABSTRACT
        ).apply {
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(symbols.astSet)
        }
    }

    private fun addSetElementFunction(declaration: SetElementDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate("set_element"),
            returnType = pluginContext.irBuiltIns.unitType,
            modality = Modality.ABSTRACT
        ).apply {
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(symbols.astSetElement)
            addValueParameter(
                name = "set",
                type = declaration.setType
            )
            addValueParameter(
                name = "element",
                type = declaration.elementType
            )
        }
    }

}
