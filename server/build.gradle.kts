plugins {
    alias(libs.plugins.kotlinJvm)
    alias(libs.plugins.ktor)
    alias(libs.plugins.kotlinSerialization)
}

group = "com.psdk.geetas"
version = "1.0.0"
application {
    mainClass = "com.psdk.geetas.ApplicationKt"
}

dependencies {
    api(project(":core"))
    implementation(libs.logback)
    implementation(libs.ktor.serverCore)
    implementation(libs.ktor.serverNetty)
    implementation(libs.ktor.serverContentNegotiation)
    implementation(libs.ktor.serializationKotlinxJson)
    testImplementation(libs.ktor.serverTestHost)
    testImplementation(libs.kotlin.testJunit)
    implementation("com.google.genai:google-genai-kotlin:1.6.0")
    implementation("org.jetbrains.kotlinx:kotlinx-serialization-json:1.12.0-RC")
}