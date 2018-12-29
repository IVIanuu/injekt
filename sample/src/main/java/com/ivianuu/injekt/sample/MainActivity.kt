package com.ivianuu.injekt.sample

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.injekt.*

class MainActivity : AppCompatActivity(), ComponentHolder {

    override val component by lazy {
        component {
            modules(mainActivityModule())
            dependencies((application as ComponentHolder).component)
        }
    }

    private val appDependency by inject<AppDependency>()
    private val mainActivityDependency by inject<MainActivityDependency>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        appDependency
        mainActivityDependency
    }

}

fun MainActivity.mainActivityModule() = module {
    factory { this@mainActivityModule }
    single { MainActivityDependency(get(), get()) }
}

class MainActivityDependency(val app: App, val mainActivity: MainActivity)