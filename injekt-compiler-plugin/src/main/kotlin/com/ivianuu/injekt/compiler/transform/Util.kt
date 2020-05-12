package com.ivianuu.injekt.compiler.transform

import org.jetbrains.kotlin.ir.IrElement
import org.jetbrains.kotlin.ir.declarations.IrDeclaration
import org.jetbrains.kotlin.ir.declarations.IrDeclarationContainer

fun IrDeclaration.getNearestDeclarationContainer(
    includeThis: Boolean = true
): IrDeclarationContainer {
    var current: IrElement? = if (includeThis) this else parent
    while (current != null) {
        if (current is IrDeclarationContainer) return current
        current = (current as? IrDeclaration)?.parent
    }

    error("Couldn't get declaration container for $this")
}
