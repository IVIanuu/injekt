package com.ivianuu.injekt

class ProviderDsl {
    inline fun <reified T> get(): T = stub()
}
