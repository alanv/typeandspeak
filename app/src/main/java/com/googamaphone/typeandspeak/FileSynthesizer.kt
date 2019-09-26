package com.googamaphone.typeandspeak

import android.app.Dialog
import android.app.ProgressDialog
import android.content.ContentValues
import android.content.Context
import android.content.DialogInterface.OnCancelListener
import android.os.Environment
import android.os.Message
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.MediaStore.Audio.Media
import android.provider.MediaStore.MediaColumns
import android.speech.tts.TextToSpeech
import android.speech.tts.TextToSpeech.Engine
import androidx.appcompat.app.AlertDialog.Builder

import com.googamaphone.typeandspeak.utils.ReferencedHandler

import java.io.File
import java.util.HashMap
import java.util.Locale

internal class FileSynthesizer(private val context: Context, private val tts: TextToSpeech) {

    private val contentValues = ContentValues(10)
    private val speechParams = HashMap<String, String>()
    private val artistValue: String = context.getString(R.string.app_name)
    private val albumValue: String = context.getString(R.string.album_name)

    private var progressDialog: ProgressDialog? = null
    private var listener: FileSynthesizerListener? = null

    private var canceled = false

    private val handler = SynthesizerHandler(this)

    private val onUtteranceCompletedListener = TextToSpeech.OnUtteranceCompletedListener { utteranceId -> handler.obtainMessage(UTTERANCE_COMPLETED, utteranceId).sendToTarget() }

    private val onCancelListener = OnCancelListener {
        canceled = true
        tts.stop()
    }

    init {

        speechParams[Engine.KEY_PARAM_UTTERANCE_ID] = UTTERANCE_ID
    }

    fun setListener(listener: FileSynthesizerListener) {
        this.listener = listener
    }

    private fun onUtteranceCompleted() {
        tts.setOnUtteranceCompletedListener(null)

        if (canceled) {
            onWriteCanceled()
        } else {
            onWriteCompleted()
        }
    }

    /**
     * Inserts media information into the database after a successful save
     * operation.
     */
    private fun onWriteCompleted() {
        val resolver = context.contentResolver
        val path = contentValues.getAsString(MediaColumns.DATA)
        val uriForPath = Media.getContentUriForPath(path)

        resolver.insert(uriForPath!!, contentValues)

        // Clears last queue element to avoid deletion on exit.
        tts.speak("", TextToSpeech.QUEUE_FLUSH, null)

        try {
            if (progressDialog!!.isShowing) {
                progressDialog!!.dismiss()
            }
        } catch (e: IllegalArgumentException) {
            e.printStackTrace()
        }

        if (listener != null) {
            listener!!.onFileSynthesized(contentValues)
        }

        contentValues.clear()
    }

    /**
     * Deletes the partially completed file after a canceled save operation.
     */
    private fun onWriteCanceled() {
        try {
            val path = contentValues.getAsString(MediaColumns.DATA)
            File(path).delete()
        } catch (e: Exception) {
            e.printStackTrace()
        }

        val title = context.getString(R.string.canceled_title)
        val message = context.getString(R.string.canceled_message)
        val alert = Builder(context).setTitle(title).setMessage(message)
                .setPositiveButton(android.R.string.ok, null).create()

        tts.stop()

        try {
            alert.show()
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }

        contentValues.clear()
    }

    fun writeInput(text: String, locale: Locale?, pitch: Int, rate: Int, filename: String) {
        var outputFilename = filename
        canceled = false

        if (outputFilename.toLowerCase().endsWith(".wav")) {
            outputFilename = outputFilename.substring(0, outputFilename.length - 4)
        }

        outputFilename = outputFilename.trim { it <= ' ' }

        if (outputFilename.isEmpty()) {
            return
        }

        val directory = Environment.getExternalStorageDirectory().path + "/typeandspeak"

        val outdir = File(directory)
        val outfile = File("$directory/$outputFilename.wav")

        val message: String
        val alert: Dialog

        if (outfile.exists()) {
            message = context.getString(R.string.exists_message, outputFilename)
            alert = Builder(context).setTitle(R.string.exists_title).setMessage(message)
                    .setPositiveButton(android.R.string.ok, null).create()
        } else if (!outdir.exists() && !outdir.mkdirs()) {
            message = context.getString(R.string.no_write_message, outputFilename)
            alert = Builder(context).setTitle(R.string.no_write_title).setMessage(message)
                    .setPositiveButton(android.R.string.ok, null).create()
        } else {
            // Attempt to set the locale.
            if (locale != null) {
                tts.language = locale
            }

            // Populate content values for the media provider.
            contentValues.clear()
            contentValues.put(MediaColumns.DISPLAY_NAME, outputFilename)
            contentValues.put(MediaColumns.TITLE, outputFilename)
            contentValues.put(AudioColumns.ARTIST, artistValue)
            contentValues.put(AudioColumns.ALBUM, albumValue)
            contentValues.put(AudioColumns.IS_ALARM, true)
            contentValues.put(AudioColumns.IS_RINGTONE, true)
            contentValues.put(AudioColumns.IS_NOTIFICATION, true)
            contentValues.put(AudioColumns.IS_MUSIC, true)
            contentValues.put(MediaColumns.MIME_TYPE, "audio/wav")
            contentValues.put(MediaColumns.DATA, outfile.absolutePath)

            tts.setPitch(pitch / 50.0f)
            tts.setSpeechRate(rate / 50.0f)
            tts.setOnUtteranceCompletedListener(onUtteranceCompletedListener)
            tts.synthesizeToFile(text, speechParams, outfile.absolutePath)

            message = context.getString(R.string.saving_message, outputFilename)

            progressDialog = ProgressDialog(context)
            progressDialog!!.setCancelable(true)
            progressDialog!!.setTitle(R.string.saving_title)
            progressDialog!!.setMessage(message)
            progressDialog!!.isIndeterminate = true
            progressDialog!!.setOnCancelListener(onCancelListener)

            alert = progressDialog as ProgressDialog
        }

        try {
            alert.show()
        } catch (e: RuntimeException) {
            e.printStackTrace()
        }

    }

    internal class SynthesizerHandler(parent: FileSynthesizer) : ReferencedHandler<FileSynthesizer>(parent) {

        override fun handleMessage(msg: Message, parent: FileSynthesizer) {
            when (msg.what) {
                UTTERANCE_COMPLETED -> parent.onUtteranceCompleted()
            }
        }
    }

    interface FileSynthesizerListener {
        fun onFileSynthesized(contentValues: ContentValues)
    }

    companion object {
        private const val UTTERANCE_ID = "synthesize"
        private const val UTTERANCE_COMPLETED = 1
    }
}
