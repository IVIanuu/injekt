package com.ivianuu.injekt.compiler.transform.module

import com.ivianuu.injekt.compiler.Path
import com.ivianuu.injekt.compiler.PropertyPath
import com.ivianuu.injekt.compiler.transform.InjektDeclarationIrBuilder
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.builders.IrBuilderWithScope
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionReference
import org.jetbrains.kotlin.ir.types.IrType

sealed class ModuleDeclaration(val statement: (IrBuilderWithScope.(() -> IrExpression) -> IrStatement)?)

class ScopeDeclaration(val scopeType: IrType) : ModuleDeclaration(null)

class DependencyDeclaration(
    val dependencyType: IrType,
    val path: PropertyPath,
    statement: IrBuilderWithScope.(() -> IrExpression) -> IrStatement
) : ModuleDeclaration(statement)

class ChildFactoryDeclaration(
    val factoryRef: IrFunctionReference,
    val factoryModuleClass: IrClass?
) : ModuleDeclaration(null)

class AliasDeclaration(
    val originalType: IrType,
    val aliasType: IrType
) : ModuleDeclaration(null)

class BindingDeclaration(
    val bindingType: IrType,
    val parameters: List<InjektDeclarationIrBuilder.FactoryParameter>,
    val scoped: Boolean,
    val inline: Boolean,
    val path: Path,
    statement: (IrBuilderWithScope.(() -> IrExpression) -> IrStatement)?
) : ModuleDeclaration(statement)

class IncludedModuleDeclaration(
    val includedType: IrType,
    val inline: Boolean,
    val path: Path,
    val capturedValueArguments: List<Parameter>,
    statement: (IrBuilderWithScope.(() -> IrExpression) -> IrStatement)?
) : ModuleDeclaration(statement) {
    data class Parameter(
        val path: Path,
        val type: IrType
    )
}

class MapDeclaration(
    val mapType: IrType
) : ModuleDeclaration(null)

class MapEntryDeclaration(
    val mapType: IrType,
    val entryKey: IrExpression,
    val entryValueType: IrType
) : ModuleDeclaration(null)

class SetDeclaration(
    val setType: IrType
) : ModuleDeclaration(null)

class SetElementDeclaration(
    val setType: IrType,
    val elementType: IrType
) : ModuleDeclaration(null)
