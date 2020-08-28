package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclaration
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstExternalPackageFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.addChild
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.name.FqName

class Psi2AstStubGenerator(
    private val storage: Psi2AstStorage
) {

    fun get(descriptor: DeclarationDescriptor): AstDeclaration {
        return when (descriptor) {
            is ClassDescriptor -> descriptor.toAstClassStub()
            is SimpleFunctionDescriptor -> descriptor.toAstSimpleFunctionStub()
            else -> error("Unexpected descriptor $descriptor")
        }
    }

    private fun ClassDescriptor.toAstClassStub() = storage.classes.getOrPut(this) {
        AstClass(
            name = name,
            kind = kind.toAstClassKind(),
            visibility = visibility.toAstVisibility(),
            expectActual = expectActualOf(isActual, isExpect),
            modality = modality.toAstModality(),
            isCompanion = isCompanionObject,
            isFun = isFun,
            isData = isData,
            isExternal = isExternal,
            isInner = isInner
        ).also {
            findPackage().fqName.getPackageFragment().addChild(it)
        }
    }

    private fun SimpleFunctionDescriptor.toAstSimpleFunctionStub() =
        storage.simpleFunctions.getOrPut(this) {
            TODO()
        }

    private fun FqName.getPackageFragment() = storage.externalPackageFragments.getOrPut(this) {
        AstExternalPackageFragment(this)
    }

}
