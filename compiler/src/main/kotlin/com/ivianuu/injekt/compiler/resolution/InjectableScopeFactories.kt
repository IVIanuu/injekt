package com.ivianuu.injekt.compiler.resolution

import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.impl.*

fun ElementInjectablesScope(
  position: FirElement,
  containingElements: List<FirElement>,
  session: FirSession,
): InjectablesScope {
  fun FirElement.isScopeOwner(): Boolean {
    if (this is FirFile) return true


    return false
  }

  val scopeOwner = containingElements.last { it.isScopeOwner() }

  return when (scopeOwner) {
    is FirFile -> FileInjectablesScope(scopeOwner, session)
    else -> throw AssertionError()
  }
}

private fun FileInjectablesScope(file: FirFile, session: FirSession) = InjectableScopeOrParent(
  name = "FILE ${file.name}",
  parent = InternalGlobalInjectablesScope(file, session),
  owner = file,
  initialInjectables = collectPackageInjectables(file.packageFqName, session)
    .filter { session.firProvider.getContainingFile(it.callable) == file },
  session = session
)

fun InternalGlobalInjectablesScope(
  file: FirFile,
  session: FirSession
) = InjectableScopeOrParent(
  name = "INTERNAL GLOBAL EXCEPT $file",
  parent = ExternalGlobalInjectablesScope(session),
  initialInjectables = collectGlobalInjectables(session)
    .filter {
      it.callable.moduleData == session.moduleData &&
          (session.firProvider.getContainingFile(it.callable) != file)
    },
  session = session
)

fun ExternalGlobalInjectablesScope(session: FirSession) =  InjectablesScope(
  name = "EXTERNAL GLOBAL",
  parent = null,
  initialInjectables = collectGlobalInjectables(session)
    .filter { it.callable.moduleData != session.moduleData },
  session = session
)

fun InjectableScopeOrParent(
  name: String,
  parent: InjectablesScope,
  owner: FirElement? = null,
  initialInjectables: List<InjektCallable> = emptyList(),
  injectablesPredicate: (InjektCallable) -> Boolean = { true },
  typeParameters: List<FirTypeParameterSymbol> = emptyList(),
  nesting: Int = parent.nesting.inc(),
  session: FirSession
): InjectablesScope {
  val finalInitialInjectables = initialInjectables.filter(injectablesPredicate)
  return if (typeParameters.isEmpty() && finalInitialInjectables.isEmpty()) parent
  else InjectablesScope(name, parent, owner, finalInitialInjectables, injectablesPredicate, typeParameters, nesting, session)
}
