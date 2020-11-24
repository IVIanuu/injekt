package com.ivianuu.injekt.compiler
class ApplicationComponentImpl(project: org.jetbrains.kotlin.com.intellij.openapi.project.Project, configuration: org.jetbrains.kotlin.config.CompilerConfiguration) : com.ivianuu.injekt.compiler.ApplicationComponent(project, configuration) {
    private inner class C(moduleDescriptor: org.jetbrains.kotlin.descriptors.ModuleDescriptor, bindingContext: org.jetbrains.kotlin.resolve.BindingContext) : com.ivianuu.injekt.compiler.generator.GenerationComponent(moduleDescriptor, bindingContext) {
        override val errorCollector: com.ivianuu.injekt.compiler.generator.ErrorCollector
            get() = run {
            var value = this@C._com_ivianuu_injekt_compiler_generator_ErrorCollector
            if (value !== this@C) return@run value as com.ivianuu.injekt.compiler.generator.ErrorCollector
            synchronized(this) {
                value = this@C._com_ivianuu_injekt_compiler_generator_ErrorCollector
                if (value !== this@C) return@run value as com.ivianuu.injekt.compiler.generator.ErrorCollector
                value = com.ivianuu.injekt.compiler.generator.ErrorCollector()
                this@C._com_ivianuu_injekt_compiler_generator_ErrorCollector = value
                return@run value as com.ivianuu.injekt.compiler.generator.ErrorCollector
            }
        }
        override val funBindingGenerator: com.ivianuu.injekt.compiler.generator.FunBindingGenerator
            get() = run {
            var value = this@C._com_ivianuu_injekt_compiler_generator_FunBindingGenerator
            if (value !== this@C) return@run value as com.ivianuu.injekt.compiler.generator.FunBindingGenerator
            synchronized(this) {
                value = this@C._com_ivianuu_injekt_compiler_generator_FunBindingGenerator
                if (value !== this@C) return@run value as com.ivianuu.injekt.compiler.generator.FunBindingGenerator
                value = com.ivianuu.injekt.compiler.generator.FunBindingGenerator(this@C.org_jetbrains_kotlin_resolve_BindingContext, this@C.com_ivianuu_injekt_compiler_generator_DeclarationStore, this@C.fileManager, this@C.com_ivianuu_injekt_compiler_generator_TypeTranslator)
                this@C._com_ivianuu_injekt_compiler_generator_FunBindingGenerator = value
                return@run value as com.ivianuu.injekt.compiler.generator.FunBindingGenerator
            }
        }
        override val fileManager: com.ivianuu.injekt.compiler.generator.FileManager
            get() = run {
            var value = this@C._com_ivianuu_injekt_compiler_generator_FileManager
            if (value !== this@C) return@run value as com.ivianuu.injekt.compiler.generator.FileManager
            synchronized(this) {
                value = this@C._com_ivianuu_injekt_compiler_generator_FileManager
                if (value !== this@C) return@run value as com.ivianuu.injekt.compiler.generator.FileManager
                value = com.ivianuu.injekt.compiler.generator.FileManager(this@C.com_ivianuu_injekt_compiler_SrcDir, this@C.com_ivianuu_injekt_compiler_log)
                this@C._com_ivianuu_injekt_compiler_generator_FileManager = value
                return@run value as com.ivianuu.injekt.compiler.generator.FileManager
            }
        }
        override val componentGenerator: com.ivianuu.injekt.compiler.generator.ComponentGenerator
            get() = com.ivianuu.injekt.compiler.generator.ComponentGenerator(this@C.org_jetbrains_kotlin_resolve_BindingContext, this@C.com_ivianuu_injekt_compiler_generator_DeclarationStore, this@C.fileManager, this@C.kotlin_Function5_1483289230, this@C.com_ivianuu_injekt_compiler_generator_TypeTranslator)
        override val indexGenerator: com.ivianuu.injekt.compiler.generator.IndexGenerator
            get() = run {
            var value = this@C._com_ivianuu_injekt_compiler_generator_IndexGenerator
            if (value !== this@C) return@run value as com.ivianuu.injekt.compiler.generator.IndexGenerator
            synchronized(this) {
                value = this@C._com_ivianuu_injekt_compiler_generator_IndexGenerator
                if (value !== this@C) return@run value as com.ivianuu.injekt.compiler.generator.IndexGenerator
                value = com.ivianuu.injekt.compiler.generator.IndexGenerator(this@C.org_jetbrains_kotlin_resolve_BindingContext, this@C.com_ivianuu_injekt_compiler_generator_DeclarationStore, this@C.fileManager)
                this@C._com_ivianuu_injekt_compiler_generator_IndexGenerator = value
                return@run value as com.ivianuu.injekt.compiler.generator.IndexGenerator
            }
        }
        var _com_ivianuu_injekt_compiler_generator_ErrorCollector: kotlin.Any? = this
        var _com_ivianuu_injekt_compiler_generator_FunBindingGenerator: kotlin.Any? = this
        val org_jetbrains_kotlin_resolve_BindingContext: org.jetbrains.kotlin.resolve.BindingContext
            get() = with(this@C) {
            bindingContext
        }
        val com_ivianuu_injekt_compiler_generator_DeclarationStore: com.ivianuu.injekt.compiler.generator.DeclarationStore
            get() = run {
            var value = this@C._com_ivianuu_injekt_compiler_generator_DeclarationStore
            if (value !== this@C) return@run value as com.ivianuu.injekt.compiler.generator.DeclarationStore
            synchronized(this) {
                value = this@C._com_ivianuu_injekt_compiler_generator_DeclarationStore
                if (value !== this@C) return@run value as com.ivianuu.injekt.compiler.generator.DeclarationStore
                value = com.ivianuu.injekt.compiler.generator.DeclarationStore(this@C.org_jetbrains_kotlin_descriptors_ModuleDescriptor)
                this@C._com_ivianuu_injekt_compiler_generator_DeclarationStore = value
                return@run value as com.ivianuu.injekt.compiler.generator.DeclarationStore
            }
        }
        val com_ivianuu_injekt_compiler_generator_FileManager: com.ivianuu.injekt.compiler.generator.FileManager
            get() = this@C.fileManager
        val com_ivianuu_injekt_compiler_generator_TypeTranslator: com.ivianuu.injekt.compiler.generator.TypeTranslator
            get() = com.ivianuu.injekt.compiler.generator.TypeTranslator(this@C.com_ivianuu_injekt_compiler_generator_DeclarationStore, this@C.errorCollector)
        var _com_ivianuu_injekt_compiler_generator_FileManager: kotlin.Any? = this
        val com_ivianuu_injekt_compiler_SrcDir: java.io.File
            get() = this@ApplicationComponentImpl.com_ivianuu_injekt_compiler_SrcDir
        val com_ivianuu_injekt_compiler_log: com.ivianuu.injekt.compiler.log = { p0: kotlin.Function0<kotlin.String> ->
        com.ivianuu.injekt.compiler.log(this@C.com_ivianuu_injekt_compiler_Logger, p0)
        }
        val kotlin_Function5_1483289230: kotlin.Function5<com.ivianuu.injekt.compiler.generator.TypeRef, org.jetbrains.kotlin.name.Name, kotlin.collections.List<com.ivianuu.injekt.compiler.generator.TypeRef>, kotlin.collections.List<com.ivianuu.injekt.compiler.generator.Callable>, com.ivianuu.injekt.compiler.generator.componentimpl.ComponentImpl?, com.ivianuu.injekt.compiler.generator.componentimpl.ComponentImpl> = { p0: com.ivianuu.injekt.compiler.generator.TypeRef, p1: org.jetbrains.kotlin.name.Name, p2: kotlin.collections.List<com.ivianuu.injekt.compiler.generator.TypeRef>, p3: kotlin.collections.List<com.ivianuu.injekt.compiler.generator.Callable>, p4: com.ivianuu.injekt.compiler.generator.componentimpl.ComponentImpl?,  ->
        com.ivianuu.injekt.compiler.generator.componentimpl.ComponentImpl(this@C.com_ivianuu_injekt_compiler_generator_DeclarationStore, this@C.kotlin_Function1_448534877, this@C.kotlin_Function1_1573881639, p0, p1, p2, p3, p4)
        }
        var _com_ivianuu_injekt_compiler_generator_IndexGenerator: kotlin.Any? = this
        var _com_ivianuu_injekt_compiler_generator_DeclarationStore: kotlin.Any? = this
        val org_jetbrains_kotlin_descriptors_ModuleDescriptor: org.jetbrains.kotlin.descriptors.ModuleDescriptor
            get() = with(this@C) {
            moduleDescriptor
        }
        val com_ivianuu_injekt_compiler_generator_ErrorCollector: com.ivianuu.injekt.compiler.generator.ErrorCollector
            get() = this@C.errorCollector
        val com_ivianuu_injekt_compiler_Logger: com.ivianuu.injekt.compiler.Logger?
            get() = com.ivianuu.injekt.compiler.logger()
        val kotlin_Function1_448534877: kotlin.Function1<com.ivianuu.injekt.compiler.generator.componentimpl.ComponentImpl, com.ivianuu.injekt.compiler.generator.componentimpl.ComponentStatements> = { p0: com.ivianuu.injekt.compiler.generator.componentimpl.ComponentImpl,  ->
        com.ivianuu.injekt.compiler.generator.componentimpl.ComponentStatements(this@C.com_ivianuu_injekt_compiler_generator_DeclarationStore, this@C.org_jetbrains_kotlin_descriptors_ModuleDescriptor, p0, this@C.com_ivianuu_injekt_compiler_generator_TypeTranslator)
        }
        val kotlin_Function1_1573881639: kotlin.Function1<com.ivianuu.injekt.compiler.generator.componentimpl.ComponentImpl, com.ivianuu.injekt.compiler.generator.componentimpl.BindingGraph> = { p0: com.ivianuu.injekt.compiler.generator.componentimpl.ComponentImpl,  ->
        com.ivianuu.injekt.compiler.generator.componentimpl.BindingGraph(p0, this@C.kotlin_Function2_356707474, this@C.com_ivianuu_injekt_compiler_generator_DeclarationStore, this@C.kotlin_Function5_1483289230, this@C.org_jetbrains_kotlin_descriptors_ModuleDescriptor, this@C.com_ivianuu_injekt_compiler_generator_TypeTranslator)
        }
        val kotlin_Function2_356707474: kotlin.Function2<com.ivianuu.injekt.compiler.generator.componentimpl.ComponentImpl, com.ivianuu.injekt.compiler.generator.componentimpl.BindingCollections?, com.ivianuu.injekt.compiler.generator.componentimpl.BindingCollections> = { p0: com.ivianuu.injekt.compiler.generator.componentimpl.ComponentImpl, p1: com.ivianuu.injekt.compiler.generator.componentimpl.BindingCollections?,  ->
        com.ivianuu.injekt.compiler.generator.componentimpl.BindingCollections(this@C.com_ivianuu_injekt_compiler_generator_DeclarationStore, p0, p1)
        }
    }
    override val registerExtensions: com.ivianuu.injekt.compiler.registerExtensions = { p0: kotlin.Boolean ->
    com.ivianuu.injekt.compiler.registerExtensions(this@ApplicationComponentImpl.org_jetbrains_kotlin_com_intellij_openapi_project_Project, this@ApplicationComponentImpl.com_ivianuu_injekt_compiler_generator_DeleteOldFilesExtension, this@ApplicationComponentImpl.com_ivianuu_injekt_compiler_checkers_InjektStorageContainerContributor, this@ApplicationComponentImpl.com_ivianuu_injekt_compiler_generator_InjektKtGenerationExtension, p0)
    }
    val com_ivianuu_injekt_compiler_SrcDir: java.io.File
        get() = run {
        var value = this@ApplicationComponentImpl._com_ivianuu_injekt_compiler_SrcDir
        if (value !== this@ApplicationComponentImpl) return@run value as java.io.File
        synchronized(this) {
            value = this@ApplicationComponentImpl._com_ivianuu_injekt_compiler_SrcDir
            if (value !== this@ApplicationComponentImpl) return@run value as java.io.File
            value = com.ivianuu.injekt.compiler.srcDir(this@ApplicationComponentImpl.org_jetbrains_kotlin_config_CompilerConfiguration)
            this@ApplicationComponentImpl._com_ivianuu_injekt_compiler_SrcDir = value
            return@run value as java.io.File
        }
    }
    val org_jetbrains_kotlin_com_intellij_openapi_project_Project: org.jetbrains.kotlin.com.intellij.openapi.project.Project
        get() = with(this@ApplicationComponentImpl) {
        project
    }
    val com_ivianuu_injekt_compiler_generator_DeleteOldFilesExtension: com.ivianuu.injekt.compiler.generator.DeleteOldFilesExtension
        get() = com.ivianuu.injekt.compiler.generator.DeleteOldFilesExtension(this@ApplicationComponentImpl.com_ivianuu_injekt_compiler_SrcDir)
    val com_ivianuu_injekt_compiler_checkers_InjektStorageContainerContributor: com.ivianuu.injekt.compiler.checkers.InjektStorageContainerContributor
        get() = run {
        var value = this@ApplicationComponentImpl._com_ivianuu_injekt_compiler_checkers_InjektStorageContainerContributor
        if (value !== this@ApplicationComponentImpl) return@run value as com.ivianuu.injekt.compiler.checkers.InjektStorageContainerContributor
        synchronized(this) {
            value = this@ApplicationComponentImpl._com_ivianuu_injekt_compiler_checkers_InjektStorageContainerContributor
            if (value !== this@ApplicationComponentImpl) return@run value as com.ivianuu.injekt.compiler.checkers.InjektStorageContainerContributor
            value = com.ivianuu.injekt.compiler.checkers.InjektStorageContainerContributor(this@ApplicationComponentImpl.com_ivianuu_injekt_compiler_checkers_BindingChecker)
            this@ApplicationComponentImpl._com_ivianuu_injekt_compiler_checkers_InjektStorageContainerContributor = value
            return@run value as com.ivianuu.injekt.compiler.checkers.InjektStorageContainerContributor
        }
    }
    val com_ivianuu_injekt_compiler_generator_InjektKtGenerationExtension: com.ivianuu.injekt.compiler.generator.InjektKtGenerationExtension
        get() = com.ivianuu.injekt.compiler.generator.InjektKtGenerationExtension(this@ApplicationComponentImpl.kotlin_Function2_28888001)
    var _com_ivianuu_injekt_compiler_SrcDir: kotlin.Any? = this
    val org_jetbrains_kotlin_config_CompilerConfiguration: org.jetbrains.kotlin.config.CompilerConfiguration
        get() = with(this@ApplicationComponentImpl) {
        configuration
    }
    var _com_ivianuu_injekt_compiler_checkers_InjektStorageContainerContributor: kotlin.Any? = this
    val com_ivianuu_injekt_compiler_checkers_BindingChecker: com.ivianuu.injekt.compiler.checkers.BindingChecker
        get() = com.ivianuu.injekt.compiler.checkers.BindingChecker()
    val kotlin_Function2_28888001: kotlin.Function2<org.jetbrains.kotlin.descriptors.ModuleDescriptor, org.jetbrains.kotlin.resolve.BindingContext, com.ivianuu.injekt.compiler.generator.GenerationComponent> = ::C
}