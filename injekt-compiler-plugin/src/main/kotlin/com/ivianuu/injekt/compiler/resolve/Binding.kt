package com.ivianuu.injekt.compiler.resolve

import com.ivianuu.injekt.compiler.asTypeName
import com.ivianuu.injekt.compiler.removeIllegalChars
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.types.KotlinType

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
) : Binding(key, containingDeclaration, dependencies)

class ModuleBinding(
    key: Key,
    containingDeclaration: IrClass,
    dependencies: List<Key>,
    val provider: IrClass,
    val module: ModuleWithAccessor,
    val isSingle: Boolean
) : Binding(key, containingDeclaration, dependencies)

data class Key(val type: KotlinType) {
    val fieldName get() = type.toString().removeIllegalChars()
    val keyConstant: Int
        get() {
            return type.asTypeName()!!
                .toString()
                .hashCode()
        }
}
