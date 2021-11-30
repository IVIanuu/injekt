/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.transform

import androidx.compose.compiler.plugins.kotlin.*
import androidx.compose.compiler.plugins.kotlin.lower.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.shaded_injekt.*
import org.jetbrains.kotlin.backend.common.extensions.*
import org.jetbrains.kotlin.backend.common.lower.*
import org.jetbrains.kotlin.backend.jvm.ir.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.ir.*
import org.jetbrains.kotlin.ir.builders.*
import org.jetbrains.kotlin.ir.declarations.*
import org.jetbrains.kotlin.ir.expressions.*
import org.jetbrains.kotlin.ir.types.*
import org.jetbrains.kotlin.ir.util.*
import org.jetbrains.kotlin.ir.visitors.*
import org.jetbrains.kotlin.resolve.descriptorUtil.*
import org.jetbrains.kotlin.utils.addToStdlib.*

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrModuleFragment.fixComposeFunInterfacesPreCompose(
  @Inject ctx: Context,
  irCtx: IrPluginContext
) {
  if (!composeCompilerInClasspath) return

  transform(
    object : IrElementTransformerVoid() {
      override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        if (expression.operator == IrTypeOperator.SAM_CONVERSION &&
            expression.argument is IrFunctionExpression &&
            expression.type.isComposableFunInterface()) {
          val functionExpression = expression.argument as IrFunctionExpression
          val declaration = functionExpression.function
          if (!declaration.hasComposableAnnotation()) {
            declaration.annotations += DeclarationIrBuilder(irCtx, declaration.symbol)
              .irCallConstructor(
                irCtx.referenceConstructors(injektFqNames().composable)
                  .single(),
                emptyList()
              )
          }
          declaration.overriddenSymbols = listOf(
            expression.type.classOrNull!!
              .owner
              .getSingleAbstractMethod()!!
              .symbol
              .also {
                if (!it.owner.hasComposableAnnotation()) {
                  it.owner.annotations += DeclarationIrBuilder(irCtx, it.owner.symbol)
                    .irCallConstructor(
                      irCtx.referenceConstructors(injektFqNames().composable)
                        .single(),
                      emptyList()
                    )
                }
              }
          )
        }

        return super.visitTypeOperator(expression)
      }

      override fun visitFunction(declaration: IrFunction): IrStatement {
        if (declaration.isFakeOverride &&
            !declaration.isFakeOverriddenFromAny() &&
            declaration.parentAsClass.defaultType.isComposableFunInterface()) {
          if (!declaration.hasComposableAnnotation()) {
            declaration.annotations += DeclarationIrBuilder(irCtx, declaration.symbol)
              .irCallConstructor(
                irCtx.referenceConstructors(injektFqNames().composable)
                  .single(),
                emptyList()
              )
          }
          if (declaration.parentAsClass.defaultType.isComposableFunInterface()) {
            (declaration as IrSimpleFunction).overriddenSymbols = listOf(
              irBuiltins.function(
                declaration.valueParameters.size +
                    changedParamCount(declaration.valueParameters.size, 1) +
                    1
              ).owner.functions.first { it.name.asString() == "invoke" }.symbol
            )
          }
        }
        return super.visitFunction(declaration)
      }

      override fun visitCall(expression: IrCall): IrExpression {
        val result = super.visitCall(expression) as IrCall
        if (result.origin != IrStatementOrigin.INVOKE) return result

        val dispatchReceiverType = result.dispatchReceiver?.type
          ?: return result
        if (dispatchReceiverType.isComposableFunInterface() &&
            !dispatchReceiverType.hasComposableAnnotation()) {
          (dispatchReceiverType.annotations as ArrayList<IrConstructorCall>)
            .add(
              DeclarationIrBuilder(irCtx, result.symbol)
                .irCallConstructor(
                  irCtx.referenceConstructors(injektFqNames().composable)
                    .single(),
                  emptyList()
                )
            )
        }

        return result
      }
    },
    null
  )
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrModuleFragment.fixComposeFunInterfacesPostCompose(@Inject ctx: Context) {
  if (!composeCompilerInClasspath) return

  transform(
    object : IrElementTransformerVoid() {
      override fun visitFunction(declaration: IrFunction): IrStatement {
        if (declaration.hasComposableAnnotation()) {
          declaration.annotations = declaration.annotations
            .transform {
              if (it.type.classifierOrFail.descriptor.fqNameSafe != injektFqNames().composable ||
                  none { it.type.classifierOrFail.descriptor.fqNameSafe == injektFqNames().composable })
                    add(it)
            }

          if (declaration.parentClassOrNull?.defaultType?.superTypes()
              ?.any { it.isComposableFunInterface() } == true) {
            (declaration as IrSimpleFunction).overriddenSymbols = listOf(
              irBuiltins.function(declaration.valueParameters.size)
                .owner.functions.first { it.name.asString() == "invoke" }.symbol
            )
          }
        }
        return super.visitFunction(declaration)
      }
    },
    null
  )
}

@OptIn(ObsoleteDescriptorBasedAPI::class)
private fun IrType.isComposableFunInterface(@Inject ctx: Context): Boolean {
  val classifier = classifierOrNull?.descriptor?.toClassifierRef() ?: return false
  return classifier.descriptor!!.cast<ClassDescriptor>().isFun &&
      classifier.defaultType.anySuperType {
        it.classifier.fqName == injektFqNames().composable
      }
}