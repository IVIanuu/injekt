/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.injekt.compiler.ast.tree.type

import com.ivianuu.injekt.compiler.ast.tree.AstVariance
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstAnnotationContainer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstTypeAlias
import com.ivianuu.injekt.compiler.ast.tree.expression.AstCall

class AstType : AstAnnotationContainer, AstTypeArgument, AstTypeProjection {

    override val variance: AstVariance?
        get() = null

    override val type: AstType
        get() = this

    lateinit var classifier: AstClassifier
    override val annotations: MutableList<AstCall> = mutableListOf()
    var hasQuestionMark: Boolean = false
    val arguments: MutableList<AstTypeArgument> = mutableListOf()
    var abbreviation: AstTypeAbbreviation? = null
}

interface AstTypeArgument

object AstStarProjection : AstTypeArgument

interface AstTypeProjection : AstTypeArgument {
    val variance: AstVariance?
    val type: AstType
}

class AstTypeAbbreviation(
    var typeAlias: AstTypeAlias,
) : AstAnnotationContainer {
    override val annotations: MutableList<AstCall> = mutableListOf()
    var hasQuestionMark: Boolean = false
    val arguments: MutableList<AstTypeArgument> = mutableListOf()
}
