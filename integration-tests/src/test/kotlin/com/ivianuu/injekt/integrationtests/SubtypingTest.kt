/*
 * Copyright 2021 Manuel Wrage
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

import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.injekt.test.*
import org.jetbrains.kotlin.types.model.*
import org.junit.*

class SubtypingTest {
    @Test
    fun testSimpleTypeWithSameClassifierIsAssignable() = withTypeCheckerContext {
        stringType shouldBeAssignable stringType
    }

    @Test
    fun testSimpleTypeWithDifferentClassifierIsNotAssignable() = withTypeCheckerContext {
        stringType shouldNotBeAssignable intType
    }

    @Test
    fun testNonNullIsAssignableToNullable() = withTypeCheckerContext {
        stringType shouldBeAssignable stringType.nullable()
    }

    @Test
    fun testNullableIsNotAssignableToNonNullable() = withTypeCheckerContext {
        stringType.nullable() shouldNotBeAssignable stringType
    }

    @Test
    fun testMatchingGenericTypeIsAssignable() = withTypeCheckerContext {
        listType.typeWith(listOf(stringType)) shouldBeAssignable listType
    }

    @Test
    fun testNotMatchingGenericTypeIsNotAssignable() = withTypeCheckerContext {
        listType.typeWith(stringType) shouldNotBeAssignable listType.typeWith(intType)
    }

    @Test
    fun testAnyTypeIsAssignableToStarProjectedType() = withTypeCheckerContext {
        stringType shouldBeAssignable starProjectedType
    }

    @Test
    fun testStarIsNotAssignableToAnyType() = withTypeCheckerContext {
        starProjectedType shouldNotBeAssignable stringType
    }

    @Test
    fun testNullableTypeIsAssignableToStarProjectedType() = withTypeCheckerContext {
        stringType.nullable() shouldBeAssignable starProjectedType
    }

    @Test
    fun testQualifiedTypeIsAssignableToStarProjectedType() = withTypeCheckerContext {
        stringType.qualified(qualifier1) shouldBeAssignable starProjectedType
    }

    @Test
    fun testNothingIsAssignableToAnyType() = withTypeCheckerContext {
        nothingType shouldBeAssignable stringType
    }

    @Test
    fun testInvariant() = withTypeCheckerContext {
        val typeClass = classType(
            typeParameters = listOf(typeParameter(variance = TypeVariance.IN).classifier)
        )
        val charSequenceTypeClass = classType(typeClass.typeWith(charSequenceType))
        val stringTypeClass = typeClass.typeWith(stringType)
        charSequenceTypeClass shouldBeAssignable stringTypeClass
    }

    @Test
    fun testNothingIsAssignableToNullableType() = withTypeCheckerContext {
        nothingType shouldBeAssignable stringType.nullable()
    }

    @Test
    fun testNothingIsAssignableToQualifiedType() = withTypeCheckerContext {
        nothingType shouldBeAssignable stringType.qualified(qualifier1)
    }

    @Test
    fun testTypeAliasIsNotAssignableToOtherTypeAliasOfTheSameExpandedType() = withTypeCheckerContext {
        typeAlias(stringType) shouldNotBeAssignable typeAlias(stringType)
    }

    @Test
    fun testTypeAliasIsAssignableToOtherTypeAliasOfTheSameExpandedType() = withTypeCheckerContext {
        typeAlias(stringType) shouldNotBeAssignable typeAlias(stringType)
    }

    @Test
    fun testTypeAliasIsSubTypeOfExpandedType() = withTypeCheckerContext {
        typeAlias(stringType) shouldBeSubTypeOf stringType
    }

    @Test
    fun testNestedTypeAliasIsSubTypeOfExpandedType() = withTypeCheckerContext {
        typeAlias(typeAlias(stringType)) shouldBeSubTypeOf stringType
    }

    @Test
    fun testSameComposabilityIsAssignable() = withTypeCheckerContext {
        composableFunction(0) shouldBeAssignable composableFunction(0)
    }

    @Test
    fun testComposableTypeAliasIsSubTypeOfComposableFunctionUpperBound() = withTypeCheckerContext {
        typeAlias(composableFunction(0)) shouldBeAssignable typeParameter(composableFunction(0))
    }

    @Test
    fun testSameQualifiersIsAssignable() = withTypeCheckerContext {
        stringType.qualified(qualifier1) shouldBeAssignable stringType.qualified(qualifier1)
    }

    @Test
    fun testDifferentQualifiersIsNotAssignable() = withTypeCheckerContext {
        stringType.qualified(qualifier1) shouldNotBeAssignable stringType.qualified(qualifier2)
    }

    @Test
    fun testQualifiedIsSubTypeOfUnqualified() = withTypeCheckerContext {
        stringType.qualified(qualifier1) shouldBeSubTypeOf stringType
    }

    @Test
    fun testQualifiedIsNotAssignableToUnqualified() = withTypeCheckerContext {
        stringType.qualified(qualifier1) shouldNotBeAssignable stringType
    }

    @Test
    fun testSameQualifiersInDifferentOrderIsSubType() = withTypeCheckerContext {
        stringType.qualified(qualifier1, qualifier2) shouldBeSubTypeOf
                stringType.qualified(qualifier2, qualifier1)
    }

    @Test
    fun testSubTypeOfTypeParameterWithNullableAnyUpperBound() = withTypeCheckerContext {
        stringType shouldBeAssignable typeParameter()
    }

    @Test
    fun testComposableSubTypeOfTypeParameterWithNullableAnyUpperBound() = withTypeCheckerContext {
        composableFunction(0) shouldBeAssignable typeParameter()
    }

    @Test
    fun testComposableIsNotSubTypeOfNonComposable() = withTypeCheckerContext {
        composableFunction(0) shouldNotBeAssignable typeParameter(function(0))
    }

    @Test
    fun testSubTypeOfTypeParameterWithNonNullAnyUpperBound() = withTypeCheckerContext {
        stringType shouldBeAssignable typeParameter(nullable = false)
    }

    @Test
    fun testNullableSubTypeOfTypeParameterWithNonNullAnyUpperBound() = withTypeCheckerContext {
        stringType.nullable() shouldNotBeAssignable typeParameter(nullable = false)
    }

    @Test
    fun testSubTypeOfTypeParameterWithUpperBound() = withTypeCheckerContext {
        subType(stringType) shouldBeSubTypeOf typeParameter(stringType)
    }

    @Test
    fun testSubTypeOfTypeParameterWithNullableUpperBound() = withTypeCheckerContext {
        subType(stringType) shouldBeSubTypeOf typeParameter(stringType.nullable())
    }

    @Test
    fun testQualifiedSubTypeOfQualifiedTypeParameter() = withTypeCheckerContext {
        stringType.qualified(qualifier1) shouldBeAssignable
                typeParameter(nullable = false).qualified(qualifier1)
    }

    @Test
    fun testNestedQualifiedSubTypeOfNestedQualifiedTypeParameter() = withTypeCheckerContext {
        listType.typeWith(stringType.qualified(qualifier1)) shouldBeAssignable
                listType.typeWith(typeParameter(nullable = false).qualified(qualifier1))
    }

    @Test
    fun testUnqualifiedSubTypeOfTypeParameterWithQualifiedUpperBound() = withTypeCheckerContext {
        stringType shouldNotBeAssignable
                typeParameter(anyNType.qualified(qualifier1))
    }

    @Test
    fun testNestedUnqualifiedSubTypeOfNestedTypeParameterWithQualifiedUpperBound() =
        withTypeCheckerContext {
            listType.typeWith(stringType) shouldNotBeAssignable
                    listType.typeWith(typeParameter(anyNType.qualified(qualifier1)))
        }

    @Test
    fun testNestedQualifiedSubTypeOfNestedTypeParameterWithQualifiedUpperBound() =
        withTypeCheckerContext {
            listType.typeWith(stringType.qualified(qualifier1)) shouldBeAssignable
                    listType.typeWith(typeParameter(anyNType.qualified(qualifier1)))
        }

    @Test
    fun testQualifiedTypeIsSubTypeOfTypeParameterWithQualifiedUpperBound() = withTypeCheckerContext {
        val sTypeParameter = typeParameter(listType.typeWith(stringType))
        val tTypeParameter = typeParameter(sTypeParameter.qualified(qualifier1))
        listType.typeWith(stringType)
            .qualified(qualifier1) shouldBeSubTypeOf tTypeParameter
    }

    @Test
    fun testQualifiedTypeAliasIsSubTypeOfTypeParameterWithSameQualifiers() = withTypeCheckerContext {
        typeAlias(
            function(0)
                .copy(isMarkedComposable = true)
                .qualified(qualifier1)
        ) shouldBeSubTypeOf typeParameter(
            function(0)
                .copy(isMarkedComposable = true)
                .qualified(qualifier1)
        )
    }

    @Test
    fun testQualifiedTypeAliasIsNotSubTypeOfTypeParameterWithOtherQualifiers() = withTypeCheckerContext {
        typeAlias(
            function(0)
                .copy(isMarkedComposable = true)
                .qualified(qualifier1)
        ) shouldNotBeSubTypeOf typeParameter(
                function(0)
                    .copy(isMarkedComposable = true)
                    .qualified(qualifier2)
            )
    }

    @Test
    fun testTypeAliasIsNotSubTypeOfTypeParameterWithOtherTypeAliasUpperBound() = withTypeCheckerContext {
        val typeAlias1 = typeAlias(function(0).typeWith(stringType))
        val typeAlias2 = typeAlias(function(0).typeWith(intType))
        val typeParameter = typeParameter(typeAlias1)
        typeAlias2 shouldNotBeSubTypeOf typeParameter
    }

    @Test
    fun testTypeAliasIsSubTypeOfOtherTypeAlias() = withTypeCheckerContext {
        val typeAlias1 = typeAlias(function(0).typeWith(stringType))
        val typeAlias2 = typeAlias(typeAlias1)
        typeAlias2 shouldBeSubTypeOf typeAlias1
    }

    @Test
    fun testTypeAliasIsSubTypeOfTypeParameterWithTypeAliasUpperBound() = withTypeCheckerContext {
        val superTypeAlias = typeAlias(function(0))
        val typeParameterS = typeParameter(superTypeAlias)
        val typeParameterT = typeParameter(typeParameterS.qualified(qualifier1))
        val subTypeAlias = typeAlias(superTypeAlias)
        subTypeAlias.qualified(qualifier1) shouldBeSubTypeOf typeParameterT
    }

    @Test
    fun testSubTypeWithTypeParameterIsAssignableToSuperTypeWithOtherTypeParameterButSameSuperTypes() = withTypeCheckerContext {
        mutableListType.typeWith(typeParameter()) shouldBeAssignable listType.typeWith(typeParameter())
    }

    @Test
    fun testComparableStackOverflowBug() = withTypeCheckerContext {
        floatType shouldNotBeSubTypeOf comparable.typeWith(intType)
    }
}