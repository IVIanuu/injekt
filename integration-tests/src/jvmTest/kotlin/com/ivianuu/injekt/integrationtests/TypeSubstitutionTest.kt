/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.injekt.test.*
import io.kotest.matchers.*
import io.kotest.matchers.maps.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.junit.*

class TypeSubstitutionTest {
  @Test fun testGetSubstitutionMap() = withTypeCheckerContext {
    val superType = typeParameter()
    val map = getSubstitutionMap(stringType, superType)
    map[superType.classifier] shouldBe stringType
  }

  @Test fun testGetSubstitutionMapWithExtraTypeParameter() = withTypeCheckerContext {
    val typeParameterU = typeParameter(fqName = FqName("U"))
    val typeParameterS = typeParameter(listType.withArguments(typeParameterU), fqName = FqName("S"))
    val typeParameterT = typeParameter(typeParameterS, fqName = FqName("T"))
    val substitutionType = listType.withArguments(stringType)
    val map = getSubstitutionMap(substitutionType, typeParameterT)
    map[typeParameterT.classifier] shouldBe substitutionType
    map[typeParameterS.classifier] shouldBe substitutionType
    map[typeParameterU.classifier] shouldBe stringType
  }

  @Test fun testGetSubstitutionMapWithNestedGenerics() = withTypeCheckerContext {
    val superType = typeParameter()
    val map = getSubstitutionMap(listType.withArguments(stringType), listType.withArguments(superType))
    map[superType.classifier] shouldBe stringType
  }

  @Test fun testGetSubstitutionMapWithSubClass() = withTypeCheckerContext {
    val classType = classType(listType.withArguments(stringType))
    val typeParameter = typeParameter()
    val map = getSubstitutionMap(classType, listType.withArguments(typeParameter))
    map.shouldHaveSize(1)
    map.shouldContain(typeParameter.classifier, stringType)
  }

  private fun TypeCheckerTestContext.getSubstitutionMap(
    subType: TypeRef,
    superType: TypeRef,
    staticTypeParameters: List<ClassifierRef> = emptyList()
  ): Map<ClassifierRef, TypeRef> {
    val context = subType.buildContext(
      superType,
      staticTypeParameters,
      true,
      ctx
    )
    return context.fixedTypeVariables
  }
}
