/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.expressions

sealed class AstConstKind<T>(val asString: kotlin.String) {
    object Null : AstConstKind<Nothing?>("Null")
    object Boolean : AstConstKind<kotlin.Boolean>("Boolean")
    object Char : AstConstKind<kotlin.Char>("Char")

    object Byte : AstConstKind<kotlin.Byte>("Byte")
    object UnsignedByte : AstConstKind<kotlin.Byte>("UByte")
    object Short : AstConstKind<kotlin.Short>("Short")
    object UnsignedShort : AstConstKind<kotlin.Short>("UShort")
    object Int : AstConstKind<kotlin.Int>("Int")
    object UnsignedInt : AstConstKind<kotlin.Int>("UInt")
    object Long : AstConstKind<kotlin.Long>("Long")
    object UnsignedLong : AstConstKind<kotlin.Long>("ULong")

    object String : AstConstKind<kotlin.String>("String")

    object Float : AstConstKind<kotlin.Float>("Float")
    object Double : AstConstKind<kotlin.Double>("Double")

    object IntegerLiteral : AstConstKind<kotlin.Long>("IntegerLiteral")
    object UnsignedIntegerLiteral : AstConstKind<kotlin.Long>("UnsignedIntegerLiteral")

    override fun toString() = asString
}
