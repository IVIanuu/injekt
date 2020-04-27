package com.ivianuu.injekt.sample

import android.app.Application
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.ivianuu.injekt.android.RetainedActivityScoped
import com.ivianuu.injekt.android.getLazy

class MainActivity : AppCompatActivity() {
    private val presenter: MyPresenter by getLazy()
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        presenter
    }
}

@RetainedActivityScoped
class MyPresenter(private val application: Application) {
    init {
        println("hello world")
        println()
    }
}
