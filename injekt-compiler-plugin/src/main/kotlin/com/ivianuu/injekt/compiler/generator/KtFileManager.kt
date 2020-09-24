package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.CacheDir
import com.ivianuu.injekt.compiler.IncrementalFileCache
import com.ivianuu.injekt.compiler.SrcDir
import com.ivianuu.injekt.compiler.log
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runReader
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

@Given(AnalysisContext::class)
class KtFileManager {

    private val fileCache = given<IncrementalFileCache>(
        // todo temporary workaround because requesting 2 givens with the same expanded type is not working
        given<ApplicationContext>().runReader { given<CacheDir>() }
            .resolve("file-cache")
    )

    val newFiles = mutableSetOf<File>()

    fun onPreCompile(files: List<KtFile>): List<KtFile> {
        log { "pre compile $files" }
        fileCache.deleteDependentsOfDeletedFiles()
        files.forEach { fileCache.deleteDependents(File(it.virtualFilePath)) }
        log { "pre compile deleted files ${fileCache.deletedFiles}" }
        return files.filterNot { it.virtualFilePath in fileCache.deletedFiles }
    }

    fun onPostCompile(files: List<KtFile>) {
        log { "post compile $files" }
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
        originatingFiles: List<File>
    ): File {
        val newFile = given<SrcDir>()
            .resolve(packageFqName.asString().replace(".", "/"))
            .also { it.mkdirs() }
            .resolve(fileName)

        check(newFile !in newFiles) {
            "Already generated file $newFile"
        }

        log { "generated file $packageFqName.$fileName $code" }

        return newFile
            .also { it.createNewFile() }
            .also { it.writeText(code) }
            .also { result ->
                originatingFiles.forEach {
                    fileCache.recordDependency(result, it)
                }
                newFiles += result
            }
    }

}
