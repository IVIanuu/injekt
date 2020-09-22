package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.compiler.irtransform.asNameId
import com.ivianuu.injekt.compiler.unsafeLazy
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.incremental.components.NoLookupLocation
import org.jetbrains.kotlin.js.resolve.diagnostics.findPsi
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.constants.BooleanValue
import org.jetbrains.kotlin.resolve.constants.StringValue
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe
import org.jetbrains.kotlin.resolve.scopes.MemberScope
import java.io.File

@Given(AnalysisContext::class)
class Indexer {

    private val fileManager = given<KtFileManager>()

    val classIndices by unsafeLazy {
        allExternalIndices
            .filter { it.type == "class" }
            .map { index ->
                if (index.indexIsDeclaration) index.indexClass
                else {
                    val memberScope = getMemberScope(index.fqName.parent())!!
                    memberScope.getContributedClassifier(
                        index.fqName.shortName(), NoLookupLocation.FROM_BACKEND
                    ) as ClassDescriptor
                }
            }
    }

    val functionIndices by unsafeLazy {
        allExternalIndices
            .filter { it.type == "function" }
            .flatMap { index ->
                val memberScope = getMemberScope(index.fqName.parent())!!
                memberScope.getContributedFunctions(
                    index.fqName.shortName(),
                    NoLookupLocation.FROM_BACKEND
                )
            }
            .distinct()
    }

    val propertyIndices by unsafeLazy {
        allExternalIndices
            .filter { it.type == "property" }
            .flatMap { index ->
                val memberScope = getMemberScope(index.fqName.parent())!!
                memberScope.getContributedVariables(
                    index.fqName.shortName(),
                    NoLookupLocation.FROM_BACKEND
                )
            }
    }

    private val allExternalIndices by unsafeLazy {
        val memberScope = moduleDescriptor.getPackage(InjektFqNames.IndexPackage).memberScope
        (memberScope.getClassifierNames() ?: emptySet())
            .mapNotNull {
                memberScope.getContributedClassifier(
                    it,
                    NoLookupLocation.FROM_BACKEND
                )
            }
            .filterIsInstance<ClassDescriptor>()
            .map { descriptor ->
                val indexAnnotation = descriptor.annotations.findAnnotation(InjektFqNames.Index)!!
                Index(
                    FqName(
                        indexAnnotation.allValueArguments["fqName".asNameId()]
                            .let { it as StringValue }
                            .value
                    ),
                    descriptor,
                    indexAnnotation.allValueArguments["type".asNameId()]
                        .let { it as StringValue }
                        .value,
                    indexAnnotation.allValueArguments["indexIsDeclaration".asNameId()]
                        .let { it as BooleanValue }
                        .value
                )
            }
    }

    private val memberScopesByFqName = mutableMapOf<FqName, MemberScope?>()
    fun getMemberScope(fqName: FqName): MemberScope? = memberScopesByFqName.getOrPut(fqName) {
        val pkg = moduleDescriptor.getPackage(fqName)

        if (fqName.isRoot || pkg.fragments.isNotEmpty()) return pkg.memberScope

        val parentMemberScope = getMemberScope(fqName.parent()) ?: return null

        val classDescriptor =
            parentMemberScope.getContributedClassifier(
                fqName.shortName(),
                NoLookupLocation.FROM_BACKEND
            ) as? ClassDescriptor ?: return null

        return classDescriptor.unsubstitutedMemberScope
    }

    private data class Index(
        val fqName: FqName,
        val indexClass: ClassDescriptor,
        val type: String,
        val indexIsDeclaration: Boolean
    )

    fun index(
        fqName: FqName,
        type: String,
        indexIsDeclaration: Boolean = false,
        annotations: List<Pair<FqName, String>> = emptyList(),
        originatingFiles: List<File>
    ) {
        val indexName = "${
            fqName.pathSegments()
                .joinToString("_")
        }Index"
        val fileName = "$indexName.kt"
        if (!fileManager.exists(InjektFqNames.IndexPackage, fileName)) {
            fileManager.generateFile(
                InjektFqNames.IndexPackage, fileName,
                buildCodeString {
                    emitLine("// injekt-generated")
                    emitLine("package ${InjektFqNames.IndexPackage}")
                    emitLine("import com.ivianuu.injekt.internal.Index")
                    annotations.forEach { emitLine("import ${it.first}") }
                    emitLine("@Index(type = \"$type\", fqName = \"$fqName\", indexIsDeclaration = $indexIsDeclaration)")
                    annotations.forEach { emitLine(it.second) }
                    emitLine("internal object $indexName")
                },
                originatingFiles
            )
        }
    }

    fun index(declaration: DeclarationDescriptor) {
        val indexName = "${
            declaration.fqNameSafe.pathSegments()
                .joinToString("_")
        }Index"
        val fileName = "$indexName.kt"
        if (!fileManager.exists(InjektFqNames.IndexPackage, fileName)) {
            val type = when (declaration) {
                is ClassDescriptor -> "class"
                is FunctionDescriptor -> "function"
                is PropertyDescriptor -> "property"
                else -> error("Unsupported descriptor $declaration")
            }
            fileManager.generateFile(
                InjektFqNames.IndexPackage, fileName,
                buildCodeString {
                    emitLine("// injekt-generated")
                    emitLine("package ${InjektFqNames.IndexPackage}")
                    emitLine("import com.ivianuu.injekt.internal.Index")
                    emitLine("@Index(type = \"$type\", fqName = \"${declaration.fqNameSafe}\", indexIsDeclaration = false)")
                    emitLine("internal object $indexName")
                },
                listOf(File((declaration.findPsi()!!.containingFile as KtFile).virtualFilePath))
            )
        }
    }

}
