package com.ivianuu.injekt.android

import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentFactory
import com.ivianuu.injekt.ApplicationScope
import com.ivianuu.injekt.Module
import com.ivianuu.injekt.ModuleMarker
import com.ivianuu.injekt.alias
import com.ivianuu.injekt.map

@ModuleMarker
private val FragmentInjectionModule = Module(ApplicationScope) {
    map<String, Fragment>()
    alias<InjektFragmentFactory, FragmentFactory>()
}