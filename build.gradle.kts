plugins {
	java
	id("org.springframework.boot") version "4.1.1"
	id("io.spring.dependency-management") version "1.1.7"
}

group = "io.github.spollito"
version = "0.0.1-SNAPSHOT"
description = "DevPlane internal developer platform"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(21)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("org.springframework.boot:spring-boot-starter-data-jpa")
	implementation("org.springframework.boot:spring-boot-starter-flyway")
	implementation("org.springframework.boot:spring-boot-starter-validation")
	implementation("org.springframework.boot:spring-boot-starter-thymeleaf")
	implementation("org.springframework.boot:spring-boot-starter-webmvc")
	implementation("org.flywaydb:flyway-database-postgresql")
	implementation("org.springdoc:springdoc-openapi-starter-webmvc-ui:3.1.1")
	implementation("org.eclipse.jgit:org.eclipse.jgit:7.8.0.202609011348-r")
	implementation("org.webjars.npm:htmx.org:2.0.11")
	implementation("org.webjars:webjars-locator-lite")
	developmentOnly("org.springframework.boot:spring-boot-docker-compose")
	runtimeOnly("org.postgresql:postgresql")
	testImplementation("org.springframework.boot:spring-boot-starter-actuator-test")
	testImplementation("org.springframework.boot:spring-boot-starter-data-jpa-test")
	testImplementation("org.springframework.boot:spring-boot-starter-flyway-test")
	testImplementation("org.springframework.boot:spring-boot-starter-thymeleaf-test")
	testImplementation("org.springframework.boot:spring-boot-starter-validation-test")
	testImplementation("org.springframework.boot:spring-boot-starter-webmvc-test")
	testImplementation("org.springframework.boot:spring-boot-testcontainers")
	testImplementation("org.jsoup:jsoup:1.23.2")
	testImplementation("org.testcontainers:testcontainers-junit-jupiter")
	testImplementation("org.testcontainers:testcontainers-postgresql")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

// Pages show exact times in the JVM's time zone, so tests pin it.
tasks.withType<Test> {
	useJUnitPlatform()
	systemProperty("user.timezone", "UTC")
}

// Tests tagged "template" generate a project and run its whole build, which takes
// a minute or two, so they run in templateTest (part of build) instead of test.
tasks.test {
	useJUnitPlatform {
		excludeTags("template")
	}
}

val templateTest = tasks.register<Test>("templateTest") {
	description = "Generates a project from each service template and builds it."
	group = LifecycleBasePlugin.VERIFICATION_GROUP
	testClassesDirs = sourceSets.test.get().output.classesDirs
	classpath = sourceSets.test.get().runtimeClasspath
	useJUnitPlatform {
		includeTags("template")
	}
	shouldRunAfter(tasks.test)
}

tasks.check {
	dependsOn(templateTest)
}
