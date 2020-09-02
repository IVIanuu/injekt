package com.ivianuu.ast.symbols.impl

import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstValueParameter
import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.symbols.CallableId

open class AstVariableSymbol<D : AstVariable<D>>(override val callableId: CallableId) :
    AstCallableSymbol<D>()

open class AstValueParameterSymbol(callableId: CallableId) : AstVariableSymbol<AstValueParameter>(
    callableId
)

open class AstPropertySymbol(
    callableId: CallableId
) : AstVariableSymbol<AstProperty>(callableId)

class AstBackingFieldSymbol(callableId: CallableId) : AstVariableSymbol<AstProperty>(callableId)
