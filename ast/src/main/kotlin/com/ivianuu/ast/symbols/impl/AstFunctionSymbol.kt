package com.ivianuu.ast.symbols.impl

import com.ivianuu.ast.declarations.AstAnonymousFunction
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.declarations.AstNamedFunction
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.symbols.AccessorSymbol
import com.ivianuu.ast.symbols.CallableId
import com.ivianuu.ast.types.AstTypeProjection
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class AstFunctionSymbol<D : AstFunction<D>>(
    override val callableId: CallableId
) : AstCallableSymbol<D>()

open class AstNamedFunctionSymbol(
    callableId: CallableId
) : AstFunctionSymbol<AstNamedFunction>(callableId)

class AstConstructorSymbol(
    callableId: CallableId
) : AstFunctionSymbol<AstConstructor>(callableId)

open class AstAccessorSymbol(
    callableId: CallableId,
    override val accessorId: CallableId
) : AstPropertySymbol(callableId), AccessorSymbol

sealed class AstFunctionWithoutNameSymbol<F : AstFunction<F>>(
    stubName: Name
) : AstFunctionSymbol<F>(CallableId(FqName("special"), stubName))

class AstAnonymousFunctionSymbol :
    AstFunctionWithoutNameSymbol<AstAnonymousFunction>(Name.identifier("anonymous"))

class AstPropertyAccessorSymbol :
    AstFunctionWithoutNameSymbol<AstPropertyAccessor>(Name.identifier("accessor"))
