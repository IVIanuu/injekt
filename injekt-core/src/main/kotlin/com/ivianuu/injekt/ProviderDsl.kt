package com.ivianuu.injekt

import com.ivianuu.injekt.internal.stub

@InjektDslMarker
class ProviderDsl {
    inline fun <reified T> get(qualifier: Qualifier? = null): T = stub()
}
