package com.ivianuu.injekt

@Target(AnnotationTarget.CLASS)
annotation class Component {
    @Target(AnnotationTarget.CLASS)
    annotation class Factory
}

@Target(AnnotationTarget.CLASS)
annotation class ChildComponent {
    @Target(AnnotationTarget.CLASS)
    annotation class Factory
}
