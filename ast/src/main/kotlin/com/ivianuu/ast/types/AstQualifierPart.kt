/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.types

import org.jetbrains.kotlin.name.Name

interface AstTypeArgumentList {
    val typeArguments: List<AstTypeProjection>
}

interface AstQualifierPart {
    val name: Name
    val typeArgumentList: AstTypeArgumentList
}