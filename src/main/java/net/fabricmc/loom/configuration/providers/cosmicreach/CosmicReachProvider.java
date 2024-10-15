/*
 * This file is part of fabric-loom, licensed under the MIT License (MIT).
 *
 * Copyright (c) 2018-2021 FabricMC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */

package net.fabricmc.loom.configuration.providers.cosmicreach;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.Objects;

import com.google.common.base.Preconditions;
import org.gradle.api.Project;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.configuration.ConfigContext;
import net.fabricmc.loom.configuration.providers.BundleMetadata;
import net.fabricmc.loom.util.download.DownloadExecutor;
import net.fabricmc.loom.util.download.GradleDownloadProgressListener;
import net.fabricmc.loom.util.gradle.ProgressGroup;

public abstract class CosmicReachProvider {
	private static final Logger LOGGER = LoggerFactory.getLogger(CosmicReachProvider.class);

	private final CosmicReachMetadataProvider metadataProvider;

	private File cosmicReachClientJar;
	private File cosmicReachServerJar;

	@Nullable
	private BundleMetadata serverBundleMetadata;

	private final ConfigContext configContext;

	public CosmicReachProvider(CosmicReachMetadataProvider metadataProvider, ConfigContext configContext) {
		this.metadataProvider = metadataProvider;
		this.configContext = configContext;
	}

	protected boolean provideClient() {
		try {
			return metadataProvider.getVersionEntry().client != null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	protected boolean provideServer() {
		try {
			return metadataProvider.getVersionEntry().server != null;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	public void provide() throws Exception {
		initFiles();

//		final CosmicReachVersionMeta.JavaVersion javaVersion = getVersionInfo().javaVersion();
//
//		if (javaVersion != null) {
//			final int requiredMajorJavaVersion = getVersionInfo().javaVersion().majorVersion();
//			final JavaVersion requiredJavaVersion = JavaVersion.toVersion(requiredMajorJavaVersion);
//
//			if (!JavaVersion.current().isCompatibleWith(requiredJavaVersion)) {
//				throw new IllegalStateException("CoSmIcReAcH " + minecraftVersion() + " requires Java " + requiredJavaVersion + " but Gradle is using " + JavaVersion.current());
//			}
//		}

		downloadJars();

		if (provideServer()) {
			serverBundleMetadata = BundleMetadata.fromJar(cosmicReachServerJar.toPath());
		}

		final CosmicReachLibraryProvider libraryProvider = new CosmicReachLibraryProvider(this, configContext.project());
		libraryProvider.provide();
	}

	protected void initFiles() {
		if (provideClient()) {
			cosmicReachClientJar = file("cosmic-reach-client.jar");
		}

		if (provideServer()) {
			cosmicReachServerJar = file("cosmic-reach-server.jar");
		}
	}

	private void downloadJars() throws IOException {
		try (ProgressGroup progressGroup = new ProgressGroup(getProject(), "Download CoSmIcReAcH jars");

		DownloadExecutor executor = new DownloadExecutor(2)) {

			if (provideClient()) {
				final VersionsManifest.Client client = getVersionInfo().client;
				getExtension().download(client.url)
						.sha256(client.sha256)
						.progress(new GradleDownloadProgressListener("CoSmIcReAcH client", progressGroup::createProgressLogger))
						.downloadPathAsync(cosmicReachClientJar.toPath(), executor);
			}

			if (provideServer()) {
				final VersionsManifest.Server server = getVersionInfo().server;
				getExtension().download(server.url)
						.sha256(server.sha256)
						.progress(new GradleDownloadProgressListener("CoSmIcReAcH server", progressGroup::createProgressLogger))
						.downloadPathAsync(cosmicReachServerJar.toPath(), executor);
			}
		}
	}

	public File workingDir() {
		return cosmicWorkingDirectory(configContext.project(), cosmicReachVersion());
	}

	public File dir(String path) {
		File dir = file(path);
		dir.mkdirs();
		return dir;
	}

	public File file(String path) {
		return new File(workingDir(), path);
	}

	public Path path(String path) {
		return file(path).toPath();
	}

	public File getCosmicReachClientJar() {
		Preconditions.checkArgument(provideClient(), "Not configured to provide client jar");
		return cosmicReachClientJar;
	}

	// This may be the server bundler jar on newer versions prob not what you want.
	public File getCosmicReachServerJar() {
		Preconditions.checkArgument(provideServer(), "Not configured to provide server jar");
		return cosmicReachServerJar;
	}

	public String cosmicReachVersion() {
		return Objects.requireNonNull(metadataProvider, "Metadata provider not setup").getCosmicReachVersion();
	}

	public VersionsManifest.Version getVersionInfo() {
		try {
			return Objects.requireNonNull(metadataProvider, "Metadata provider not setup").getVersionEntry();
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	@Nullable
	public BundleMetadata getServerBundleMetadata() {
		return serverBundleMetadata;
	}

	public abstract List<Path> getCosmicReachJars();

	protected Project getProject() {
		return configContext.project();
	}

	protected LoomGradleExtension getExtension() {
		return configContext.extension();
	}

	public boolean refreshDeps() {
		return getExtension().refreshDeps();
	}

	public static File cosmicWorkingDirectory(Project project, String version) {
		LoomGradleExtension extension = LoomGradleExtension.get(project);
		File workingDir = new File(extension.getFiles().getUserCache(), version);
		workingDir.mkdirs();
		return workingDir;
	}
}
