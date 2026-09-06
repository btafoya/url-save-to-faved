package com.tafoyaventures.urlsavetofaved

data class FavedConfig(
    val serverUrl: String,
    val username: String,
    val password: String
)

data class FavedTag(
    val id: Int,
    val name: String,
    val color: String?,
    val parentId: Int?,
    val depth: Int = 0
)
