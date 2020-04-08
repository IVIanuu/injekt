package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.resolve.DescriptorFactory
import org.jetbrains.kotlin.resolve.extensions.SyntheticResolveExtension
import org.jetbrains.kotlin.resolve.lazy.LazyClassContext
import org.jetbrains.kotlin.resolve.lazy.declarations.PackageMemberDeclarationProvider
import org.jetbrains.kotlin.resolve.scopes.MemberScope

class InjektResolveExtension : SyntheticResolveExtension {
    override fun generateSyntheticClasses(
        thisDescriptor: PackageFragmentDescriptor,
        name: Name,
        ctx: LazyClassContext,
        declarationProvider: PackageMemberDeclarationProvider,
        result: MutableSet<ClassDescriptor>
    ) {
        result += declarationProvider.getPropertyDeclarations(name)
            .filter { it.annotationEntries.any { it.text.contains("@BehaviorMarker") } }
            .map { property ->
                object : ClassDescriptorImpl(
                    thisDescriptor,
                    Name.identifier(property.name!!),
                    Modality.FINAL,
                    ClassKind.ANNOTATION_CLASS,
                    emptyList(),
                    SourceElement.NO_SOURCE,
                    false,
                    ctx.storageManager
                ) {
                    init {
                        val constructor =
                            DescriptorFactory.createPrimaryConstructorForObject(
                                this,
                                SourceElement.NO_SOURCE
                            )
                        initialize(
                            MemberScope.Empty,
                            setOf(constructor),
                            constructor
                        )
                        constructor.initialize(
                            emptyList(),
                            Visibilities.PUBLIC,
                            emptyList()
                        )
                        constructor.returnType = this.getDefaultType()
                    }
                }
            }
    }
}