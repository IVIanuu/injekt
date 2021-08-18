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

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.resolution.*
import io.kotest.matchers.maps.shouldContain
import io.kotest.matchers.maps.shouldHaveSize
import io.kotest.matchers.shouldBe
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.junit.Test

class TypeSubstitutionTest {
  @Test fun testGetSubstitutionMap() = withTypeCheckerContext {
    val superType = typeParameter()
    val map = getSubstitutionMap(stringType, superType)
    map[superType.classifier] shouldBe stringType
  }

  @Test fun testGetSubstitutionMapWithExtraTypeParameter() = withTypeCheckerContext {
    val typeParameterU = typeParameter(fqName = FqName("U"))
    val typeParameterS = typeParameter(listType.typeWith(typeParameterU), fqName = FqName("S"))
    val typeParameterT = typeParameter(typeParameterS, fqName = FqName("T"))
    val substitutionType = listType.typeWith(stringType)
    val map = getSubstitutionMap(substitutionType, typeParameterT)
    map[typeParameterT.classifier] shouldBe substitutionType
    map[typeParameterS.classifier] shouldBe substitutionType
    map[typeParameterU.classifier] shouldBe stringType
  }

  @Test fun testGetSubstitutionMapWithNestedGenerics() = withTypeCheckerContext {
    val superType = typeParameter()
    val map = getSubstitutionMap(listType.typeWith(stringType), listType.typeWith(superType))
    map[superType.classifier] shouldBe stringType
  }

  @Test fun testGetSubstitutionMapWithTags() = withTypeCheckerContext {
    val untaggedSuperType = typeParameter()
    val taggedSuperType = tag1.wrap(untaggedSuperType)
    val substitutionType = tag1.wrap(stringType)
    val map = getSubstitutionMap(substitutionType, taggedSuperType)
    map[untaggedSuperType.classifier] shouldBe stringType
  }

  @Test fun testGetSubstitutionMapWithSubClass() = withTypeCheckerContext {
    val classType = classType(listType.typeWith(stringType))
    val typeParameter = typeParameter()
    val map = getSubstitutionMap(classType, listType.typeWith(typeParameter))
    map.shouldHaveSize(1)
    map.shouldContain(typeParameter.classifier, stringType)
  }

  @Test fun testGetSubstitutionMapWithSameTags() = withTypeCheckerContext {
    val typeParameterS = typeParameter()
    val typeParameterT = typeParameter(tag1.wrap(typeParameterS))
    val substitutionType = tag1.wrap(stringType)
    val map = getSubstitutionMap(substitutionType, typeParameterT)
    map[typeParameterT.classifier] shouldBe substitutionType
    map[typeParameterS.classifier] shouldBe stringType
  }

  @Test fun testGetSubstitutionMapInScopedLikeScenario() = withTypeCheckerContext {
    val scoped = typeFor(FqName("com.ivianuu.injekt.scope.Scoped"))
    val (scopedT, scopedU, scopedN) = analysisContext.injektContext.memberScopeForFqName(
      FqName("com.ivianuu.injekt.scope.Scoped.Companion"),
      NoLookupLocation.FROM_BACKEND
    )!!
      .getContributedFunctions("scopedValue".asNameId(), NoLookupLocation.FROM_BACKEND)
      .single()
      .typeParameters
      .map { it.toClassifierRef() }
    val namedScope = typeFor(FqName("com.ivianuu.injekt.scope.AppScope"))
    val substitutionType = scoped.wrap(stringType)
      .let {
        it.withArguments(listOf(namedScope) + it.arguments.drop(1))
      }
    val (_, map) = buildContextForSpreadingInjectable(
      buildBaseContextForSpreadingInjectable(substitutionType, emptyList()),
      scopedT.defaultType,
      substitutionType
    )
    map[scopedT] shouldBe substitutionType
    map[scopedU] shouldBe stringType
    map[scopedN] shouldBe namedScope
  }

  private fun TypeCheckerTestContext.getSubstitutionMap(
    subType: TypeRef,
    superType: TypeRef,
    staticTypeParameters: List<ClassifierRef> = emptyList()
  ): Map<ClassifierRef, TypeRef> {
    val context = subType.buildContext(
      subType.buildBaseContext(staticTypeParameters),
      superType,
      true
    )
    return context.fixedTypeVariables
  }
}
