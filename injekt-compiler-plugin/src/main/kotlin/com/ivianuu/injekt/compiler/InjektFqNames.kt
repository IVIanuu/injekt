package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.name.FqName

object InjektFqNames {
    val InjektPackage = FqName("com.ivianuu.injekt")
    val InjektInternalPackage = FqName("com.ivianuu.injekt.internal")

    val ApplicationScope = FqName("com.ivianuu.injekt.ApplicationScope")
    val Behavior = FqName("com.ivianuu.injekt.Behavior")
    val Component = FqName("com.ivianuu.injekt.Component")
    val Injekt = FqName("com.ivianuu.injekt.Injekt")
    val Key = FqName("com.ivianuu.injekt.Key")
    val Module = FqName("com.ivianuu.injekt.Module")
    val ModuleImpl = FqName("com.ivianuu.injekt.ModuleImpl")
    val Param = FqName("com.ivianuu.injekt.Param")
    val Parameters = FqName("com.ivianuu.injekt.Parameters")
    val Qualifier = FqName("com.ivianuu.injekt.Qualifier")
    val Scope = FqName("com.ivianuu.injekt.Scope")
}