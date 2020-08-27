package com.ivianuu.injekt.compiler.ast

import com.ivianuu.injekt.compiler.ast.tree.AstElement

fun <T : AstElement> T.deepCopy(): T = this
