package org.oriundo

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform