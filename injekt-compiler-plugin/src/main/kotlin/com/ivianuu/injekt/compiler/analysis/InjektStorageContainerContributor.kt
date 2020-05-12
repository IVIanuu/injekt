package com.ivianuu.injekt.compiler.analysis

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform

class InjektStorageContainerContributor(
    private val typeAnnotationChecker: TypeAnnotationChecker
) : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        container.useInstance(AnnotatedBindingChecker())
        container.useInstance(ClassOfChecker(typeAnnotationChecker))
        container.useInstance(FactoryChecker())
        container.useInstance(MapChecker())
        container.useInstance(ModuleChecker(typeAnnotationChecker))
        container.useInstance(QualifierChecker())
        container.useInstance(QualifiedExpressionCollector())
        container.useInstance(ScopeChecker())
        container.useInstance(MembersInjectorChecker())
        container.useInstance(typeAnnotationChecker)
    }
}
