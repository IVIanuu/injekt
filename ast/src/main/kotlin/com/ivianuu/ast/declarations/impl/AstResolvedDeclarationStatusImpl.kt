/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.declarations.AstResolvedDeclarationStatus
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility

class AstResolvedDeclarationStatusImpl(
    visibility: Visibility,
    modality: Modality
) : AstDeclarationStatusImpl(visibility, modality), AstResolvedDeclarationStatus {

    internal constructor(
        visibility: Visibility,
        modality: Modality,
        flags: Int
    ) : this(visibility, modality) {
        this.flags = flags
    }

    override val visibility: Visibility
        get() = super.visibility

    override val modality: Modality
        get() = super.modality!!
}
