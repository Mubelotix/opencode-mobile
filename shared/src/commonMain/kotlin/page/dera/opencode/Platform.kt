package page.dera.opencode

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform