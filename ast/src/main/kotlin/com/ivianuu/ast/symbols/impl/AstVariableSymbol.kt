package com.ivianuu.ast.symbols.impl

import com.ivianuu.ast.declarations.AstField
import com.ivianuu.ast.declarations.AstProperty
import com.ivianuu.ast.declarations.AstVariable
import com.ivianuu.ast.expressions.AstExpression
import com.ivianuu.ast.symbols.CallableId
import org.jetbrains.kotlin.name.Name

open class AstVariableSymbol<D : AstVariable<D>>(override val callableId: CallableId) :
    AstCallableSymbol<D>() {
    constructor(name: Name) : this(CallableId(name))  // TODO?
}

open class AstPropertySymbol(
    callableId: CallableId,
    override val overriddenSymbol: AstPropertySymbol? = null,
    override val isIntersectionOverride: Boolean = false,
) : AstVariableSymbol<AstProperty>(callableId) {
    // TODO: should we use this constructor for local variables?
    constructor(name: Name) : this(CallableId(name))
}

class AstBackingFieldSymbol(callableId: CallableId) : AstVariableSymbol<AstProperty>(callableId)

class AstDelegateFieldSymbol<D : AstVariable<D>>(callableId: CallableId) :
    AstVariableSymbol<D>(callableId) {
    val delegate: AstExpression
        get() = ast.delegate!!
}

class AstFieldSymbol(callableId: CallableId) : AstVariableSymbol<AstField>(callableId)
