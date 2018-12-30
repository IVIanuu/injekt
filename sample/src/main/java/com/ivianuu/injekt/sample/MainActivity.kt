package com.ivianuu.injekt.sample

import android.os.Bundle
import android.util.Log
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.injekt.*
import com.ivianuu.injekt.annotations.Single
import kotlin.reflect.KClass

class MainActivity : AppCompatActivity(), ComponentHolder {

    override val component by lazy {
        component {
            dependencies((application as ComponentHolder).component)
            modules(mainActivityModule())
        }
    }

    //private val appDependency by inject<AppDependency>()
    //private val mainActivityDependency by inject<MainActivityDependency>()

    private val servicesMap by inject<Map<KClass<out Service>, Service>>(SERVICES_MAP)
    private val servicesSet by inject<Set<Service>>(SERVICES_SET)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        //     appDependency
        //     mainActivityDependency

        Log.d("App", "services set $servicesSet \n\n services map $servicesMap")
    }

}

fun MainActivity.mainActivityModule() = module {
    factory { this@mainActivityModule }
    factory { MyServiceThree() } intoSet SERVICES_SET intoMap (SERVICES_MAP to MyServiceThree::class)
}

@Single
class MainActivityDependency(
    val app: App,
    val mainActivity: MainActivity
)