package com.example.nalla_nudi.data

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "words")
data class Word(
    @PrimaryKey val id: Int,
    val english_term: String,
    val kannada_meaning: String,
    val kannada_explanation: String,
    val example_sentence: String,
    val subject_category: String,

    // ✅ ADD THESE (IMPORTANT FOR UI)
    var is_saved: Boolean = false,
    var is_learned: Boolean = false
)