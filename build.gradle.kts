plugins {
	java
	jacoco
	id("org.springframework.boot") version "4.0.6"
	id("io.spring.dependency-management") version "1.1.7"
	id("org.sonarqube") version "6.2.0.5505"
	id("com.gradleup.shadow") version "8.3.6"
}

group = "com.unstampedpages.currency"
version = "0.0.1-SNAPSHOT"

java {
	toolchain {
		languageVersion = JavaLanguageVersion.of(25)
	}
}

repositories {
	mavenCentral()
}

dependencies {
	implementation("org.springframework.boot:spring-boot-starter-web")
	implementation("org.springframework.boot:spring-boot-starter-actuator")
	implementation("com.amazonaws.serverless:aws-serverless-java-container-springboot4:3.0.1") {
		exclude(group = "org.springframework.cloud", module = "spring-cloud-function-serverless-web")
	}
	annotationProcessor("org.springframework.boot:spring-boot-configuration-processor")

	testImplementation("org.springframework.boot:spring-boot-starter-test")
	testImplementation("org.wiremock:wiremock-standalone:3.9.1")
	testRuntimeOnly("org.junit.platform:junit-platform-launcher")
}

tasks.shadowJar {
	// Exclude Multi-Release JAR version directories — Lambda's zip unpacker cannot handle
	// the META-INF/versions/ structure and throws BadFunctionCode if it is present
	exclude("META-INF/versions/**")

	// Merge Spring service files so autoconfiguration survives JAR flattening
	mergeServiceFiles {
		include("META-INF/services/**")
		include("META-INF/spring.factories")
		include("META-INF/spring.handlers")
		include("META-INF/spring.schemas")
		include("META-INF/spring/org.springframework.boot.autoconfigure.AutoConfiguration.imports")
		include("META-INF/spring/org.springframework.boot.actuate.autoconfigure.web.ManagementContextConfiguration.imports")
	}

	isZip64 = true  // Required when the dependency count is large
}

tasks.withType<Test> {
	useJUnitPlatform()
}

tasks.jacocoTestReport {
	dependsOn(tasks.test)
	reports {
		xml.required = true
	}
}
