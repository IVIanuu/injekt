/*
 * Copyright 2021 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.memberScopeForFqName
import com.ivianuu.injekt.compiler.resolution.ClassifierRef
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.buildContext
import com.ivianuu.injekt.compiler.resolution.buildContextForSpreadingInjectable
import com.ivianuu.injekt.compiler.resolution.toClassifierRef
import com.ivianuu.injekt.compiler.resolution.withArguments
import com.ivianuu.injekt.compiler.resolution.wrap
import com.ivianuu.injekt.test.codegen
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

  @Test fun testGetSubstitutionMapWithTags() = withTypeCheckerContext {
    val untaggedSuperType = typeParameter()
    val taggedSuperType = tag1.wrap(untaggedSuperType)
    val substitutionType = tag1.wrap(stringType)
    val map = getSubstitutionMap(substitutionType, taggedSuperType)
    map[untaggedSuperType.classifier] shouldBe stringType
  }

  @Test fun testGetSubstitutionMapWithSubClass() = withTypeCheckerContext {
    val classType = classType(listType.withArguments(stringType))
    val typeParameter = typeParameter()
    val map = getSubstitutionMap(classType, listType.withArguments(typeParameter))
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
    val scoped = typeFor(FqName("com.ivianuu.injekt.common.Scoped"))
    val (scopedT, scopedU, scopedN) = memberScopeForFqName(
      FqName("com.ivianuu.injekt.common.Scoped.Companion"),
      NoLookupLocation.FROM_BACKEND,
      ctx
    )!!.first
      .getContributedFunctions("scoped".asNameId(), NoLookupLocation.FROM_BACKEND)
      .single()
      .typeParameters
      .map { it.toClassifierRef(ctx) }
    val substitutionType = scoped.wrap(stringType)
      .let {
        it.withArguments(listOf(intType) + it.arguments.drop(1))
      }
    val (_, map) = buildContextForSpreadingInjectable(
      scopedT.defaultType,
      substitutionType,
      emptyList(),
      ctx
    )
    map[scopedT] shouldBe substitutionType
    map[scopedU] shouldBe stringType
    map[scopedN] shouldBe intType
  }

  @Test fun todoExtractToTypeOnlyTest() = codegen(
    """
      interface Key<R>

      interface DialogKey<R> : Key<R>

      @Tag annotation class KeyUiTag<K : Key<*>>
      typealias KeyUi<K> = @KeyUiTag<K> @Composable () -> Unit

      typealias ModelKeyUi<K, S> = (ModelKeyUiScope<K, S>) -> Unit
      
      interface ModelKeyUiScope<K : Key<*>, S>
      
      @Provide fun <@Spread U : ModelKeyUi<K, S>, K : Key<*>, S> modelKeyUi(): KeyUi<K> = TODO()
    """,
    """
      object DonationKey : DialogKey<Unit>

      object DonationModel

      @Provide val donationUi: ModelKeyUi<DonationKey, DonationModel> = TODO()
    """,
    """
      fun invoke() = inject<KeyUi<DonationKey>>()
    """
  )

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
