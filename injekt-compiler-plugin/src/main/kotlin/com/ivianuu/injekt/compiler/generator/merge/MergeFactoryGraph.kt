package com.ivianuu.injekt.compiler.generator.merge

import com.ivianuu.injekt.compiler.generator.TypeRef
import org.jetbrains.kotlin.name.FqName

data class MergeDeclarations(
    val entryPoints: List<MergeEntryPointDescriptor>,
    val modules: List<MergeModuleDescriptor>,
    val mergeFactories: List<TypeRef>
)

data class MergeEntryPointDescriptor(
    val component: TypeRef,
    val entryPoint: TypeRef
)

data class MergeModuleDescriptor(
    val component: TypeRef,
    val module: TypeRef
)

data class MergeFactoryDescriptor(val fqName: FqName) {

    val entryPoints = mutableListOf<FqName>()
    val modules = mutableListOf<FqName>()

    val children = mutableListOf<FqName>()
    val parents = mutableListOf<FqName>()

}