package com.googamaphone.typeandspeak

import com.googamaphone.typeandspeak.utils.LogUtils

import android.content.Intent
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.Engine
import android.util.Log

import java.io.File
import java.util.Comparator
import java.util.Locale
import java.util.TreeSet

internal object TextToSpeechUtils {

    private val LOCALE_COMPARATOR = Comparator<Locale> { lhs, rhs -> lhs.displayName.compareTo(rhs.displayName) }

    fun loadTtsLanguages(tts: TextToSpeech, data: Intent?): Set<Locale> {
        if (data == null) {
            LogUtils.log(TextToSpeechUtils::class.java, Log.ERROR, "Received null intent")
            return emptySet()
        }

        val availableLangs = TreeSet(LOCALE_COMPARATOR)

        return if (getAvailableVoicesICS(availableLangs, data)
                || getAvailableVoicesFallback(availableLangs, data)
                || getAvailableVoicesBruteForce(availableLangs, tts)) {
            availableLangs
        } else emptySet()

    }

    private fun getAvailableVoicesICS(supportedLocales: TreeSet<Locale>, intent: Intent): Boolean {
        val availableLangs = intent
                .getStringArrayListExtra(Engine.EXTRA_AVAILABLE_VOICES)
                ?: return false

        for (availableLang in availableLangs) {
            val locale = parseLocale(availableLang) ?: continue

            supportedLocales.add(locale)
        }

        return !supportedLocales.isEmpty()
    }

    @Suppress("DEPRECATION")
    private fun getAvailableVoicesFallback(langsList: TreeSet<Locale>, extras: Intent): Boolean {
        val root = extras.getStringExtra(Engine.EXTRA_VOICE_DATA_ROOT_DIRECTORY)
        val files = extras.getStringArrayExtra(Engine.EXTRA_VOICE_DATA_FILES)
        val langs = extras.getStringArrayExtra(Engine.EXTRA_VOICE_DATA_FILES_INFO)
        if (root == null || files == null || langs == null) {
            LogUtils.log(TextToSpeechUtils::class.java, Log.ERROR, "Missing data on available voices")
            return false
        }

        for (i in files.indices) {
            val file = File(root, files[i])
            if (!file.canRead()) {
                LogUtils.log(TextToSpeechUtils::class.java, Log.ERROR,
                        "Cannot read file for " + langs[i])
                continue
            }

            val locale = parseLocale(langs[i])
            if (locale == null) {
                LogUtils.log(TextToSpeechUtils::class.java, Log.ERROR,
                        "Failed to parse locale for " + langs[i])
                continue
            }

            langsList.add(locale)
        }

        return !langsList.isEmpty()
    }

    private fun getAvailableVoicesBruteForce(langsList: TreeSet<Locale>, tts: TextToSpeech): Boolean {
        val systemLocales = Locale.getAvailableLocales()

        // Check every language supported by the system against the TTS.
        for (systemLocale in systemLocales) {
            val status = tts.isLanguageAvailable(systemLocale)
            if (status != TextToSpeech.LANG_AVAILABLE
                    && status != TextToSpeech.LANG_COUNTRY_AVAILABLE
                    && status != TextToSpeech.LANG_COUNTRY_VAR_AVAILABLE) {
                continue
            }

            langsList.add(systemLocale)
        }

        return !langsList.isEmpty()
    }

    private fun parseLocale(language: String): Locale? {
        val langCountryVariant = language.split("-".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()

        return when {
            langCountryVariant.size == 1 -> Locale(langCountryVariant[0])
            langCountryVariant.size == 2 -> Locale(langCountryVariant[0], langCountryVariant[1])
            langCountryVariant.size == 3 -> Locale(langCountryVariant[0], langCountryVariant[1], langCountryVariant[2])
            else -> null
        }
    }
}
