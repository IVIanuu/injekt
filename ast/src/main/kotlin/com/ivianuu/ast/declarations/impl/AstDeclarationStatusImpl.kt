/*
 * Copyright 2010-2019 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast.declarations.impl

import com.ivianuu.ast.AstPureAbstractElement
import com.ivianuu.ast.declarations.AstDeclarationStatus
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.ACTUAL
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.COMPANION
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.CONST
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.DATA
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.EXPECT
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.EXTERNAL
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.FROM_ENUM
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.FROM_SEALED
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.FUN
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.INFIX
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.INLINE
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.INNER
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.LATEINIT
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.OPERATOR
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.OVERRIDE
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.STATIC
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.SUSPEND
import com.ivianuu.ast.declarations.impl.AstDeclarationStatusImpl.Modifier.TAILREC
import com.ivianuu.ast.visitors.AstTransformer
import com.ivianuu.ast.visitors.AstVisitor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.Visibility

open class AstDeclarationStatusImpl(
    override val visibility: Visibility,
    override val modality: Modality?
) : AstPureAbstractElement(), AstDeclarationStatus {
    protected var flags: Int = 0

    private operator fun get(modifier: Modifier): Boolean = (flags and modifier.mask) != 0

    private operator fun set(modifier: Modifier, value: Boolean) {
        flags = if (value) {
            flags or modifier.mask
        } else {
            flags and modifier.mask.inv()
        }
    }

    override var isExpect: Boolean
        get() = this[EXPECT]
        set(value) {
            this[EXPECT] = value
        }

    override var isActual: Boolean
        get() = this[ACTUAL]
        set(value) {
            this[ACTUAL] = value
        }

    override var isOverride: Boolean
        get() = this[OVERRIDE]
        set(value) {
            this[OVERRIDE] = value
        }

    override var isOperator: Boolean
        get() = this[OPERATOR]
        set(value) {
            this[OPERATOR] = value
        }

    override var isInfix: Boolean
        get() = this[INFIX]
        set(value) {
            this[INFIX] = value
        }

    override var isInline: Boolean
        get() = this[INLINE]
        set(value) {
            this[INLINE] = value
        }

    override var isTailRec: Boolean
        get() = this[TAILREC]
        set(value) {
            this[TAILREC] = value
        }

    override var isExternal: Boolean
        get() = this[EXTERNAL]
        set(value) {
            this[EXTERNAL] = value
        }

    override var isConst: Boolean
        get() = this[CONST]
        set(value) {
            this[CONST] = value
        }

    override var isLateInit: Boolean
        get() = this[LATEINIT]
        set(value) {
            this[LATEINIT] = value
        }

    override var isInner: Boolean
        get() = this[INNER]
        set(value) {
            this[INNER] = value
        }

    override var isCompanion: Boolean
        get() = this[COMPANION]
        set(value) {
            this[COMPANION] = value
        }

    override var isData: Boolean
        get() = this[DATA]
        set(value) {
            this[DATA] = value
        }

    override var isSuspend: Boolean
        get() = this[SUSPEND]
        set(value) {
            this[SUSPEND] = value
        }

    override var isStatic: Boolean
        get() = this[STATIC]
        set(value) {
            this[STATIC] = value
        }

    override var isFromSealedClass: Boolean
        get() = this[FROM_SEALED]
        set(value) {
            this[FROM_SEALED] = value
        }

    override var isFromEnumClass: Boolean
        get() = this[FROM_ENUM]
        set(value) {
            this[FROM_ENUM] = value
        }

    override var isFun: Boolean
        get() = this[FUN]
        set(value) {
            this[FUN] = value
        }

    private enum class Modifier(val mask: Int) {
        EXPECT(0x1),
        ACTUAL(0x2),
        OVERRIDE(0x4),
        OPERATOR(0x8),
        INFIX(0x10),
        INLINE(0x20),
        TAILREC(0x40),
        EXTERNAL(0x80),
        CONST(0x100),
        LATEINIT(0x200),
        INNER(0x400),
        COMPANION(0x800),
        DATA(0x1000),
        SUSPEND(0x2000),
        STATIC(0x4000),
        FROM_SEALED(0x8000),
        FROM_ENUM(0x10000),
        FUN(0x20000)
    }

    override fun <R, D> acceptChildren(visitor: AstVisitor<R, D>, data: D) {}

    override fun <D> transformChildren(
        transformer: AstTransformer<D>,
        data: D
    ): AstDeclarationStatusImpl {
        return this
    }

    fun resolved(visibility: Visibility, modality: Modality): AstDeclarationStatus {
        return AstResolvedDeclarationStatusImpl(visibility, modality, flags)
    }
}
