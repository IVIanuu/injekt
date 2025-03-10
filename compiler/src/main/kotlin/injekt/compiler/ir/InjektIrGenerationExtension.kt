/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package injekt.compiler.ir

import androidx.compose.compiler.plugins.kotlin.lower.*
import injekt.compiler.*
import injekt.compiler.fir.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.fir.backend.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
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
    moduleFragment.addInjektMetadata(ctx, pluginContext)
    moduleFragment.dumpToFiles(dumpDir, ctx)
  }
}

private fun IrModuleFragment.addInjektMetadata(ctx: InjektContext, irCtx: IrPluginContext) {
  transform(
    object : IrElementTransformerVoid() {
      override fun visitDeclaration(declaration: IrDeclarationBase): IrStatement {
        fun addMetadata(value: String) {
          val metadataHolderDeclaration = if (declaration !is IrTypeAlias) declaration
          else {
            val parent = declaration.parent.cast<IrDeclarationContainer>()
            IrFactoryImpl.buildFun {
              origin = IrDeclarationOrigin.DEFINED
              name = (declaration.name.asString() + "\$MetadataHolder").asNameId()
              returnType = irCtx.irBuiltIns.unitType
              visibility = DescriptorVisibilities.PUBLIC
              startOffset = declaration.startOffset
              endOffset = declaration.endOffset
            }.apply {
              this.parent = parent
              parent.declarations += this
              body = DeclarationIrBuilder(irCtx, symbol).irBlockBody {
                +irReturn(irUnit())
              }

              irCtx.metadataDeclarationRegistrar.registerFunctionAsMetadataVisible(this)
            }
          }

          irCtx.metadataDeclarationRegistrar.addMetadataVisibleAnnotationsToElement(
            metadataHolderDeclaration,
            DeclarationIrBuilder(irCtx, metadataHolderDeclaration.symbol)
              .irCallConstructor(
                irCtx.referenceConstructors(InjektFqNames.InjektMetadata)
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

            val classifierMetadata = firClassifierSymbol.classifierMetadata(ctx)
            if (classifierMetadata.shouldBePersisted(ctx))
              addMetadata(classifierMetadata.toPersistedClassifierMetadata(ctx).encode())
          }

          if (declaration is IrFunction || declaration is IrProperty) {
            val firCallableSymbol = declaration.safeAs<IrMetadataSourceOwner>()
              ?.metadata?.safeAs<FirMetadataSource>()?.fir?.symbol?.cast<FirCallableSymbol<*>>()!!

            val callableMetadata = firCallableSymbol.callableMetadata(ctx)
            if (callableMetadata.shouldBePersisted(ctx))
              addMetadata(callableMetadata.toPersistedCallableMetadata(ctx).encode())
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
            try {
              irFile.dumpSrc()
            } catch(e: Throwable) {
              irFile.dumpKotlinLike(
                KotlinLikeDumpOptions(
                  useNamedArguments = true,
                  printFakeOverridesStrategy = FakeOverridesStrategy.NONE
                )
              )
            }
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
