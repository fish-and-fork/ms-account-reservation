plugins {
    java
    id("org.springframework.boot") version "4.1.1"
    id("io.spring.dependency-management") version "1.1.7"
    id("io.freefair.lombok") version "9.8.2"
    id("org.openapi.generator") version "7.26.0"
}

group = "com.example"
version = "0.0.1-SNAPSHOT"
description = "ms-account-reservation"

java {
    toolchain {
        languageVersion.set(JavaLanguageVersion.of(25))
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation("org.springframework.boot:spring-boot-starter-actuator")
    implementation("org.springframework.boot:spring-boot-starter-data-jpa")
    implementation("org.springframework.boot:spring-boot-starter-liquibase")
    implementation("org.springframework.boot:spring-boot-starter-webmvc")
    implementation("org.springframework.boot:spring-boot-starter-validation")
    implementation("org.springframework.boot:spring-boot-starter-security")
    implementation("io.jsonwebtoken:jjwt-api:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-impl:0.13.0")
    runtimeOnly("io.jsonwebtoken:jjwt-jackson:0.13.0")
    implementation("io.swagger.core.v3:swagger-annotations:2.2.54") // для аннотации @Schema в сгенерированных интерфейсах
    implementation("org.openapitools:jackson-databind-nullable:0.2.12") // для аннотации @JsonNullable там же
    implementation("org.mapstruct:mapstruct:1.6.3")
    annotationProcessor("org.mapstruct:mapstruct-processor:1.6.3")
    runtimeOnly("org.postgresql:postgresql")
    testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
    testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
    testImplementation("org.springframework.boot:spring-boot-starter-liquibase-test")
    testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
    testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.withType<Test> {
    useJUnitPlatform()
}

tasks.compileJava {
    dependsOn(tasks.openApiGenerate)
}

openApiGenerate {
    generatorName.set("spring")
    inputSpec.set("$rootDir/src/main/resources/static/ms-upgrade-api.yaml")
    outputDir.set(layout.buildDirectory.dir("generated/api").get().asFile.path)

    apiPackage.set("com.example.api")
    modelPackage.set("com.example.dto")

    configOptions.set(
        mapOf(
            "interfaceOnly" to "true",
            "useSpringBoot4" to "true",
            "useJackson3" to "true",
            "skipDefaultInterface" to "true",   // убирает default-методы, чтобы точно была своя реализация
            "documentationProvider" to "none",  // смена провайдера для поддержки annotationLibrary "none"
            "annotationLibrary" to "none",      // отключает генерацию @Schema и других Swagger-аннотаций, т.к. Swagger возьмёт всё из yaml
        )
    )

    typeMappings.set(
        mapOf(
            "OffsetDateTime" to "Instant"
        )
    )
    importMappings.set(
        mapOf(
            "Nullable" to "org.jspecify.annotations.Nullable", // заменяет deprecated аннотацию Spring на JSpecify
            "java.time.OffsetDateTime" to "java.time.Instant", // в entity используется Instant
        )
    )
}

sourceSets {
    getByName("main") {
        java {
            srcDir(files("${openApiGenerate.outputDir.get()}/src/main/java"))
        }
    }
}