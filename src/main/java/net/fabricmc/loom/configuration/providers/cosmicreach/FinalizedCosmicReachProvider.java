package net.fabricmc.loom.configuration.providers.cosmicreach;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.StringJoiner;
import java.util.function.Function;

import net.fabricmc.loom.LoomGradleExtension;
import net.fabricmc.loom.configuration.ConfigContext;
import net.fabricmc.loom.configuration.mods.dependency.LocalMavenHelper;
import net.fabricmc.loom.configuration.processors.ProcessorContextImpl;
import net.fabricmc.loom.extension.LoomFiles;

import net.fabricmc.tinyremapper.OutputConsumerPath;

import org.gradle.api.Project;

public abstract class FinalizedCosmicReachProvider<M extends CosmicReachProvider> implements MappedMinecraftProvider {

	protected final M minecraftProvider;
	private final Project project;
	protected final LoomGradleExtension extension;

	public FinalizedCosmicReachProvider(Project project, M minecraftProvider) {
		this.minecraftProvider = minecraftProvider;
		this.project = project;
		this.extension = LoomGradleExtension.get(project);
	}

	public record ProvideContext(boolean applyDependencies, boolean refreshOutputs, ConfigContext configContext) {
		ProvideContext withApplyDependencies(boolean applyDependencies) {
			return new ProvideContext(applyDependencies, refreshOutputs(), configContext());
		}
	}

	public enum MavenScope {
		// Output files will be stored per project
		LOCAL(LoomFiles::getLocalCosmicReachRepo),
		// Output files will be stored globally
		GLOBAL(LoomFiles::getGlobalCosmicReachRepo);

		private final Function<LoomFiles, File> fileFunction;

		MavenScope(Function<LoomFiles, File> fileFunction) {
			this.fileFunction = fileFunction;
		}

		public Path getRoot(LoomGradleExtension extension) {
			return fileFunction.apply(extension.getFiles()).toPath();
		}
	}

	public MavenScope getMavenScope() {
		return MavenScope.GLOBAL;
	}

	public static final class MergedImpl extends FinalizedCosmicReachProvider<MergedCosmicReachProvider> implements Merged {
		public MergedImpl(Project project, MergedCosmicReachProvider minecraftProvider) {
			super(project, minecraftProvider);
		}

		public List<CosmicReachJar.Type> getDependencyTypes() {
			return List.of(CosmicReachJar.Type.MERGED);
		}

		@Override
		public List<RemappedJars> getRemappedJars() {
			return List.of(
					new RemappedJars(minecraftProvider.getMergedJar(), getMergedJar())
			);
		}
	}

	public static final class SplitImpl extends FinalizedCosmicReachProvider<SplitCosmicReachProvider> implements Split {
		public SplitImpl(Project project, SplitCosmicReachProvider minecraftProvider) {
			super(project, minecraftProvider);
		}

		@Override
		public List<CosmicReachJar.Type> getDependencyTypes() {
			return List.of(CosmicReachJar.Type.CLIENT_ONLY, CosmicReachJar.Type.COMMON);
		}

		@Override
		public List<RemappedJars> getRemappedJars() {
			return List.of(
					new RemappedJars(minecraftProvider.getCosmicReachCommonJar(), getCommonJar()),
					new RemappedJars(minecraftProvider.getCosmicReachClientOnlyJar(), getClientOnlyJar())
			);
		}
	}

	public static final class SingleJarImpl extends FinalizedCosmicReachProvider<SingleJarCosmicReachProvider> implements SingleJar {
		private final SingleJarEnvType env;

		private SingleJarImpl(Project project, SingleJarCosmicReachProvider minecraftProvider, SingleJarEnvType env) {
			super(project, minecraftProvider);
			this.env = env;
		}

		public static SingleJarImpl server(Project project, SingleJarCosmicReachProvider minecraftProvider) {
			return new SingleJarImpl(project, minecraftProvider, SingleJarEnvType.SERVER);
		}

		public static SingleJarImpl client(Project project, SingleJarCosmicReachProvider minecraftProvider) {
			return new SingleJarImpl(project, minecraftProvider, SingleJarEnvType.CLIENT);
		}

		@Override
		public List<CosmicReachJar.Type> getDependencyTypes() {
			return List.of(envType());
		}

		@Override
		public List<RemappedJars> getRemappedJars() {
			return List.of(
					new RemappedJars(minecraftProvider.getCosmicReachEnvOnlyJar(), getEnvOnlyJar())
			);
		}

		@Override
		public SingleJarEnvType env() {
			return env;
		}
	}

	// Returns a list of MinecraftJar.Type's that this provider exports to be used as a dependency
	public List<CosmicReachJar.Type> getDependencyTypes() {
		return Collections.emptyList();
	}

	private boolean areOutputsValid(List<RemappedJars> remappedJars) {
		for (RemappedJars remappedJar : remappedJars) {
			if (!getMavenHelper(remappedJar.type()).exists(null)) {
				return false;
			}
		}

		return true;
	}

	public abstract List<RemappedJars> getRemappedJars();

	public List<CosmicReachJar> provide(ProvideContext context) throws Exception {
		final List<RemappedJars> remappedJars = getRemappedJars();
		final List<CosmicReachJar> minecraftJars = remappedJars.stream()
				.map(RemappedJars::outputJar)
				.toList();

		if (remappedJars.isEmpty()) {
			throw new IllegalStateException("No remapped jars provided");
		}

		if (!areOutputsValid(remappedJars) || context.refreshOutputs() || !hasBackupJars(minecraftJars)) {
			try {
				remapInputs(remappedJars, context.configContext());
				createBackupJars(minecraftJars);
			} catch (Throwable t) {
				cleanOutputs(remappedJars);

				t.printStackTrace();
				throw new RuntimeException("Failed to remap minecraft", t);
			}
		}

		if (context.applyDependencies()) {
			final List<CosmicReachJar.Type> dependencyTargets = getDependencyTypes();

			if (!dependencyTargets.isEmpty()) {
				CosmicReachSourceSets.get(getProject()).applyDependencies(
						(configuration, type) -> getProject().getDependencies().add(configuration, getDependencyNotation(type)),
						dependencyTargets
				);
			}
		}

		return minecraftJars;
	}

	private void remapInputs(List<RemappedJars> remappedJars, ConfigContext configContext) {

		for (RemappedJars remappedJar : remappedJars) {
			try {
				Files.deleteIfExists(remappedJar.outputJarPath());
			} catch (IOException e) {
				throw new RuntimeException(e);
			}

			try {
//				outputConsumer.addNonClassFiles(remappedJar.inputJar());

				FileInputStream stream0 = new FileInputStream(remappedJar.inputJar.toFile());
				if (!remappedJar.outputJar().toFile().exists()) {
					try {
						remappedJar.outputJar().toFile().createNewFile();
						remappedJar.outputJar().toFile().getParentFile().mkdirs();
					} catch (Exception ignore) {}
				}
				FileOutputStream stream = new FileOutputStream(remappedJar.outputJar.toFile());
				stream.write(stream0.readAllBytes());
				stream.close();
				stream0.close();

			} catch (Exception e) {
				e.printStackTrace();
				throw new RuntimeException("Failed to load JAR " + remappedJar.inputJar() + " -> " + remappedJar.outputJar());
			}
		}
	}

	public record RemappedJars(Path inputJar, CosmicReachJar outputJar, Path... remapClasspath) {
		public Path outputJarPath() {
			return outputJar().getPath();
		}

		public String name() {
			return outputJar().getName();
		}

		public CosmicReachJar.Type type() {
			return outputJar().getType();
		}
	}

	// Create two copies of the remapped jar, the backup jar is used as the input of genSources
	public static Path getBackupJarPath(CosmicReachJar minecraftJar) {
		final Path outputJarPath = minecraftJar.getPath();
		return outputJarPath.resolveSibling(outputJarPath.getFileName() + ".backup");
	}

	protected boolean hasBackupJars(List<CosmicReachJar> minecraftJars) {
		for (CosmicReachJar minecraftJar : minecraftJars) {
			if (!Files.exists(getBackupJarPath(minecraftJar))) {
				return false;
			}
		}

		return true;
	}

	protected void createBackupJars(List<CosmicReachJar> minecraftJars) throws IOException {
		for (CosmicReachJar minecraftJar : minecraftJars) {
			Files.copy(
					minecraftJar.getPath(),
					getBackupJarPath(minecraftJar),
					StandardCopyOption.REPLACE_EXISTING
			);
		}
	}

	private void cleanOutputs(List<RemappedJars> remappedJars) throws IOException {
		for (RemappedJars remappedJar : remappedJars) {
			Files.deleteIfExists(remappedJar.outputJarPath());
			Files.deleteIfExists(getBackupJarPath(remappedJar.outputJar()));
		}
	}

	public Project getProject() {
		return project;
	}

	protected String getName(CosmicReachJar.Type type) {
		return type.toString().toLowerCase(Locale.ROOT);
	}

	protected String getVersion() {
		return extension.getCosmicReachProvider().cosmicReachVersion();
	}

	protected String getDependencyNotation(CosmicReachJar.Type type) {
		return "finalforeach:cosmicreach:%s:%s".formatted(getVersion(), getName(type));
	}

	public LocalMavenHelper getMavenHelper(CosmicReachJar.Type type) {
		return new LocalMavenHelper("finalforeach", "cosmicreach", getVersion(), getName(type), getMavenScope().getRoot(extension));
	}

	public Path getJar(CosmicReachJar.Type type) {
		return getMavenHelper(type).getOutputFile(null);
	}
}