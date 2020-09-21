package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.CacheDir
import com.ivianuu.injekt.compiler.IncrementalFileCache
import com.ivianuu.injekt.compiler.SrcDir
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runReader
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

@Given(AnalysisContext::class)
class KtFileManager {

    private val fileCache = IncrementalFileCache(
        // todo temporary workaround because requesting 2 givens with the same expanded type is not working
        cacheFile = given<ApplicationContext>().runReader { given<CacheDir>() }
            .resolve("file-cache")
    )

    fun onPreCompile(files: List<KtFile>): List<KtFile> {
        files.forEach { fileCache.deleteDependents(File(it.virtualFilePath)) }
        return files.filterNot { it.text.startsWith("// injekt-generated") }
    }

    fun onPostCompile() {
        fileCache.flush()
    }

    fun exists(packageFqName: FqName, fileName: String): Boolean =
        given<SrcDir>().resolve(packageFqName.asString().replace(".", "/"))
            .resolve(fileName)
            .exists()

    fun generateFile(
        packageFqName: FqName,
        fileName: String,
        code: String,
        originatingDeclarations: List<DeclarationDescriptor>,
        originatingFiles: List<File>
    ): File {
        return given<SrcDir>()
            .resolve(packageFqName.asString().replace(".", "/"))
            .also { it.mkdirs() }
            .resolve(fileName)
            .also { it.createNewFile() }
            .also { it.writeText(code) }
            .also { result ->
                originatingDeclarations.forEach {
                    recordLookup(result.absolutePath, it)
                }
                originatingFiles.forEach {
                    fileCache.recordDependency(result, it)
                }
            }
    }

    @JvmName("generateFilesWithFqNames")
    fun generateFile(
        packageFqName: FqName,
        fileName: String,
        code: String,
        originatingDeclarations: List<FqName>,
        originatingFiles: List<File>
    ): File {
        return given<SrcDir>()
            .resolve(packageFqName.asString().replace(".", "/"))
            .also { it.mkdirs() }
            .resolve(fileName)
            .also { it.createNewFile() }
            .also { it.writeText(code) }
            .also { result ->
                originatingDeclarations.forEach {
                    recordLookup(result.absolutePath, it)
                    originatingFiles.forEach {
                        fileCache.recordDependency(result, it)
                    }
                }
            }
    }

}
