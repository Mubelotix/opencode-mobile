package page.dera.opencode.api

import platform.Foundation.NSDate

actual fun epochSecondsNow(): Double = NSDate().timeIntervalSince1970
