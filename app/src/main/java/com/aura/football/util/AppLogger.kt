package com.aura.football.util

import com.aura.football.BuildConfig

object AppLogger {

    fun d(tag: String, message: String) {
        if (!BuildConfig.DEBUG) return
        log("d", tag, message, null)
    }

    fun w(tag: String, message: String, throwable: Throwable? = null) {
        log("w", tag, message, throwable)
    }

    fun e(tag: String, message: String, throwable: Throwable? = null) {
        log("e", tag, message, throwable)
    }

    private fun log(level: String, tag: String, message: String, throwable: Throwable?) {
        runCatching {
            val logClass = Class.forName("android.util.Log")
            val methodName = when (level) {
                "d" -> "d"
                "w" -> "w"
                else -> "e"
            }

            val method = if (throwable == null) {
                logClass.getMethod(methodName, String::class.java, String::class.java)
            } else {
                logClass.getMethod(
                    methodName,
                    String::class.java,
                    String::class.java,
                    Throwable::class.java
                )
            }

            if (throwable == null) {
                method.invoke(null, tag, message)
            } else {
                method.invoke(null, tag, message, throwable)
            }
        }
    }
}
