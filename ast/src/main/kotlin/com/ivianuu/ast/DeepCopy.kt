package com.ivianuu.ast

import com.ivianuu.ast.tree.AstElement

fun <T : AstElement> T.deepCopy(): T = this
