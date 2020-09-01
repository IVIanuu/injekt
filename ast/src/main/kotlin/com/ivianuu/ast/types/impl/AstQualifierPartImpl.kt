/*
 * Copyright 2010-2018 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.types.impl

import com.ivianuu.ast.types.AstQualifierPart
import com.ivianuu.ast.types.AstTypeArgumentList
import com.ivianuu.ast.types.AstTypeProjection
import org.jetbrains.kotlin.name.Name

class AstTypeArgumentListImpl(override val source: AstSourceElement) : AstTypeArgumentList {
    override val typeArguments = mutableListOf<AstTypeProjection>()
}

class AstQualifierPartImpl(
    override val name: Name,
    override val typeArgumentList: AstTypeArgumentList
) : AstQualifierPart