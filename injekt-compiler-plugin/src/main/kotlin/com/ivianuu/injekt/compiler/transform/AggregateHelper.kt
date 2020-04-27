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
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.ClassifierDescriptor
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.incremental.components.LookupLocation
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrDeclarationOrigin
import org.jetbrains.kotlin.ir.declarations.IrFile
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.resolve.scopes.MemberScopeImpl
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.utils.Printer
import org.jetbrains.kotlin.utils.addToStdlib.cast
import java.io.File

fun IrModuleFragment.addEmptyClass(
    context: IrPluginContext,
    project: Project,
    name: Name,
    packageFqName: FqName
) {
    val classDescriptor = ClassDescriptorImpl(
        context.moduleDescriptor.getPackage(packageFqName),
        name,
        Modality.FINAL,
        ClassKind.CLASS,
        emptyList(),
        SourceElement.NO_SOURCE,
        false,
        LockBasedStorageManager.NO_LOCKS
    ).apply {
        initialize(
            MemberScope.Empty,
            emptySet(),
            null
        )
    }

    addClass(
        context.psiSourceManager.cast(),
        project,
        IrClassImpl(
            UNDEFINED_OFFSET,
            UNDEFINED_OFFSET,
            IrDeclarationOrigin.DEFINED,
            IrClassSymbolImpl(classDescriptor)
        ).apply clazz@{
            createImplicitParameterDeclarationWithWrappedDescriptor()

            metadata = MetadataSource.Class(classDescriptor)

            addConstructor {
                isPrimary = true
            }.apply {
                body = DeclarationIrBuilder(context, symbol).irBlockBody {
                    +IrDelegatingConstructorCallImpl(
                        startOffset, endOffset,
                        context.irBuiltIns.unitType,
                        context.symbolTable.referenceConstructor(
                            context.builtIns.any.unsubstitutedPrimaryConstructor!!
                        )
                    )
                    +IrInstanceInitializerCallImpl(
                        startOffset,
                        endOffset,
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
    irClass.parent = file

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