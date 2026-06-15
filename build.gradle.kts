plugins {
    id("java")
    id("application")
}

group = "com.vg"
version = "1.0.0-SNAPSHOT"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

application {
    mainClass.set("com.vg.sandbox.VGShowcase")
    applicationDefaultJvmArgs = listOf("-Dfile.encoding=UTF-8")
}

tasks.register<JavaExec>("runVGShowcase") {
    group = "application"
    description = "Run the VG Framework Complete Feature Showcase"
    classpath = sourceSets["main"].runtimeClasspath
    mainClass.set("com.vg.sandbox.VGShowcase")
    jvmArgs("-Dfile.encoding=UTF-8")
}

repositories {
    mavenCentral()
}

val lwjglVersion = "3.3.4"
val lwjglNatives = "natives-windows"

dependencies {
    implementation(platform("org.lwjgl:lwjgl-bom:$lwjglVersion"))

    implementation("org.lwjgl:lwjgl")
    implementation("org.lwjgl:lwjgl-glfw")
    implementation("org.lwjgl:lwjgl-stb")
    implementation("org.lwjgl:lwjgl-vulkan")

    runtimeOnly("org.lwjgl:lwjgl::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-glfw::$lwjglNatives")
    runtimeOnly("org.lwjgl:lwjgl-stb::$lwjglNatives")
}

tasks.withType<JavaCompile> {
    options.compilerArgs.add("-Xlint:all")
}

tasks.withType<JavaExec> {
    workingDir = project.projectDir
}

tasks.withType<Test> {
    useJUnitPlatform()
}