/*
 * Copyright 2019 Manuel Wrage
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

package com.ivianuu.injekt.compiler

import com.squareup.kotlinpoet.ClassName
import com.squareup.kotlinpoet.ParameterizedTypeName.Companion.parameterizedBy
import com.squareup.kotlinpoet.TypeName
import org.jetbrains.kotlin.cli.common.messages.CompilerMessageSeverity
import org.jetbrains.kotlin.cli.common.messages.MessageCollector
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.annotations.AnnotationDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.descriptorUtil.module
import org.jetbrains.kotlin.types.KotlinType

lateinit var messageCollector: MessageCollector

fun msg(block: () -> String) {
    messageCollector.report(CompilerMessageSeverity.WARNING, "inject: ${block()}")
}

fun ClassDescriptor.asClassName() = fqNameSafe.asClassName()

fun FqName.asClassName() = ClassName.bestGuess(asString()) // todo

fun KotlinType.asTypeName(): TypeName {
    val type = constructor.declarationDescriptor!!.fqNameSafe.asClassName()
    return if (arguments.isNotEmpty()) {
        type.parameterizedBy(*arguments.map { it.type.asTypeName() }.toTypedArray())
    } else type
}

val FactoryAnnotation = FqName("com.ivianuu.injekt.Factory")
val InjektConstructorAnnotation = FqName("com.ivianuu.injekt.InjektConstructor")
val NameAnnotation = FqName("com.ivianuu.injekt.Name")
val ParamAnnotation = FqName("com.ivianuu.injekt.Param")
val ScopeAnnotation = FqName("com.ivianuu.injekt.Scope")
val SingleAnnotation = FqName("com.ivianuu.injekt.Single")

fun DeclarationDescriptor.hasAnnotatedAnnotations(annotation: FqName): Boolean =
    annotations.any { it.hasAnnotation(annotation, module) }

fun AnnotationDescriptor.hasAnnotation(annotation: FqName, module: ModuleDescriptor): Boolean {
    val thisFqName = this.fqName ?: return false
    val descriptor =
        module.findClassAcrossModuleDependencies(ClassId.topLevel(thisFqName)) ?: return false
    return descriptor.annotations.hasAnnotation(annotation)
}