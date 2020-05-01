package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.FqName

object QualifiedExpressionsStore {

    private val qualifiersByKey = mutableMapOf<Int, List<FqName>>()

    fun putQualifiers(
        fileName: String,
        startOffset: Int,
        endOffset: Int,
        qualifiers: List<FqName>
    ) {
        qualifiersByKey[key(fileName, startOffset, endOffset)] = qualifiers
    }

    fun getQualifiers(
        fileName: String,
        startOffset: Int,
        endOffset: Int
    ): List<FqName>? {
        return qualifiersByKey[key(fileName, startOffset, endOffset)]
    }

    private fun key(fileName: String, startOffset: Int, endOffset: Int): Int =
        fileName.hashCode() + startOffset + endOffset

}