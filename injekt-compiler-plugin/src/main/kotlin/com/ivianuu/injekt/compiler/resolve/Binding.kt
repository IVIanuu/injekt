package com.ivianuu.injekt.compiler.resolve

import com.ivianuu.injekt.compiler.removeIllegalChars
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrField
import org.jetbrains.kotlin.types.KotlinType

data class Binding(
    val key: Key,
    val bindingType: BindingType,
    val containingDeclaration: IrClass,
    val dependencies: List<Key>
) {
    sealed class BindingType {
        data class ComponentProvider(
            val provider: IrField,
            val componentWithAccessor: ComponentWithAccessor
        ) : BindingType()
        data class ModuleProvider(
            val provider: IrClass,
            val module: ModuleWithAccessor
        ) : BindingType()
    }
}

data class Key(val type: KotlinType) {
    val fieldName get() = type.toString().removeIllegalChars()
    val keyConstant get() = type.toString()
}
