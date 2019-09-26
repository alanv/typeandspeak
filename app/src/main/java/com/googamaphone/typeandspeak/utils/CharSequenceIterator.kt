package com.googamaphone.typeandspeak.utils

import java.text.CharacterIterator

class CharSequenceIterator : CharacterIterator, Cloneable {

    private var charSequence: CharSequence? = null

    /** The current position.  */
    private var cursor: Int = 0

    private constructor(other: CharSequenceIterator) {
        charSequence = other.charSequence
        cursor = other.cursor
    }

    constructor(charSequence: CharSequence?) {
        this.charSequence = charSequence
        cursor = 0
    }

    fun setCharSequence(charSequence: CharSequence?) {
        this.charSequence = charSequence

        if (this.charSequence == null) {
            cursor = 0
        } else if (cursor > this.charSequence!!.length) {
            cursor = this.charSequence!!.length
        }
    }

    override fun clone(): Any {
        return CharSequenceIterator(this)
    }

    override fun getBeginIndex(): Int {
        return 0
    }

    override fun getEndIndex(): Int {
        return if (charSequence == null) {
            0
        } else charSequence!!.length

    }

    override fun getIndex(): Int {
        return cursor
    }

    override fun setIndex(location: Int): Char {
        require(!(cursor < beginIndex || cursor > endIndex)) { "Index out of bounds" }

        cursor = location

        return current()
    }

    override fun next(): Char {
        val nextIndex = index + 1

        return if (nextIndex > endIndex) {
            CharacterIterator.DONE
        } else setIndex(nextIndex)

    }

    override fun previous(): Char {
        val previousIndex = index - 1

        return if (previousIndex < beginIndex) {
            CharacterIterator.DONE
        } else setIndex(previousIndex)

    }

    override fun current(): Char {
        val index = index

        return if (index < beginIndex || index >= endIndex) {
            CharacterIterator.DONE
        } else charSequence!![getIndex()]

    }

    override fun first(): Char {
        return setIndex(beginIndex)
    }

    override fun last(): Char {
        val lastIndex = endIndex - 1

        return if (lastIndex < beginIndex) {
            CharacterIterator.DONE
        } else setIndex(lastIndex)

    }
}
