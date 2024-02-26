/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class)

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import io.kotest.matchers.*
import io.kotest.matchers.maps.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.diagnostics.*
import org.jetbrains.kotlin.fir.*
import org.jetbrains.kotlin.fir.analysis.checkers.*
import org.jetbrains.kotlin.fir.analysis.checkers.context.*
import org.jetbrains.kotlin.fir.analysis.checkers.expression.*
import org.jetbrains.kotlin.fir.analysis.extensions.*
import org.jetbrains.kotlin.fir.expressions.*
import org.jetbrains.kotlin.fir.extensions.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.model.*
import org.junit.*

class TypeTest {
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

  @Test fun testSimpleInference() = withTypeCheckerContext {
    val superType = typeParameter()
    val map = runInference(stringType, superType)
    map[superType.classifier] shouldBe stringType
  }

  @Test fun testInferenceWithExtraTypeParameter() = withTypeCheckerContext {
    val typeParameterU = typeParameter(fqName = FqName("U"))
    val typeParameterS = typeParameter(listType.withArguments(typeParameterU), fqName = FqName("S"))
    val typeParameterT = typeParameter(typeParameterS, fqName = FqName("T"))
    val substitutionType = listType.withArguments(stringType)
    val map = runInference(substitutionType, typeParameterT)
    map[typeParameterT.classifier] shouldBe substitutionType
    map[typeParameterS.classifier] shouldBe substitutionType
    map[typeParameterU.classifier] shouldBe stringType
  }

  @Test fun testInferenceWithNestedGenerics() = withTypeCheckerContext {
    val superType = typeParameter()
    val map = runInference(listType.withArguments(stringType), listType.withArguments(superType))
    map[superType.classifier] shouldBe stringType
  }

  @Test fun testInferenceWithTags() = withTypeCheckerContext {
    val untaggedSuperType = typeParameter()
    val taggedSuperType = tag1.wrap(untaggedSuperType)
    val substitutionType = tag1.wrap(stringType)
    val map = runInference(substitutionType, taggedSuperType)
    map[untaggedSuperType.classifier] shouldBe stringType
  }

  @Test fun testInferenceWithSubClass() = withTypeCheckerContext {
    val classType = classType(listType.withArguments(stringType))
    val typeParameter = typeParameter()
    val map = runInference(classType, listType.withArguments(typeParameter))
    map.shouldHaveSize(1)
    map.shouldContain(typeParameter.classifier, stringType)
  }

  @Test fun testInferenceWithSameTags() = withTypeCheckerContext {
    val typeParameterS = typeParameter()
    val typeParameterT = typeParameter(tag1.wrap(typeParameterS))
    val substitutionType = tag1.wrap(stringType)
    val map = runInference(substitutionType, typeParameterT)
    map[typeParameterT.classifier] shouldBe substitutionType
    map[typeParameterS.classifier] shouldBe stringType
  }

  @Test fun testInferenceInScopedLikeScenario() = withTypeCheckerContext {
    val scoped = typeFor(FqName("com.ivianuu.injekt.common.Scoped"))

    val (scopedT, scopedU, scopedN) = collectDeclarationsInFqName(
      FqName("com.ivianuu.injekt.common.Scoped.Companion"),
      ctx
    )
      .single { it.fqName.shortName() == "scoped".asNameId() }
      .typeParameterSymbols!!
      .map { it.toInjektClassifier(ctx) }

    val substitutionType = scoped.wrap(stringType)
      .let {
        it.withArguments(listOf(intType) + it.arguments.drop(1))
      }
    val context = runAddOnInjectableInference(
      scopedT.defaultType,
      substitutionType,
      emptyList(),
      ctx
    )
    context.fixedTypeVariables[scopedT] shouldBe substitutionType
    context.fixedTypeVariables[scopedU] shouldBe stringType
    context.fixedTypeVariables[scopedN] shouldBe intType
  }

  private fun TypeCheckerTestContext.runInference(
    subType: InjektType,
    superType: InjektType,
    staticTypeParameters: List<InjektClassifier> = emptyList()
  ): Map<InjektClassifier, InjektType> {
    val context = subType.runCandidateInference(
      superType,
      staticTypeParameters,
      true,
      ctx
    )
    return context.fixedTypeVariables
  }
}

fun withTypeCheckerContext(block: TypeCheckerTestContext.() -> Unit) {
  codegen(
    """
      fun invoke() = listOf<Unit>()
    """,
    config = {
      compilerPluginRegistrars += object : CompilerPluginRegistrar() {
        override val supportsK2: Boolean
          get() = true

        override fun ExtensionStorage.registerExtensions(configuration: CompilerConfiguration) {
          FirExtensionRegistrarAdapter.registerExtension(
            object : FirExtensionRegistrar() {
              override fun ExtensionRegistrarContext.configurePlugin() {
                +FirAdditionalCheckersExtension.Factory { session ->
                  object : FirAdditionalCheckersExtension(session) {
                    override val expressionCheckers: ExpressionCheckers =
                      object : ExpressionCheckers() {
                        override val callCheckers: Set<FirCallChecker> = setOf(
                          object : FirCallChecker(MppCheckerKind.Platform) {
                            override fun check(
                              expression: FirCall,
                              context: CheckerContext,
                              reporter: DiagnosticReporter
                            ) {
                              block(TypeCheckerTestContext(context.session))
                            }
                          }
                        )
                      }
                  }
                }
              }
            }
          )
        }
      }
    }
  )
}

class TypeCheckerTestContext(session: FirSession) {
  val ctx = InjektContext().also { it.session = session }

  val comparable = typeFor(StandardNames.FqNames.comparable)
  val any = typeFor(StandardNames.FqNames.any.toSafe())
  val nullableAny = any.nullable()
  val floatType = typeFor(StandardNames.FqNames._float.toSafe())
  val intType = typeFor(StandardNames.FqNames._int.toSafe())
  val stringType = typeFor(StandardNames.FqNames.string.toSafe())
  val charSequenceType = typeFor(StandardNames.FqNames.charSequence.toSafe())
  val listType = typeFor(StandardNames.FqNames.list)
  val mutableListType = typeFor(StandardNames.FqNames.mutableList)
  val mapType = typeFor(StandardNames.FqNames.map)
  val nothing = typeFor(StandardNames.FqNames.nothing.toSafe())
  val nullableNothing = nothing.nullable()

  val tag1 = typeFor(FqName("com.ivianuu.injekt.integrationtests.Tag1"))
  val tag2 = typeFor(FqName("com.ivianuu.injekt.integrationtests.Tag2"))

  private var id = 0

  fun subType(
    vararg superTypes: InjektType,
    fqName: FqName = FqName("SubType${id}"),
  ) = InjektClassifier(
    key = fqName.asString(),
    classId = ClassId.topLevel(fqName),
    fqName = fqName,
    lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) {
      if (superTypes.isNotEmpty()) superTypes.toList() else listOf(any)
    },
  ).defaultType

  fun classType(
    vararg superTypes: InjektType,
    typeParameters: List<InjektClassifier> = emptyList(),
    fqName: FqName = FqName("ClassType${id++}"),
  ) = InjektClassifier(
    key = fqName.asString(),
    fqName = fqName,
    classId = ClassId.topLevel(fqName),
    lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) {
      if (superTypes.isNotEmpty()) superTypes.toList() else listOf(any)
    },
    typeParameters = typeParameters
  ).defaultType

  fun typeParameter(
    fqName: FqName = FqName("TypeParameter${id++}"),
    nullable: Boolean = true,
    variance: TypeVariance = TypeVariance.INV
  ) = typeParameter(
    upperBounds = emptyArray(),
    nullable = nullable,
    fqName = fqName,
    variance = variance
  )

  fun typeParameter(
    vararg upperBounds: InjektType,
    nullable: Boolean = true,
    variance: TypeVariance = TypeVariance.INV,
    fqName: FqName = FqName("TypeParameter${id++}"),
  ) = InjektClassifier(
    key = fqName.asString(),
    fqName = fqName,
    classId = null,
    lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) {
      if (upperBounds.isNotEmpty()) upperBounds.toList() else
        listOf(any.copy(isMarkedNullable = nullable))
    },
    isTypeParameter = true,
    variance = variance
  ).defaultType

  fun typeFor(fqName: FqName) = findClassifierForFqName(fqName, ctx)
    ?.toInjektClassifier(ctx)?.defaultType ?: error("Wtf $fqName")

  infix fun InjektType.shouldBeAssignableTo(other: InjektType) {
    shouldBeAssignableTo(other, emptyList())
  }

  fun InjektType.shouldBeAssignableTo(
    other: InjektType,
    staticTypeParameters: List<InjektClassifier> = emptyList()
  ) {
    val context = runCandidateInference(
      other,
      staticTypeParameters,
      true,
      ctx
    )
    if (!context.isOk)
      throw AssertionError("'$this' is not assignable to '$other'")
  }

  infix fun InjektType.shouldNotBeAssignableTo(other: InjektType) {
    shouldNotBeAssignableTo(other, emptyList())
  }

  fun InjektType.shouldNotBeAssignableTo(
    other: InjektType,
    staticTypeParameters: List<InjektClassifier> = emptyList()
  ) {
    val context = runCandidateInference(
      other,
      staticTypeParameters,
      true,
      ctx
    )
    if (context.isOk)
      throw AssertionError("'$this' is assignable to '$other'")
  }

  infix fun InjektType.shouldBeSubTypeOf(other: InjektType) {
    if (!isSubTypeOf(other, ctx))
      throw AssertionError("'$this' is not sub type of '$other'")
  }

  infix fun InjektType.shouldNotBeSubTypeOf(other: InjektType) {
    if (isSubTypeOf(other, ctx))
      throw AssertionError("'$this' is sub type of '$other'")
  }
}

fun InjektType.nullable() = copy(isMarkedNullable = true)

fun InjektType.withArguments(vararg arguments: InjektType) =
  withArguments(arguments.toList())
