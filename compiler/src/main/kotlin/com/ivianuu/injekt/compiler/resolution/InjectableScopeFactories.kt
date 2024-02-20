@file:OptIn(SymbolInternals::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*

fun ElementInjectablesScope(
  position: FirElement,
  containingElements: List<FirElement>,
  session: FirSession,
): InjectablesScope {
  fun FirElement.isScopeOwner(): Boolean {
    if (this is FirFile || this is FirFunction) return true
    return false
  }

  val scopeOwner = containingElements.last { it.isScopeOwner() }

  val parentScope = containingElements
    .take(containingElements.indexOf(scopeOwner))
    .let { parentElements ->
      parentElements
        .lastOrNull { it.isScopeOwner() }
        ?.let { ElementInjectablesScope(it, parentElements, session) }
    }

  return when (scopeOwner) {
    is FirFile -> FileInjectablesScope(scopeOwner, session)
    is FirFunction -> FunctionInjectablesScope(scopeOwner, parentScope!!, session)
    else -> throw AssertionError("Unexpected scope owner $scopeOwner")
  }
}

private fun ValueParameterDefaultValueInjectablesScope(
  valueParameter: FirValueParameterSymbol,
  parent: InjectablesScope,
  session: FirSession
): InjectablesScope {
  val function = valueParameter.containingFunctionSymbol
  val parameterScopes = FunctionParameterInjectablesScopes(
    parent,
    function,
    valueParameter,
    session
  )
  return InjectableScopeOrParent(
    name = "DEFAULT VALUE ${valueParameter.name}",
    parent = parameterScopes,
    owner = function.fir,
    typeParameters = function.typeParameterSymbols,
    session = session
  )
}

private fun FunctionInjectablesScope(
  function: FirFunction,
  parent: InjectablesScope,
  session: FirSession
): InjectablesScope {
  val parameterScopes = FunctionParameterInjectablesScopes(parent, function.symbol, null, session)
  val baseName = if (function is FirConstructor) "CONSTRUCTOR" else "FUNCTION"
  val typeParameters = if (function is FirConstructor)
    function.symbol.getConstructedClass(session)!!.typeParameterSymbols
  else function.typeParameters.map { it.symbol }
  return InjectableScopeOrParent(
    name = "$baseName ${function.nameOrSpecialName}",
    parent = parameterScopes,
    owner = function,
    typeParameters = typeParameters,
    nesting = parameterScopes.nesting,
    session = session
  )
}

private fun FunctionParameterInjectablesScopes(
  parent: InjectablesScope,
  function: FirFunctionSymbol<*>,
  until: FirValueParameterSymbol? = null,
  session: FirSession
): InjectablesScope {
  val maxIndex = function.valueParameterSymbols.indexOfFirst { it == until }

  return function.valueParameterSymbols
    .filterIndexed { index, valueParameter ->
      (maxIndex == -1 || index < maxIndex) &&
          valueParameter.hasAnnotation(InjektFqNames.Provide, session)
    }
    .fold(parent) { acc, nextParameter ->
      FunctionParameterInjectablesScope(
        parent = acc,
        function = function,
        parameter = nextParameter,
        session = session
      )
    }
}

private fun FunctionParameterInjectablesScope(
  parent: InjectablesScope,
  function: FirFunctionSymbol<*>,
  parameter: FirValueParameterSymbol,
  session: FirSession
) = InjectableScopeOrParent(
  name = "FUNCTION PARAMETER ${parameter.name}",
  parent = parent,
  owner = parameter.fir,
  initialInjectables = listOf(parameter.toInjektCallable()),
  typeParameters = function.typeParameterSymbols,
  nesting = if (parent.name.startsWith("FUNCTION PARAMETER")) parent.nesting
  else parent.nesting + 1,
  session = session
)

private fun FileInjectablesScope(file: FirFile, session: FirSession) = InjectableScopeOrParent(
  name = "FILE ${file.name}",
  parent = InternalGlobalInjectablesScope(file, session),
  owner = file,
  initialInjectables = collectPackageInjectables(file.packageFqName, session)
    .filter { session.firProvider.getContainingFile(it.callable) == file },
  session = session
)

private fun InternalGlobalInjectablesScope(
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

private fun ExternalGlobalInjectablesScope(session: FirSession) =  InjectablesScope(
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
