/*
 * Copyright 2018 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ivianuu.injekt.gradle.archive

import com.android.tools.build.jetifier.core.utils.Log
import java.io.BufferedOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.file.Files
import java.nio.file.Path
import java.nio.file.Paths
import java.nio.file.StandardCopyOption
import java.nio.file.attribute.FileTime
import java.time.Instant
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

/**
 * Represents an archive (zip, jar, aar ...)
 */
class Archive(
    override val relativePath: Path,
    files: List<ArchiveItem>
) : ArchiveItem {

    override var parent: Archive? = null

    init {
        files.forEach {
            if (it is Archive) it.parent = this
            else if (it is ArchiveFile) it.parent = this
        }
    }

    private val _files: MutableList<ArchiveItem> = files.toMutableList()

    val files: List<ArchiveItem> = _files

    companion object {
        /** Defines file extensions that are recognized as archives */
        val ARCHIVE_EXTENSIONS = listOf(".jar", ".zip", ".aar")

        const val TAG = "Archive"
    }

    override val fileName: String = relativePath.fileName.toString()

    override var markedForRemoval: Boolean = false

    private var targetPath: Path = relativePath

    override val wasChanged: Boolean
        get() = files.any { it.wasChanged }

    /**
     * Sets path where the file should be saved after transformation.
     */
    fun setTargetPath(path: Path) {
        targetPath = path
    }

    override fun findAllFiles(selector: (ArchiveFile) -> Boolean, result: FileSearchResult) {
        files.forEach { it.findAllFiles(selector, result) }
    }

    override fun accept(visitor: ArchiveItemVisitor) {
        visitor.visit(this)
    }

    @Throws(IOException::class)
    fun writeSelfToDir(outputDirPath: Path): File {
        val outputPath = Paths.get(outputDirPath.toString(), fileName)

        return writeSelfToFile(outputPath)
    }

    fun writeSelf(): File {
        return writeSelfToFile(targetPath)
    }

    /**
     * Copies the original file into the target path.
     *
     * Meaning that any changes made to this archive so far won't be propagated to the target.
     * @return Returns the copied file in the target path.
     */
    fun copySelfFromOriginToTarget(): File {
        Files.copy(relativePath, targetPath, StandardCopyOption.REPLACE_EXISTING)
        return targetPath.toFile()
    }

    fun removeItem(item: ArchiveItem) {
        _files.remove(item)
    }

    fun addItem(item: ArchiveItem) {
        _files.add(item)
    }

    @Throws(IOException::class)
    fun writeSelfToFile(outputPath: Path): File {
        if (Files.exists(outputPath)) {
            Log.i(TAG, "Deleting old output file")
            Files.delete(outputPath)
        }

        // Create directories if they don't exist yet
        if (outputPath.parent != null) {
            // By default createDirectories should check if the directory already exists and not
            // throw anything. However apparently it does not consider "tmp" to be directory and
            // throws exception. That's why we need to check for exists here.
            if (!Files.exists(outputPath.parent)) {
                Files.createDirectories(outputPath.parent)
            }
        }

        Log.i(TAG, "Writing archive: %s", outputPath.toUri())
        val file = outputPath.toFile()
        Files.createFile(outputPath)
        val stream = BufferedOutputStream(FileOutputStream(file))
        stream.use {
            writeSelfTo(it)
        }
        return file
    }

    @Throws(IOException::class)
    override fun writeSelfTo(outputStream: OutputStream) {
        val out = ZipOutputStream(outputStream)

        for (file in files) {
            if (file.markedForRemoval) {
                continue
            }

            Log.v(TAG, "Writing file: %s", file.relativePath)
            // Make sure we always use '/' as separator in ZipOutputStream otherwise we might end
            // up with a corrupted zip file on a non-Unix OS (b/109738608).
            val path = file.relativePath.toString().replace('\\', '/')
            val entry = ZipEntry(path)
            entry.lastModifiedTime = FileTime.from(Instant.now()) // b/78249473
            out.putNextEntry(entry)
            file.writeSelfTo(out)
            out.closeEntry()
        }
        out.finish()
    }

    object Builder {

        /**
         * @param recursive Whether nested archives should be also extracted.
         */
        @Throws(IOException::class)
        fun extract(archiveFile: File, recursive: Boolean = true): Archive {
            Log.i(TAG, "Extracting: %s", archiveFile.absolutePath)

            val inputStream = FileInputStream(archiveFile)
            inputStream.use {
                return extractArchive(it, archiveFile.toPath(), recursive)
            }
        }

        @Throws(IOException::class)
        private fun extractArchive(
            inputStream: InputStream,
            relativePath: Path,
            recursive: Boolean
        ): Archive {
            val zipIn = ZipInputStream(inputStream)
            val files = mutableListOf<ArchiveItem>()

            var entry: ZipEntry? = zipIn.nextEntry
            // iterates over entries in the zip file
            while (entry != null) {
                if (!entry.isDirectory) {
                    val entryPath = Paths.get(entry.name)
                    if (isArchive(entry) && recursive) {
                        Log.i(TAG, "Extracting nested: %s", entryPath)
                        files.add(extractArchive(zipIn, entryPath, recursive))
                    } else {
                        files.add(extractFile(zipIn, entryPath))
                    }
                }
                zipIn.closeEntry()
                entry = zipIn.nextEntry
            }
            // Cannot close the zip stream at this moment as that would close also any parent zip
            // streams in case we are processing a nested archive.

            return Archive(relativePath, files.toList())
        }

        @Throws(IOException::class)
        private fun extractFile(
            zipIn: ZipInputStream,
            relativePath: Path
        ): ArchiveFile {
            Log.v(TAG, "Extracting archive: %s", relativePath)

            val data = zipIn.readBytes()
            return ArchiveFile(relativePath, data)
        }

        private fun isArchive(zipEntry: ZipEntry): Boolean {
            return ARCHIVE_EXTENSIONS.any { zipEntry.name.endsWith(it, ignoreCase = true) }
        }
    }
}
