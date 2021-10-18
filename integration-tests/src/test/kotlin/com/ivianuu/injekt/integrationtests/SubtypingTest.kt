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

import com.ivianuu.injekt.compiler.resolution.copy
import com.ivianuu.injekt.compiler.resolution.withArguments
import com.ivianuu.injekt.compiler.resolution.wrap
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.types.model.TypeVariance
import org.junit.Test

class SubtypingTest {
  @Test fun testNullableAnyIsSuperTypeOfEveryOtherType() = withTypeCheckerContext {
    stringType shouldBeSubTypeOf nullableAny
    stringType.nullable() shouldBeSubTypeOf nullableAny
    tag1.wrap(stringType) shouldBeSubTypeOf nullableAny
    stringType.copy(isMarkedComposable = true) shouldBeSubTypeOf nullableAny
    stringType.nullable().copy(isMarkedComposable = true) shouldBeSubTypeOf nullableAny
  }

  @Test fun testNonNullAnyIsSuperTypeOfEveryOtherNonNullType() = withTypeCheckerContext {
    stringType shouldBeSubTypeOf any
    stringType.nullable() shouldNotBeSubTypeOf any
    stringType.copy(isMarkedComposable = true) shouldBeSubTypeOf any
    stringType.nullable().copy(isMarkedComposable = true) shouldNotBeSubTypeOf any
  }

  @Test fun testNonNullNothingIsSubTypeOfEveryOtherNonNullType() = withTypeCheckerContext {
    nothing shouldBeSubTypeOf stringType
    nothing shouldBeSubTypeOf stringType.nullable()
    nothing shouldBeSubTypeOf stringType.copy(isMarkedComposable = true)
    nothing shouldBeSubTypeOf stringType.nullable().copy(isMarkedComposable = true)
  }

  @Test fun testNullableNothingIsSubTypeOfEveryOtherNullableType() = withTypeCheckerContext {
    nullableNothing shouldNotBeSubTypeOf stringType
    nullableNothing shouldBeSubTypeOf stringType.nullable()
    nullableNothing shouldNotBeSubTypeOf stringType.copy(isMarkedComposable = true)
    nullableNothing shouldBeSubTypeOf stringType.nullable().copy(isMarkedComposable = true)
  }

  @Test fun testSimpleTypeWithSameClassifierIsAssignable() = withTypeCheckerContext {
    stringType shouldBeAssignableTo stringType
  }

  @Test fun testSimpleTypeWithDifferentClassifierIsNotAssignable() = withTypeCheckerContext {
    stringType shouldNotBeAssignableTo intType
  }

  @Test fun testNonNullIsAssignableToNullable() = withTypeCheckerContext {
    stringType shouldBeAssignableTo stringType.nullable()
  }

  @Test fun testNullableIsNotAssignableToNonNullable() = withTypeCheckerContext {
    stringType.nullable() shouldNotBeAssignableTo stringType
  }

  @Test fun testMatchingGenericTypeIsAssignable() = withTypeCheckerContext {
    listType.withArguments(typeParameter()) shouldBeAssignableTo
        listType.withArguments(listOf(stringType))
  }

  @Test fun testMatchingGenericTypeIsAssignable8() = withTypeCheckerContext {
    listType.withArguments(tag1.wrap(typeParameter())) shouldBeAssignableTo
        listType.withArguments(listOf(tag1.wrap(stringType)))
  }

  @Test fun testMatchingGenericTypeIsAssignable2() = withTypeCheckerContext {
    val tpB = typeParameter(fqName = FqName("B"))
    val tpA = typeParameter(tpB, fqName = FqName("A"))
    val type = classType(typeParameters = listOf(tpA.classifier, tpB.classifier))
    type.withArguments(listOf(stringType, charSequenceType)) shouldBeAssignableTo type
  }

  @Test fun testMatchingGenericTypeIsAssignable3() = withTypeCheckerContext {
    val tpB = typeParameter(tag1.wrap(charSequenceType), fqName = FqName("B"))
    val tpA = typeParameter(tpB, fqName = FqName("A"))
    tag1.wrap(stringType) shouldBeAssignableTo tpA
  }

  @Test fun testMatchingGenericTypeIsAssignable5() = withTypeCheckerContext {
    val tpB = typeParameter(stringType, fqName = FqName("B"))
    val tpA = typeParameter(listType.withArguments(tpB), fqName = FqName("A"))
    listType.withArguments(intType) shouldNotBeAssignableTo tpA
  }

  @Test fun testMatchingGenericTypeIsAssignable6() = withTypeCheckerContext {
    val scopeS = typeParameter(stringType)
    val other = mapType
    scopeS shouldNotBeAssignableTo other
  }

  @Test fun testNotMatchingGenericTypeIsNotAssignable() = withTypeCheckerContext {
    listType.withArguments(stringType) shouldNotBeAssignableTo listType.withArguments(intType)
  }

  @Test fun testInvariant() = withTypeCheckerContext {
    val typeClass = classType(
      typeParameters = listOf(typeParameter(variance = TypeVariance.IN).classifier)
    )
    val charSequenceTypeClass = classType(typeClass.withArguments(charSequenceType))
    val stringTypeClass = typeClass.withArguments(stringType)
    charSequenceTypeClass shouldBeAssignableTo stringTypeClass
  }

  @Test fun testTypeAliasIsNotAssignableToOtherTypeAliasOfTheSameExpandedType() =
    withTypeCheckerContext {
      typeAlias(stringType) shouldNotBeAssignableTo typeAlias(stringType)
    }

  @Test fun testTypeAliasIsAssignableToOtherTypeAliasOfTheSameExpandedType() =
    withTypeCheckerContext {
      typeAlias(stringType) shouldNotBeAssignableTo typeAlias(stringType)
    }

  @Test fun testTypeAliasIsSubTypeOfExpandedType() = withTypeCheckerContext {
    typeAlias(stringType) shouldBeSubTypeOf stringType
  }

  @Test fun testNestedTypeAliasIsSubTypeOfExpandedType() = withTypeCheckerContext {
    typeAlias(typeAlias(stringType)) shouldBeSubTypeOf stringType
  }

  @Test fun testSameComposabilityIsAssignable() = withTypeCheckerContext {
    composableFunction(0) shouldBeAssignableTo composableFunction(0)
  }

  @Test fun testSameTagsIsAssignable() = withTypeCheckerContext {
    tag1.wrap(stringType) shouldBeAssignableTo tag1.wrap(stringType)
  }

  @Test fun testDifferentTagsIsNotAssignable() = withTypeCheckerContext {
    tag1.wrap(stringType) shouldNotBeAssignableTo tag2.wrap(stringType)
  }

  @Test fun testTaggedIsNotSubTypeOfUntagged() = withTypeCheckerContext {
    tag1.wrap(stringType) shouldNotBeSubTypeOf stringType
  }

  @Test fun testTaggedIsNotAssignableToUntagged() = withTypeCheckerContext {
    tag1.wrap(stringType) shouldNotBeAssignableTo stringType
  }

  @Test fun testUntaggedTypeParameterIsNotAssignableToTaggedType() = withTypeCheckerContext {
    typeParameter(stringType) shouldNotBeAssignableTo tag1.wrap(stringType)
  }

  @Test fun testComposableTypeAliasIsSubTypeOfComposableFunctionUpperBound() =
    withTypeCheckerContext {
      typeAlias(composableFunction(0)) shouldBeAssignableTo typeParameter(composableFunction(0))
    }

  @Test fun testSubTypeOfTypeParameterWithNullableAnyUpperBound() = withTypeCheckerContext {
    stringType shouldBeAssignableTo typeParameter()
  }

  @Test fun testIsNotSubTypeOfTypeParameterInScope() = withTypeCheckerContext {
    val typeParameter = typeParameter()
    stringType.shouldNotBeAssignableTo(
      typeParameter,
      listOf(typeParameter.classifier)
    )
  }

  @Test fun testIsSubTypeOfTypeParameterInScope() = withTypeCheckerContext {
    val superTypeParameter = typeParameter()
    val subTypeParameter = typeParameter(superTypeParameter)
    subTypeParameter.shouldBeAssignableTo(
      superTypeParameter,
      listOf(superTypeParameter.classifier, subTypeParameter.classifier)
    )
  }

  @Test fun testComposableSubTypeOfTypeParameterWithNullableAnyUpperBound() =
    withTypeCheckerContext {
      composableFunction(0) shouldBeAssignableTo typeParameter()
    }

  @Test fun testComposableIsNotSubTypeOfNonComposable() = withTypeCheckerContext {
    composableFunction(0) shouldNotBeAssignableTo typeParameter(function(0))
  }

  @Test fun testSubTypeOfTypeParameterWithNonNullAnyUpperBound() = withTypeCheckerContext {
    stringType shouldBeAssignableTo typeParameter(nullable = false)
  }

  @Test fun testNullableSubTypeOfTypeParameterWithNonNullAnyUpperBound() = withTypeCheckerContext {
    stringType.nullable() shouldNotBeAssignableTo typeParameter(nullable = false)
  }

  @Test fun testSubTypeOfTypeParameterWithUpperBound() = withTypeCheckerContext {
    subType(stringType) shouldBeAssignableTo typeParameter(stringType)
  }

  @Test fun testSubTypeOfTypeParameterWithNullableUpperBound() = withTypeCheckerContext {
    subType(stringType) shouldBeAssignableTo typeParameter(stringType.nullable())
  }

  @Test fun testNestedTaggedSubTypeOfNestedTaggedTypeParameter() = withTypeCheckerContext {
    listType.withArguments(tag1.wrap(stringType)) shouldBeAssignableTo
        listType.withArguments(tag1.wrap(typeParameter(nullable = false)))
  }

  @Test fun testTypeAliasIsNotSubTypeOfTypeParameterWithOtherTypeAliasUpperBound() =
    withTypeCheckerContext {
      val typeAlias1 = typeAlias(function(0).withArguments(stringType))
      val typeAlias2 = typeAlias(function(0).withArguments(intType))
      val typeParameter = typeParameter(typeAlias1)
      typeAlias2 shouldNotBeSubTypeOf typeParameter
    }

  @Test fun testTypeAliasIsSubTypeOfOtherTypeAlias() = withTypeCheckerContext {
    val typeAlias1 = typeAlias(function(0).withArguments(stringType))
    val typeAlias2 = typeAlias(typeAlias1)
    typeAlias2 shouldBeSubTypeOf typeAlias1
  }

  @Test
  fun testSubTypeWithTypeParameterIsAssignableToSuperTypeWithOtherTypeParameterButSameSuperTypes() =
    withTypeCheckerContext {
      mutableListType.withArguments(typeParameter()) shouldBeAssignableTo listType.withArguments(typeParameter())
    }

  @Test fun testComparableStackOverflowBug() = withTypeCheckerContext {
    floatType shouldNotBeSubTypeOf comparable.withArguments(intType)
  }
}