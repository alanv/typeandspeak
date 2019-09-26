package com.googamaphone.typeandspeak

import android.content.Context
import android.content.Intent
import android.database.Cursor
import android.net.Uri
import android.os.AsyncTask
import android.os.Bundle
import android.os.Environment
import android.provider.BaseColumns
import android.provider.MediaStore
import android.provider.MediaStore.Audio.AudioColumns
import android.provider.MediaStore.Audio.Media
import android.text.format.DateFormat
import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView.OnItemClickListener
import android.widget.CursorAdapter
import android.widget.TextView

import com.googamaphone.PinnedDialog
import com.googamaphone.PinnedDialogManager

import java.io.File
import java.io.IOException
import java.util.Date

class LibraryActivity : AppCompatListActivity() {

    private var cursorAdapter: CursorAdapter? = null

    class LibraryPinnedDialogManager(
            private val context: Context,
            private val onClickListener: View.OnClickListener,
            private val cursorAdapter: CursorAdapter?
    ) : PinnedDialogManager() {
        override fun onCreatePinnedDialog(id: Int): PinnedDialog? {
            when (id) {
                PINNED_ACTIONS -> {
                    val dialog = PinnedDialog(context)
                            .setContentView(R.layout.pinned_actions)
                    dialog.findViewById(R.id.delete).setOnClickListener(onClickListener)
                    dialog.findViewById(R.id.ringtone).setOnClickListener(onClickListener)
                    dialog.findViewById(R.id.share).setOnClickListener(onClickListener)
                    return dialog
                }
                PINNED_CONFIRM_DELETE -> {
                    val dialog = PinnedDialog(context)
                            .setContentView(R.layout.pinned_confirm_delete)
                    dialog.findViewById(R.id.confirm_delete).setOnClickListener(onClickListener)
                    dialog.findViewById(R.id.cancel_delete).setOnClickListener(onClickListener)
                    return dialog
                }
            }

            return super.onCreatePinnedDialog(id)
        }

        override fun onPreparePinnedDialog(id: Int, dialog: PinnedDialog?, arguments: Bundle?) {
            when (id) {
                PINNED_ACTIONS -> {
                    val position = arguments!!.getInt(KEY_POSITION)
                    dialog!!.findViewById(R.id.delete).setTag(R.id.tag_position, position)
                    dialog.findViewById(R.id.ringtone).setTag(R.id.tag_position, position)
                    dialog.findViewById(R.id.share).setTag(R.id.tag_position, position)
                }
                PINNED_CONFIRM_DELETE -> {
                    Exception().printStackTrace()
                    val position = arguments!!.getInt(KEY_POSITION)
                    dialog!!.findViewById(R.id.confirm_delete).setTag(R.id.tag_position, position)
                    dialog.findViewById(R.id.cancel_delete).setTag(R.id.tag_position, position)

                    val cursor = cursorAdapter!!.cursor
                    cursor.moveToPosition(position)

                    val titleIndex = cursor.getColumnIndex(AudioColumns.TITLE)
                    val title = cursor.getString(titleIndex)

                    (dialog.findViewById(R.id.message) as TextView).text = context.getString(
                            R.string.confirm_delete_message, title)
                }
                else -> {
                    super.onPreparePinnedDialog(id, dialog, arguments)
                }
            }
        }
    }

    private val onClickListener: View.OnClickListener = View.OnClickListener { v -> handleOnClick(v) }

    private fun handleOnClick(v: View) {
        val position = v.getTag(R.id.tag_position) as Int

        when (v.id) {
            R.id.more_button -> {
                val arguments = Bundle()
                arguments.putInt(KEY_POSITION, position)
                pinnedDialogManager.showPinnedDialog(PINNED_ACTIONS, v, arguments)
            }
            R.id.delete -> {
                val pinnedView = pinnedDialogManager.getPinnedView(PINNED_ACTIONS)

                pinnedDialogManager.dismissPinnedDialog(PINNED_ACTIONS)

                val arguments = Bundle()
                arguments.putInt(KEY_POSITION, position)
                pinnedDialogManager.showPinnedDialog(PINNED_CONFIRM_DELETE, pinnedView!!,
                        arguments)
            }
            R.id.ringtone -> {
                startActivity(Intent("android.settings.SOUND_SETTINGS"))
            }
            R.id.confirm_delete -> {
                pinnedDialogManager.dismissPinnedDialog(PINNED_CONFIRM_DELETE)

                val cursor = cursorAdapter!!.cursor
                cursor.moveToPosition(position)

                val dataIndex = cursor.getColumnIndex(AudioColumns.DATA)
                val dataPath = cursor.getString(dataIndex)

                if (File(dataPath).delete()) {
                    val idIndex = cursor.getColumnIndex(AudioColumns._ID)
                    val id = cursor.getLong(idIndex)
                    val uriForPath = Media.getContentUriForPath(dataPath)

                    contentResolver.delete(uriForPath!!, AudioColumns._ID + "=?", arrayOf("" + id))
                }

                requestCursor()
            }
            R.id.cancel_delete -> {
                pinnedDialogManager.dismissPinnedDialog(PINNED_CONFIRM_DELETE)
            }
            R.id.share -> {
                pinnedDialogManager.dismissPinnedDialog(PINNED_ACTIONS)

                val cursor = cursorAdapter!!.cursor
                cursor.moveToPosition(position)

                val columnIndex = cursor.getColumnIndex(AudioColumns.DATA)
                val dataPath = cursor.getString(columnIndex)
                val shareIntent = Intent()
                shareIntent.action = Intent.ACTION_SEND
                shareIntent.putExtra(Intent.EXTRA_STREAM, Uri.parse(dataPath))
                shareIntent.type = "audio/wav"

                val chooserIntent = Intent.createChooser(shareIntent,
                        getString(R.string.share_to))
                startActivity(chooserIntent)
            }
        }
    }

    private val pinnedDialogManager = LibraryPinnedDialogManager(this, onClickListener, cursorAdapter)

    private val onMoreClickListener = OnItemClickListener { _, view, position, _ ->
        when (view.id) {
            R.id.more_button -> {
                val arguments = Bundle()
                arguments.putInt(KEY_POSITION, position)
                pinnedDialogManager.showPinnedDialog(PINNED_ACTIONS, view, arguments)
            }
            R.id.central_block -> {
                val cursor = cursorAdapter!!.cursor
                val dataIndex = cursor.getColumnIndex(AudioColumns.DATA)

                cursor.moveToPosition(position)

                val data = cursor.getString(dataIndex)
                val playback = PlaybackDialog(this@LibraryActivity, true)

                try {
                    playback.setFile(data)
                    playback.show()
                } catch (e: IOException) {
                    e.printStackTrace()
                }

            }
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.library)

        SetupActionBar().run()

        requestCursor()
    }

    private fun requestCursor() {
        val loadMediaTask = object : LoadMediaFromAlbum(this) {
            override fun onPostExecute(result: Cursor) {
                cursorAdapter = LibraryCursorAdapter(this@LibraryActivity, result,
                        R.layout.library_item, FROM, TO)

                listView.adapter = cursorAdapter
            }
        }

        loadMediaTask.execute()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                val intent = Intent(this, TypeAndSpeak::class.java)
                intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                startActivity(intent)
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    internal inner class SetupActionBar : Runnable {
        override fun run() {
            supportActionBar!!.setDisplayHomeAsUpEnabled(true)
        }
    }

    internal inner class LibraryCursorAdapter(
            context: Context,
            c: Cursor,
            private val mLayoutId: Int,
            private val mFrom: Array<String>,
            private val mTo: IntArray
    ) : CursorAdapter(context, c, false) {

        init {
            require(mFrom.size == mTo.size) { "From and to arrays must be of equal length." }
        }

        override fun bindView(view: View, context: Context, cursor: Cursor) {
            val position = cursor.position
            val id = getItemId(position)

            for (i in mFrom.indices) {
                val columnIndex = cursor.getColumnIndex(mFrom[i])
                val value = cursor.getString(columnIndex)
                val textView = view.findViewById<TextView>(mTo[i])

                if (Media.DATE_ADDED == mFrom[i]) {
                    val longValue = java.lang.Long.parseLong(value)
                    val date = Date(longValue * 1000)
                    val dateFormat = DateFormat.getMediumDateFormat(context)
                    val timeFormat = DateFormat.getTimeFormat(context)
                    val formatted = context.getString(R.string.date_at_time,
                            dateFormat.format(date), timeFormat.format(date))

                    textView.text = formatted
                } else {
                    textView.text = value
                }
            }

            view.findViewById<View>(R.id.central_block).setOnClickListener { v -> onMoreClickListener.onItemClick(listView, v, position, id) }

            view.findViewById<View>(R.id.more_button).setOnClickListener { v -> onMoreClickListener.onItemClick(listView, v, position, id) }
        }

        override fun newView(context: Context, cursor: Cursor, parent: ViewGroup): View {
            val inflater = LayoutInflater.from(context)

            return inflater.inflate(mLayoutId, null)
        }
    }

    internal open class LoadMediaFromAlbum(private val mContext: Context) : AsyncTask<Void, Void, Cursor>() {

        public override fun doInBackground(vararg arg: Void): Cursor? {
            val album = mContext.getString(R.string.album_name)
            val resolver = mContext.contentResolver
            val directory = Environment.getExternalStorageDirectory().path + "/typeandspeak"

            val projection = arrayOf(BaseColumns._ID, AudioColumns.TITLE, AudioColumns.DATA, AudioColumns.DATE_ADDED)
            val selection = AudioColumns.ALBUM + "=? OR " + AudioColumns.DATA + " LIKE ?"
            val args = arrayOf(album, "$directory/%")

            return resolver.query(Media.EXTERNAL_CONTENT_URI, projection,
                    selection, args, AudioColumns.DATE_ADDED + " DESC")
        }
    }

    companion object {
        private val FROM = arrayOf(AudioColumns.TITLE, AudioColumns.DATE_ADDED)

        private val TO = intArrayOf(R.id.title, R.id.date)

        private const val PINNED_ACTIONS = 1
        private const val PINNED_CONFIRM_DELETE = 2

        private const val KEY_POSITION = "position"
    }
}
