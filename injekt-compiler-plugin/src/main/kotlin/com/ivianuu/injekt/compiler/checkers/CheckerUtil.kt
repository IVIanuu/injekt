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

package com.ivianuu.injekt.compiler.checkers

import com.ivianuu.injekt.compiler.InjektFqNames
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.annotations.Annotated
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.builtIns
import org.jetbrains.kotlin.types.KotlinType
import org.jetbrains.kotlin.types.replace
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection

fun Annotated.hasAnnotation(fqName: FqName): Boolean =
    annotations.hasAnnotation(fqName)

fun AnnotationDescriptor.hasAnnotation(annotation: FqName): Boolean =
    type.constructor.declarationDescriptor!!.hasAnnotation(annotation)

fun Annotated.hasAnnotatedAnnotations(
    annotation: FqName
): Boolean = annotations.any { it.hasAnnotation(annotation) }

fun Annotated.getAnnotatedAnnotations(annotation: FqName): List<AnnotationDescriptor> =
    annotations.filter { it.hasAnnotation(annotation) }

fun FunctionDescriptor.getFunctionType(): KotlinType {
    val parameters =
        listOfNotNull(extensionReceiverParameter?.type) + valueParameters.map { it.type }
    return (if (isSuspend) builtIns.getSuspendFunction(parameters.size)
    else builtIns.getFunction(parameters.size))
        .defaultType
        .replace(newArguments = parameters.map { it.asTypeProjection() } + returnType!!.asTypeProjection())
}

fun FunctionDescriptor.getGivenFunctionType(): KotlinType {
    val assistedParameters =
        listOfNotNull(extensionReceiverParameter?.type) + valueParameters.map { it.type }
            .filter { it.hasAnnotation(InjektFqNames.Assisted) }
    return (if (isSuspend) builtIns.getSuspendFunction(assistedParameters.size)
    else builtIns.getFunction(assistedParameters.size))
        .defaultType
        .replace(newArguments = assistedParameters.map { it.asTypeProjection() } + returnType!!.asTypeProjection())
}
