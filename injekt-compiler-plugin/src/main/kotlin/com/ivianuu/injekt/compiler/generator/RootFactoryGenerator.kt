package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektAttributes
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.frontend.hasAnnotation
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.ModuleDescriptor
import org.jetbrains.kotlin.descriptors.findClassAcrossModuleDependencies
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.name.ClassId
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.name.Name
import org.jetbrains.kotlin.psi.KtDeclaration
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.psi.namedDeclarationRecursiveVisitor
import org.jetbrains.kotlin.resolve.constants.StringValue
import java.io.File

@Given(KtGenerationContext::class)
class RootFactoryGenerator : KtGenerator {
    private val fileManager = given<KtFileManager>()

    private val thisCompilationRootFactories = mutableSetOf<FqName>()

    fun addRootFactory(fqName: FqName) {
        thisCompilationRootFactories += fqName
    }

    override fun generate(files: List<KtFile>) {
        var initTrigger: KtDeclaration? = null
        files.forEach { file ->
            file.accept(
                namedDeclarationRecursiveVisitor {
                    val descriptor = it.descriptor<DeclarationDescriptor>()
                    if (descriptor.hasAnnotation(InjektFqNames.InitializeInjekt)) {
                        initTrigger = initTrigger ?: it
                    }
                }
            )
        }

        if (initTrigger == null) return

        given<ModuleDescriptor>().getPackage(InjektFqNames.IndexPackage)
            .memberScope
            .let { memberScope ->
                (memberScope.getClassifierNames() ?: emptySet<Name>())
                    .map {
                        memberScope.getContributedClassifier(
                            it,
                            NoLookupLocation.FROM_BACKEND
                        )
                    }
            }
            .filterIsInstance<ClassDescriptor>()
            .mapNotNull { index ->
                index.annotations.findAnnotation(InjektFqNames.Index)
                    ?.takeIf { annotation ->
                        annotation.allValueArguments[Name.identifier("type")]
                            .let { it as StringValue }
                            .value == "class"
                    }
                    ?.let { annotation ->
                        val fqName =
                            annotation.allValueArguments.getValue(Name.identifier("fqName"))
                                .let { it as StringValue }
                                .value
                                .let { FqName(it) }
                        if (!isInjektCompiler &&
                            fqName.asString().startsWith("com.ivianuu.injekt.compiler")
                        ) return@mapNotNull null
                        given<ModuleDescriptor>()
                            .findClassAcrossModuleDependencies(ClassId.topLevel(fqName))
                    }
            }
            .mapNotNull { index ->
                index.annotations.findAnnotation(InjektFqNames.RootContextFactory)
                    ?.allValueArguments
                    ?.values
                    ?.single()
                    ?.let { it as StringValue }
                    ?.value
                    ?.let { FqName(it) }
            }
            .let { (thisCompilationRootFactories + it).toSet() }
            .forEach { generateRootContext(it, initTrigger!!) }
    }

    private fun generateRootContext(
        fqName: FqName,
        initTrigger: KtDeclaration
    ) {
        val code = buildCodeString {
            emitLine("// injekt-generated")
            emitLine("package ${fqName.parent()}")
            emitLine("object ${fqName.shortName()}")
        }

        fileManager.generateFile(
            packageFqName = fqName.parent(),
            fileName = "${fqName.shortName()}.kt",
            code = code,
            originatingDeclarations = listOf<DeclarationDescriptor>(initTrigger.descriptor()),
            originatingFiles = listOf(
                File(initTrigger.containingKtFile.virtualFilePath)
            )
        )
        given<InjektAttributes>()[InjektAttributes.IsRootFactory(fqName)] = true
    }
}
