/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package injekt.compiler.ir

import injekt.compiler.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.io.*

class InjektIrGenerationExtension(
  private val dumpDir: File,
  private val ctx: InjektContext
) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val compilationDeclarations = CompilationDeclarations()
    moduleFragment.transform(compilationDeclarations, null)
    moduleFragment.transform(InjectCallTransformer(compilationDeclarations, pluginContext, ctx), null)
    moduleFragment.patchDeclarationParents()
    moduleFragment.addInjektMetadata(ctx, pluginContext)
    moduleFragment.fixDefaultValueParameterReturnTypeForCompose(pluginContext)
    moduleFragment.dumpToFiles(dumpDir, ctx)
  }
}


/**
 * Compose has a bug which makes inliner crash if a default value's type of a parameter
 * in a inline @Composable function is of type Nothing
 */
fun IrModuleFragment.fixDefaultValueParameterReturnTypeForCompose(
  irCtx: IrPluginContext
) {
  transform(
    object : IrElementTransformerVoid() {
      override fun visitValueParameter(declaration: IrValueParameter): IrStatement {
        val result = super.visitValueParameter(declaration) as IrValueParameter
        val defaultValue = result.defaultValue
        if (defaultValue == null ||
          !defaultValue.expression.type.isNothing()) return declaration

        val function = result.parent as IrFunction
        if (!function.hasAnnotation(InjektFqNames.Composable) ||
          !function.isInline) return declaration

        declaration.defaultValue = DeclarationIrBuilder(
          irCtx,
          result.symbol,
          defaultValue.startOffset,
          defaultValue.endOffset
        ).run { irExprBody(irAs(irNull(), result.type)) }

        return result
      }
    },
    null
  )
}
