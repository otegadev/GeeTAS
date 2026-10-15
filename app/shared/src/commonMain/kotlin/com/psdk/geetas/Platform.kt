package com.psdk.geetas

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform