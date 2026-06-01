package com.example.data

object KeyboardLayouts {

    val ENGLISH_ROWS = listOf(
        listOf("q", "w", "e", "r", "t", "y", "u", "i", "o", "p"),
        listOf("a", "s", "d", "f", "g", "h", "j", "k", "l"),
        listOf("SHIFT", "z", "x", "c", "v", "b", "n", "m", "DEL"),
        listOf("LANG", "VOICE", "SPACE", "CLIP", "AI", "ENTER")
    )

    val ENGLISH_SHIFT_ROWS = listOf(
        listOf("Q", "W", "E", "R", "T", "Y", "U", "I", "O", "P"),
        listOf("A", "S", "D", "F", "G", "H", "J", "K", "L"),
        listOf("SHIFT", "Z", "X", "C", "V", "B", "N", "M", "DEL"),
        listOf("LANG", "VOICE", "SPACE", "CLIP", "AI", "ENTER")
    )

    // Phonetic is romanized input, so it uses the same visual keys as English letters, but shows Bangla suggestions and transliterates typed keys.
    // Let's also define national and probhat keys for direct input!
    
    // Probhat standard keys
    val PROBHAT_ROWS = listOf(
        listOf("য়", "ড়", "ঢ", "ফ", "ত", "থ", "দ", "ধ", "ন", "প"),
        listOf("আ", "ই", "উ", "এ", "ও", "ক", "খ", "গ", "ঘ", "ঙ"),
        listOf("SHIFT", "চ", "ছ", "জ", "ঝ", "ট", "ঠ", "ড", "DEL"),
        listOf("LANG", "VOICE", "SPACE", "CLIP", "AI", "ENTER")
    )

    val PROBHAT_SHIFT_ROWS = listOf(
        listOf("্য", "া", "ি", "ী", "ু", "ূ", "ৃ", "ে", "ৈ", "ো"),
        listOf("ৌ", "্", "ং", "ঃ", "ঁ", "ল", "স", "শ", "ষ", "হ"),
        listOf("SHIFT", "অ", "উ", "ঋ", "ঞ", "ণ", "ত", "থ", "DEL"),
        listOf("LANG", "VOICE", "SPACE", "CLIP", "AI", "ENTER")
    )

    // Jatiya / National layout keys
    val NATIONAL_ROWS = listOf(
        listOf("ক", "খ", "গ", "ঘ", "ঙ", "চ", "ছ", "জ", "ঝ", "ঞ"),
        listOf("ট", "ঠ", "ড", "ঢ", "ণ", "ত", "th", "দ", "ধ", "ন"),
        listOf("SHIFT", "প", "ফ", "ব", "ভ", "ম", "য", "র", "DEL"),
        listOf("LANG", "VOICE", "SPACE", "CLIP", "AI", "ENTER")
    )

    val NATIONAL_SHIFT_ROWS = listOf(
        listOf("া", "ি", "ী", "ু", "ূ", "ে", "ৈ", "ো", "ৌ", "ৃ"),
        listOf("১", "২", "৩", "৪", "৫", "৬", "৭", "৮", "৯", "০"),
        listOf("SHIFT", "অ", "আ", "ই", "ঈ", "উ", "ঊ", "ঋ", "DEL"),
        listOf("LANG", "VOICE", "SPACE", "CLIP", "AI", "ENTER")
    )

    // Arabic standard layout keys
    val ARABIC_ROWS = listOf(
        listOf("ض", "ص", "ث", "ق", "ف", "غ", "ع", "ه", "خ", "ح"),
        listOf("ش", "س", "ي", "ب", "ل", "ا", "ت", "ن", "م", "ك"),
        listOf("SHIFT", "ئ", "ء", "ؤ", "ر", "لا", "ى", "ة", "DEL"),
        listOf("LANG", "VOICE", "SPACE", "CLIP", "AI", "ENTER")
    )

    val ARABIC_SHIFT_ROWS = listOf(
        listOf("َ", "ً", "ُ", "ٌ", "لإ", "إ", "أ", "آ", "ْ", "ّ"),
        listOf("ِ", "ٍ", "]", "[", "لأ", "أ", "ـ", "،", "/", "؛"),
        listOf("SHIFT", "ْ", "ّ", "َ", "ُ", "ِ", "آ", "؟", "DEL"),
        listOf("LANG", "VOICE", "SPACE", "CLIP", "AI", "ENTER")
    )

    // Chakma script keys (\u11100 to \u1114F)
    val CHAKMA_ROWS = listOf(
        listOf("\u11107", "\u11108", "\u11109", "\u1110A", "\u1110B", "\u1110C", "\u1110D", "\u1110E", "\u1110F", "\u11110"), // 𑄇-𑄐
        listOf("\u11111", "\u11112", "\u11113", "\u11114", "\u11115", "\u11116", "\u11117", "\u11118", "\u11119", "\u1111A"), // 𑄑-𑄚
        listOf("SHIFT", "\u1111B", "\u1111C", "\u1111D", "\u1111E", "\u1111F", "\u11120", "\u11121", "DEL"), // 𑄛-𑄡
        listOf("LANG", "VOICE", "SPACE", "CLIP", "AI", "ENTER")
    )

    val CHAKMA_SHIFT_ROWS = listOf(
        listOf("\u11127", "\u11128", "\u11129", "\u1112A", "\u1112B", "\u1112C", "\u1112D", "\u1112E", "\u1112F", "\u11130"), // modifiers
        listOf("\u11136", "\u11137", "\u11138", "\u11139", "\u1113A", "\u1113B", "\u1113C", "\u1113D", "\u1113E", "\u1113F"), // digits
        listOf("SHIFT", "\u11103", "\u11104", "\u11105", "\u11106", "\u11122", "\u11123", "\u11124", "DEL"),
        listOf("LANG", "VOICE", "SPACE", "CLIP", "AI", "ENTER")
    )

    val FULL_EMOJIS = listOf(
        "😀", "😃", "😄", "😁", "😆", "😅", "😂", "🤣", "😊", "😇",
        "🙂", "🙃", "😉", "😌", "😍", "🥰", "😘", "😗", "😙", "😚",
        "😋", "😛", "😝", "😜", "🤪", "🤨", "🧐", "🤓", "😎", "🤩",
        "🥳", "😏", "😒", "😞", "😔", "😟", "😕", "🙁", "☹️", "😣",
        "😖", "😫", "😩", "🥺", "😢", "😭", "😤", "😠", "😡", "🤬",
        "🤯", "😳", "🥵", "🥶", "😱", "😨", "😰", "😥", "😓", "🤗",
        "🤔", "🤭", "🤫", "🤥", "😶", "😐", "😑", "😬", "🙄", "😯",
        "😦", "😧", "😮", "😲", "🥱", "😴", "🤤", "😪", "😵", "🤐",
        "🥴", "🤢", "🤮", "🤧", "😷", "🤒", "🤕", "🤑", "🤠", "😈",
        "❤️", "🧡", "💛", "💚", "💙", "💜", "🖤", "🤍", "💖", "✨",
        "👍", "👎", "👊", "✊", "🤛", "🤜", "🤞", "✌️", "🤟", "🤘"
    )
}
