package com.googamaphone.typeandspeak

import java.util.Locale

import android.content.Context
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import android.widget.ImageView
import android.widget.TextView

/**
 * An implementation of [ArrayAdapter] that displays locales with their
 * proper display names and flags.
 */
class LanguageAdapter(context: Context, layoutId: Int, private val textId: Int, private val imageId: Int) : ArrayAdapter<Locale>(context, layoutId, textId) {

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getView(position, convertView, parent)

        setFlagDrawable(position, view)

        return view
    }

    override fun getDropDownView(position: Int, convertView: View?, parent: ViewGroup): View {
        val view = super.getDropDownView(position, convertView, parent)

        setFlagDrawable(position, view)

        return view
    }

    /**
     * Sets the flag for the specified view and locale.
     *
     * @param position The position of the locale within the adapter.
     * @param view The view that represents the locale.
     */
    private fun setFlagDrawable(position: Int, view: View) {
        val locale = getItem(position)
        val drawableId = getFlagForLocale(locale)

        val textView = view.findViewById<TextView>(textId)
        val displayName = getDisplayNameForLocale(context, locale)
        textView.text = displayName

        val imageView = view.findViewById<ImageView>(imageId)
        if (drawableId <= 0) {
            imageView.visibility = View.GONE
        } else {
            imageView.setImageResource(drawableId)
            imageView.visibility = View.VISIBLE
        }
    }

    companion object {
        val LOCALE_ADD_MORE = Locale("addmore")

        private fun getDisplayNameForLocale(context: Context, locale: Locale?): CharSequence {
            return if (LOCALE_ADD_MORE == locale) {
                context.getString(R.string.add_more)
            } else locale!!.displayName

        }

        /**
         * Returns the drawable identifier for the flag associated specified locale.
         * If the locale does not have a flag, returns the drawable identifier for
         * the default flag.
         *
         * @param locale A locale.
         * @return The drawable identifier for the locale's flag.
         */
        private fun getFlagForLocale(locale: Locale?): Int {
            if (LOCALE_ADD_MORE == locale) {
                return -1
            }

            val language = locale!!.isO3Language
            val country = locale.isO3Country

            // First, check for country code.
            when {
                "usa".equals(country, ignoreCase = true) -> return R.drawable.united_states
                "ita".equals(country, ignoreCase = true) -> return R.drawable.italy
                "deu".equals(country, ignoreCase = true) -> return R.drawable.germany
                "gbr".equals(country, ignoreCase = true) -> return R.drawable.united_kingdom
                "fra".equals(country, ignoreCase = true) -> return R.drawable.france
                "chn".equals(country, ignoreCase = true) -> return R.drawable.china
                "twn".equals(country, ignoreCase = true) -> return R.drawable.taiwan
                "jpn".equals(country, ignoreCase = true) -> return R.drawable.japan
                "spa".equals(country, ignoreCase = true) -> return R.drawable.spain
                "mex".equals(country, ignoreCase = true) -> return R.drawable.mexico
                "kor".equals(country, ignoreCase = true) -> return R.drawable.korea

                // Next, check for language code.
                Locale.ENGLISH.isO3Language.equals(language, ignoreCase = true) -> return R.drawable.united_kingdom
                Locale.GERMAN.isO3Language.equals(language, ignoreCase = true) -> return R.drawable.germany
                Locale.FRENCH.isO3Language.equals(language, ignoreCase = true) -> return R.drawable.france
                Locale.ITALIAN.isO3Language.equals(language, ignoreCase = true) -> return R.drawable.italy
                Locale.CHINESE.isO3Language.equals(language, ignoreCase = true) -> return R.drawable.china
                Locale.JAPANESE.isO3Language.equals(language, ignoreCase = true) -> return R.drawable.japan
                Locale.KOREAN.isO3Language.equals(language, ignoreCase = true) -> return R.drawable.korea
                "spa".equals(language, ignoreCase = true) -> return R.drawable.spain
                else -> return R.drawable.unknown
            }

        }
    }
}
