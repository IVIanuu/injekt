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
import com.ivianuu.injekt.compiler.NameProvider
import com.ivianuu.injekt.compiler.addMetadataIfNotLocal
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
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class ModuleDescriptor(
    private val moduleFunction: IrFunction,
    private val originalModuleFunction: IrFunction,
    private val pluginContext: IrPluginContext,
    private val symbols: InjektSymbols
) {

    lateinit var moduleClass: IrClass

    private val nameProvider = NameProvider()

    val clazz = buildClass {
        kind = ClassKind.INTERFACE
        name = Name.identifier("Descriptor")
    }.apply {
        createImplicitParameterDeclarationWithWrappedDescriptor()
        addMetadataIfNotLocal()
        copyTypeParametersFrom(moduleFunction)
    }

    fun addDeclarations(moduleDeclarations: List<ModuleDeclaration>) {
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
            returnType = declaration.scopeType
                .remapTypeParameters(originalModuleFunction, moduleFunction)
                .remapTypeParameters(moduleFunction, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            addMetadataIfNotLocal()
            annotations += InjektDeclarationIrBuilder(pluginContext, clazz.symbol)
                .noArgSingleConstructorCall(symbols.astScope)
        }
    }

    private fun addDependencyFunction(declaration: DependencyDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocateForType(declaration.dependencyType).asString(),
            returnType = declaration.dependencyType
                .remapTypeParameters(moduleClass, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            addMetadataIfNotLocal()
            annotations += InjektDeclarationIrBuilder(pluginContext, clazz.symbol)
                .noArgSingleConstructorCall(symbols.astDependency)
            declaration.path
                ?.asAnnotation(DeclarationIrBuilder(pluginContext, symbol), symbols)
                ?.let { annotations += it }
        }
    }

    private fun addChildFactoryFunction(declaration: ChildFactoryDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocateForGroup(declaration.factoryRef.symbol.owner.name.asString()),
            returnType = declaration.factoryRef.symbol.owner.returnType
                .remapTypeParameters(originalModuleFunction, moduleFunction)
                .remapTypeParameters(moduleFunction, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            addMetadataIfNotLocal()
            annotations += InjektDeclarationIrBuilder(pluginContext, clazz.symbol)
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
                    valueParameter.type
                        .remapTypeParameters(originalModuleFunction, moduleFunction)
                        .remapTypeParameters(moduleFunction, clazz)
                )
            }
        }
    }

    private fun addAliasFunction(declaration: AliasDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocateForGroup(
                declaration.originalType.classifierOrFail.descriptor.name.asString() +
                        "as${declaration.aliasType.classifierOrFail.descriptor.name.asString()}"
            ),
            returnType = declaration.aliasType
                .remapTypeParameters(originalModuleFunction, moduleFunction)
                .remapTypeParameters(moduleFunction, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            addMetadataIfNotLocal()
            annotations += InjektDeclarationIrBuilder(pluginContext, clazz.symbol)
                .noArgSingleConstructorCall(symbols.astAlias)
            addValueParameter(
                name = "original",
                type = declaration.originalType
                    .remapTypeParameters(originalModuleFunction, moduleFunction)
                    .remapTypeParameters(moduleFunction, clazz)
            )
        }
    }

    private fun addBindingFunction(declaration: BindingDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocateForType(declaration.bindingType).asString(),
            returnType = declaration.bindingType
                .remapTypeParameters(moduleClass, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            addMetadataIfNotLocal()
            val builder = InjektDeclarationIrBuilder(pluginContext, clazz.symbol)
            annotations += builder.noArgSingleConstructorCall(symbols.astBinding)
            if (declaration.scoped) {
                annotations += builder.noArgSingleConstructorCall(symbols.astScoped)
            }
            if (declaration.instance) {
                annotations += builder.noArgSingleConstructorCall(symbols.astInstance)
            }
            declaration.path
                ?.asAnnotation(DeclarationIrBuilder(pluginContext, symbol), symbols)
                ?.let { annotations += it }
        }
    }

    private fun addIncludedModuleFunction(declaration: IncludedModuleDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocateForGroup("module"),
            returnType = declaration.includedType.remapTypeParameters(moduleClass, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            addMetadataIfNotLocal()
            annotations += InjektDeclarationIrBuilder(pluginContext, clazz.symbol)
                .noArgSingleConstructorCall(symbols.astModule)
            declaration.path
                ?.asAnnotation(DeclarationIrBuilder(pluginContext, symbol), symbols)
                ?.let { annotations += it }
        }
    }

    private fun addMapFunction(declaration: MapDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocateForGroup("map"),
            returnType = declaration.mapType
                .remapTypeParameters(originalModuleFunction, moduleFunction)
                .remapTypeParameters(moduleFunction, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            addMetadataIfNotLocal()
            annotations += InjektDeclarationIrBuilder(pluginContext, clazz.symbol)
                .noArgSingleConstructorCall(symbols.astMap)
        }
    }

    private fun addMapEntryFunction(declaration: MapEntryDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocateForGroup("mapEntry"),
            returnType = pluginContext.irBuiltIns.unitType,
            modality = Modality.ABSTRACT
        ).apply {
            addMetadataIfNotLocal()
            annotations += InjektDeclarationIrBuilder(pluginContext, clazz.symbol)
                .noArgSingleConstructorCall(symbols.astMapEntry)
            if (declaration.providerPath != null) {
                annotations += declaration.providerPath.asAnnotation(
                    DeclarationIrBuilder(pluginContext, symbol),
                    symbols
                )
            }
            addValueParameter(
                name = "map",
                type = declaration.mapType
                    .remapTypeParameters(originalModuleFunction, moduleFunction)
                    .remapTypeParameters(moduleFunction, clazz)
            )
            addValueParameter(
                name = "entry",
                type = declaration.entryValueType
                    .remapTypeParameters(originalModuleFunction, moduleFunction)
                    .remapTypeParameters(moduleFunction, clazz)
            ).apply {
                annotations += InjektDeclarationIrBuilder(pluginContext, symbol)
                    .irMapKeyConstructorForKey(declaration.entryKey)
            }
        }
    }

    private fun addSetFunction(declaration: SetDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocateForGroup("set"),
            returnType = declaration.setType
                .remapTypeParameters(originalModuleFunction, moduleFunction)
                .remapTypeParameters(moduleFunction, clazz),
            modality = Modality.ABSTRACT
        ).apply {
            addMetadataIfNotLocal()
            annotations += InjektDeclarationIrBuilder(pluginContext, clazz.symbol)
                .noArgSingleConstructorCall(symbols.astSet)
        }
    }

    private fun addSetElementFunction(declaration: SetElementDeclaration) {
        clazz.addFunction(
            name = nameProvider.allocateForGroup("setElement"),
            returnType = pluginContext.irBuiltIns.unitType,
            modality = Modality.ABSTRACT
        ).apply {
            addMetadataIfNotLocal()
            annotations += InjektDeclarationIrBuilder(pluginContext, clazz.symbol)
                .noArgSingleConstructorCall(symbols.astSetElement)
            if (declaration.providerPath != null) {
                annotations += declaration.providerPath.asAnnotation(
                    DeclarationIrBuilder(pluginContext, symbol),
                    symbols
                )
            }
            addValueParameter(
                name = "set",
                type = declaration.setType
                    .remapTypeParameters(originalModuleFunction, moduleFunction)
                    .remapTypeParameters(moduleFunction, clazz)
            )
            addValueParameter(
                name = "element",
                type = declaration.elementType
                    .remapTypeParameters(originalModuleFunction, moduleFunction)
                    .remapTypeParameters(moduleFunction, clazz)
            )
        }
    }

}
