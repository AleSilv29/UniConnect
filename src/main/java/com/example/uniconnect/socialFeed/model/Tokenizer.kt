package com.example.uniconnect.socialFeed.model

class Tokenizer {
    private val wordIndex = mutableMapOf<String, Int>()

    fun fitOnTexts(texts: List<String?>) {
        val words = texts.flatMap { it!!.split(" ") }.distinct()
        wordIndex.clear()
        words.forEachIndexed { index, word ->
            wordIndex[word] = index + 1
        }
    }

    fun textsToSequences(text: String): List<Int> {
        return text.split(" ").mapNotNull { wordIndex[it] }
    }
}
