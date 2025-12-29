package com.mocharealm.accompanist.lyrics.ui.utils

import android.os.Build

actual fun Char.isCjk(): Boolean {
    val cjkBlock = mutableListOf(
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
        Character.UnicodeBlock.HANGUL_COMPATIBILITY_JAMO
    )
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        cjkBlock.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_E)
        cjkBlock.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_F)
        cjkBlock.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_G)
    }
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        cjkBlock.add(Character.UnicodeBlock.CJK_UNIFIED_IDEOGRAPHS_EXTENSION_H)
    }
    return Character.UnicodeBlock.of(this) in cjkBlock
}

actual fun Char.isArabic(): Boolean {
    val arabicBlock = mutableListOf(
        Character.UnicodeBlock.ARABIC,
        Character.UnicodeBlock.ARABIC_SUPPLEMENT,
        Character.UnicodeBlock.ARABIC_EXTENDED_A,
        Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_A,
        Character.UnicodeBlock.ARABIC_PRESENTATION_FORMS_B
    )

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.BAKLAVA) {
        arabicBlock.add(Character.UnicodeBlock.ARABIC_EXTENDED_B)
    }

    return Character.UnicodeBlock.of(this) in arabicBlock
}

actual fun Char.isDevanagari(): Boolean {
    val devanagariBlock = listOf(
        Character.UnicodeBlock.DEVANAGARI,
        Character.UnicodeBlock.DEVANAGARI_EXTENDED
    )

    return Character.UnicodeBlock.of(this) in devanagariBlock
}