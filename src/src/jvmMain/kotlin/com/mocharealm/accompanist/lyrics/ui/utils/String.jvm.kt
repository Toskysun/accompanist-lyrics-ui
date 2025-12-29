package com.mocharealm.accompanist.lyrics.ui.utils

private val cjkBlock = listOf(
    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS,
    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_A,
    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_B,
    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_C,
    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_D,
    Character.UnicodeBlock.CJK_COMPATIBILITY_IDEOGRAPHS,
    Character.UnicodeBlock.CJK_SYMBOLS_AND_PUNCTUATION,
    Character.UnicodeBlock.HIRAGANA,
    Character.UnicodeBlock.KATAKANA,
    Character.UnicodeBlock.HANGUL_SYLLABLES,
    Character.UnicodeBlock.HANGUL_JAMO,
    Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO,
    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E,
    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F,
    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_G,
    Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_H
)

private val arabicBlock = mutableListOf(
    Character.UnicodeBlock.ARABIC,
    Character.UnicodeBlock.ARABIC_SUPPLEMENT,
    Character.UnicodeBlock.ARABIC_EXTENDED_A,
    Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A,
    Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B,
    Character.UnicodeBlock.ARABIC_EXTENDED_B
)

private val devanagariBlock = listOf(
    Character.UnicodeBlock.DEVANAGARI,
    Character.UnicodeBlock.DEVANAGARI_EXTENDED
)

actual fun Char.isCjk(): Boolean {
    return Character.UnicodeBlock.of(this) in cjkBlock
}

actual fun Char.isArabic(): Boolean {
    return Character.UnicodeBlock.of(this) in arabicBlock
}

actual fun Char.isDevanagari(): Boolean {
    return Character.UnicodeBlock.of(this) in devanagariBlock
}