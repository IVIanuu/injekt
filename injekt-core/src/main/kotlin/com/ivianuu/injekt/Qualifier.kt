package com.ivianuu.injekt

import com.ivianuu.injekt.internal.stub

interface Qualifier {
    operator fun plus(qualifier: Qualifier): Qualifier =
        stub()
}
