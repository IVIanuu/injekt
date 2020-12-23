package com.ivianuu.injekt.ide

import com.intellij.ide.ApplicationInitializedListener
import com.intellij.openapi.application.Application
import com.intellij.openapi.application.ApplicationManager
import com.intellij.openapi.extensions.Extensions
import com.intellij.openapi.project.Project
import com.intellij.openapi.project.ProjectManager
import com.intellij.openapi.project.ProjectManagerListener
import com.intellij.openapi.vfs.VirtualFile
import com.intellij.psi.PsiFile
import com.intellij.psi.PsiManager
import com.intellij.psi.search.FileTypeIndex
import com.intellij.util.messages.Topic
import com.ivianuu.injekt.compiler.analysis.GivenCallResolutionInterceptorExtension
import com.ivianuu.injekt.compiler.analysis.InjektDiagnosticSuppressor
import com.ivianuu.injekt.compiler.analysis.InjektStorageComponentContainerContributor
import com.ivianuu.injekt.compiler.analysis.InjektTypeResolutionInterceptor
import org.jetbrains.kotlin.descriptors.DeclarationDescriptor
import org.jetbrains.kotlin.extensions.StorageComponentContainerContributor
import org.jetbrains.kotlin.extensions.internal.CandidateInterceptor
import org.jetbrains.kotlin.extensions.internal.TypeResolutionInterceptor
import org.jetbrains.kotlin.idea.KotlinFileType
import org.jetbrains.kotlin.idea.resolve.ResolutionFacade
import org.jetbrains.kotlin.idea.search.projectScope
import org.jetbrains.kotlin.psi.KtFile
import org.jetbrains.kotlin.resolve.diagnostics.DiagnosticSuppressor
import org.jetbrains.kotlin.resolve.lazy.BodyResolveMode

class AppInitializer : ApplicationInitializedListener {
    override fun componentsInitialized() {
        val app = ApplicationManager.getApplication()

        app?.projectOpened { project ->
            StorageComponentContainerContributor.registerExtension(
                project,
                InjektStorageComponentContainerContributor()
            )
            CandidateInterceptor.registerExtension(
                project,
                GivenCallResolutionInterceptorExtension()
            )

            TypeResolutionInterceptor.registerExtension(
                project,
                InjektTypeResolutionInterceptor()
            )

            @Suppress("DEPRECATION")
            Extensions.getRootArea().getExtensionPoint(DiagnosticSuppressor.EP_NAME)
                .registerExtension(InjektDiagnosticSuppressor())

            registerGivenCallCheckerRunner(project)
        }
    }
}

fun <A> Application.registerTopic(topic: Topic<A>, listeners: A): Unit =
    messageBus.connect(this).subscribe(topic, listeners)

fun Application.projectOpened(opened: (Project) -> Unit): Unit =
    registerTopic(ProjectManager.TOPIC, object : ProjectManagerListener {
        override fun projectOpened(project: Project): Unit =
            opened(project)
    })

fun <F : PsiFile> List<VirtualFile>.files(project: Project): List<F> =
    mapNotNull { PsiManager.getInstance(project).findFile(it) as? F }

fun Project.ktFiles(): List<VirtualFile> =
    FileTypeIndex.getFiles(KotlinFileType.INSTANCE, projectScope()).filterNotNull()

fun VirtualFile.relevantFile(): Boolean =
    isValid &&
            this.fileType is KotlinFileType &&
            (isInLocalFileSystem || ApplicationManager.getApplication().isUnitTestMode)

fun Project.relevantFiles(): List<KtFile> =
    ktFiles()
        .filter { it.relevantFile() && it.isInLocalFileSystem }
        .files(this)

fun KtFile.resolve(
    facade: ResolutionFacade,
    resolveMode: BodyResolveMode = BodyResolveMode.PARTIAL,
): Pair<KtFile, List<DeclarationDescriptor>> =
    this to declarations.map { facade.resolveToDescriptor(it, resolveMode) }