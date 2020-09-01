/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast

import com.ivianuu.ast.expressions.AstLoop

class AstLoopTarget(
    labelName: String?
) : AstAbstractTarget<AstLoop>(labelName)