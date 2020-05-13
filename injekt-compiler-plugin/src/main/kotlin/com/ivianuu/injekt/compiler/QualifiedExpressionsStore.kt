package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor

object QualifiedExpressionsStore {

    private val qualifiersByKey = mutableMapOf<Int, List<AnnotationDescriptor>>()

    fun putQualifiers(
        fileName: String,
        startOffset: Int,
        endOffset: Int,
        qualifiers: List<AnnotationDescriptor>
    ) {
        qualifiersByKey[key(fileName, startOffset, endOffset)] = qualifiers
    }

    fun getQualifiers(
        fileName: String,
        startOffset: Int,
        endOffset: Int
    ): List<AnnotationDescriptor>? {
        return qualifiersByKey[key(fileName, startOffset, endOffset)]
    }

    private fun key(fileName: String, startOffset: Int, endOffset: Int): Int =
        fileName.hashCode() + startOffset + endOffset

}