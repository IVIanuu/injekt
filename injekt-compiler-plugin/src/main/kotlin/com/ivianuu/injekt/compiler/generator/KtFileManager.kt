package com.ivianuu.injekt.compiler.generator

import com.ivianuu.injekt.ApplicationContext
import com.ivianuu.injekt.Given
import com.ivianuu.injekt.compiler.CacheDir
import com.ivianuu.injekt.compiler.IncrementalFileCache
import com.ivianuu.injekt.compiler.IrFileStore
import com.ivianuu.injekt.compiler.LookupManager
import com.ivianuu.injekt.compiler.SrcDir
import com.ivianuu.injekt.given
import com.ivianuu.injekt.runReader
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.name.FqName
import org.jetbrains.kotlin.psi.KtFile
import java.io.File

@Given(ApplicationContext::class)
class KtFileManager {

    private val fileStore = given<IrFileStore>()

    private val fileCache = IncrementalFileCache(
        // todo temporary workaround because requesting 2 givens with the same expanded type would not distinct
        cacheFile = given<ApplicationContext>().runReader { given<CacheDir>() }
            .resolve("file-cache")
    )
    private val lookupManager = given<LookupManager>()

    fun onPreCompile(files: List<KtFile>): List<KtFile> {
        fileStore.clear()
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
        originatingDeclarations: List<DeclarationDescriptor>
    ): File {
        return given<SrcDir>()
            .resolve(packageFqName.asString().replace(".", "/"))
            .also { it.mkdirs() }
            .resolve(fileName)
            .also { it.createNewFile() }
            .also { it.writeText(code) }
            .also { result ->
                originatingDeclarations.forEach {
                    lookupManager.recordLookup(
                        result.absolutePath,
                        it
                    )
                }
            }
    }

    @JvmName("generateFilesWithFqNames")
    fun generateFile(
        packageFqName: FqName,
        fileName: String,
        code: String,
        originatingDeclarations: List<FqName>
    ): File {
        return given<SrcDir>()
            .resolve(packageFqName.asString().replace(".", "/"))
            .also { it.mkdirs() }
            .resolve(fileName)
            .also { it.createNewFile() }
            .also { it.writeText(code) }
            .also { result ->
                originatingDeclarations.forEach {
                    lookupManager.recordLookup(
                        result.absolutePath,
                        it
                    )
                }
            }
    }

}
