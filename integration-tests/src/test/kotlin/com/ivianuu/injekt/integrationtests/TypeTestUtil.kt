/*
 * Copyright 2020 Manuel Wrage
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

import com.ivianuu.injekt.compiler.InjektContext
import com.ivianuu.injekt.compiler.resolution.ClassifierRef
import com.ivianuu.injekt.compiler.resolution.STAR_PROJECTION_TYPE
import com.ivianuu.injekt.compiler.resolution.TypeRef
import com.ivianuu.injekt.compiler.resolution.copy
import com.ivianuu.injekt.compiler.resolution.isAssignableTo
import com.ivianuu.injekt.compiler.resolution.isSubTypeOf
import com.ivianuu.injekt.compiler.resolution.toTypeRef
import com.ivianuu.injekt.test.codegen
import org.jetbrains.kotlin.analyzer.AnalysisResult
import org.jetbrains.kotlin.builtins.StandardNames
import org.jetbrains.kotlin.com.intellij.mock.MockProject
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.compiler.plugin.ComponentRegistrar
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassifierAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.jvm.extensions.AnalysisHandlerExtension

fun withTypeCheckerContext(
    block: TypeCheckerContext.() -> Unit,
) {
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
                                block(TypeCheckerContext(module))
                                return null
                            }
                        }
                    )
                }
            }
        }
    )
}

class TypeCheckerContext(val module: ModuleDescriptor) {

    val context = InjektContext(module)
    val comparable = typeFor(StandardNames.FqNames.comparable)
    val anyType = typeFor(StandardNames.FqNames.any.toSafe())
    val anyNType = anyType.copy(isMarkedNullable = true)
    val floatType = typeFor(StandardNames.FqNames._float.toSafe())
    val intType = typeFor(StandardNames.FqNames._int.toSafe())
    val stringType = typeFor(StandardNames.FqNames.string.toSafe())
    val charSequenceType = typeFor(StandardNames.FqNames.charSequence.toSafe())
    val listType = typeFor(StandardNames.FqNames.list)
    val mutableListType = typeFor(StandardNames.FqNames.mutableList)
    val starProjectedType = STAR_PROJECTION_TYPE

    fun composableFunction(parameterCount: Int) = typeFor(
        FqName("kotlin.Function$parameterCount")
    ).copy(isMarkedComposable = true)

    fun function(parameterCount: Int) = typeFor(
        FqName("kotlin.Function$parameterCount")
    )

    val qualifier1 = typeFor(FqName("com.ivianuu.injekt.test.Qualifier1"))

    val qualifier2 = typeFor(FqName("com.ivianuu.injekt.test.Qualifier2"))

    private var id = 0

    fun subType(
        vararg superTypes: TypeRef,
        fqName: FqName = FqName("SubType${id}"),
    ) = ClassifierRef(
        fqName = fqName,
        superTypes = if (superTypes.isNotEmpty()) superTypes.toList() else listOf(anyType),
    ).defaultType

    fun typeAlias(
        expandedType: TypeRef,
        fqName: FqName = FqName("Alias${id++}"),
    ) = ClassifierRef(
        fqName = fqName,
        superTypes = listOf(expandedType),
        isTypeAlias = true
    ).defaultType

    fun classType(
        vararg superTypes: TypeRef,
        fqName: FqName = FqName("ClassType${id++}"),
    ) = ClassifierRef(
        fqName = fqName,
        superTypes = if (superTypes.isNotEmpty()) superTypes.toList() else listOf(anyType),
    ).defaultType

    fun typeParameter(
        fqName: FqName = FqName("TypeParameter${id++}"),
        nullable: Boolean = true,
    ): TypeRef =
        typeParameter(upperBounds = *emptyArray(), nullable = nullable, fqName = fqName)

    fun typeParameter(
        vararg upperBounds: TypeRef,
        nullable: Boolean = true,
        fqName: FqName = FqName("TypeParameter${id++}"),
    ) = ClassifierRef(
        fqName = fqName,
        superTypes = if (upperBounds.isNotEmpty()) upperBounds.toList() else
            listOf(anyType.copy(isMarkedNullable = nullable)),
        isTypeParameter = true
    ).defaultType

    fun typeFor(fqName: FqName) = module.findClassifierAcrossModuleDependencies(
        ClassId.topLevel(fqName)
    )!!.defaultType.toTypeRef(context, null)

    infix fun TypeRef.shouldBeAssignable(other: TypeRef) {
        if (!isAssignableTo(context, other)) {
            throw AssertionError("'$this' is not assignable to '$other'")
        }
    }

    infix fun TypeRef.shouldNotBeAssignable(other: TypeRef) {
        if (isAssignableTo(context, other)) {
            throw AssertionError("'$this' is assignable to '$other'")
        }
    }

    infix fun TypeRef.shouldBeSubTypeOf(other: TypeRef) {
        if (!isSubTypeOf(context, other)) {
            throw AssertionError("'$this' is not sub type of '$other'")
        }
    }

    infix fun TypeRef.shouldNotBeSubTypeOf(other: TypeRef) {
        if (isSubTypeOf(context, other)) {
            throw AssertionError("'$this' is sub type of '$other'")
        }
    }
}

fun TypeRef.nullable() = copy(isMarkedNullable = true)

fun TypeRef.nonNull() = copy(isMarkedNullable = false)

fun TypeRef.qualified(qualifier: TypeRef) = copy(qualifier = qualifier)

fun TypeRef.typeWith(vararg typeArguments: TypeRef) =
    copy(arguments = typeArguments.toList())
