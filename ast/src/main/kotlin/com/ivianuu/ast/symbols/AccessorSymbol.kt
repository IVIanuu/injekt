package com.ivianuu.ast.symbols

interface AccessorSymbol {
    val callableId: CallableId // it's an id of related property (synthetic or not)
    val accessorId: CallableId // it's an id of accessor function
}
