package com.ivianuu.injekt.gradle

import org.gradle.api.tasks.Internal
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments
import org.jetbrains.kotlin.gradle.tasks.CompilerPluginOptions
import org.jetbrains.kotlin.gradle.tasks.KotlinCompile
import org.jetbrains.kotlin.gradle.tasks.KtGpAccessor
import org.jetbrains.kotlin.incremental.classpathAsList
import org.jetbrains.kotlin.incremental.destinationAsFile

open class GenerateStubsTask : KotlinCompile() {

    lateinit var kotlinCompileTask: KotlinCompile

    private val compileKotlinArgumentsContributor: Any by project.provider {
        KtGpAccessor.getKotlinArgumentsContributor(kotlinCompileTask)
    }

    @get:Internal
    internal val pluginOptions = CompilerPluginOptions()
    override fun setupCompilerArgs(
        args: K2JVMCompilerArguments,
        defaultsOnly: Boolean,
        ignoreClasspathResolutionErrors: Boolean
    ) {
        KtGpAccessor.contributeArguments(
            compileKotlinArgumentsContributor,
            args,
            defaultsOnly,
            ignoreClasspathResolutionErrors
        )

        args.pluginOptions = args.pluginOptions!! + pluginOptions.arguments

        args.classpathAsList = (classpath + project.files(additionalClasspath)).toList()
        args.destinationAsFile = this.destinationDir
    }

}
