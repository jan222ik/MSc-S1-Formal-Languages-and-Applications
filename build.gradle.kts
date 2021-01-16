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


dependencies {
    implementation(kotlin("stdlib"))
    compile(files("$buildDir/classes/java/main/"))
    testCompile("junit", "junit", "4.12")
}
