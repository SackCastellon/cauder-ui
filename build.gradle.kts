import org.jetbrains.kotlin.gradle.tasks.KotlinCompile

plugins {
    kotlin("jvm") version "1.4.30"
    id("org.openjfx.javafxplugin") version "0.0.9"
    id("org.beryx.jlink") version "2.23.3"
}

group = "es.upv.mist"
version = "0.1"

repositories {
    mavenCentral()
    jcenter()
    maven("https://jitpack.io")
}

dependencies {
    implementation(kotlin("stdlib-jdk8"))
    implementation(kotlin("reflect"))
    implementation(files("libs/OtpErlang.jar"))
    implementation("com.github.ajalt.clikt:clikt:3.1.0")
    implementation("org.koin:koin-core:2.2.2")
    implementation("com.github.edvin:tornadofx2:master") {
        exclude("org.jetbrains.kotlin")
        exclude("org.openjfx")
    }

    testImplementation(kotlin("test-junit5"))
    testImplementation("org.junit.jupiter:junit-jupiter-api:5.6.0")
    testRuntimeOnly("org.junit.jupiter:junit-jupiter-engine:5.6.0")
}

tasks.test {
    useJUnitPlatform()
}

tasks.withType<KotlinCompile>() {
    kotlinOptions {
        jvmTarget = "15"
        freeCompilerArgs = listOf(
            "-Xopt-in=org.koin.core.component.KoinApiExtension",
            "-Xopt-in=kotlin.io.path.ExperimentalPathApi"
        )
    }
}

application {
    mainModule.set("cauder")
    mainClass.set("es.upv.mist.cauder.MainKt")
    applicationDefaultJvmArgs = listOf(
        "--add-opens=javafx.graphics/javafx.scene=tornadofx",
        "--add-opens=javafx.controls/javafx.scene.control=tornadofx"
    )
}

javafx {
    version = "15.0.1"
    modules = listOf("javafx.controls", "javafx.graphics", "javafx.fxml", "javafx.media", "javafx.swing", "javafx.web")
}

jlink {
    options.set(listOf("--strip-debug", "--compress", "2", "--no-header-files", "--no-man-pages"))

    launcher {
        jvmArgs = application.applicationDefaultJvmArgs.toList()
    }

    jpackage {
        val os = org.gradle.internal.os.OperatingSystem.current()
        val platformOptions = when {
            os.isWindows -> listOf("--win-per-user-install", "--win-dir-chooser", "--win-menu", "--win-shortcut")
            os.isMacOsX -> listOf("--mac-package-name", "cauder-ui")
            os.isLinux -> listOf("--linux-package-name", "cauder-ui", "--linux-shortcut")
            else -> error("Unexpected OS: $os")
        }

        installerOptions = listOf("--resource-dir", "src/main/resources") + platformOptions
    }
}
