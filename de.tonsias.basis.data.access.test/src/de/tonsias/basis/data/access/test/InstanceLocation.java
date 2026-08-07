package de.tonsias.basis.data.access.test;

import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.Platform;

/**
 * Resolves the workspace directory the same way {@code LoadServiceImpl} and
 * {@code SaveServiceImpl} do, so the tests look at the files the services
 * actually wrote.
 */
final class InstanceLocation {

	private InstanceLocation() {
	}

	static Path resolve(String relativePath) {
		String dir = Platform.getInstanceLocation().getURL().getPath().substring(1);
		return Paths.get(dir, relativePath);
	}
}
