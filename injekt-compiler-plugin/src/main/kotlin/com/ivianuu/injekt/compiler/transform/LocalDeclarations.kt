/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.transform

import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.visitors.*

class LocalDeclarations : IrElementTransformerVoid() {
  val localClasses = mutableSetOf<IrClass>()
  val localFunctions = mutableSetOf<IrFunction>()
  val localVariables = mutableSetOf<IrVariable>()

  override fun visitClass(declaration: IrClass): IrStatement {
    if (declaration.visibility == DescriptorVisibilities.LOCAL)
      localClasses += declaration
    return super.visitClass(declaration)
  }

  override fun visitFunction(declaration: IrFunction): IrStatement {
    if (declaration.visibility == DescriptorVisibilities.LOCAL)
      localFunctions += declaration
    return super.visitFunction(declaration)
  }

  override fun visitVariable(declaration: IrVariable): IrStatement {
    localVariables += declaration
    return super.visitVariable(declaration)
  }
}
