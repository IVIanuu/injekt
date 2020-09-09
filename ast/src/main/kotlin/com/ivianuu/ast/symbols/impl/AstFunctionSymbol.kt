package com.ivianuu.ast.symbols.impl

import com.ivianuu.ast.declarations.AstAnonymousFunction
import com.ivianuu.ast.declarations.AstConstructor
import com.ivianuu.ast.declarations.AstFunction
import com.ivianuu.ast.declarations.AstNamedFunction
import com.ivianuu.ast.declarations.AstPropertyAccessor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

sealed class AstFunctionSymbol<D : AstFunction<D>> : AstCallableSymbol<D>()

class AstNamedFunctionSymbol(override val fqName: FqName) : AstFunctionSymbol<AstNamedFunction>() {
    constructor(name: Name) : this(FqName(name.asString()))
}

class AstConstructorSymbol(override val fqName: FqName) : AstFunctionSymbol<AstConstructor>()

sealed class AstFunctionWithoutNameSymbol<F : AstFunction<F>>(
    stubName: Name
) : AstFunctionSymbol<F>() {
    override val fqName: FqName = FqName(stubName.asString())
}

class AstAnonymousFunctionSymbol : AstFunctionWithoutNameSymbol<AstAnonymousFunction>(
    Name.identifier("anonymous")
)

class AstPropertyAccessorSymbol :
    AstFunctionWithoutNameSymbol<AstPropertyAccessor>(Name.identifier("accessor"))
