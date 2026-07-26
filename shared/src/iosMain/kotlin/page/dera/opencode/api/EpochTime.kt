package page.dera.opencode.api

import kotlinx.cinterop.ExperimentalForeignApi
import platform.posix.time

@OptIn(ExperimentalForeignApi::class)
actual fun epochSecondsNow(): Double = time(null).toDouble()
