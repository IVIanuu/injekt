/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.symbols

import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name

data class CallableId(val fqName: FqName) {
    constructor(name: Name) : this(FqName(name.asString()))
}

data class ClassId(val fqName: FqName) {
    constructor(name: Name) : this(FqName(name.asString()))
}
