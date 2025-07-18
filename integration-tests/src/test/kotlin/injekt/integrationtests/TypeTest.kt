/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

@file:OptIn(ExperimentalCompilerApi::class, DeprecatedForRemovalCompilerApi::class)

package injekt.integrationtests

import injekt.compiler.*
import injekt.compiler.resolution.*
import io.kotest.matchers.*
import io.kotest.matchers.maps.*
import org.jetbrains.kotlin.*
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
import org.jetbrains.kotlin.fir.resolve.providers.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.types.model.*
import org.junit.*

class TypeTest {
  @Test fun testNullableAnyIsSuperTypeOfEveryOtherType() = typeTest {
    stringType shouldBeSubTypeOf nullableAny
    stringType.nullable() shouldBeSubTypeOf nullableAny
    tag1.wrap(stringType) shouldBeSubTypeOf nullableAny
  }

  @Test fun testNonNullAnyIsSuperTypeOfEveryOtherNonNullType() = typeTest {
    stringType shouldBeSubTypeOf any
    stringType.nullable() shouldNotBeSubTypeOf any
  }

  @Test fun testNonNullNothingIsSubTypeOfEveryOtherNonNullType() = typeTest {
    nothing shouldBeSubTypeOf stringType
    nothing shouldBeSubTypeOf stringType.nullable()
  }

  @Test fun testNullableNothingIsSubTypeOfEveryOtherNullableType() = typeTest {
    nullableNothing shouldNotBeSubTypeOf stringType
    nullableNothing shouldBeSubTypeOf stringType.nullable()
  }

  @Test fun testSimpleTypeWithSameClassifierIsAssignable() = typeTest {
    stringType shouldBeAssignableTo stringType
  }

  @Test fun testSimpleTypeWithDifferentClassifierIsNotAssignable() = typeTest {
    stringType shouldNotBeAssignableTo intType
  }

  @Test fun testNonNullIsAssignableToNullable() = typeTest {
    stringType shouldBeAssignableTo stringType.nullable()
  }

  @Test fun testNullableIsNotAssignableToNonNullable() = typeTest {
    stringType.nullable() shouldNotBeAssignableTo stringType
  }

  @Test fun testMatchingGenericTypeIsAssignable() = typeTest {
    listType.withArguments(typeParameter()) shouldBeAssignableTo
        listType.withArguments(listOf(stringType))
  }

  @Test fun testMatchingGenericTypeIsAssignable8() = typeTest {
    listType.withArguments(tag1.wrap(typeParameter())) shouldBeAssignableTo
        listType.withArguments(listOf(tag1.wrap(stringType)))
  }

  @Test fun testMatchingGenericTypeIsAssignable2() = typeTest {
    val tpB = typeParameter(fqName = FqName("B"))
    val tpA = typeParameter(tpB, fqName = FqName("A"))
    val type = classType(typeParameters = listOf(tpA.classifier, tpB.classifier))
    type.withArguments(listOf(stringType, charSequenceType)) shouldBeAssignableTo type
  }

  @Test fun testMatchingGenericTypeIsAssignable3() = typeTest {
    val tpB = typeParameter(tag1.wrap(charSequenceType), fqName = FqName("B"))
    val tpA = typeParameter(tpB, fqName = FqName("A"))
    tag1.wrap(stringType) shouldBeAssignableTo tpA
  }

  @Test fun testMatchingGenericTypeIsAssignable5() = typeTest {
    val tpB = typeParameter(stringType, fqName = FqName("B"))
    val tpA = typeParameter(listType.withArguments(tpB), fqName = FqName("A"))
    listType.withArguments(intType) shouldNotBeAssignableTo tpA
  }

  @Test fun testMatchingGenericTypeIsAssignable6() = typeTest {
    val scopeS = typeParameter(stringType)
    val other = mapType
    scopeS shouldNotBeAssignableTo other
  }

  @Test fun testNotMatchingGenericTypeIsNotAssignable() = typeTest {
    listType.withArguments(stringType) shouldNotBeAssignableTo listType.withArguments(intType)
  }

  @Test fun testInvariant() = typeTest {
    val typeClass = classType(
      typeParameters = listOf(typeParameter(variance = TypeVariance.IN).classifier)
    )
    val charSequenceTypeClass = classType(typeClass.withArguments(charSequenceType))
    val stringTypeClass = typeClass.withArguments(stringType)
    charSequenceTypeClass shouldBeAssignableTo stringTypeClass
  }

  @Test fun testSameTagsIsAssignable() = typeTest {
    tag1.wrap(stringType) shouldBeAssignableTo tag1.wrap(stringType)
  }

  @Test fun testDifferentTagsIsNotAssignable() = typeTest {
    tag1.wrap(stringType) shouldNotBeAssignableTo tag2.wrap(stringType)
  }

  @Test fun testTaggedIsNotSubTypeOfUntagged() = typeTest {
    tag1.wrap(stringType) shouldNotBeSubTypeOf stringType
  }

  @Test fun testTaggedIsNotAssignableToUntagged() = typeTest {
    tag1.wrap(stringType) shouldNotBeAssignableTo stringType
  }

  @Test fun testUntaggedTypeParameterIsNotAssignableToTaggedType() = typeTest {
    typeParameter(stringType) shouldNotBeAssignableTo tag1.wrap(stringType)
  }

  @Test fun testSubTypeOfTypeParameterWithNullableAnyUpperBound() = typeTest {
    stringType shouldBeAssignableTo typeParameter()
  }

  @Test fun testIsNotSubTypeOfTypeParameterInScope() = typeTest {
    val typeParameter = typeParameter()
    stringType.shouldNotBeAssignableTo(
      typeParameter,
      listOf(typeParameter.classifier)
    )
  }

  @Test fun testIsSubTypeOfTypeParameterInScope() = typeTest {
    val superTypeParameter = typeParameter()
    val subTypeParameter = typeParameter(superTypeParameter)
    subTypeParameter.shouldBeAssignableTo(
      superTypeParameter,
      listOf(superTypeParameter.classifier, subTypeParameter.classifier)
    )
  }

  @Test fun testSubTypeOfTypeParameterWithNonNullAnyUpperBound() = typeTest {
    stringType shouldBeAssignableTo typeParameter(nullable = false)
  }

  @Test fun testNullableSubTypeOfTypeParameterWithNonNullAnyUpperBound() = typeTest {
    stringType.nullable() shouldNotBeAssignableTo typeParameter(nullable = false)
  }

  @Test fun testSubTypeOfTypeParameterWithUpperBound() = typeTest {
    subType(stringType) shouldBeAssignableTo typeParameter(stringType)
  }

  @Test fun testSubTypeOfTypeParameterWithNullableUpperBound() = typeTest {
    subType(stringType) shouldBeAssignableTo typeParameter(stringType.nullable())
  }

  @Test fun testNestedTaggedSubTypeOfNestedTaggedTypeParameter() = typeTest {
    listType.withArguments(tag1.wrap(stringType)) shouldBeAssignableTo
        listType.withArguments(tag1.wrap(typeParameter(nullable = false)))
  }

  @Test
  fun testSubTypeWithTypeParameterIsAssignableToSuperTypeWithOtherTypeParameterButSameSuperTypes() =
    typeTest {
      mutableListType.withArguments(typeParameter()) shouldBeAssignableTo listType.withArguments(typeParameter())
    }

  @Test fun testComparableStackOverflowBug() = typeTest {
    floatType shouldNotBeSubTypeOf comparable.withArguments(intType)
  }

  @Test fun testStarProjectionIsNotSubTypeNormalType() = typeTest {
    scope.withArguments(STAR_PROJECTION_TYPE) shouldNotBeSubTypeOf scope.withArguments(any)
  }

  @Test fun testSimpleInference() = typeTest {
    val superType = typeParameter()
    val map = runInference(stringType, superType)
    map[superType.classifier] shouldBe stringType
  }

  @Test fun testInferenceWithExtraTypeParameter() = typeTest {
    val typeParameterU = typeParameter(fqName = FqName("U"))
    val typeParameterS = typeParameter(listType.withArguments(typeParameterU), fqName = FqName("S"))
    val typeParameterT = typeParameter(typeParameterS, fqName = FqName("T"))
    val substitutionType = listType.withArguments(stringType)
    val map = runInference(substitutionType, typeParameterT)
    map[typeParameterT.classifier] shouldBe substitutionType
    map[typeParameterS.classifier] shouldBe substitutionType
    map[typeParameterU.classifier] shouldBe stringType
  }

  @Test fun testInferenceWithNestedGenerics() = typeTest {
    val superType = typeParameter()
    val map = runInference(listType.withArguments(stringType), listType.withArguments(superType))
    map[superType.classifier] shouldBe stringType
  }

  @Test fun testInferenceWithTags() = typeTest {
    val untaggedSuperType = typeParameter()
    val taggedSuperType = tag1.wrap(untaggedSuperType)
    val substitutionType = tag1.wrap(stringType)
    val map = runInference(substitutionType, taggedSuperType)
    map[untaggedSuperType.classifier] shouldBe stringType
  }

  @Test fun testInferenceWithSubClass() = typeTest {
    val classType = classType(listType.withArguments(stringType))
    val typeParameter = typeParameter()
    val map = runInference(classType, listType.withArguments(typeParameter))
    map.shouldHaveSize(1)
    map.shouldContain(typeParameter.classifier, stringType)
  }

  @Test fun testInferenceWithSameTags() = typeTest {
    val typeParameterS = typeParameter()
    val typeParameterT = typeParameter(tag1.wrap(typeParameterS))
    val substitutionType = tag1.wrap(stringType)
    val map = runInference(substitutionType, typeParameterT)
    map[typeParameterT.classifier] shouldBe substitutionType
    map[typeParameterS.classifier] shouldBe stringType
  }

  @Test fun testInferenceInScopedLikeScenario() = typeTest {
    val scoped = typeFor(FqName("injekt.common.Scoped"))

    val (scopedT, scopedU, scopedN) = collectDeclarationsInFqName(
      FqName("injekt.common.Scoped.Companion"),
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

  @Test fun testNestedJavaType() = multiCodegen(
    """
      inline fun buildNotification(
        channel: java.util.Map<String, String>,
        scope: Scope<*> = inject,
        builder: java.util.Map.Entry<String, String>.() -> Unit = {}
      ): java.util.Map<String, String> = TODO()
    """,
    """
      fun invoke(scope: Scope<*> = inject) = buildNotification(
        TODO()
      ) {
      }
    """
  )
}

private fun typeTest(assertions: TypeCheckerTestContext.() -> Unit) {
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
                              assertions(TypeCheckerTestContext(context.session))
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
  val scope = typeFor(FqName("injekt.common.Scope"))

  val tag1 = typeFor(FqName("injekt.integrationtests.Tag1"))
  val tag2 = typeFor(FqName("injekt.integrationtests.Tag2"))

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

  fun typeFor(fqName: FqName) = ctx.session.symbolProvider
    .getClassLikeSymbolByClassId(ClassId.topLevel(fqName))
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
