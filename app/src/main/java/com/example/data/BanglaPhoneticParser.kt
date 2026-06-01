package com.example.data

object BanglaPhoneticParser {

    private val STANDALONE_VOWELS = mapOf(
        "a" to "অ", "A" to "আ", "i" to "ই", "I" to "ঈ", "u" to "উ",
        "U" to "ঊ", "e" to "এ", "o" to "ও", "oi" to "ঐ", "ou" to "ঔ",
        "rri" to "ঋ"
    )

    private val VOWEL_MODIFIERS = mapOf(
        "a" to "া", "A" to "া", "i" to "ি", "I" to "ী", "u" to "ু",
        "U" to "ূ", "e" to "ে", "o" to "ো", "oi" to "ৈ", "ou" to "ৌ",
        "rri" to "ৃ"
    )

    private val CONSONANTS = mapOf(
        "kh" to "খ", "gh" to "ঘ", "ch" to "চ", "chh" to "ছ", "jh" to "ঝ",
        "Th" to "ঠ", "Dh" to "ঢ", "th" to "থ", "dh" to "ধ", "ph" to "ফ",
        "bh" to "ভ", "sh" to "শ", "Rh" to "ঢ়", "kh" to "খ",
        "k" to "ক", "g" to "গ", "j" to "জ", "T" to "ট", "D" to "ড",
        "t" to "ত", "d" to "দ", "p" to "প", "b" to "ব", "m" to "ম",
        "r" to "র", "l" to "ল", "S" to "ষ", "s" to "স", "h" to "হ",
        "R" to "ড়", "y" to "য়", "w" to "ও", "z" to "য", "ng" to "ং", 
        "NG" to "ঁ", "t`" to "ৎ"
    )

    // Check if the substring is a vowel sound
    private fun isVowel(str: String): Boolean {
        return STANDALONE_VOWELS.containsKey(str) || str == "a" || str == "o" || str == "e" || str == "i" || str == "u"
    }

    /**
     * Translates a Romanized phonetic string into Bangla.
     */
    fun parse(input: String): String {
        if (input.isEmpty()) return ""
        
        val result = StringBuilder()
        var i = 0
        val len = input.length
        
        var lastWasConsonant = false
        var lastConsonantBengali = ""

        while (i < len) {
            // Find greedy matches
            var matched = false
            
            // 1. Try 3-character compounds (like "rri", "chh")
            if (i + 3 <= len) {
                val sub = input.substring(i, i + 3)
                if (CONSONANTS.containsKey(sub)) {
                    val bangla = CONSONANTS[sub]!!
                    if (lastWasConsonant) {
                        result.append("\u09CD") // append Hasant to join conjunct
                    }
                    result.append(bangla)
                    lastConsonantBengali = bangla
                    lastWasConsonant = true
                    i += 3
                    matched = true
                } else if (STANDALONE_VOWELS.containsKey(sub)) {
                    val vowel = if (lastWasConsonant) VOWEL_MODIFIERS[sub] ?: "" else STANDALONE_VOWELS[sub]!!
                    result.append(vowel)
                    lastWasConsonant = false
                    i += 3
                    matched = true
                }
            }
            
            // 2. Try 2-character compounds (like "sh", "th", "kh", "oi", "ou")
            if (!matched && i + 2 <= len) {
                val sub = input.substring(i, i + 2)
                if (CONSONANTS.containsKey(sub)) {
                    val bangla = CONSONANTS[sub]!!
                    if (lastWasConsonant) {
                        result.append("\u09CD") // Hasant
                    }
                    result.append(bangla)
                    lastConsonantBengali = bangla
                    lastWasConsonant = true
                    i += 2
                    matched = true
                } else if (STANDALONE_VOWELS.containsKey(sub)) {
                    val vowel = if (lastWasConsonant) VOWEL_MODIFIERS[sub] ?: "" else STANDALONE_VOWELS[sub]!!
                    result.append(vowel)
                    lastWasConsonant = false
                    i += 2
                    matched = true
                }
            }
            
            // 3. Try 1-character match
            if (!matched) {
                val sub = input.substring(i, i + 1)
                if (CONSONANTS.containsKey(sub)) {
                    val bangla = CONSONANTS[sub]!!
                    if (lastWasConsonant) {
                        result.append("\u09CD") // Hasant
                    }
                    result.append(bangla)
                    lastConsonantBengali = bangla
                    lastWasConsonant = true
                    i += 1
                } else if (STANDALONE_VOWELS.containsKey(sub) || sub == "a" || sub == "o" || sub == "e" || sub == "i" || sub == "u") {
                    val vowel = if (lastWasConsonant) VOWEL_MODIFIERS[sub] ?: "" else STANDALONE_VOWELS[sub] ?: sub
                    result.append(vowel)
                    lastWasConsonant = false
                    i += 1
                } else {
                    // Non-phonetic key like numbers, punctuation
                    result.append(sub)
                    lastWasConsonant = false
                    i += 1
                }
            }
        }
        
        return result.toString()
    }
}
