package net.fabricmc.loom

/**
 * Created by Mitchell Skaggs on 6/12/2019.
 */
static String genBuildFile() {
	"""
plugins {
	id 'fabric-loom'
	id 'maven-publish'
}
sourceCompatibility = JavaVersion.VERSION_1_8
targetCompatibility = JavaVersion.VERSION_1_8

archivesBaseName = project.archives_base_name
version = project.mod_version
group = project.maven_group

minecraft {
}

dependencies {
	//to change the versions see the gradle.properties file
	minecraft "com.mojang:minecraft:\${project.minecraft_version}"
	mappings "net.fabricmc:yarn:\${project.yarn_mappings}"
	modCompile "net.fabricmc:fabric-loader:\${project.loader_version}"

	// Fabric API. This is technically optional, but you probably want it anyway.
	modCompile "net.fabricmc.fabric-api:fabric-api:\${project.fabric_version}"

	// PSA: Some older mods, compiled on Loom 0.2.1, might have outdated Maven POMs.
	// You may need to force-disable transitiveness on them.
}

processResources {
	inputs.property "version", project.version

	from(sourceSets.main.resources.srcDirs) {
		include "fabric.mod.json"
		expand "version": project.version
	}

	from(sourceSets.main.resources.srcDirs) {
		exclude "fabric.mod.json"
	}
}

// ensure that the encoding is set to UTF-8, no matter what the system default is
// this fixes some edge cases with special characters not displaying correctly
// see http://yodaconditions.net/blog/fix-for-java-file-encoding-problems-with-gradle.html
tasks.withType(JavaCompile) {
	options.encoding = "UTF-8"
}

// Loom will automatically attach sourcesJar to a RemapSourcesJar task and to the "build" task
// if it is present.
// If you remove this task, sources will not be generated.
task sourcesJar(type: Jar, dependsOn: classes) {
	classifier = "sources"
	from sourceSets.main.allSource
}

jar {
	from "LICENSE"
}

// configure the maven publication
publishing {
	publications {
		mavenJava(MavenPublication) {
			// add all the jars that should be included when publishing to maven
			artifact(jar) {
				builtBy remapJar
			}
			artifact(sourcesJar) {
				builtBy remapSourcesJar
			}
		}
	}

	// select the repositories you want to publish to
	repositories {
		// uncomment to publish to the local maven
		// mavenLocal()
	}
}
"""
}

static String genPropsFile(String mcVersion, String yarnVersion, String loaderVersion, String fabricVersion) {
	"""
org.gradle.caching=true
org.gradle.parallel=true

# Fabric Properties
# check these on https://fabricmc.net/use
minecraft_version=$mcVersion
yarn_mappings=$yarnVersion
loader_version=$loaderVersion

# Mod Properties
mod_version = 1.0.0
maven_group = net.fabricmc
archives_base_name = fabric-example-mod

# Dependencies
# currently not on the main fabric site, check on the maven: https://maven.fabricmc.net/net/fabricmc/fabric
fabric_version=$fabricVersion
"""
}

static String genSettingsFile(String name) {
	"""
rootProject.name = '$name'
"""
}
