/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.transform

import androidx.compose.compiler.plugins.kotlin.*
import androidx.compose.compiler.plugins.kotlin.lower.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
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
import org.jetbrains.kotlin.types.checker.*
import org.jetbrains.kotlin.utils.addToStdlib.*

@OptIn(ObsoleteDescriptorBasedAPI::class)
fun IrModuleFragment.fixComposeFunInterfacesPreCompose(irCtx: IrPluginContext) {
  if (!composeCompilerInClasspath) return

  transform(
    object : IrElementTransformerVoid() {
      override fun visitTypeOperator(expression: IrTypeOperatorCall): IrExpression {
        if (expression.operator == IrTypeOperator.SAM_CONVERSION &&
            expression.type.isComposableFunInterface &&
            expression.argument.type.isComposableFunInterface
        )
              return expression.argument

        if (expression.operator == IrTypeOperator.SAM_CONVERSION &&
            expression.argument is IrFunctionExpression &&
            expression.type.isComposableFunInterface
        ) {
          val functionExpression = expression.argument as IrFunctionExpression
          val declaration = functionExpression.function
          if (!declaration.hasComposableAnnotation()) {
            declaration.annotations += DeclarationIrBuilder(irCtx, declaration.symbol)
              .irCallConstructor(
                irCtx.referenceConstructors(InjektFqNames.Composable)
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
                      irCtx.referenceConstructors(InjektFqNames.Composable)
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
            declaration.parentAsClass.defaultType.isComposableFunInterface
        ) {
          if (!declaration.hasComposableAnnotation()) {
            declaration.annotations += DeclarationIrBuilder(irCtx, declaration.symbol)
              .irCallConstructor(
                irCtx.referenceConstructors(InjektFqNames.Composable)
                  .single(),
                emptyList()
              )
          }
          if (declaration.parentAsClass.defaultType.isComposableFunInterface) {
            (declaration as IrSimpleFunction).overriddenSymbols = listOf(
              irBuiltins.functionN(
                declaration.valueParameters.size +
                    changedParamCount(declaration.valueParameters.size, 1) +
                    1
              )
                .functions
                .first { it.name.asString() == "invoke" }
                .symbol
            )
          }
        }
        return super.visitFunction(declaration)
      }

      override fun visitCall(expression: IrCall): IrExpression {
        val result = super.visitCall(expression) as IrCall
        val dispatchReceiverType = result.dispatchReceiver?.type
          ?: return result
        if (dispatchReceiverType.isComposableFunInterface &&
            !dispatchReceiverType.hasComposableAnnotation()) {
          (dispatchReceiverType.annotations as ArrayList<IrConstructorCall>)
            .add(
              DeclarationIrBuilder(irCtx, result.symbol)
                .irCallConstructor(
                  irCtx.referenceConstructors(InjektFqNames.Composable)
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
fun IrModuleFragment.fixComposeFunInterfacesPostCompose() {
  if (!composeCompilerInClasspath) return

  transform(
    object : IrElementTransformerVoid() {
      override fun visitFunction(declaration: IrFunction): IrStatement {
        if (declaration.hasComposableAnnotation()) {
          declaration.annotations = declaration.annotations
            .transform {
              if (it.type.classifierOrFail.descriptor.fqNameSafe != InjektFqNames.Composable ||
                  none { it.type.classifierOrFail.descriptor.fqNameSafe == InjektFqNames.Composable })
                    add(it)
            }

          if (declaration.parentClassOrNull?.defaultType?.superTypes()
              ?.any { it.isComposableFunInterface } == true) {
            (declaration as IrSimpleFunction).overriddenSymbols = listOf(
              irBuiltins.functionN(declaration.valueParameters.size)
                .functions.first { it.name.asString() == "invoke" }.symbol
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
private val IrType.isComposableFunInterface: Boolean
  get() {
    val classifier = classifierOrNull?.descriptor ?: return false
    return classifier.safeAs<ClassDescriptor>()?.isFun == true &&
        classifier.defaultType.anySuperTypeConstructor {
          it.declarationDescriptor?.defaultType?.isComposableType == true
        }
  }
