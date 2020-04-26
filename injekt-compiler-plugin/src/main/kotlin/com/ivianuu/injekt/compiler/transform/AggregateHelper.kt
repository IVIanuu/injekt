package com.ivianuu.injekt.compiler.transform

import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.vfs.local.CoreLocalFileSystem
import org.jetbrains.kotlin.com.intellij.openapi.vfs.local.CoreLocalVirtualFile
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.buildClass
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.util.constructors
import org.jetbrains.kotlin.ir.util.defaultType
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File

fun IrModuleFragment.addEmptyClass(
    context: IrPluginContext,
    project: Project,
    name: Name,
    packageFqName: FqName
) {
    addClass(
        context.psiSourceManager.cast(),
        project,
        buildClass {
            this.name = name
        }.apply clazz@{
            createImplicitParameterDeclarationWithWrappedDescriptor()

            addConstructor {
                returnType = defaultType
                isPrimary = true
            }.apply {
                body = DeclarationIrBuilder(context, symbol).irBlockBody {
                    +IrDelegatingConstructorCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        context.irBuiltIns.unitType,
                        context.irBuiltIns.anyClass
                            .constructors.single()
                    )
                    +IrInstanceInitializerCallImpl(
                        UNDEFINED_OFFSET,
                        UNDEFINED_OFFSET,
                        this@clazz.symbol,
                        context.irBuiltIns.unitType
                    )
                }
            }
        },
        packageFqName
    )
}

fun IrModuleFragment.addClass(
    psiSourceManager: PsiSourceManager,
    project: Project,
    irClass: IrClass,
    packageFqName: FqName
) {
    files += fileForClass(psiSourceManager, project, irClass, packageFqName)
}

private fun IrModuleFragment.fileForClass(
    psiSourceManager: PsiSourceManager,
    project: Project,
    irClass: IrClass,
    packageFqName: FqName
): IrFile {
    val sourceFile = File("${irClass.name}.kt")

    val virtualFile = CoreLocalVirtualFile(CoreLocalFileSystem(), sourceFile)

    val ktFile = KtFile(
        SingleRootFileViewProvider(
            PsiManager.getInstance(project),
            virtualFile
        ),
        false
    )

    val memberScope =
        MutableClassMemberScope()

    val packageFragmentDescriptor =
        object : PackageFragmentDescriptorImpl(
            descriptor,
            packageFqName
        ) {
            override fun getMemberScope(): MemberScope = memberScope
        }

    val fileEntry = psiSourceManager.getOrCreateFileEntry(ktFile)
    val file = IrFileImpl(
        fileEntry,
        packageFragmentDescriptor
    )
    psiSourceManager.putFileEntry(file, fileEntry)

    memberScope.classDescriptors += irClass.descriptor

    file.addChild(irClass)

    return file
}

private class MutableClassMemberScope : MemberScopeImpl() {
    val classDescriptors = mutableListOf<ClassDescriptor>()

    override fun getContributedClassifier(
        name: Name,
        location: LookupLocation
    ): ClassifierDescriptor? {
        return classDescriptors.firstOrNull { it.name == name }
    }

    override fun getClassifierNames(): Set<Name>? {
        return classDescriptors.mapTo(mutableSetOf()) { it.name }
    }

    override fun printScopeStructure(p: Printer) {
    }
}