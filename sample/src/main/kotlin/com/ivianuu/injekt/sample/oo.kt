package com.ivianuu.injekt.sample


fun lol(cond: Boolean?) {
    if ( cond?.let {
            val tmp = compute(cond)
            tmp < 0 && tmp > 1
        } == true) {
        println("if")
    } else if ( cond?.let {
            val tmp = compute(cond)
            tmp < 0 && tmp > 1
        } == true) {
        println("else")
    }

    while (
        cond?.let {
            val tmp = compute(cond)
            tmp < 0 && tmp > 1
        } == true
    ) {
        println()
    }

    while (true) {
        if ((cond?.let {
                val tmp = compute(cond)
                tmp < 0 && tmp > 1
            } == true).not()) break
        println()
    }

    while (true) {
        println()
        if ((cond?.let {
                val tmp = compute(cond)
                tmp < 0 && tmp > 1
            } == true).not()) break
    }
}

private fun compute(cond: Boolean?): Int = error("")