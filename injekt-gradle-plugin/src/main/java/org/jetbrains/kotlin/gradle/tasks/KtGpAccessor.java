package org.jetbrains.kotlin.gradle.tasks;

import org.gradle.api.Project;
import org.jetbrains.kotlin.cli.common.arguments.K2JVMCompilerArguments;
import org.jetbrains.kotlin.gradle.internal.CompilerArgumentsContributorKt;
import org.jetbrains.kotlin.gradle.plugin.mpp.AbstractKotlinCompilation;

@SuppressWarnings("KotlinInternalInJava")
public class KtGpAccessor {

    public static void registerKotlinCompileTaskData(
            String taskName,
            AbstractKotlinCompilation compilation
    ) {
        KotlinCompileTaskData.Companion.register(taskName, compilation);
    }

    public static AbstractKotlinCompilation getKotlinCompileTaskDataCompilation(
            Project project,
            String taskName
    ) {
        return KotlinCompileTaskData.Companion.get(project, taskName)
                .getCompilation();
    }

    public static Object getKotlinArgumentsContributor(
            KotlinCompile kotlinCompile
    ) {
        return kotlinCompile.getCompilerArgumentsContributor$kotlin_gradle_plugin();
    }

    public static void contributeArguments(
            Object argumentsContributor,
            K2JVMCompilerArguments args,
            boolean defaultsOnly,
            boolean ignoreClasspathResolutionErrors
    ) {
        ((org.jetbrains.kotlin.gradle.internal.CompilerArgumentsContributor) argumentsContributor)
                .contributeArguments(args, CompilerArgumentsContributorKt.compilerArgumentsConfigurationFlags(
                        defaultsOnly, ignoreClasspathResolutionErrors
                ));
    }

}
