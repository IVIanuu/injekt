/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class)

package com.ivianuu.injekt.compiler.ir

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.fir.callableInfo
import com.ivianuu.injekt.compiler.fir.classifierInfo
import com.ivianuu.injekt.compiler.fir.encode
import com.ivianuu.injekt.compiler.fir.shouldBePersisted
import com.ivianuu.injekt.compiler.fir.toPersistedCallableInfo
import com.ivianuu.injekt.compiler.fir.toPersistedClassifierInfo
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.fir.backend.FirMetadataSource
import org.jetbrains.kotlin.fir.resolve.providers.symbolProvider
import org.jetbrains.kotlin.fir.symbols.impl.FirCallableSymbol
import org.jetbrains.kotlin.fir.symbols.impl.FirClassifierSymbol
import org.jetbrains.kotlin.ir.IrStatement
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.irCallConstructor
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationBase
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFunction
import org.jetbrains.kotlin.ir.declarations.IrMetadataSourceOwner
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.IrProperty
import org.jetbrains.kotlin.ir.declarations.IrTypeAlias
import org.jetbrains.kotlin.ir.expressions.impl.IrConstImpl
import org.jetbrains.kotlin.ir.util.classIdOrFail
import org.jetbrains.kotlin.ir.util.isLocal
import org.jetbrains.kotlin.ir.visitors.IrElementTransformerVoid
import org.jetbrains.kotlin.utils.addToStdlib.*

fun IrModuleFragment.persistInfos(ctx: InjektContext, irCtx: IrPluginContext) {
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
