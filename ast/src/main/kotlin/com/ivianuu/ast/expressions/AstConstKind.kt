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
    object UByte : AstConstKind<kotlin.UByte>("UByte")
    object Short : AstConstKind<kotlin.Short>("Short")
    object UShort : AstConstKind<kotlin.UShort>("UShort")
    object Int : AstConstKind<kotlin.Int>("Int")
    object UInt : AstConstKind<kotlin.UInt>("UInt")
    object Long : AstConstKind<kotlin.Long>("Long")
    object ULong : AstConstKind<kotlin.ULong>("ULong")

    object String : AstConstKind<kotlin.String>("String")

    object Float : AstConstKind<kotlin.Float>("Float")
    object Double : AstConstKind<kotlin.Double>("Double")

    override fun toString() = asString
}
