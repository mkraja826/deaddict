plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
}

dependencies {
    implementation(project(":core:programs"))
    testImplementation(libs.junit)
}

tasks.test {
    useJUnit()
}
