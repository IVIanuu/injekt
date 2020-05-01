package com.ivianuu.injekt.compiler.analysis

import org.jetbrains.kotlin.container.StorageComponentContainer
import org.jetbrains.kotlin.container.useInstance
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.platform.TargetPlatform

class InjektStorageContainerContributor(
    private val moduleChecker: ModuleChecker
) : StorageComponentContainerContributor {
    override fun registerModuleComponents(
        container: StorageComponentContainer,
        platform: TargetPlatform,
        moduleDescriptor: ModuleDescriptor
    ) {
        container.useInstance(QualifiedExpressionCollector())
        container.useInstance(FactoryChecker())
        container.useInstance(MapChecker())
        container.useInstance(moduleChecker)
        container.useInstance(ScopeChecker())
    }
}