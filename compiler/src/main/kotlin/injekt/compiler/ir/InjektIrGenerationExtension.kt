/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package injekt.compiler.ir

import injekt.compiler.*
import injekt.compiler.fir.*
import injekt.compiler.fir.callableInfo
import injekt.compiler.fir.classifierInfo
import injekt.compiler.fir.encode
import injekt.compiler.fir.shouldBePersisted
import injekt.compiler.fir.toPersistedCallableInfo
import injekt.compiler.fir.toPersistedClassifierInfo
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.resolve.*
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

        if (!declaration.isLocal && declaration.origin == IrDeclarationOrigin.DEFINED) {
          if (declaration is IrClass || declaration is IrTypeAlias) {
            val firClassifierSymbol = (if (declaration is IrTypeAlias)
              ctx.session.symbolProvider.getClassLikeSymbolByClassId(declaration.classIdOrFail)
            else declaration.safeAs<IrMetadataSourceOwner>()
              ?.metadata?.safeAs<FirMetadataSource>()?.fir?.symbol?.safeAs<FirClassifierSymbol<*>>())
              ?: error("wtf")

            val classifierInfo = firClassifierSymbol.classifierInfo(ctx)
            if (classifierInfo.shouldBePersisted(ctx))
              addMetadata(classifierInfo.toPersistedClassifierInfo(ctx).encode())
          }

          if (declaration is IrFunction || declaration is IrProperty) {
            val firCallableSymbol = declaration.safeAs<IrMetadataSourceOwner>()
              ?.metadata?.safeAs<FirMetadataSource>()?.fir?.symbol?.cast<FirCallableSymbol<*>>()!!

            val callableInfo = firCallableSymbol.callableInfo(ctx)
            if (callableInfo.shouldBePersisted(ctx))
              addMetadata(callableInfo.toPersistedCallableInfo(ctx).encode())
          }
        }

        return super.visitDeclaration(declaration)
      }
    },
    null
  )
}

var dumpAllFiles = false

fun IrModuleFragment.dumpToFiles(dumpDir: File, context: InjektContext) {
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
