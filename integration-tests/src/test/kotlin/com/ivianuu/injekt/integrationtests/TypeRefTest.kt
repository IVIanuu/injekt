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

import com.ivianuu.injekt.compiler.resolution.typeWith
import org.junit.Test

class TypeRefTest {

    @Test
    fun testSimpleTypeWithSameClassifierIsAssignable() = withAnalysisContext {
        stringType shouldBeAssignable stringType
    }

    @Test
    fun testSimpleTypeWithDifferentClassifierIsNotAssignable() = withAnalysisContext {
        stringType shouldNotBeAssignable intType
    }

    @Test
    fun testNonNullIsAssignableToNullable() = withAnalysisContext {
        stringType shouldBeAssignable stringType.nullable()
    }

    @Test
    fun testNullableIsNotAssignableToNonNullable() = withAnalysisContext {
        stringType.nullable() shouldNotBeAssignable stringType
    }

    @Test
    fun testMatchingGenericTypeIsAssignable() = withAnalysisContext {
        listType.typeWith(listOf(stringType)) shouldBeAssignable listType
    }

    @Test
    fun testNotMatchingGenericTypeIsNotAssignable() = withAnalysisContext {
        listType.typeWith(stringType) shouldNotBeAssignable listType.typeWith(intType)
    }

    @Test
    fun testAnyTypeIsAssignableToStarProjectedType() = withAnalysisContext {
        starProjectedType shouldBeAssignable stringType
    }

    @Test
    fun testStarProjectedTypeMatchesNullableType() = withAnalysisContext {
        starProjectedType shouldBeAssignable stringType.nullable()
    }

    /*@Test
    fun testStarProjectedTypeMatchesQualifiedType() = withAnalysisContext {
        starProjectedType shouldBeAssignable qualifier1.wrapWith(stringType)
    }*/

    @Test
    fun testRandomTypeIsNotSubTypeOfTypeAliasWithAnyExpandedType() = withAnalysisContext {
        stringType shouldNotBeAssignable typeAlias(anyType)
    }

    @Test
    fun testTypeAliasIsNotAssignableToOtherTypeAliasOfTheSameExpandedType() = withAnalysisContext {
        typeAlias(stringType) shouldNotBeAssignable typeAlias(stringType)
    }

    @Test
    fun testTypeAliasIsAssignableToOtherTypeAliasOfTheSameExpandedType() = withAnalysisContext {
        typeAlias(stringType) shouldNotBeAssignable typeAlias(stringType)
    }

    @Test
    fun testTypeAliasIsSubTypeOfExpandedType() = withAnalysisContext {
        typeAlias(stringType) shouldBeSubTypeOf stringType
    }

    @Test
    fun testNestedTypeAliasIsSubTypeOfExpandedType() = withAnalysisContext {
        typeAlias(typeAlias(stringType)) shouldBeSubTypeOf stringType
    }

    @Test
    fun testSameComposabilityIsAssignable() = withAnalysisContext {
        composableFunction(0) shouldBeAssignable composableFunction(0)
    }

    @Test
    fun testComposableTypeAliasIsSubTypeOfComposableFunctionUpperBound() = withAnalysisContext {
        typeAlias(composableFunction(0)) shouldBeAssignable typeParameter(composableFunction(0))
    }

    /*@Test
    fun testSameQualifiersIsAssignable() = withAnalysisContext {
        stringType.wrapped(qualifier1) shouldBeAssignable stringType.wrapped(qualifier1)
    }

    @Test
    fun testDifferentQualifiersIsNotAssignable() = withAnalysisContext {
        stringType.wrapped(qualifier1) shouldNotBeAssignable stringType.wrapped(qualifier2)
    }*/

    @Test
    fun testSubTypeOfTypeParameterWithNullableAnyUpperBound() = withAnalysisContext {
        stringType shouldBeAssignable typeParameter()
    }

    @Test
    fun testComposableSubTypeOfTypeParameterWithNullableAnyUpperBound() = withAnalysisContext {
        composableFunction(0) shouldBeAssignable typeParameter()
    }

    @Test
    fun testComposableIsNotSubTypeOfNonComposable() = withAnalysisContext {
        composableFunction(0) shouldNotBeAssignable typeParameter(function(0))
    }

    @Test
    fun testSubTypeOfTypeParameterWithNonNullAnyUpperBound() = withAnalysisContext {
        stringType shouldBeAssignable typeParameter(nullable = false)
    }

    @Test
    fun testNullableSubTypeOfTypeParameterWithNonNullAnyUpperBound() = withAnalysisContext {
        stringType.nullable() shouldNotBeAssignable typeParameter(nullable = false)
    }

    @Test
    fun testSubTypeOfTypeParameterWithUpperBound() = withAnalysisContext {
        subType(stringType) shouldBeSubTypeOf typeParameter(stringType)
    }

    @Test
    fun testSubTypeOfTypeParameterWithNullableUpperBound() = withAnalysisContext {
        subType(stringType) shouldBeSubTypeOf typeParameter(stringType.nullable())
    }

    /*@Test
    fun testQualifiedSubTypeOfQualifiedTypeParameter() = withAnalysisContext {
        stringType.wrapped(qualifier1) shouldBeAssignable
                typeParameter(nullable = false).wrapped(qualifier1)
    }

    @Test
    fun testNestedQualifiedSubTypeOfNestedQualifiedTypeParameter() = withAnalysisContext {
        listType.typeWith(stringType.wrapped(qualifier1)) shouldBeAssignable
                listType.typeWith(typeParameter(nullable = false).wrapped(qualifier1))
    }

    @Test
    fun testUnqualifiedSubTypeOfTypeParameterWithQualifiedUpperBound() = withAnalysisContext {
        stringType shouldNotBeAssignable
                typeParameter(anyNType.wrapped(qualifier1))
    }

    @Test
    fun testNestedUnqualifiedSubTypeOfNestedTypeParameterWithQualifiedUpperBound() =
        withAnalysisContext {
            listType.typeWith(stringType) shouldNotBeAssignable
                    listType.typeWith(typeParameter(anyNType.wrapped(qualifier1)))
        }

    @Test
    fun testNestedQualifiedSubTypeOfNestedTypeParameterWithQualifiedUpperBound() =
        withAnalysisContext {
            listType.typeWith(stringType.wrapped(qualifier1)) shouldBeAssignable
                    listType.typeWith(typeParameter(anyNType.wrapped(qualifier1)))
        }

    @Test
    fun testQualifiedTypeIsSubTypeOfTypeParameterWithQualifiedUpperBound() = withAnalysisContext {
        val sTypeParameter = typeParameter(listType.typeWith(stringType))
        val tTypeParameter = typeParameter(sTypeParameter.wrapped(qualifier1))
        listType.typeWith(stringType)
            .wrapped(qualifier1) shouldBeSubTypeOf tTypeParameter
    }

    @Test
    fun testQualifiedTypeAliasIsSubTypeOfTypeParameterWithSameQualifiers() = withAnalysisContext {
        typeAlias(
            function(0)
                .copy(isMarkedComposable = true)
                .wrapped(qualifier1)
        ) shouldBeSubTypeOf typeParameter(
            function(0)
                .copy(isMarkedComposable = true)
                .wrapped(qualifier1)
        )
    }

    @Test
    fun testQualifiedTypeAliasIsNotSubTypeOfTypeParameterWithOtherQualifiers() = withAnalysisContext {
        typeAlias(
            function(0)
                .copy(isMarkedComposable = true)
                .wrapped(qualifier1)
        ) shouldNotBeSubTypeOf typeParameter(
                function(0)
                    .copy(isMarkedComposable = true)
                    .wrapped(qualifier2)
            )
    }*/

    @Test
    fun testTypeAliasIsNotSubTypeOfTypeParameterWithOtherTypeAliasUpperBound() = withAnalysisContext {
        val typeAlias1 = typeAlias(function(0).typeWith(stringType))
        val typeAlias2 = typeAlias(function(0).typeWith(intType))
        val typeParameter = typeParameter(typeAlias1)
        typeAlias2 shouldNotBeSubTypeOf typeParameter
    }

    @Test
    fun testTypeAliasIsSubTypeOfOtherTypeAlias() = withAnalysisContext {
        val typeAlias1 = typeAlias(function(0).typeWith(stringType))
        val typeAlias2 = typeAlias(typeAlias1)
        typeAlias2 shouldBeSubTypeOf typeAlias1
    }

    /*@Test
    fun testTypeAliasIsSubTypeOfTypeParameterWithTypeAliasUpperBound() = withAnalysisContext {
        val superTypeAlias = typeAlias(function(0))
        val typeParameterS = typeParameter(superTypeAlias)
        val typeParameterT = typeParameter(typeParameterS.wrapped(qualifier1))
        val subTypeAlias = typeAlias(superTypeAlias)
        subTypeAlias.wrapped(qualifier1) shouldBeSubTypeOf typeParameterT
    }*/

    @Test
    fun testSubTypeWithTypeParameterIsAssignableToSuperTypeWithOtherTypeParameterButSameSuperTypes() = withAnalysisContext {
        mutableListType.typeWith(typeParameter()) shouldBeAssignable listType.typeWith(typeParameter())
    }

    /*@Test
    fun testQualifiedSubTypeWithTypeParameterIsNotAssignableToSuperTypeWithOtherTypeParameterButSameSuperTypes() = withAnalysisContext {
        mutableListType.typeWith(typeParameter())
            .wrapped(qualifier1) shouldNotBeAssignable listType.typeWith(typeParameter())
    }*/

    @Test
    fun testComparableStackOverflowBug() = withAnalysisContext {
        floatType shouldNotBeSubTypeOf comparable.typeWith(intType)
    }

}