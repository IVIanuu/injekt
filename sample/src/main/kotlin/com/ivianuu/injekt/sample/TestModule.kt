package com.ivianuu.injekt.sample

import android.app.Activity
import android.app.Application
import android.content.Context
import com.ivianuu.injekt.Component
import com.ivianuu.injekt.ComponentDsl
import com.ivianuu.injekt.Module

val MyComponent = Component("app") {
    api("", 0, 0L)
}

@Module
fun ComponentDsl.api(url: String, url2: Int, nope: Long) {
    factory { null as Context }
    factory { null as Activity }
    factory { null as Application }
    factory { get<Context>().packageName!! }
    factory {
        val activity = get<Activity>()
        val application = get<Application>()
        application.resources!!
    }
}

@Module
fun ComponentDsl.myOther() {
    factory {
        get<String>()
        null as MainActivity
    }
}

/**
class myParent : Component {
val unitProvider = Provider {  }
override fun <T> get(key: String): T {
return when (key) {
"kotlin.Unit" -> unitProvider()
else -> error("Unexpected key $key")
} as T
}
}

class middle(
private val parent: Component
) : Component {
override fun <T> get(key: String): T {
return when (key) {
"kotlin.Unit" -> parent.get<T>(key)
else -> error("Unexpected key $key")
} as T
}
}

class myModule {
val middleComponent = Component("")
}

@Module
fun ComponentDsl.mymodule() {
parent("middle", middle(error("")))
}

val mybaby = Component("app") {
mymodule()
}

class myBaby(private val myModule: myModule) : Component {
override fun <T> get(key: String): T {
return when (key) {
"kotlin.Unit" -> myModule.middleComponent.get<T>(key)
else -> error("Unexpected key $key")
}
}
}*/