package com.ivianuu.injekt.sample

import com.ivianuu.injekt.Module
import com.ivianuu.injekt.Qualifier
import com.ivianuu.injekt.transient
import java.io.File

@Module
fun dataModule() {
    @DatabaseFile
    transient { get<App>().cacheDir!! }
}

@Target(AnnotationTarget.EXPRESSION, AnnotationTarget.TYPE)
@Retention(AnnotationRetention.SOURCE)
@Qualifier
annotation class DatabaseFile

@ApplicationScoped
class Database(private val file: @DatabaseFile File)

@ApplicationScoped
class Repo(private val database: Database, private val api: Api) {
    fun refresh() {
    }
}

@ApplicationScoped
class Api
