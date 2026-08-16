import com.github.jengelman.gradle.plugins.shadow.tasks.ShadowJar

plugins {
    java
    id("com.gradleup.shadow") version "8.3.5"
    `maven-publish`
}

group = "com.alkacode"
version = "1.0.13"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(21))
    }
}

repositories {
    mavenLocal()
    mavenCentral()
    maven("https://repo.papermc.io/repository/maven-public/")
    maven("https://jitpack.io")
    maven("https://repo.extendedclip.com/content/repositories/placeholderapi/")
}

dependencies {
    compileOnly("io.papermc.paper:paper-api:1.21.8-R0.1-SNAPSHOT")
    compileOnly("me.clip:placeholderapi:2.11.6")
    // AlkaCore e necessario tambem porque AlkaEconomyPlugin agora estende
    // com.alkacode.core.plugin.AlkaPlugin (o javac precisa da hierarquia completa).
    compileOnly("com.alkacode:AlkaCore:1.0.3")
    compileOnly("com.alkacode:AlkaEconomy:1.0.5")
    // integracao soft - so escuta DropCollectedEvent se o plugin estiver instalado
    // (publicado via `./gradlew publishToMavenLocal` no projeto AlkaDrop).
    compileOnly("com.alkacode:AlkaDrop:1.0.1")

    implementation("org.xerial:sqlite-jdbc:3.46.1.3")
}

tasks.withType<JavaCompile> {
    options.encoding = "UTF-8"
    options.release.set(21)
}

tasks.named<ShadowJar>("shadowJar") {
    archiveClassifier.set("")
    relocate("org.sqlite", "com.alkacode.shop.libs.sqlite")
}

tasks.build {
    dependsOn(tasks.shadowJar)
}

tasks.processResources {
    filteringCharset = "UTF-8"
    // sem isso, o Gradle nao percebe que so `version` mudou e reusa o plugin.yml
    // antigo do cache (processResources fica UP-TO-DATE incorretamente).
    inputs.property("version", project.version)
    filesMatching("plugin.yml") {

        expand("version" to project.version)

    }
}

// publica o jar "puro" no repositorio Maven local, para o AlkaMines consumir
// AlkaShopAPI via compileOnly (integracao soft de auto-venda).
publishing {
    publications {
        create<MavenPublication>("maven") {
            groupId = project.group.toString()
            artifactId = "AlkaShop"
            version = project.version.toString()
            from(components["java"])
        }
    }
}
