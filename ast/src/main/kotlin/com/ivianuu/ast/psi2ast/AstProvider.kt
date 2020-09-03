package com.ivianuu.ast.psi2ast

import com.ivianuu.ast.AstElement

interface AstProvider {
    fun <E : AstElement> getDeclaration()
}
