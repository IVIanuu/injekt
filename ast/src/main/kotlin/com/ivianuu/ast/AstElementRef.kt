package com.ivianuu.ast

class AstElementRef<T : AstElement> {
    lateinit var value: T
    fun bind(value: T) {
        this.value = value
    }
}
