/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.ir

import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.symbols.IrSymbol
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid

class CompilationDeclarations : IrElementTransformerVoid() {
  val declarations = mutableListOf<IrSymbol>()
  override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
    declarations += declaration.symbol
    return super.visitDeclaration(declaration)
  }
}
