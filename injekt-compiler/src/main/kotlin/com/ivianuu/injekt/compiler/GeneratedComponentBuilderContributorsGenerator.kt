package com.ivianuu.injekt.compiler

import org.jetbrains.kotlin.backend.common.deepCopyWithVariables
import org.jetbrains.kotlin.backend.common.extensions.IrPluginContext
import org.jetbrains.kotlin.backend.common.ir.addChild
import org.jetbrains.kotlin.backend.common.ir.createImplicitParameterDeclarationWithWrappedDescriptor
import org.jetbrains.kotlin.backend.common.lower.DeclarationIrBuilder
import org.jetbrains.kotlin.com.intellij.openapi.project.Project
import org.jetbrains.kotlin.com.intellij.openapi.vfs.local.CoreLocalFileSystem
import org.jetbrains.kotlin.com.intellij.openapi.vfs.local.CoreLocalVirtualFile
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.psi.SingleRootFileViewProvider
import org.jetbrains.kotlin.descriptors.ClassKind
import org.jetbrains.kotlin.descriptors.Modality
import org.jetbrains.kotlin.descriptors.SourceElement
import org.jetbrains.kotlin.descriptors.Visibilities
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.descriptors.impl.ClassDescriptorImpl
import org.jetbrains.kotlin.descriptors.impl.PackageFragmentDescriptorImpl
import org.jetbrains.kotlin.ir.UNDEFINED_OFFSET
import org.jetbrains.kotlin.ir.builders.declarations.addConstructor
import org.jetbrains.kotlin.ir.builders.declarations.addGetter
import org.jetbrains.kotlin.ir.builders.declarations.addProperty
import org.jetbrains.kotlin.ir.builders.irBlockBody
import org.jetbrains.kotlin.ir.builders.irCall
import org.jetbrains.kotlin.ir.builders.irGetObject
import org.jetbrains.kotlin.ir.builders.irReturn
import org.jetbrains.kotlin.ir.declarations.IrClass
import org.jetbrains.kotlin.ir.declarations.IrModuleFragment
import org.jetbrains.kotlin.ir.declarations.MetadataSource
import org.jetbrains.kotlin.ir.declarations.impl.IrClassImpl
import org.jetbrains.kotlin.ir.declarations.impl.IrFileImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrDelegatingConstructorCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrInstanceInitializerCallImpl
import org.jetbrains.kotlin.ir.expressions.impl.IrVarargImpl
import org.jetbrains.kotlin.ir.symbols.impl.IrClassSymbolImpl
import org.jetbrains.kotlin.ir.types.typeWith
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi2ir.PsiSourceManager
import org.jetbrains.kotlin.psi2ir.findFirstFunction
import org.jetbrains.kotlin.resolve.calls.components.isVararg
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import org.jetbrains.kotlin.storage.LockBasedStorageManager
import org.jetbrains.kotlin.types.KotlinTypeFactory
import org.jetbrains.kotlin.types.typeUtil.asTypeProjection
import java.io.File

class GeneratedComponentBuilderContributorsGenerator(
    private val moduleFragment: IrModuleFragment,
    private val pluginContext: IrPluginContext,
    private val project: Project,
    private val thisContributors: List<IrClass>
) {

    private val aggregatePackage =
        pluginContext.moduleDescriptor.getPackage(FqName("com.ivianuu.injekt.aggregate"))
    private val componentBuilderContributor = getClass(InjektClassNames.ComponentBuilderContributor)

    private fun getClass(fqName: FqName) =
        pluginContext.moduleDescriptor.findClassAcrossModuleDependencies(ClassId.topLevel(fqName))!!

    fun generate() {
        val psiSourceManager = pluginContext.psiSourceManager as PsiSourceManager
        val className =
            Name.identifier("GeneratedComponentContributors")

        val sourceFile = File("$className.kt")

        val virtualFile = CoreLocalVirtualFile(CoreLocalFileSystem(), sourceFile)

        val ktFile = KtFile(
            SingleRootFileViewProvider(
                PsiManager.getInstance(project),
                virtualFile
            ),
            false
        )

        val memberScope = MutableMemberScope()

        val packageFragmentDescriptor =
            object : PackageFragmentDescriptorImpl(
                moduleFragment.descriptor,
                FqName("com.ivianuu.injekt")
            ) {
                override fun getMemberScope(): MemberScope = memberScope
            }

        val fileEntry = psiSourceManager.getOrCreateFileEntry(ktFile)
        val file = IrFileImpl(
            fileEntry,
            packageFragmentDescriptor
        )
        psiSourceManager.putFileEntry(file, fileEntry)

        moduleFragment.files += file

        val classDescriptor = ClassDescriptorImpl(
            packageFragmentDescriptor,
            className,
            Modality.FINAL,
            ClassKind.OBJECT,
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

        memberScope.classDescriptors += classDescriptor

        file.addChild(
            IrClassImpl(
                UNDEFINED_OFFSET,
                UNDEFINED_OFFSET,
                InjektOrigin,
                IrClassSymbolImpl(classDescriptor)
            ).apply clazz@{
                createImplicitParameterDeclarationWithWrappedDescriptor()

                metadata = MetadataSource.Class(classDescriptor)

                addConstructor {
                    origin = InjektOrigin
                    isPrimary = true
                    visibility = Visibilities.PUBLIC
                }.apply {
                    body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                        +IrDelegatingConstructorCallImpl(
                            startOffset, endOffset,
                            context.irBuiltIns.unitType,
                            pluginContext.symbolTable.referenceConstructor(
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

                addProperty {
                    name = Name.identifier("contributors")
                }.apply {
                    addGetter {
                        returnType = KotlinTypeFactory.simpleType(
                            pluginContext.builtIns.list.defaultType,
                            arguments = listOf(pluginContext.builtIns.anyType.asTypeProjection())
                        ).let { pluginContext.typeTranslator.translateType(it) }
                    }.apply {
                        dispatchReceiverParameter = thisReceiver!!.deepCopyWithVariables()

                        body = DeclarationIrBuilder(pluginContext, symbol).irBlockBody {
                            val allContributors = aggregatePackage
                                .memberScope
                                .getClassifierNames()!!
                                .map { FqName(it.asString().replace("_", ".")) }
                                .map { fqName ->
                                    try {
                                        getClass(fqName)
                                    } catch (e: Exception) {
                                        null
                                    }
                                        ?: error("Not found for $fqName this desc ${thisContributors.map { it.descriptor.fqNameSafe }}")
                                } + thisContributors.map { it.descriptor }

                            val listOf =
                                pluginContext.moduleDescriptor.getPackage(FqName("kotlin.collections"))
                                    .memberScope
                                    .findFirstFunction("listOf") {
                                        it.valueParameters.singleOrNull()?.isVararg ?: false
                                    }
                            +irReturn(
                                DeclarationIrBuilder(context, symbol).irCall(
                                    pluginContext.symbolTable.referenceSimpleFunction(listOf),
                                    type = KotlinTypeFactory.simpleType(
                                        pluginContext.builtIns.list.defaultType,
                                        arguments = listOf(
                                            pluginContext.builtIns.anyType.asTypeProjection()
                                        )
                                    ).let { pluginContext.typeTranslator.translateType(it) }
                                ).apply {
                                    putTypeArgument(0, pluginContext.irBuiltIns.anyType)
                                    putValueArgument(
                                        0,
                                        IrVarargImpl(
                                            UNDEFINED_OFFSET,
                                            UNDEFINED_OFFSET,
                                            pluginContext.irBuiltIns.arrayClass
                                                .typeWith(getTypeArgument(0)!!),
                                            getTypeArgument(0)!!,
                                            allContributors.map { contributor ->
                                                irGetObject(
                                                    pluginContext.symbolTable
                                                        .referenceClass(contributor)
                                                )
                                            }
                                        )
                                    )
                                }
                            )
                        }
                    }
                }
            }
        )
    }

}