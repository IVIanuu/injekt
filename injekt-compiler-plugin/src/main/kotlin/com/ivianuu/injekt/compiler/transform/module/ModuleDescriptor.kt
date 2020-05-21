/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.ClassPath
import com.ivianuu.injekt.compiler.InjektSymbols
import com.ivianuu.injekt.compiler.buildClass
import com.ivianuu.injekt.compiler.remapTypeParameters
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.copyTypeParametersFrom
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.ir.builders.declarations.addFunction
import org.jetbrains.kotlin.ir.builders.declarations.addValueParameter
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irString
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFunctionImpl
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ModuleDescriptor(
    private val module: ModuleImpl,
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
            name = nameProvider.allocateForType(declaration.scopeType).asString(),
            returnType = declaration.scopeType.remapTypeParameters(module.function, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            (this as IrFunctionImpl).metadata = MetadataSource.Function(descriptor)
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(module.symbols.astScope)
        }
    }

    private fun addDependencyFunction(declaration: DependencyDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocateForType(declaration.dependencyType).asString(),
            returnType = declaration.dependencyType
                .remapTypeParameters(module.clazz, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            (this as IrFunctionImpl).metadata = MetadataSource.Function(descriptor)
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(module.symbols.astDependency)
            annotations += declaration.path
                .asAnnotation(DeclarationIrBuilder(pluginContext, symbol), symbols)
        }
    }

    private fun addChildFactoryFunction(declaration: ChildFactoryDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate(declaration.factoryRef.symbol.owner.name.asString()),
            returnType = declaration.factoryRef.symbol.owner.returnType
                .remapTypeParameters(module.clazz, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            (this as IrFunctionImpl).metadata = MetadataSource.Function(descriptor)
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(symbols.astChildFactory)
            if (declaration.factoryModuleClass != null) {
                annotations += ClassPath(declaration.factoryModuleClass)
                    .asAnnotation(DeclarationIrBuilder(pluginContext, symbol), symbols)
            }
            annotations += DeclarationIrBuilder(pluginContext, symbol).run {
                irCall(symbols.astName.constructors.single()).apply {
                    putValueArgument(
                        0,
                        irString(declaration.factoryRef.symbol.descriptor.fqNameSafe.asString())
                    )
                }
            }

            declaration.factoryRef.symbol.owner.valueParameters.forEach { valueParameter ->
                addValueParameter(
                    valueParameter.name.asString(),
                    valueParameter.type.remapTypeParameters(module.clazz, clazz)
                )
            }
        }
    }

    private fun addAliasFunction(declaration: AliasDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate(
                declaration.originalType.classifierOrFail.descriptor.name.asString() +
                        "as${declaration.aliasType.classifierOrFail.descriptor.name.asString()}"
            ),
            returnType = declaration.aliasType.remapTypeParameters(module.function, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            (this as IrFunctionImpl).metadata = MetadataSource.Function(descriptor)
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(symbols.astAlias)
            addValueParameter(
                name = "original",
                type = declaration.originalType.remapTypeParameters(module.function, clazz)
            )
        }
    }

    private fun addBindingFunction(declaration: BindingDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocateForType(declaration.bindingType).asString(),
            returnType = declaration.bindingType
                .remapTypeParameters(module.function, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            (this as IrFunctionImpl).metadata = MetadataSource.Function(descriptor)
            val builder = InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
            annotations += builder.noArgSingleConstructorCall(symbols.astBinding)
            if (declaration.scoped) {
                annotations += builder.noArgSingleConstructorCall(symbols.astScoped)
            }
            if (declaration.inline) {
                annotations += builder.noArgSingleConstructorCall(symbols.astInline)
            }
            annotations += declaration.path.asAnnotation(
                DeclarationIrBuilder(pluginContext, symbol),
                symbols
            )

            declaration.parameters.forEach { parameter ->
                addValueParameter(
                    name = parameter.name,
                    type = parameter.type.remapTypeParameters(module.function, clazz)
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
            returnType = declaration.includedType.remapTypeParameters(module.clazz, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            (this as IrFunctionImpl).metadata = MetadataSource.Function(descriptor)
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(symbols.astModule)
            if (declaration.inline) {
                annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                    .noArgSingleConstructorCall(symbols.astInline)
            }
            annotations += declaration.path.asAnnotation(
                DeclarationIrBuilder(pluginContext, symbol),
                symbols
            )

            declaration.capturedValueArguments.forEachIndexed { index, parameter ->
                addValueParameter(
                    "capture_$index",
                    parameter.type.remapTypeParameters(module.clazz, clazz)
                ).apply {
                    annotations += parameter.path.asAnnotation(
                        DeclarationIrBuilder(pluginContext, symbol),
                        symbols
                    )
                }
            }
        }
    }

    private fun addMapFunction(declaration: MapDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate("map"),
            returnType = declaration.mapType.remapTypeParameters(module.function, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            (this as IrFunctionImpl).metadata = MetadataSource.Function(descriptor)
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(symbols.astMap)
        }
    }

    private fun addMapEntryFunction(declaration: MapEntryDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate("mapEntry"),
            returnType = pluginContext.irBuiltIns.unitType,
            modality = Modality.ABSTRACT
        ).apply {
            (this as IrFunctionImpl).metadata = MetadataSource.Function(descriptor)
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(symbols.astMapEntry)
            addValueParameter(
                name = "map",
                type = declaration.mapType.remapTypeParameters(module.function, clazz)
            )
            addValueParameter(
                name = "entry",
                type = declaration.entryValueType.remapTypeParameters(module.function, clazz)
            ).apply {
                annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
                    .irMapKeyConstructorForKey(declaration.entryKey)
            }
        }
    }

    private fun addSetFunction(declaration: SetDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate("set"),
            returnType = declaration.setType.remapTypeParameters(module.function, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            (this as IrFunctionImpl).metadata = MetadataSource.Function(descriptor)
            annotations += InjektDeclarationIrBuilder(module.pluginContext, module.clazz.symbol)
                .noArgSingleConstructorCall(symbols.astSet)
        }
    }

    private fun addSetElementFunction(declaration: SetElementDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocate("setElement"),
            returnType = pluginContext.irBuiltIns.unitType,
            modality = Modality.ABSTRACT
        ).apply {
            (this as IrFunctionImpl).metadata = MetadataSource.Function(descriptor)
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
