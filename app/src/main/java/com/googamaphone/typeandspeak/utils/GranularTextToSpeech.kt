package com.googamaphone.typeandspeak.utils

import java.text.BreakIterator
import java.util.HashMap
import java.util.Locale

import android.os.Message
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.Engine
import android.speech.tts.TextToSpeech.OnUtteranceCompletedListener
import android.text.TextUtils

/**
 * A wrapper class for [TextToSpeech] that adds support for reading at a
 * given granularity level using a [BreakIterator].
 */
class GranularTextToSpeech private constructor(private val tts: TextToSpeechStub, defaultLocale: Locale?) {

    private val charSequenceIterator = CharSequenceIterator(null)
    private val params: HashMap<String, String> = HashMap()

    private var breakIterator: BreakIterator? = null
    private var listener: SingAlongListener? = null
    private var currentSequence: CharSequence? = null

    private var unitEnd = 0
    private var unitStart = 0

    private var paused = false

    /**
     * Flag that lets the utterance completion listener know whether to advance
     * automatically. Automatically resets after each completed utterance.
     */
    private var bypassAdvance = false

    val isSpeaking: Boolean
        get() = currentSequence != null

    private val handler = SingAlongHandler(this)

    private val onUtteranceCompletedListener = OnUtteranceCompletedListener { utteranceId -> handler.obtainMessage(UTTERANCE_COMPLETED, utteranceId).sendToTarget() }

    constructor(tts: TextToSpeech, defaultLocale: Locale?) : this(TextToSpeechWrapper(tts), defaultLocale)

    init {
        params[Engine.KEY_PARAM_UTTERANCE_ID] = "SingAlongTTS"

        breakIterator = if (defaultLocale != null) {
            BreakIterator.getSentenceInstance(defaultLocale)
        } else {
            BreakIterator.getSentenceInstance(Locale.US)
        }
    }

    fun setListener(listener: SingAlongListener) {
        this.listener = listener
    }

    fun setLocale(locale: Locale) {
        breakIterator = BreakIterator.getSentenceInstance(locale)

        // Reset the text since we had to recreate the break iterator.
        setText(currentSequence)
    }

    fun speak() {
        pause()

        tts.setOnUtteranceCompletedListener(onUtteranceCompletedListener)

        if (listener != null) {
            listener!!.onSequenceStarted()
        }

        resume()
    }

    fun setText(text: CharSequence?) {
        currentSequence = text
        unitStart = 0
        unitEnd = 0
        charSequenceIterator.setCharSequence(currentSequence)
        breakIterator!!.text = charSequenceIterator
    }

    fun pause() {
        paused = true
        tts.stop()
    }

    fun resume() {
        paused = false
        onUtteranceCompleted()
    }

    operator fun next() {
        nextInternal()
        bypassAdvance = !paused
        tts.stop()
    }

    fun previous() {
        previousInternal()
        bypassAdvance = !paused
        tts.stop()
    }

    fun setSegmentFromCursor(cursor: Int) {
        var nextCursor = cursor
        if (nextCursor >= currentSequence!!.length || nextCursor < 0) {
            nextCursor = 0
        }

        if (safeIsBoundary(breakIterator!!, nextCursor)) {
            unitStart = breakIterator!!.current()
            safeFollowing(breakIterator!!, nextCursor)
            unitEnd = breakIterator!!.current()
        } else {
            unitEnd = breakIterator!!.current()
            safePreceding(breakIterator!!, nextCursor)
            unitStart = breakIterator!!.current()
        }

        bypassAdvance = true

        if (listener != null) {
            listener!!.onUnitSelected(unitStart, unitEnd)
        }
    }

    fun stop() {
        paused = true

        tts.stop()
        tts.setOnUtteranceCompletedListener(null)

        if (listener != null) {
            listener!!.onSequenceCompleted()
        }

        setText(null)

        unitStart = 0
        unitEnd = 0
    }

    /**
     * Move the break iterator forward by one unit. If the cursor is in the
     * middle of a unit, it will move to the next unit.
     *
     * @return `true` if the iterator moved forward or `false` if it
     * already at the last unit.
     */
    private fun nextInternal(): Boolean {
        do {
            val result = safeFollowing(breakIterator!!, unitEnd)

            if (result == BreakIterator.DONE) {
                return false
            }

            unitStart = unitEnd
            unitEnd = breakIterator!!.current()
        } while (isWhitespace(currentSequence!!.subSequence(unitStart, unitEnd)))

        if (listener != null) {
            listener!!.onUnitSelected(unitStart, unitEnd)
        }

        return true
    }

    /**
     * Move the break iterator backward by one unit. If the cursor is in the
     * middle of a unit, it will move to the beginning of the unit.
     *
     * @return `true` if the iterator moved backward or `false` if
     * it already at the first unit.
     */
    private fun previousInternal(): Boolean {
        do {
            val result = safePreceding(breakIterator!!, unitStart)

            if (result == BreakIterator.DONE) {
                return false
            }

            unitEnd = unitStart
            unitStart = breakIterator!!.current()
        } while (isWhitespace(currentSequence!!.subSequence(unitStart, unitEnd)))

        if (listener != null) {
            listener!!.onUnitSelected(unitStart, unitEnd)
        }

        return true
    }

    private fun onUtteranceCompleted() {
        if (currentSequence == null) {
            // Shouldn't be speaking now.
            return
        }

        if (paused) {
            // Don't move to the next segment if paused.
            return
        }

        if (bypassAdvance) {
            bypassAdvance = false
        } else if (!nextInternal()) {
            stop()
            return
        }

        speakCurrentUnit()
    }

    private fun speakCurrentUnit() {
        if (currentSequence!!.isEmpty()) {
            return
        }

        sanityCheck()

        val text = currentSequence!!.subSequence(unitStart, unitEnd)
        tts.speak(text.toString(), TextToSpeech.QUEUE_FLUSH, params)
    }

    private fun sanityCheck() {
        val length = currentSequence!!.length

        if (unitStart < 0 || unitStart >= currentSequence!!.length) {
            throw IndexOutOfBoundsException("Unit start (" + unitStart
                    + ") is invalid for string with length " + length)
        } else if (unitEnd < 0 || unitEnd > currentSequence!!.length) {
            throw IndexOutOfBoundsException("Unit end (" + unitEnd
                    + ") is invalid for string with length" + length)
        }
    }

    internal class SingAlongHandler(parent: GranularTextToSpeech) : ReferencedHandler<GranularTextToSpeech>(parent) {

        override fun handleMessage(msg: Message, parent: GranularTextToSpeech) {
            when (msg.what) {
                UTTERANCE_COMPLETED -> parent.onUtteranceCompleted()
                RESUME_SPEAKING -> parent.resume()
            }
        }
    }

    interface TextToSpeechStub {
        fun setOnUtteranceCompletedListener(
                mOnUtteranceCompletedListener: OnUtteranceCompletedListener?)

        fun speak(string: String, queueFlush: Int, mParams: HashMap<String, String>): Int

        fun stop()
    }

    internal class TextToSpeechWrapper(private val mTts: TextToSpeech) : TextToSpeechStub {

        override fun setOnUtteranceCompletedListener(listener: OnUtteranceCompletedListener?) {
            mTts.setOnUtteranceCompletedListener(listener)
        }

        override fun speak(text: String, queueMode: Int, params: HashMap<String, String>): Int {
            return mTts.speak(text, queueMode, params)
        }

        override fun stop() {
            mTts.stop()
        }
    }

    interface SingAlongListener {
        fun onSequenceStarted()

        fun onUnitSelected(start: Int, end: Int)

        fun onSequenceCompleted()
    }

    companion object {
        private const val UTTERANCE_COMPLETED = 1
        private const val RESUME_SPEAKING = 2

        private fun isWhitespace(text: CharSequence): Boolean {
            return TextUtils.getTrimmedLength(text) == 0
        }

        private fun safeIsBoundary(iterator: BreakIterator, offset: Int): Boolean {
            return try {
                iterator.isBoundary(offset)
            } catch (e: IllegalArgumentException) {
                false
            }
        }

        private fun safeFollowing(iterator: BreakIterator, offset: Int): Int {
            return try {
                iterator.following(offset)
            } catch (e: IllegalArgumentException) {
                BreakIterator.DONE
            }
        }

        private fun safePreceding(iterator: BreakIterator, offset: Int): Int {
            return try {
                iterator.preceding(offset)
            } catch (e: IllegalArgumentException) {
                BreakIterator.DONE
            }
        }
    }
}
