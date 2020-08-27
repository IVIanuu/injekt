package com.ivianuu.injekt.compiler.ast.psi

import com.ivianuu.injekt.compiler.asNameId
import com.ivianuu.injekt.compiler.ast.AstCall
import com.ivianuu.injekt.compiler.ast.AstCallableId
import com.ivianuu.injekt.compiler.ast.AstClass
import com.ivianuu.injekt.compiler.ast.AstConstructor
import com.ivianuu.injekt.compiler.ast.AstDeclaration
import com.ivianuu.injekt.compiler.ast.AstFile
import com.ivianuu.injekt.compiler.ast.AstSimpleFunction
import com.ivianuu.injekt.compiler.ast.AstTypeParameter
import com.ivianuu.injekt.compiler.ast.AstValueParameter
import com.ivianuu.injekt.compiler.ast.string.Ast2StringTranslator
import org.jetbrains.kotlin.backend.common.serialization.findPackage
import org.jetbrains.kotlin.cli.jvm.compiler.EnvironmentConfigFiles
import org.jetbrains.kotlin.cli.jvm.compiler.KotlinCoreEnvironment
import org.jetbrains.kotlin.com.intellij.openapi.util.Disposer
import org.jetbrains.kotlin.com.intellij.psi.PsiManager
import org.jetbrains.kotlin.com.intellij.testFramework.LightVirtualFile
import org.jetbrains.kotlin.config.CompilerConfiguration
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.ConstructorDescriptor
import org.jetbrains.kotlin.descriptors.SimpleFunctionDescriptor
import org.jetbrains.kotlin.descriptors.TypeParameterDescriptor
import org.jetbrains.kotlin.descriptors.ValueParameterDescriptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.psi.KtClassOrObject
import org.jetbrains.kotlin.psi.KtConstructor
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.KtFunction
import org.jetbrains.kotlin.resolve.BindingContext
import org.jetbrains.kotlin.resolve.BindingTrace
import org.jetbrains.kotlin.resolve.descriptorUtil.classId
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

class Psi2AstGenerator(
    private val bindingTrace: BindingTrace
) {

    private val classes =
        mutableMapOf<ClassDescriptor, AstClass>()
    private val simpleFunctions =
        mutableMapOf<SimpleFunctionDescriptor, AstSimpleFunction>()
    private val constructors =
        mutableMapOf<ConstructorDescriptor, AstConstructor>()

    fun generateFile(file: KtFile): AstFile {
        return AstFile(
            packageFqName = file.packageFqName,
            name = file.name.asNameId(),
            annotations = generateAnnotations()
        ).apply {
            declarations += generateDeclarations(file.declarations)
                .onEach {
                    it.parent = this
                }
        }
    }

    private fun generateDeclarations(declarations: List<KtDeclaration>): MutableList<AstDeclaration> =
        mutableListOf<AstDeclaration>().apply {
            this += declarations
                .filter {
                    // todo
                    it is KtClassOrObject || it is KtFunction
                }
                .map { generateDeclaration(it) }
        }

    private fun generateDeclaration(declaration: KtDeclaration): AstDeclaration =
        when (declaration) {
            is KtClassOrObject -> getOrCreateClass(
                bindingTrace.get(
                    BindingContext.DECLARATION_TO_DESCRIPTOR,
                    declaration
                ) as ClassDescriptor
            )
            is KtFunction -> getOrCreateFunction(
                bindingTrace.get(
                    BindingContext.DECLARATION_TO_DESCRIPTOR,
                    declaration
                ) as SimpleFunctionDescriptor
            )
            is KtConstructor<*> -> getOrCreateConstructor(
                bindingTrace.get(
                    BindingContext.DECLARATION_TO_DESCRIPTOR,
                    declaration
                ) as ConstructorDescriptor
            )
            else -> error("Unexpected declaration $declaration")
        }

    fun getOrCreateClass(descriptor: ClassDescriptor): AstClass {
        val declaration = descriptor.findPsi() as KtClassOrObject
        return classes.getOrPut(descriptor) {
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
                isInner = descriptor.isInner,
                isExternal = descriptor.isExternal
            ).apply {
                annotations += generateAnnotations()
                typeParameters += generateTypeParameters(descriptor.declaredTypeParameters)
                declarations += generateDeclarations(declaration.declarations)
                    .onEach { it.parent = this }
            }
        }
    }

    fun getOrCreateFunction(descriptor: SimpleFunctionDescriptor): AstSimpleFunction {
        return simpleFunctions.getOrPut(descriptor) {
            AstSimpleFunction(
                callableId = AstCallableId(
                    descriptor.findPackage().fqName,
                    descriptor.dispatchReceiverParameter?.value?.type?.constructor?.declarationDescriptor
                        ?.let { it as ClassDescriptor }?.fqNameSafe,
                    descriptor.name
                ),
                visibility = descriptor.visibility.toAstVisibility(),
                multiPlatformModality = multiPlatformModalityOf(
                    descriptor.isActual,
                    descriptor.isExpect
                ),
                modality = descriptor.modality.toAstModality(),
                isInfix = descriptor.isInfix,
                isOperator = descriptor.isOperator,
                isTailrec = descriptor.isTailrec,
                isSuspend = descriptor.isSuspend
            ).apply {
                annotations += generateAnnotations()
                typeParameters += generateTypeParameters(descriptor.typeParameters)
                valueParameters += generateValueParameters(descriptor.valueParameters)
                overriddenFunctions += descriptor.overriddenDescriptors
                    .map { getOrCreateFunction(it as SimpleFunctionDescriptor) }
            }
        }
    }

    fun getOrCreateConstructor(descriptor: ConstructorDescriptor): AstConstructor {
        return constructors.getOrPut(descriptor) {
            AstConstructor(
                callableId = AstCallableId(
                    descriptor.findPackage().fqName,
                    null,
                    descriptor.name
                ),
                visibility = descriptor.visibility.toAstVisibility(),
                multiPlatformModality = multiPlatformModalityOf(
                    descriptor.isActual,
                    descriptor.isExpect
                ),
                isPrimary = descriptor.isPrimary
            ).apply {
                annotations += generateAnnotations()
                valueParameters += generateValueParameters(descriptor.valueParameters)
            }
        }
    }

    // todo
    private fun generateAnnotations() = mutableListOf<AstCall>()

    /*private fun generateModifiers(modifiers: KtModifierList?) =
        modifiers?.node?.children().orEmpty().mapNotNull { node ->
            when (node) {
                is KtAnnotationEntry -> null
                is KtAnnotation -> null
                is PsiWhiteSpace -> null
                else -> AstModifier.values().singleOrNull { it.name.toLowerCase() == node.text }
            }
        }.toMutableList()*/

    private fun generateValueParameters(valueParameters: List<ValueParameterDescriptor>) =
        mutableListOf<AstValueParameter>()

    // todo
    private fun generateTypeParameters(typeParameters: List<TypeParameterDescriptor>) =
        mutableListOf<AstTypeParameter>()

}

object Ast2PsiTranslator {

    private val proj by lazy {
        KotlinCoreEnvironment.createForProduction(
            Disposer.newDisposable(),
            CompilerConfiguration(),
            EnvironmentConfigFiles.JVM_CONFIG_FILES
        ).project
    }

    fun generateFile(element: AstFile): KtFile =
        PsiManager.getInstance(proj).findFile(
            LightVirtualFile(
                "tmp.kt", KotlinFileType.INSTANCE,
                Ast2StringTranslator.generate(element)
            )
        ) as KtFile

}
