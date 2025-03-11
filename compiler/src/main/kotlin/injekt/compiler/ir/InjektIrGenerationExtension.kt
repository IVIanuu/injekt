/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package injekt.compiler.ir

import injekt.compiler.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.util.*
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
    moduleFragment.dumpToFiles(dumpDir, ctx)
  }
}
