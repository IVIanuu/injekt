package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstExternalPackageFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.addChild
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName

class Psi2AstStubGenerator {

    private val packageFragments = mutableMapOf<FqName, AstExternalPackageFragment>()
    private val classes = mutableMapOf<ClassDescriptor, AstClass>()

    fun getOrCreateClass(descriptor: ClassDescriptor): AstClass = classes.getOrPut(descriptor) {
        AstClass(
            name = descriptor.name,
            kind = descriptor.kind.toAstClassKind(),
            visibility = descriptor.visibility.toAstVisibility(),
            expectActual = multiPlatformModalityOf(
                descriptor.isActual,
                descriptor.isExpect
            ),
            modality = descriptor.modality.toAstModality(),
            isCompanion = descriptor.isCompanionObject,
            isFun = descriptor.isFun,
            isData = descriptor.isData,
            isExternal = descriptor.isExternal,
            isInner = descriptor.isInner
        ).also { descriptor.findPackage().fqName.getPackageFragment().addChild(it) }
    }

    private fun FqName.getPackageFragment() = packageFragments.getOrPut(this) {
        AstExternalPackageFragment(this)
    }

    private fun DeclarationDescriptor.toAst(): AstDeclaration = when (this) {
        is ClassDescriptor -> this.toAst()
        else -> error("Unexpected declaration $this")
    }

}
