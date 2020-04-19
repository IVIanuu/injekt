package com.ivianuu.injekt.compiler.resolve

import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField

sealed class Binding(
    val key: Key,
    val containingDeclaration: IrClass,
    val dependencies: List<Key>
)

class ParentComponentBinding(
    key: Key,
    containingDeclaration: IrClass,
    dependencies: List<Key>,
    val providerField: IrField,
    val componentWithAccessor: ComponentWithAccessor
) : Binding(
    key = key,
    containingDeclaration = containingDeclaration,
    dependencies = dependencies
)

class ModuleBinding(
    key: Key,
    containingDeclaration: IrClass,
    dependencies: List<Key>,
    val provider: IrClass,
    val module: ModuleWithAccessor,
    val isSingle: Boolean
) : Binding(
    key = key,
    containingDeclaration = containingDeclaration,
    dependencies = dependencies
)
