/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
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
    tag1.wrap(stringType) shouldBeSubTypeOf nullableAny
  }

  @Test fun testNonNullAnyIsSuperTypeOfEveryOtherNonNullType() = withTypeCheckerContext {
    stringType shouldBeSubTypeOf any
    stringType.nullable() shouldNotBeSubTypeOf any
  }

  @Test fun testNonNullNothingIsSubTypeOfEveryOtherNonNullType() = withTypeCheckerContext {
    nothing shouldBeSubTypeOf stringType
    nothing shouldBeSubTypeOf stringType.nullable()
  }

  @Test fun testNullableNothingIsSubTypeOfEveryOtherNullableType() = withTypeCheckerContext {
    nullableNothing shouldNotBeSubTypeOf stringType
    nullableNothing shouldBeSubTypeOf stringType.nullable()
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

  @Test
  fun testSubTypeWithTypeParameterIsAssignableToSuperTypeWithOtherTypeParameterButSameSuperTypes() =
    withTypeCheckerContext {
      mutableListType.withArguments(typeParameter()) shouldBeAssignableTo listType.withArguments(typeParameter())
    }

  @Test fun testComparableStackOverflowBug() = withTypeCheckerContext {
    floatType shouldNotBeSubTypeOf comparable.withArguments(intType)
  }
}
