/*
 * Copyright 2010-2020 JetBrains s.r.o. and Kotlin Programming Language contributors.
 * Use of this source code is governed by the Apache 2.0 license that can be found in the license/LICENSE.txt file.
 */

package com.ivianuu.ast

object Visibilities {
    object Private : Visibility("private", isPublicAPI = false)

    object PrivateToThis : Visibility("private_to_this", isPublicAPI = false) {
        override val internalDisplayName: String
            get() = "private/*private to this*/"
    }

    object Protected : Visibility("protected", isPublicAPI = true)
    object Internal : Visibility("internal", isPublicAPI = false)
    object Public : Visibility("public", isPublicAPI = true)
    object Local : Visibility("local", isPublicAPI = false)
    object InvisibleFake : Visibility("invisible_fake", isPublicAPI = false)
    object Unknown : Visibility("unknown", isPublicAPI = false)

    @OptIn(ExperimentalStdlibApi::class)
    private val ORDERED_VISIBILITIES: Map<Visibility, Int> = buildMap {
        put(PrivateToThis, 0)
        put(Private, 0);
        put(Internal, 1);
        put(Protected, 1);
        put(Public, 2);
    }

    fun compare(first: Visibility, second: Visibility): Int? {
        val result = first.compareTo(second)
        if (result != null) {
            return result
        }
        val oppositeResult = second.compareTo(first)
        return if (oppositeResult != null) {
            -oppositeResult
        } else null
    }

    internal fun compareLocal(first: Visibility, second: Visibility): Int? {
        if (first === second) return 0
        val firstIndex = ORDERED_VISIBILITIES[first]
        val secondIndex = ORDERED_VISIBILITIES[second]
        return if (firstIndex == null || secondIndex == null || firstIndex == secondIndex) {
            null
        } else firstIndex - secondIndex
    }

    fun isPrivate(visibility: Visibility): Boolean {
        return visibility === Private || visibility === PrivateToThis
    }

    val DEFAULT_VISIBILITY = Public
}
