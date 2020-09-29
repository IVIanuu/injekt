package com.ivianuu.injekt.merge

@Target(AnnotationTarget.CLASS)
annotation class MergeComponent {
    @Target(AnnotationTarget.CLASS)
    annotation class Factory
}

@Target(AnnotationTarget.CLASS)
annotation class MergeChildComponent {
    @Target(AnnotationTarget.CLASS)
    annotation class Factory
}

annotation class GenerateMergeComponents

fun <T> mergeComponentFactory(): T = error("Must be compiled with the injekt compiler")

@MergeComponent
interface ApplicationComponent
