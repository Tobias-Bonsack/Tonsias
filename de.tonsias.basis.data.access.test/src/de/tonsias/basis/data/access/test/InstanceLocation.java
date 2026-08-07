package de.tonsias.basis.data.access.test;

import java.nio.file.Path;

import de.tonsias.basis.data.access.osgi.impl.InstanceLocationUtil;

/**
 * Resolves the workspace directory the same way {@code LoadServiceImpl} and
 * {@code SaveServiceImpl} do, so the tests look at the files the services
 * actually wrote.
 */
final class InstanceLocation {

	private InstanceLocation() {
	}

	static Path resolve(String relativePath) {
		return InstanceLocationUtil.resolve(relativePath);
	}
}
