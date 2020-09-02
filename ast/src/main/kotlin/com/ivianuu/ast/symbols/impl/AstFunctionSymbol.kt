package com.ivianuu.ast.symbols.impl

import com.ivianuu.ast.declarations.AstAnonymousFunction
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.declarations.AstPropertyAccessor
import com.ivianuu.ast.declarations.AstSimpleFunction
import com.ivianuu.ast.symbols.AccessorSymbol
import com.ivianuu.ast.symbols.CallableId
import com.ivianuu.ast.types.AstTypeProjection
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class AstFunctionSymbol<D : AstFunction<D>>(
    override val callableId: CallableId
) : AstCallableSymbol<D>() {
    open val parameters: List<AstTypeProjection>
        get() = emptyList()
}

open class AstNamedFunctionSymbol(
    callableId: CallableId,
    override val overriddenSymbol: AstNamedFunctionSymbol? = null
) : AstFunctionSymbol<AstSimpleFunction>(callableId)

class AstConstructorSymbol(
    callableId: CallableId,
    override val overriddenSymbol: AstConstructorSymbol? = null
) : AstFunctionSymbol<AstConstructor>(callableId)

open class AstAccessorSymbol(
    callableId: CallableId,
    override val accessorId: CallableId
) : AstPropertySymbol(callableId), AccessorSymbol

sealed class AstFunctionWithoutNameSymbol<F : AstFunction<F>>(
    stubName: Name
) : AstFunctionSymbol<F>(CallableId(FqName("special"), stubName)) {
    override val parameters: List<AstTypeProjection>
        get() = emptyList()
}

class AstAnonymousFunctionSymbol :
    AstFunctionWithoutNameSymbol<AstAnonymousFunction>(Name.identifier("anonymous"))

class AstPropertyAccessorSymbol :
    AstFunctionWithoutNameSymbol<AstPropertyAccessor>(Name.identifier("accessor"))
