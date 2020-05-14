/*
 * Copyright 2020 Manuel Wrage
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *  
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

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