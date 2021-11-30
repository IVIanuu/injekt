/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.compiler.transform

import androidx.compose.compiler.plugins.kotlin.hasComposableAnnotation
import androidx.compose.compiler.plugins.kotlin.lower.changedParamCount
import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.composeCompilerInClasspath
import com.ivianuu.injekt.compiler.injektFqNames
import com.ivianuu.injekt.compiler.resolution.anySuperType
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.transform
import com.ivianuu.shaded_injekt.Inject
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.backend.jvm.ir.getSingleAbstractMethod
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.ObsoleteDescriptorBasedAPI
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrSimpleFunction
import org.jetbrains.kotlin.ir.expressions.IrCall
import org.jetbrains.kotlin.ir.expressions.IrConstructorCall
import org.jetbrains.kotlin.ir.expressions.IrExpression
import org.jetbrains.kotlin.ir.expressions.IrFunctionExpression
import org.jetbrains.kotlin.ir.expressions.IrStatementOrigin
import org.jetbrains.kotlin.ir.expressions.IrTypeOperator
import org.jetbrains.kotlin.ir.expressions.IrTypeOperatorCall
import org.jetbrains.kotlin.ir.types.IrType
import org.jetbrains.kotlin.ir.types.classOrNull
import org.jetbrains.kotlin.ir.types.classifierOrFail
import org.jetbrains.kotlin.ir.types.classifierOrNull
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.ir.util.functions
import org.jetbrains.kotlin.ir.util.isFakeOverriddenFromAny
import org.jetbrains.kotlin.ir.util.isFakeOverride
import org.jetbrains.kotlin.ir.util.parentAsClass
import org.jetbrains.kotlin.ir.util.parentClassOrNull
import org.jetbrains.kotlin.ir.util.superTypes
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.utils.addToStdlib.cast

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