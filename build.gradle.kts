plugins {
    id("java")
}
val deployDirectory = "C:\\Users\\Andrew\\Documents\\DoomPvP\\plugins"

group = "com.golfing8"
version = "0.0.1-SNAPSHOT"

repositories {
    mavenCentral()
    mavenLocal()
}

dependencies {
    testImplementation(platform("org.junit:junit-bom:5.9.1"))
    testImplementation("org.junit.jupiter:junit-jupiter")

    annotationProcessor("org.projectlombok:lombok:1.18.24")
    compileOnly("org.projectlombok:lombok:1.18.24")

    compileOnly("org.jetbrains:annotations:24.1.0")
    compileOnly("net.techcable.tacospigot:WineSpigot:1.8.8-R0.2-SNAPSHOT")
    compileOnly(group = "com.golfing8", name = "KCommon", version = "1.0").isChanging = true
}

tasks.create("deploy") {
    dependsOn(tasks.jar)

    doFirst {
        val outputFile = tasks.getByName("jar").outputs.files.first()
        val targetFile = File(deployDirectory, outputFile.name)

        outputFile.copyTo(targetFile, overwrite = true)
    }
}

tasks.test {
    useJUnitPlatform()
}