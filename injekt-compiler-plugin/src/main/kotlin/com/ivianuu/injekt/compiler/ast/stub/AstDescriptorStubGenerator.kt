package com.ivianuu.injekt.compiler.ast.stub

import com.ivianuu.injekt.compiler.ast.AstClass
import com.ivianuu.injekt.compiler.ast.AstDeclaration
import com.ivianuu.injekt.compiler.ast.psi.multiPlatformModalityOf
import com.ivianuu.injekt.compiler.ast.psi.toAstClassId
import com.ivianuu.injekt.compiler.ast.psi.toAstClassKind
import com.ivianuu.injekt.compiler.ast.psi.toAstModality
import com.ivianuu.injekt.compiler.ast.psi.toAstVisibility
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.resolve.descriptorUtil.classId

class AstDescriptorStubGenerator {

    private val classes = mutableMapOf<ClassDescriptor, AstClass>()

    fun generateClass(descriptor: ClassDescriptor): AstClass = classes.getOrPut(descriptor) {
        AstClass(
            classId = descriptor.classId!!.toAstClassId(),
            kind = descriptor.kind.toAstClassKind(),
            visibility = descriptor.visibility.toAstVisibility(),
            multiPlatformModality = multiPlatformModalityOf(
                descriptor.isActual,
                descriptor.isExpect
            ),
            modality = descriptor.modality.toAstModality(),
            isCompanion = descriptor.isCompanionObject,
            isFun = descriptor.isFun,
            isData = descriptor.isData,
            isExternal = descriptor.isExternal,
            isInner = descriptor.isInner
        ).apply {
        }
    }

    private fun DeclarationDescriptor.toAst(): AstDeclaration = when (this) {
        is ClassDescriptor -> this.toAst()
        else -> error("Unexpected declaration $this")
    }

}
