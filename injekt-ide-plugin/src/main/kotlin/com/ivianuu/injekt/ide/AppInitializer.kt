package com.ivianuu.injekt.ide

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.util.messages.Topic
import com.ivianuu.injekt.compiler.analysis.GivenCallResolutionInterceptorExtension
import com.ivianuu.injekt.compiler.analysis.InjektDiagnosticSuppressor
import com.ivianuu.injekt.compiler.analysis.InjektStorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.internal.CandidateInterceptor
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor

class AppInitializer : ApplicationInitializedListener {
    override fun componentsInitialized() {
        ApplicationManager.getApplication()
            ?.projectOpened { project ->
                StorageComponentContainerContributor.registerExtension(
                    project,
                    InjektStorageComponentContainerContributor()
                )
                CandidateInterceptor.registerExtension(
                    project,
                    GivenCallResolutionInterceptorExtension()
                )
                @Suppress("DEPRECATION")
                Extensions.getRootArea().getExtensionPoint(DiagnosticSuppressor.EP_NAME)
                    .registerExtension(InjektDiagnosticSuppressor())
            }
    }

    fun <A> Application.registerTopic(topic: Topic<A>, listeners: A): Unit =
        messageBus.connect(this).subscribe(topic, listeners)

    fun Application.projectOpened(opened: (Project) -> Unit): Unit =
        registerTopic(ProjectManager.TOPIC, object : ProjectManagerListener {
            override fun projectOpened(project: Project): Unit =
                opened(project)
        })

    fun Application.registerCliExtension(
        f: Project.() -> Unit,
    ): Unit = projectOpened { project -> f(project) }

}
