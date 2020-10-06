package com.ivianuu.injekt.compiler

import com.google.devtools.ksp.processing.Resolver
import com.ivianuu.injekt.Binding

@Binding(GeneratorComponent::class)
class InjektTypes(resolver: Resolver) {
    val assisted = resolver.getClassDeclarationByName(InjektPackage.child("Assisted")).asType()
    val binding = resolver.getClassDeclarationByName(InjektPackage.child("Binding")).asType()
    val component = resolver.getClassDeclarationByName(InjektPackage.child("Component")).asType()
    val childComponent = resolver.getClassDeclarationByName(InjektPackage.child("ChildComponent")).asType()
    val funBinding = resolver.getClassDeclarationByName(InjektPackage.child("FunBinding")).asType()
    val mapEntries = resolver.getClassDeclarationByName(InjektPackage.child("MapEntries")).asType()
    val module = resolver.getClassDeclarationByName(InjektPackage.child("Module")).asType()
    val setElements = resolver.getClassDeclarationByName(InjektPackage.child("SetElements")).asType()

    val functionAlias = resolver.getClassDeclarationByName(InternalPackage.child("FunctionAlias")).asType()

    val bindingModule = resolver.findClassDeclarationByName(MergePackage.child("BindingModule"))?.asType()
    val mergeComponent = resolver.findClassDeclarationByName(MergePackage.child("MergeComponent"))?.asType()
    val mergeChildComponent = resolver.findClassDeclarationByName(MergePackage.child("MergeChildComponent"))?.asType()
    val mergeInto = resolver.findClassDeclarationByName(MergePackage.child("MergeInto"))?.asType()
    val generateMergeComponents = resolver.findClassDeclarationByName(MergePackage.child("GenerateMergeComponents"))?.asType()

    val composable = resolver.findClassDeclarationByName("androidx.compose.runtime.Composable")?.asType()

    companion object {
        val InjektPackage = "com.ivianuu.injekt"
        val InternalPackage = InjektPackage.child("internal")
        val IndexPackage = InternalPackage.child("index")
        val MergePackage = InjektPackage.child("merge")
    }
}
