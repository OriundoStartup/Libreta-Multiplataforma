package org.orinundo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform