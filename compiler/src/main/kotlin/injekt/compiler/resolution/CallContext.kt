/*
 * Copyright 2024 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(UnsafeCastFunction::class, SymbolInternals::class)

package injekt.compiler.resolution

import injekt.compiler.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*
import org.jetbrains.kotlin.fir.types.*
import org.jetbrains.kotlin.utils.addToStdlib.*

enum class CallContext { DEFAULT, SUSPEND, COMPOSABLE }

fun CallContext.canCall(other: CallContext): Boolean =
  this == other || other == CallContext.DEFAULT

context(_: InjektContext)
fun FirCallableSymbol<*>.callContext(): CallContext {
  when {
    isSuspend -> return CallContext.SUSPEND
    hasAnnotation(InjektFqNames.Composable, session) -> return CallContext.COMPOSABLE
    else -> {
      if (this is FirAnonymousFunctionSymbol)
        fir.cast<FirAnonymousFunction>()
          .typeRef.coneTypeOrNull?.toInjektType()
          ?.callContext
          ?.let { return it }

       return CallContext.DEFAULT
    }
  }
}

val InjektType.callContext: CallContext
  get() = classifier.fqName.asString().let {
    when {
      it.startsWith(InjektFqNames.suspendFunction) ||
         it.startsWith(InjektFqNames.kSuspendFunction) -> CallContext.SUSPEND
      it.startsWith(InjektFqNames.composableFunction) ||
         it.startsWith(InjektFqNames.kComposableFunction) -> CallContext.COMPOSABLE
      else -> CallContext.DEFAULT
    }
  }
