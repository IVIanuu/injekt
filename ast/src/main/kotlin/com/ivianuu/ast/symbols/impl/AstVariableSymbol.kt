package com.ivianuu.ast.symbols.impl

import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.symbols.CallableId
import org.jetbrains.kotlin.name.Name

open class AstVariableSymbol<D : AstVariable<D>>(override val callableId: CallableId) :
    AstCallableSymbol<D>() {
    constructor(name: Name) : this(CallableId(name))
}

open class AstValueParameterSymbol(callableId: CallableId) : AstVariableSymbol<AstValueParameter>(
    callableId
) {
    constructor(name: Name) : this(CallableId(name))
}

open class AstPropertySymbol(
    callableId: CallableId
) : AstVariableSymbol<AstProperty>(callableId) {
    constructor(name: Name) : this(CallableId(name))
}
