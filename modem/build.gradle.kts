plugins {
    alias(libs.plugins.kotlin.jvm)
}

kotlin {
    jvmToolchain(17)
    explicitApi()
}

dependencies {
    testImplementation(libs.junit)
    testImplementation(libs.kotlinx.coroutines.test)
}

val checkModemPurity = tasks.register<app.shoutsend.buildtools.tasks.CheckModemPurityTask>("checkModemPurity") {
    sourceDir.set(file("src/main"))
}

tasks.named("test") {
    dependsOn(checkModemPurity)
}

val modemBenchmark = tasks.register<JavaExec>("modemBenchmark") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.shoutsend.modem.channel.BatchRunner")
    workingDir = rootDir
}

val modemPerf = tasks.register<JavaExec>("modemPerf") {
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("app.shoutsend.modem.perf.DspPerfRunner")
    workingDir = rootDir
}
