package com.ivianuu.injekt.internal

fun stub(): Nothing = error("Must be compiled with the injekt compiler")

fun <T> stub(): T = stub()
