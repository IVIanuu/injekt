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

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.compiler.resolution.ClassifierRef
import com.ivianuu.injekt.compiler.resolution.copy
import com.ivianuu.injekt.compiler.resolution.getSubstitutionMap
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class TypeSubstitutionTest {

    @Test
    fun testGetSubstitutionMap() = withTypeCheckerContext {
        val superType = typeParameter()
        val map = getSubstitutionMap(context, listOf(stringType to superType))
        stringType shouldBe map[superType.classifier]
    }

    @Test
    fun testGetSubstitutionMapWithQualifierInSuperType() = withTypeCheckerContext {
        val qualifier = ClassifierRef(
            FqName("MyQualifier"),
            typeParameters = listOf(
                ClassifierRef(
                    fqName = FqName("MyQualifier.T")
                )
            )
        ).defaultType
        val classType = classType(anyType.qualified(qualifier.typeWith(stringType)))
        val typeParameter = typeParameter()
        val superType = typeParameter(anyNType.qualified(qualifier.typeWith(typeParameter)))
        val map = getSubstitutionMap(context, listOf(classType to superType))
        map[superType.classifier] shouldBe classType
        map[typeParameter.classifier] shouldBe stringType
    }

    @Test
    fun testGetSubstitutionMapWithExtraTypeParameter() = withTypeCheckerContext {
        val typeParameterU = typeParameter()
        val typeParameterS = typeParameter(listType.typeWith(typeParameterU))
        val typeParameterT = typeParameter(typeParameterS)
        val substitutionType = listType.typeWith(stringType)
        val map = getSubstitutionMap(context, listOf(substitutionType to typeParameterT))
        map[typeParameterT.classifier] shouldBe substitutionType
        map[typeParameterS.classifier] shouldBe substitutionType
        map[typeParameterU.classifier] shouldBe stringType
    }

    @Test
    fun testGetSubstitutionMapWithNestedGenerics() = withTypeCheckerContext {
        val superType = typeParameter()
        val map = getSubstitutionMap(context, listOf(listType.typeWith(stringType) to listType.typeWith(superType)))
        map[superType.classifier] shouldBe stringType
    }

    @Test
    fun testGetSubstitutionMapWithQualifiers() = withTypeCheckerContext {
        val unqualifiedSuperType = typeParameter()
        val qualifiedSuperType = unqualifiedSuperType.qualified(qualifier1)
        val substitutionType = stringType.qualified(qualifier1)
        val map = getSubstitutionMap(context, listOf(substitutionType to qualifiedSuperType))
        map[unqualifiedSuperType.classifier] shouldBe stringType
    }

    @Test
    fun testGetSubstitutionMapWithGenericQualifierArguments() = withTypeCheckerContext {
        val typeParameter1 = typeParameter()
        val typeParameter2 = typeParameter()
        val qualifier = ClassifierRef(
            FqName("MyQualifier"),
            typeParameters = listOf(
                ClassifierRef(
                    fqName = FqName("MyQualifier.T")
                )
            )
        )
        val superType = typeParameter1.qualified(qualifier.defaultType.typeWith(typeParameter2))
        val substitutionType = stringType.qualified(qualifier.defaultType.typeWith(intType))
        val map = getSubstitutionMap(context, listOf(substitutionType to superType))
        map[typeParameter1.classifier] shouldBe stringType
        map[typeParameter2.classifier] shouldBe intType
    }

    @Test
    fun testGetSubstitutionMapPrefersInput() = withTypeCheckerContext {
        val typeParameter1 = typeParameter()
        val typeParameter2 = typeParameter(typeParameter1)
        val map = getSubstitutionMap(
            context,
            listOf(
                listType.typeWith(stringType) to listType.typeWith(typeParameter2),
                charSequenceType to typeParameter1
            )
        )
        map[typeParameter1.classifier] shouldBe charSequenceType
        map[typeParameter2.classifier] shouldBe stringType
    }

    @Test
    fun testGetSubstitutionMapWithSubClass() = withTypeCheckerContext {
        val classType = classType(listType.typeWith(stringType))
        val typeParameter = typeParameter()
        val map = getSubstitutionMap(context, listOf(classType to listType.typeWith(typeParameter)))
        map.shouldHaveSize(1)
        map.shouldContain(typeParameter.classifier, stringType)
    }

}