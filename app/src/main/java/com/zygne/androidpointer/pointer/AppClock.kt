package com.zygne.androidpointer.pointer

object AppClock : Clock {

    private const val MS_IN_SECONDS = 1000

    override fun getTimeMillis(): Long {
        return System.currentTimeMillis()
    }

    override fun getTimeSeconds(): Long {
        return System.currentTimeMillis().div(MS_IN_SECONDS)
    }
}