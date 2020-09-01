/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast

abstract class AstAbstractTarget<E : AstTargetElement>(
    override val labelName: String?
) : AstTarget<E> {
    override lateinit var labeledElement: E

    override fun bind(element: E) {
        labeledElement = element
    }
}