package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.Path
import com.ivianuu.injekt.compiler.PropertyPath
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.types.IrType

sealed class ModuleDeclaration

class ScopeDeclaration(val scopeType: IrType) : ModuleDeclaration()

class DependencyDeclaration(
    val dependencyType: IrType,
    val path: PropertyPath
) : ModuleDeclaration()

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
    val parameters: List<InjektDeclarationIrBuilder.ProviderParameter>,
    val scoped: Boolean,
    val path: Path
) : ModuleDeclaration()

class IncludedModuleDeclaration(
    val includedType: IrType,
    val path: PropertyPath
) : ModuleDeclaration()

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
