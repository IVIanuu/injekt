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
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.model.*
import org.junit.*

class SubtypingTest {
  @Test fun testNullableAnyIsSuperTypeOfEveryOtherType() = withTypeCheckerContext {
    stringType shouldBeSubTypeOf nullableAny
    stringType.nullable() shouldBeSubTypeOf nullableAny
    qualifier1.wrap(stringType) shouldBeSubTypeOf nullableAny
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
    listType.typeWith(typeParameter()) shouldBeAssignableTo
        listType.withArguments(listOf(stringType))
  }

  @Test fun testMatchingGenericTypeIsAssignable8() = withTypeCheckerContext {
    listType.typeWith(qualifier1.wrap(typeParameter())) shouldBeAssignableTo
        listType.withArguments(listOf(qualifier1.wrap(stringType)))
  }

  @Test fun testMatchingGenericTypeIsAssignable2() = withTypeCheckerContext {
    val tpB = typeParameter(fqName = FqName("B"))
    val tpA = typeParameter(tpB, fqName = FqName("A"))
    val type = classType(typeParameters = listOf(tpA.classifier, tpB.classifier))
    type.withArguments(listOf(stringType, charSequenceType)) shouldBeAssignableTo type
  }

  @Test fun testMatchingGenericTypeIsAssignable3() = withTypeCheckerContext {
    val tpB = typeParameter(qualifier1.wrap(charSequenceType), fqName = FqName("B"))
    val tpA = typeParameter(tpB, fqName = FqName("A"))
    qualifier1.wrap(stringType) shouldBeAssignableTo tpA
  }

  @Test fun testMatchingGenericTypeIsAssignable5() = withTypeCheckerContext {
    val tpB = typeParameter(stringType, fqName = FqName("B"))
    val tpA = typeParameter(listType.typeWith(tpB), fqName = FqName("A"))
    listType.typeWith(intType) shouldNotBeAssignableTo tpA
  }

  @Test fun testMatchingGenericTypeIsAssignable6() = withTypeCheckerContext {
    val givenScopeS = typeParameter(stringType)
    val other = mapType
    givenScopeS shouldNotBeAssignableTo other
  }

  @Test fun testNotMatchingGenericTypeIsNotAssignable() = withTypeCheckerContext {
    listType.typeWith(stringType) shouldNotBeAssignableTo listType.typeWith(intType)
  }

  @Test fun testInvariant() = withTypeCheckerContext {
    val typeClass = classType(
      typeParameters = listOf(typeParameter(variance = TypeVariance.IN).classifier)
    )
    val charSequenceTypeClass = classType(typeClass.typeWith(charSequenceType))
    val stringTypeClass = typeClass.typeWith(stringType)
    charSequenceTypeClass shouldBeAssignableTo stringTypeClass
  }

  @Test fun testTypeAliasIsNotAssignableToOtherTypeAliasOfTheSameExpandedType() = withTypeCheckerContext {
    typeAlias(stringType) shouldNotBeAssignableTo typeAlias(stringType)
  }

  @Test fun testTypeAliasIsAssignableToOtherTypeAliasOfTheSameExpandedType() = withTypeCheckerContext {
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

  @Test fun testComposableTypeAliasIsSubTypeOfComposableFunctionUpperBound() = withTypeCheckerContext {
    typeAlias(composableFunction(0)) shouldBeAssignableTo typeParameter(composableFunction(0))
  }

  @Test fun testSameQualifiersIsAssignable() = withTypeCheckerContext {
    qualifier1.wrap(stringType) shouldBeAssignableTo qualifier1.wrap(stringType)
  }

  @Test fun testDifferentQualifiersIsNotAssignable() = withTypeCheckerContext {
    qualifier1.wrap(stringType) shouldNotBeAssignableTo qualifier2.wrap(stringType)
  }

  @Test fun testQualifiedIsNotSubTypeOfUnqualified() = withTypeCheckerContext {
    qualifier1.wrap(stringType) shouldNotBeSubTypeOf stringType
  }

  @Test fun testQualifiedIsNotAssignableToUnqualified() = withTypeCheckerContext {
    qualifier1.wrap(stringType) shouldNotBeAssignableTo stringType
  }

  @Test fun testUnqualifiedTypeParameterIsNotAssignableToQualifiedType() = withTypeCheckerContext {
    typeParameter(stringType) shouldNotBeAssignableTo qualifier1.wrap(stringType)
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

  @Test fun testComposableSubTypeOfTypeParameterWithNullableAnyUpperBound() = withTypeCheckerContext {
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

  @Test fun testNestedQualifiedSubTypeOfNestedQualifiedTypeParameter() = withTypeCheckerContext {
    listType.typeWith(qualifier1.wrap(stringType)) shouldBeAssignableTo
        listType.typeWith(qualifier1.wrap(typeParameter(nullable = false)))
  }

  @Test fun testTypeAliasIsNotSubTypeOfTypeParameterWithOtherTypeAliasUpperBound() =
    withTypeCheckerContext {
      val typeAlias1 = typeAlias(function(0).typeWith(stringType))
      val typeAlias2 = typeAlias(function(0).typeWith(intType))
      val typeParameter = typeParameter(typeAlias1)
      typeAlias2 shouldNotBeSubTypeOf typeParameter
    }

  @Test fun testTypeAliasIsSubTypeOfOtherTypeAlias() = withTypeCheckerContext {
    val typeAlias1 = typeAlias(function(0).typeWith(stringType))
    val typeAlias2 = typeAlias(typeAlias1)
    typeAlias2 shouldBeSubTypeOf typeAlias1
  }

  @Test fun testTypeAliasIsAssignableToTypeParameterWithTypeAliasUpperBound() = withTypeCheckerContext {
    /*val superTypeAlias = typeAlias(function(0))
    val typeParameterS = typeParameter(superTypeAlias)
    val typeParameterT = typeParameter(typeParameterS.qualified(qualifier1))
    val subTypeAlias = typeAlias(superTypeAlias)
    subTypeAlias.qualified(qualifier1) shouldBeAssignableTo typeParameterT*/
    // todo
  }

  @Test fun testSubTypeWithTypeParameterIsAssignableToSuperTypeWithOtherTypeParameterButSameSuperTypes() =
    withTypeCheckerContext {
      mutableListType.typeWith(typeParameter()) shouldBeAssignableTo listType.typeWith(typeParameter())
    }

  @Test fun testComparableStackOverflowBug() = withTypeCheckerContext {
    floatType shouldNotBeSubTypeOf comparable.typeWith(intType)
  }
}