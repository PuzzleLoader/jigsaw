package net.fabricmc.loom.configuration.providers.cosmicreach;

import java.nio.file.Path;
import java.util.List;

public interface MappedMinecraftProvider {
	default List<Path> getCosmicReachJarPaths() {
		return getCosmicReachJars().stream().map(CosmicReachJar::getPath).toList();
	}

	List<CosmicReachJar> getCosmicReachJars();

	interface ProviderImpl extends MappedMinecraftProvider {
		Path getJar(CosmicReachJar.Type type);
	}

	interface Merged extends ProviderImpl {
		default CosmicReachJar getMergedJar() {
			return new CosmicReachJar.Merged(getJar(CosmicReachJar.Type.MERGED));
		}

		@Override
		default List<CosmicReachJar> getCosmicReachJars() {
			return List.of(getMergedJar());
		}
	}

	interface Split extends ProviderImpl {
		default CosmicReachJar getCommonJar() {
			return new CosmicReachJar.Common(getJar(CosmicReachJar.Type.COMMON));
		}

		default CosmicReachJar getClientOnlyJar() {
			return new CosmicReachJar.ClientOnly(getJar(CosmicReachJar.Type.CLIENT_ONLY));
		}

		@Override
		default List<CosmicReachJar> getCosmicReachJars() {
			return List.of(getCommonJar(), getClientOnlyJar());
		}
	}

	interface SingleJar extends ProviderImpl {
		SingleJarEnvType env();

		default CosmicReachJar.Type envType() {
			return env().getType();
		}

		default CosmicReachJar getEnvOnlyJar() {
			return env().getJar().apply(getJar(env().getType()));
		}

		@Override
		default List<CosmicReachJar> getCosmicReachJars() {
			return List.of(getEnvOnlyJar());
		}
	}
	}