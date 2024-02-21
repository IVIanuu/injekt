@file:OptIn(SymbolInternals::class)

package com.ivianuu.injekt.compiler.resolution

import com.ivianuu.injekt.compiler.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.declarations.*
import org.jetbrains.kotlin.fir.declarations.utils.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.fir.symbols.*
import org.jetbrains.kotlin.fir.symbols.impl.*

fun ElementInjectablesScope(
  position: FirElement,
  containingElements: List<FirElement>,
  session: FirSession,
): InjectablesScope {
  fun FirElement.isScopeOwner(): Boolean {
    if (this is FirFile || this is FirFunction || this is FirBlock) return true
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
    is FirBlock -> BlockInjectablesScope(scopeOwner, position, parentScope!!, session)
    else -> throw AssertionError("Unexpected scope owner $scopeOwner")
  }
}

private fun BlockInjectablesScope(
  block: FirBlock,
  position: FirElement,
  parent: InjectablesScope,
  session: FirSession
): InjectablesScope {
  val injectablesBeforePosition = block.statements
    .filter { declaration ->
      declaration.source!!.endOffset < position.source!!.startOffset &&
          declaration.hasAnnotation(InjektFqNames.Provide, session)
    }
    .flatMap {
      when (it) {
        is FirRegularClass -> it.symbol.collectInjectableConstructors(session)
        is FirCallableDeclaration -> listOf(it.symbol.toInjektCallable(session))
        else -> throw AssertionError("Unexpected declaration $it")
      }
    }
  if (injectablesBeforePosition.isEmpty()) return parent

  return InjectableScopeOrParent(
    name = "BLOCK AT ${position.source!!.startOffset}",
    parent = parent,
    initialInjectables = injectablesBeforePosition,
    nesting = if (injectablesBeforePosition.size > 1) parent.nesting
    else parent.nesting + 1,
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
  initialInjectables = listOf(parameter.toInjektCallable(session)),
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
  typeParameters: List<FirTypeParameterSymbol> = emptyList(),
  nesting: Int = parent.nesting.inc(),
  session: FirSession
): InjectablesScope = if (typeParameters.isEmpty() && initialInjectables.isEmpty()) parent
else InjectablesScope(name, parent, owner, initialInjectables, typeParameters, nesting, session)
