package com.ivianuu.injekt.gradle

import org.gradle.api.artifacts.transform.InputArtifact
import org.gradle.api.artifacts.transform.TransformAction
import org.gradle.api.artifacts.transform.TransformOutputs
import org.gradle.api.artifacts.transform.TransformParameters
import org.gradle.api.file.FileSystemLocation
import org.gradle.api.provider.Provider

abstract class GradleTransform : TransformAction<TransformParameters.None> {

    @get:InputArtifact
    abstract val inputArtifact: Provider<FileSystemLocation>

    override fun transform(outputs: TransformOutputs) {
        /*val inputFile = inputArtifact.get().asFile

        if (inputFile.absolutePath.contains("injekt") &&
            !inputFile.absolutePath.contains("compiler") &&
            !inputFile.absolutePath.contains("stub") &&
            !inputFile.absolutePath.contains("dagger-1-shadowed")) {
            error("lol ${inputFile.absolutePath}")
        }

        if ((inputFile.name.endsWith(".jar") || inputFile.name.endsWith(".aar")) &&
                inputFile.name != "injekt-stub.jar") {
            println("lalala process ${inputFile.name}")
            val archive = Archive.Builder.extract(inputFile)
            val filesToRemove = FileSearchResult()
            archive.findAllFiles(
                selector = { it.fileName == "GeneratedComponentContributors.class" },
                result = filesToRemove
            )

            if (filesToRemove.all.isNotEmpty()) {
                val outputFile = outputs.file("transformed-${inputFile.name}")

                filesToRemove.all.forEach {
                    println("lalala remove ${it.fileName} from $inputFile")
                    it.parent!!.removeItem(it)
                }

                archive.writeSelfTo(outputFile.outputStream())
            } else {
                println("lalala no occurrence found in $inputFile")
                outputs.file(inputFile)
            }
        } else {
            println("lalala do not change $inputFile")
            outputs.file(inputFile)
        }*/
    }
}
