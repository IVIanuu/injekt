package com.ivianuu.injekt.sample

import com.ivianuu.injekt.Provider

@ApplicationScoped
class Database

@ApplicationScoped
class Api

@ApplicationScoped
class Repo(val database: Database, val api: Api)

class Lol(private val databaseProvider: @Provider () -> Database) {
    init {
        databaseProvider
    }
}
