package com.ivianuu.injekt.sample

@ApplicationScoped
class Database

@ApplicationScoped
class Api

@ApplicationScoped
class Repo(val database: Database, val api: Api)
