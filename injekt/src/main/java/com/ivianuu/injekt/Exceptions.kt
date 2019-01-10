package com.ivianuu.injekt

open class InjektException(message: String, throwable: Throwable? = null) :
    RuntimeException(message, throwable)

class OverrideException(message: String) : InjektException(message)

class InstanceCreationException(message: String, cause: Throwable) : InjektException(message, cause)

class BindingNotFoundException(message: String) : InjektException(message)