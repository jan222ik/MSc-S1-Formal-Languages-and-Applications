plugins {
    java
    kotlin("jvm") version "1.4.21"
    id("ca.coglinc.javacc").version("2.4.0")

}

group = "org.example"
version = "1.0-SNAPSHOT"

repositories {
    mavenCentral()
}

tasks.test {
    useJUnitPlatform()
    testLogging {
        events("passed", "skipped", "failed")
    }
}

dependencies {
    implementation(kotlin("stdlib"))
    compile(files("$buildDir/classes/java/main/"))
    testImplementation(platform("org.junit:junit-bom:5.7.0"))
    testImplementation("org.junit.jupiter:junit-jupiter")
}
