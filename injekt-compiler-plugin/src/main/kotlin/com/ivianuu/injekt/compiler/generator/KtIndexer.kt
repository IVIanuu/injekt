package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.InjektFqNames
import com.ivianuu.injekt.given
import org.jetbrains.kotlin.descriptors.ClassDescriptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.descriptors.FunctionDescriptor
import org.jetbrains.kotlin.descriptors.PropertyDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.resolve.descriptorUtil.fqNameSafe

@Given(ApplicationContext::class)
class KtIndexer {

    private val fileManager = given<KtFileManager>()

    fun index(
        fqName: FqName,
        type: String,
        indexIsDeclaration: Boolean = false,
        annotations: List<Pair<FqName, String>> = emptyList()
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
                listOf(fqName)
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
                listOf(declaration)
            )
        }
    }

}
