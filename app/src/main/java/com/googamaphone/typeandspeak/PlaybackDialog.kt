package com.googamaphone.typeandspeak

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Message
import androidx.appcompat.app.AlertDialog
import android.view.LayoutInflater
import android.view.View
import android.widget.ImageButton
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener

import com.googamaphone.typeandspeak.utils.ReferencedHandler

import java.io.File
import java.io.IOException

internal class PlaybackDialog(context: Context, private val mFromLibrary: Boolean) : AlertDialog(context) {
    private val mediaPlayer: MediaPlayer
    private val contentView: View
    private val progress: SeekBar
    private val playButton: ImageButton
    private val shareButton: ImageButton
    private val audioManager: AudioManager

    private var savedFile: File? = null

    private var advanceSeekBar: Boolean = false
    private var mediaPlayerReleased: Boolean = false
    private var mediaPlayerPrepared: Boolean = false

    private val poller = MediaPoller(this)

    private val onCompletionListener = MediaPlayer.OnCompletionListener { mp -> handleCompletion(mp) }

    private fun handleCompletion(mp : MediaPlayer) {
        manageAudioFocus(false)
        mp.seekTo(0)

        progress.progress = 0
        playButton.setImageResource(android.R.drawable.ic_media_play)
        poller.stopPolling()
    }

    private val mViewClickListener = View.OnClickListener { v -> handleClick(v) }

    private fun handleClick(v: View) {
        when (v.id) {
            R.id.play -> {
                val button = v as ImageButton

                if (!mediaPlayerPrepared) {
                    // The media player isn't ready yet, do nothing.
                } else if (mediaPlayer.isPlaying) {
                    button.setImageResource(android.R.drawable.ic_media_play)
                    mediaPlayer.pause()
                    manageAudioFocus(false)
                    poller.stopPolling()
                } else {
                    button.setImageResource(android.R.drawable.ic_media_pause)
                    mediaPlayer.start()
                    manageAudioFocus(true)
                    poller.startPolling()
                }
            }
            R.id.share -> {
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(savedFile))
                shareIntent.type = "audio/wav"

                val chooserIntent = Intent.createChooser(shareIntent,
                        context.getString(R.string.share_to))

                context.startActivity(chooserIntent)
            }
        }
    }

    private val mOnSeekBarChangeListener = object : OnSeekBarChangeListener {
        override fun onStartTrackingTouch(seekBar: SeekBar) {
            advanceSeekBar = false
        }

        override fun onStopTrackingTouch(seekBar: SeekBar) {
            advanceSeekBar = true
        }

        override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
            handleProgressChanged(progress, fromUser)
        }
    }

    private fun handleProgressChanged(progress: Int, fromUser: Boolean) {
        if (!mediaPlayerPrepared) {
            // The media player isn't ready yet, do nothing.
        } else if (fromUser) {
            mediaPlayer.seekTo(progress)
        }
    }

    private val mOnPreparedListener = MediaPlayer.OnPreparedListener { mediaPlayerPrepared = true }

    private val mDialogClickListener = DialogInterface.OnClickListener { _, which ->
        when (which) {
            DialogInterface.BUTTON_NEGATIVE -> {
                val context = getContext()
                val intent = Intent(context, LibraryActivity::class.java)
                context.startActivity(intent)
            }
        }
    }

    init {
        advanceSeekBar = true

        mediaPlayer = MediaPlayer()
        mediaPlayer.setOnPreparedListener(mOnPreparedListener)
        mediaPlayer.setOnCompletionListener(onCompletionListener)

        contentView = LayoutInflater.from(context).inflate(R.layout.playback, null)

        playButton = contentView.findViewById(R.id.play)
        playButton.setOnClickListener(mViewClickListener)

        shareButton = contentView.findViewById(R.id.share)
        shareButton.setOnClickListener(mViewClickListener)

        progress = contentView.findViewById(R.id.progress)
        progress.setOnSeekBarChangeListener(mOnSeekBarChangeListener)

        audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager

        if (mFromLibrary) {
            shareButton.visibility = View.GONE
        } else {
            setTitle(R.string.saved_title)
            setMessage(context.getString(R.string.saved_message))
            setButton(DialogInterface.BUTTON_NEGATIVE, context.getString(R.string.menu_library),
                    mDialogClickListener)
        }

        setButton(DialogInterface.BUTTON_POSITIVE, context.getString(android.R.string.ok),
                mDialogClickListener)
        setView(contentView)
    }

    public override fun onStop() {
        poller.stopPolling()
        mediaPlayer.release()

        mediaPlayerReleased = true

        manageAudioFocus(false)
    }

    private fun manageAudioFocus(gain: Boolean) {
        if (gain) {
            audioManager.requestAudioFocus(null,
                    AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN_TRANSIENT)
        } else {
            audioManager.abandonAudioFocus(null)
        }
    }

    @Throws(IOException::class)
    fun setFile(path: String) {
        if (mediaPlayerReleased) {
            throw IOException("Media player was already released!")
        }

        if (!mFromLibrary) {
            setMessage(context.getString(R.string.saved_message, path))
        }

        savedFile = File(path)

        mediaPlayer.setDataSource(savedFile!!.absolutePath)
        mediaPlayer.prepare()
    }

    internal class MediaPoller(parent: PlaybackDialog) : ReferencedHandler<PlaybackDialog>(parent) {

        private var mStopPolling: Boolean = false

        override fun handleMessage(msg: Message, parent: PlaybackDialog) {
            when (msg.what) {
                MSG_CHECK_PROGRESS -> if (!mStopPolling && parent.mediaPlayer.isPlaying) {
                    if (parent.advanceSeekBar) {
                        parent.progress.max = parent.mediaPlayer.duration
                        parent.progress.progress = parent.mediaPlayer.currentPosition
                    }

                    startPolling()
                }
            }
        }

        fun stopPolling() {
            mStopPolling = true
            removeMessages(MSG_CHECK_PROGRESS)
        }

        fun startPolling() {
            mStopPolling = false
            removeMessages(MSG_CHECK_PROGRESS)
            sendEmptyMessageDelayed(MSG_CHECK_PROGRESS, 200)
        }

        companion object {
            private const val MSG_CHECK_PROGRESS = 1
        }
    }
}
