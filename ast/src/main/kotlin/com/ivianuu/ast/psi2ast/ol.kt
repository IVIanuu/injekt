package com.ivianuu.ast.psi2ast

fun test12(c: Boolean?) {
    L@ while (true) {
        L2@while (c ?: break) {}
    }
}

public fun test1(c: kotlin.Boolean?): kotlin.Unit {
    tmp3@ while (true) {
        val tmp8: kotlin.Boolean? = c
        tmp6@ while (if ((tmp8 != null)) tmp8 else break@tmp3 ) {

        }
    }
}