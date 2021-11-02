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

import com.ivianuu.injekt.Provide
import com.ivianuu.injekt.compiler.Context
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.classifierDescriptorForFqName
import com.ivianuu.injekt.compiler.resolution.ClassifierRef
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.buildContext
import com.ivianuu.injekt.compiler.resolution.copy
import com.ivianuu.injekt.compiler.resolution.isSubTypeOf
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.compiler.resolution.withArguments
import com.ivianuu.injekt.test.codegen
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.cli.jvm.compiler.CliBindingTrace
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension
import org.jetbrains.kotlin.types.model.TypeVariance

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
  @Provide val ctx = Context(module, InjektFqNames.Default, CliBindingTrace())

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

  fun composableFunction(parameterCount: Int) = typeFor(
    FqName("kotlin.Function$parameterCount")
  ).copy(isMarkedComposable = true)

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

  fun typeAlias(
    expandedType: TypeRef,
    fqName: FqName = FqName("Alias${id++}"),
  ) = ClassifierRef(
    key = fqName.asString(),
    fqName = fqName,
    lazySuperTypes = lazy(LazyThreadSafetyMode.NONE) { listOf(expandedType) },
    isTypeAlias = true
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
        listOf(any.copy(isMarkedNullable = nullable))
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

fun TypeRef.nullable() = copy(isMarkedNullable = true)

fun TypeRef.nonNull() = copy(isMarkedNullable = false)

fun TypeRef.withArguments(vararg arguments: TypeRef) =
  withArguments(arguments.toList())
