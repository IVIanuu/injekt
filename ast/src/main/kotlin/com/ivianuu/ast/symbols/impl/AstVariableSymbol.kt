package com.ivianuu.ast.symbols.impl

import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.AstVariable
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

open class AstVariableSymbol<D : AstVariable<D>>(
    override val fqName: FqName
) : AstCallableSymbol<D>() {
    constructor(name: Name) : this(FqName.ROOT.child(name))
}

open class AstValueParameterSymbol(
    override val fqName: FqName
) : AstVariableSymbol<AstValueParameter>(fqName) {
    constructor(name: Name) : this(FqName.ROOT.child(name))
}

open class AstPropertySymbol(
    override val fqName: FqName
) : AstVariableSymbol<AstProperty>(fqName) {
    constructor(name: Name) : this(FqName.ROOT.child(name))
}
