package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.IncrementalFileCache
import com.ivianuu.injekt.compiler.SrcDir
import com.ivianuu.injekt.compiler.log
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

@Given(GenerationComponent::class)
class FileManager(
    private val srcDir: SrcDir,
    private val log: log,
    fileCacheFactory: (File) -> IncrementalFileCache
) {

    private val fileCache = fileCacheFactory(srcDir.resolve("cache-dir"))

    val newFiles = mutableListOf<File>()

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

    fun generateFile(
        packageFqName: FqName,
        fileName: String,
        code: String,
        originatingFile: File
    ): File {
        val newFile = srcDir
            .resolve(packageFqName.asString().replace(".", "/"))
            .also { it.mkdirs() }
            .resolve(fileName)
            .also { newFiles += it }

        fileCache.recordDependency(newFile, originatingFile)

        log { "generated file $packageFqName.$fileName $code" }

        return newFile
            .also { it.createNewFile() }
            .also { it.writeText(code) }
    }
}
