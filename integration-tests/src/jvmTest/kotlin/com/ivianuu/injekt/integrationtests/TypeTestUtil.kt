/*
 * Copyright 2022 Manuel Wrage. Use of this source code is governed by the Apache 2.0 license.
 */

package com.ivianuu.injekt.integrationtests

import com.ivianuu.injekt.*
import com.ivianuu.injekt.compiler.*
import com.ivianuu.injekt.compiler.resolution.*
import com.ivianuu.injekt.test.*
import org.jetbrains.kotlin.analyzer.*
import org.jetbrains.kotlin.builtins.*
import org.jetbrains.kotlin.cli.jvm.compiler.*
import org.jetbrains.kotlin.com.intellij.mock.*
import org.jetbrains.kotlin.com.intellij.openapi.project.*
import org.jetbrains.kotlin.compiler.plugin.*
import org.jetbrains.kotlin.config.*
import org.jetbrains.kotlin.descriptors.*
import org.jetbrains.kotlin.incremental.components.*
import org.jetbrains.kotlin.name.*
import org.jetbrains.kotlin.psi.*
import org.jetbrains.kotlin.resolve.*
import org.jetbrains.kotlin.resolve.jvm.extensions.*
import org.jetbrains.kotlin.types.model.*

fun withTypeCheckerContext(block: TypeCheckerTestContext.() -> Unit) {
  codegen(
    """
    """,
    config = {
      compilerPlugins += object : ComponentRegistrar {
        override fun registerProjectComponents(
          project: MockProject,
          configuration: CompilerConfiguration,
        ) {
          AnalysisHandlerExtension.registerExtension(
            project,
            object : AnalysisHandlerExtension {
              override fun analysisCompleted(
                project: Project,
                module: ModuleDescriptor,
                bindingTrace: BindingTrace,
                files: Collection<KtFile>,
              ): AnalysisResult? {
                block(TypeCheckerTestContext(module))
                return null
              }
            }
          )
        }
      }
    }
  )
}

class TypeCheckerTestContext(module: ModuleDescriptor) {
  @Provide val ctx = Context(module, CliBindingTrace())

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

  fun function(parameterCount: Int) = typeFor(
    FqName("kotlin.Function$parameterCount")
  )

  val tag1 = typeFor(FqName("com.ivianuu.injekt.test.Tag1"))

  val tag2 = typeFor(FqName("com.ivianuu.injekt.test.Tag2"))

  private var id = 0

  fun subType(
    vararg superTypes: TypeRef,
    fqName: FqName = FqName("SubType${id}"),
  ) = ClassifierRef(
    key = fqName.asString(),
    fqName = fqName,
    lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) {
      if (superTypes.isNotEmpty()) superTypes.toList() else listOf(any)
    },
  ).defaultType

  fun classType(
    vararg superTypes: TypeRef,
    typeParameters: List<ClassifierRef> = emptyList(),
    fqName: FqName = FqName("ClassType${id++}"),
  ) = ClassifierRef(
    key = fqName.asString(),
    fqName = fqName,
    lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) {
      if (superTypes.isNotEmpty()) superTypes.toList() else listOf(any)
    },
    typeParameters = typeParameters
  ).defaultType

  fun typeParameter(
    fqName: FqName = FqName("TypeParameter${id++}"),
    nullable: Boolean = true,
    variance: TypeVariance = TypeVariance.INV
  ): TypeRef =
    typeParameter(
      upperBounds = *emptyArray(),
      nullable = nullable,
      fqName = fqName,
      variance = variance
    )

  fun typeParameter(
    vararg upperBounds: TypeRef,
    nullable: Boolean = true,
    variance: TypeVariance = TypeVariance.INV,
    fqName: FqName = FqName("TypeParameter${id++}"),
  ) = ClassifierRef(
    key = fqName.asString(),
    fqName = fqName,
    lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) {
      if (upperBounds.isNotEmpty()) upperBounds.toList() else
        listOf(any.copy(isNullable = nullable))
    },
    isTypeParameter = true,
    variance = variance
  ).defaultType

  fun typeFor(fqName: FqName) = classifierDescriptorForFqName(
    fqName, NoLookupLocation.FROM_BACKEND, ctx)
    ?.defaultType?.toTypeRef(ctx = ctx) ?: error("Wtf $fqName")

  infix fun TypeRef.shouldBeAssignableTo(other: TypeRef) {
    shouldBeAssignableTo(other, emptyList())
  }

  fun TypeRef.shouldBeAssignableTo(
    other: TypeRef,
    staticTypeParameters: List<ClassifierRef> = emptyList()
  ) {
    val context = buildContext(
      other,
      staticTypeParameters,
      true,
      ctx
    )
    if (!context.isOk) {
      throw AssertionError("'$this' is not assignable to '$other'")
    }
  }

  infix fun TypeRef.shouldNotBeAssignableTo(other: TypeRef) {
    shouldNotBeAssignableTo(other, emptyList())
  }

  fun TypeRef.shouldNotBeAssignableTo(
    other: TypeRef,
    staticTypeParameters: List<ClassifierRef> = emptyList()
  ) {
    val context = buildContext(
      other,
      staticTypeParameters,
      true,
      ctx
    )
    if (context.isOk) {
      throw AssertionError("'$this' is assignable to '$other'")
    }
  }

  infix fun TypeRef.shouldBeSubTypeOf(other: TypeRef) {
    if (!isSubTypeOf(other, ctx)) {
      throw AssertionError("'$this' is not sub type of '$other'")
    }
  }

  infix fun TypeRef.shouldNotBeSubTypeOf(other: TypeRef) {
    if (isSubTypeOf(other, ctx)) {
      throw AssertionError("'$this' is sub type of '$other'")
    }
  }
}

fun TypeRef.nullable() = copy(isNullable = true)

fun TypeRef.withArguments(vararg arguments: TypeRef) =
  withArguments(arguments.toList())
