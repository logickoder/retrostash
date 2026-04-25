package dev.logickoder.retrostash.example

import java.util.logging.Logger as DesktopLogger
import java.util.logging.Level
import kotlin.jvm.java

actual object Logger {
    private val logger: DesktopLogger = DesktopLogger.getLogger(Logger::class.java.name)

    init {
        logger.level = Level.FINE
    }

    actual fun e(
        tag: String,
        message: String,
        throwable: Throwable?
    ) {
        if (throwable != null) {
            logger.log(Level.SEVERE, "ERROR: [$tag] $message", throwable)
        } else {
            logger.severe("ERROR: [$tag] $message")
        }
    }

    actual fun d(tag: String, message: String) {
        logger.info("DEBUG: [$tag] $message")
    }

    actual fun i(tag: String, message: String) {
        logger.info("INFO: [$tag] $message")
    }
}