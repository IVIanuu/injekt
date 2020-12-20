package com.ivianuu.injekt.ide

fun registerGenerator() {
    /*val generatorManager = GeneratorManager(project, File("build/generated/source/injekt"))

         PackageFragmentProviderExtension.registerExtension(
             project,
             GeneratorPackageFragmentProviderExtension(generatorManager)
         )

         fun projectOpened() {
             ReadAction.nonBlocking {
                 val projectFiles = project.relevantFiles()
                 println("relvant files $projectFiles")
                 generatorManager.refresh(projectFiles)
             }.submit(generatorManager.cacheExecutor)
         }

         runBackgroundableTask("Initialize injekt") { projectOpened() }

         Extensions.getRootArea()
             .getExtensionPoint(StartupActivity.POST_STARTUP_ACTIVITY)
             .registerExtension(StartupActivity {
                 projectOpened()
             }, LoadingOrder.FIRST, project)

         val editorQueue =
             MergingUpdateQueue("arrow doc events", 500, true,
                 null, project, null, Alarm.ThreadToUse.POOLED_THREAD)

         /*VirtualFileManager.getInstance().addAsyncFileListener(
             { events ->
                 println("prepare events $events")

                 val relevantFiles =
                     events.filter { vfile -> vfile.isValid && vfile is KotlinFileType }
                         .mapNotNull { vFile -> vFile.file }

                 if (relevantFiles.isEmpty()) {
                     return@addAsyncFileListener null
                 }

                 object : AsyncFileListener.ChangeApplier {
                     override fun afterVfsChange() {
                         println("after vfs change ${relevantFiles}")
                     }
                 }
             },
             project
         )*/

         EditorFactory.getInstance().eventMulticaster.addDocumentListener(object : BulkAwareDocumentListener.Simple {
             override fun afterDocumentChange(document: Document) {
                 editorQueue.queue(Update.create(document) {
                     println("document changed $document")
                     ReadAction.nonBlocking {
                         FileDocumentManager.getInstance()
                             .getFile(document)
                             // proceed unless
                             ?.takeUnless {
                                 it is LightVirtualFile ||
                                         !it.relevantFile() ||
                                         !FileIndexFacade.getInstance(project).isInSourceContent(it)
                             }
                             ?.let { _ ->
                                 PsiDocumentManager.getInstance(project)
                                     .getPsiFile(document)
                                     ?.safeAs<KtFile>()
                                     ?.takeIf { it.isPhysical && !it.isCompiled }
                                     ?.let { ktFile ->
                                         println("transforming ${ktFile.name} after change in editor")
                                         generatorManager.refresh(listOf(ktFile))
                                     }
                             }
                     }.expireWith(project)
                         .submit(generatorManager.cacheExecutor)
                 })
             }
         }, project)*/
}