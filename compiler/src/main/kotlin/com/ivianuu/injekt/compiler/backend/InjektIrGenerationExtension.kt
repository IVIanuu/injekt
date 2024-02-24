/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.backend

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.frontend.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.resolve.*
import java.io.*

var dumpAllFiles = false

class InjektIrGenerationExtension(
  private val dumpDir: File,
  private val ctx: InjektContext
) : IrGenerationExtension {
  override fun generate(moduleFragment: IrModuleFragment, pluginContext: IrPluginContext) {
    val compilationDeclarations = CompilationDeclarations()
    moduleFragment.transform(compilationDeclarations, null)
    moduleFragment.transform(InjectCallTransformer(compilationDeclarations, pluginContext, ctx), null)
    moduleFragment.patchDeclarationParents()
    moduleFragment.persistInfos(ctx, pluginContext)
    moduleFragment.dumpToFiles(dumpDir, ctx)
  }
}

private fun IrModuleFragment.persistInfos(ctx: InjektContext, irCtx: IrPluginContext) {
  transform(
    object : IrElementTransformerVoid() {
      override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        fun addMetadata(value: String) {
          irCtx.metadataDeclarationRegistrar.addMetadataVisibleAnnotationsToElement(
            declaration,
            DeclarationIrBuilder(irCtx, declaration.symbol)
              .irCallConstructor(
                irCtx.referenceConstructors(InjektFqNames.DeclarationInfo)
                  .single(),
                emptyList()
              ).apply {
                putValueArgument(
                  0,
                  IrConstImpl.string(
                    UNDEFINED_OFFSET,
                    UNDEFINED_OFFSET,
                    irCtx.irBuiltIns.stringType,
                    value
                  )
                )
              }
          )
        }

        if (declaration is IrClass || declaration is IrTypeAlias) {
          val classifierInfo: ClassifierInfo? =
            ctx.cachedOrNull("classifier_info", declaration.symbol.uniqueKey(ctx))
          if (classifierInfo != null && classifierInfo.shouldBePersisted(ctx))
            addMetadata(classifierInfo.toPersistedClassifierInfo(ctx).encode())
        }

        if (declaration is IrFunction || declaration is IrProperty) {
          val callableInfo: CallableInfo? =
            ctx.cachedOrNull("callable_info", declaration.symbol.uniqueKey(ctx))
          if (callableInfo != null && callableInfo.shouldBePersisted(ctx))
            addMetadata(callableInfo.toPersistedCallableInfo(ctx).encode())
        }

        return super.visitDeclaration(declaration)
      }
    },
    null
  )
}

private fun IrModuleFragment.dumpToFiles(dumpDir: File, context: InjektContext) {
  files
    .filter {
      dumpAllFiles ||
          context.cachedOrNull<_, Unit>(INJECTIONS_OCCURRED_IN_FILE_KEY, it.fileEntry.name) != null
    }
    .forEach { irFile ->
      val file = File(irFile.fileEntry.name)
      val content = try {
        buildString {
          appendLine(
            irFile.dumpKotlinLike(
              KotlinLikeDumpOptions(
                useNamedArguments = true,
                printFakeOverridesStrategy = FakeOverridesStrategy.NONE
              )
            )
          )
        }
      } catch (e: Throwable) {
        e.stackTraceToString()
      }
      val newFile = dumpDir
        .resolve(irFile.packageFqName.asString().replace(".", "/"))
        .also { it.mkdirs() }
        .resolve(file.name.removeSuffix(".kt"))
      try {
        newFile.createNewFile()
        newFile.writeText(content)
        println("Generated $newFile:\n$content")
      } catch (e: Throwable) {
        throw RuntimeException("Failed to create file ${newFile.absolutePath}\n$content")
      }
    }
}
