/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, ObsoleteDescriptorBasedAPI::class,
  FirIncompatiblePluginAPI::class
)

package com.ivianuu.injekt.compiler.transform

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.builders.declarations.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.declarations.impl.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.expressions.impl.*
import org.jetbrains.kotlin.ir.symbols.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.types.impl.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.utils.addToStdlib.*
import java.io.*

fun ClassDescriptor.irClass(irCtx: IrPluginContext): IrClass =
  irCtx.symbolTable.descriptorExtension.referenceClass(this).ensureBound(irCtx).owner

fun CallableDescriptor.irCallable(irCtx: IrPluginContext): IrFunction =
  if (this is PropertyDescriptor)
    irCtx.symbolTable.descriptorExtension.referenceProperty(this).ensureBound(irCtx).owner.getter!!
  else irCtx.symbolTable.referenceFunction(this).ensureBound(irCtx).owner

fun TypeRef.toIrType(irCtx: IrPluginContext): IrTypeArgument {
  if (isStarProjection) return IrStarProjectionImpl
  return when {
    classifier.isTag -> arguments.last().toIrType(irCtx)
      .typeOrNull!!
      .cast<IrSimpleType>()
      .let { type ->
        val tagConstructor = irCtx.referenceClass(classifier.fqName)!!.constructors.single()
        IrSimpleTypeImpl(
          type.originalKotlinType,
          type.classifier,
          type.nullability,
          type.arguments,
          listOf(
            DeclarationIrBuilder(irCtx, tagConstructor)
              .irCall(
                tagConstructor,
                tagConstructor.owner.returnType
                  .classifierOrFail
                  .typeWith(
                    arguments.dropLast(1)
                      .map { it.toIrType(irCtx).typeOrNull ?: irCtx.irBuiltIns.anyNType }
                  )
              ).apply {
                tagConstructor.owner.typeParameters.indices
                  .forEach { index ->
                    putTypeArgument(index, arguments[index].toIrType(irCtx).typeOrNull!!)
                  }
              }
          ) + type.annotations,
          type.abbreviation
        )
      }
    else -> IrSimpleTypeImpl(
      if (classifier.isTypeParameter) {
        when (val container = classifier.descriptor!!.containingDeclaration) {
          is CallableDescriptor -> container.irCallable(irCtx).typeParameters
          is ClassDescriptor -> container.irClass(irCtx).typeParameters
          else -> throw AssertionError("Unexpected container $container")
        }.single { it.descriptor.name == classifier.descriptor.name }.symbol
      } else irCtx.symbolTable.referenceClassifier(classifier.descriptor!!).ensureBound(irCtx),
      isMarkedNullable,
      arguments.map { it.toIrType(irCtx) },
      emptyList()
    )
  }
}

fun <T : IrSymbol> T.ensureBound(irCtx: IrPluginContext): T = apply {
  if (!isBound)
    (irCtx as IrPluginContextImpl).linker.run {
      getDeclaration(this@ensureBound)
      postProcess(false)
    }
}

fun IrBuilderWithScope.irLambda(
  type: IrType,
  startOffset: Int = UNDEFINED_OFFSET,
  endOffset: Int = UNDEFINED_OFFSET,
  body: IrBuilderWithScope.(IrFunction) -> IrExpression,
): IrExpression {
  type as IrSimpleType
  val returnType = type.arguments.last().typeOrNull!!

  val lambda = IrFactoryImpl.buildFun {
    origin = IrDeclarationOrigin.LOCAL_FUNCTION_FOR_LAMBDA
    name = Name.special("<anonymous>")
    this.returnType = returnType
    visibility = DescriptorVisibilities.LOCAL
  }.apply {
    parent = scope.getLocalDeclarationParent()
    type.arguments.forEachIndexed { index, typeArgument ->
      if (index < type.arguments.lastIndex) {
        addValueParameter(
          scope.inventNameForTemporary("p"),
          typeArgument.typeOrNull!!
        )
      }
    }
    annotations = annotations + type.annotations.map {
      it.deepCopyWithSymbols()
    }
    this.body = DeclarationIrBuilder(context, symbol).run {
      irBlockBody {
        +irReturn(body(this, this@apply))
      }
    }
  }

  return IrFunctionExpressionImpl(
    startOffset = startOffset,
    endOffset = endOffset,
    type = type,
    function = lambda,
    origin = IrStatementOrigin.LAMBDA
  )
}

fun IrModuleFragment.dumpToFiles(dumpDir: File, ctx: Context) {
  files
    .filter {
      dumpAllFiles ||
          ctx.cachedOrNull<_, Unit>(INJECTIONS_OCCURRED_IN_FILE_KEY, it.fileEntry.name) != null
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
