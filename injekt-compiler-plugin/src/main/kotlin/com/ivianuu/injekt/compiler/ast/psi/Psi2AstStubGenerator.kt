package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.ast.tree.AstElement
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstClass
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstDeclarationContainer
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstExternalPackageFragment
import com.ivianuu.injekt.compiler.ast.tree.declaration.AstSimpleFunction
import com.ivianuu.injekt.compiler.ast.tree.declaration.addChild
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.PackageFragmentDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor

class Psi2AstStubGenerator(
    private val storage: Psi2AstStorage
) {

    lateinit var translator: Psi2AstTranslator

    fun get(descriptor: DeclarationDescriptor): AstElement {
        return when (descriptor) {
            is PackageFragmentDescriptor -> descriptor.toAstPackageFragment()
            is ClassDescriptor -> descriptor.toAstClassStub()
            is SimpleFunctionDescriptor -> descriptor.toAstSimpleFunctionStub()
            else -> error("Unexpected descriptor $descriptor ${descriptor.javaClass}")
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
        ).apply {
            storage.classes[this@toAstClassStub] = this
            (get(containingDeclaration) as AstDeclarationContainer).addChild(this)
            with(translator) {
                annotations += this@toAstClassStub.annotations.toAstAnnotations()
                typeParameters += declaredTypeParameters.toAstTypeParameters()
                    .onEach { it.parent = this@apply }
                // todo children
            }
        }
    }

    private fun SimpleFunctionDescriptor.toAstSimpleFunctionStub() =
        storage.simpleFunctions.getOrPut(this) {
            println("ohps $this")
            get(containingDeclaration)
            AstSimpleFunction(
                name = name,
                visibility = visibility.toAstVisibility(),
                expectActual = expectActualOf(isActual, isExpect),
                modality = modality.toAstModality(),
                returnType = with(translator) {
                    returnType!!.toAstType()
                },
                isInline = isInline,
                isExternal = isExternal,
                isInfix = isInfix,
                isOperator = isOperator,
                isTailrec = isTailrec,
                isSuspend = isSuspend
            ).apply {
                storage.simpleFunctions[this@toAstSimpleFunctionStub] = this
                with(translator) {
                    annotations += this@toAstSimpleFunctionStub.annotations.toAstAnnotations()
                    typeParameters += this@toAstSimpleFunctionStub.typeParameters.toAstTypeParameters()
                        .onEach { it.parent = this@apply }
                    valueParameters += this@toAstSimpleFunctionStub.valueParameters.toAstValueParameters()
                        .onEach { it.parent = this@apply }
                    overriddenFunctions += overriddenDescriptors
                        .map { (it as SimpleFunctionDescriptor).toAstSimpleFunction() }
                }
            }
        }

    private fun PackageFragmentDescriptor.toAstPackageFragment() =
        storage.externalPackageFragments.getOrPut(this) {
            AstExternalPackageFragment(fqName)
        }

}
