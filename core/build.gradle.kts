plugins {
    alias(libs.plugins.kotlin.jvm)
    alias(libs.plugins.kotlin.serialization)
    jacoco
}

java {
    sourceCompatibility = JavaVersion.VERSION_17
    targetCompatibility = JavaVersion.VERSION_17
}

kotlin {
    compilerOptions {
        jvmTarget.set(org.jetbrains.kotlin.gradle.dsl.JvmTarget.JVM_17)
    }
}

dependencies {
    implementation(libs.kotlinx.serialization.json)
    testImplementation(libs.junit)
}

tasks.test {
    // Ilova ichidagi haqiqiy retseptlar bazasini testlarda tekshirish uchun
    systemProperty("assetsDir", rootProject.file("app/src/main/assets").absolutePath)
    finalizedBy(tasks.jacocoTestReport)
}

tasks.jacocoTestReport {
    dependsOn(tasks.test)
    reports {
        xml.required.set(true)
        html.required.set(true)
    }
}

tasks.register<JacocoCoverageVerification>("matcherCoverage") {
    dependsOn(tasks.test)
    executionData.setFrom(tasks.test.map { it.extensions.getByType<JacocoTaskExtension>().destinationFile!! })
    sourceDirectories.setFrom(files("src/main/kotlin"))
    classDirectories.setFrom(
        files(layout.buildDirectory.dir("classes/kotlin/main")).asFileTree.matching {
            include("uz/uydanimabor/core/Matcher*")
        }
    )
    violationRules {
        rule {
            limit {
                counter = "LINE"
                minimum = "0.80".toBigDecimal()
            }
        }
    }
}
tasks.check { dependsOn("matcherCoverage") }
