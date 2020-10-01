package com.ivianuu.injekt.merge

import com.ivianuu.injekt.Binding

typealias App = Any

@MergeComponent
abstract class ApplicationComponent(@Binding val app: App)
