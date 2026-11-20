
package com.example.teduh.model

data class Mood(
    val id: Int = 0,
    val date: String,
    val score: Int,
    val emotion: String,
    val trigger: String,
    val journal: String
)
