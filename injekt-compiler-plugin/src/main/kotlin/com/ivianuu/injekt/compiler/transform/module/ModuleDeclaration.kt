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

import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrValueParameter
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.types.IrType

sealed class ModuleDeclaration

sealed class ModuleDeclarationWithProperty(
    val property: IrProperty,
    val variableExpression: IrExpression
) : ModuleDeclaration()

class ScopeDeclaration(val scopeType: IrType) : ModuleDeclaration()

class DependencyDeclaration(
    val dependencyType: IrType,
    property: IrProperty,
    variableExpression: IrExpression
) : ModuleDeclarationWithProperty(property, variableExpression)

class ChildFactoryDeclaration(
    val factoryRef: IrFunctionReference,
    val factoryModuleClass: IrClass?
) : ModuleDeclaration()

class AliasDeclaration(
    val originalType: IrType,
    val aliasType: IrType
) : ModuleDeclaration()

class BindingDeclaration(
    val bindingType: IrType,
    val parameters: List<InjektDeclarationIrBuilder.FactoryParameter>,
    val scoped: Boolean,
    val instance: Boolean,
    property: IrProperty,
    variableExpression: IrExpression
) : ModuleDeclarationWithProperty(property, variableExpression)

class IncludedModuleDeclaration(
    val includedType: IrType,
    val moduleLambdaMap: Map<IrValueParameter, IrType>,
    property: IrProperty,
    variableExpression: IrExpression
) : ModuleDeclarationWithProperty(property, variableExpression)

class MapDeclaration(
    val mapType: IrType
) : ModuleDeclaration()

class MapEntryDeclaration(
    val mapType: IrType,
    val entryKey: IrExpression,
    val entryValueType: IrType
) : ModuleDeclaration()

class SetDeclaration(
    val setType: IrType
) : ModuleDeclaration()

class SetElementDeclaration(
    val setType: IrType,
    val elementType: IrType
) : ModuleDeclaration()
