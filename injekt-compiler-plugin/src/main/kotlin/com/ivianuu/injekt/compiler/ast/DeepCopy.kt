package com.ivianuu.injekt.compiler.ast

fun <T : AstElement> T.deepCopy(): T = this