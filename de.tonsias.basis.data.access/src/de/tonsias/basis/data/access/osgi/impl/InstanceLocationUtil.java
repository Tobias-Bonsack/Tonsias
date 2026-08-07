package de.tonsias.basis.data.access.osgi.impl;

import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;

/**
 * Resolves the workspace directory every persistence service writes to.
 * <p>
 * The instance location is a {@link URL}, and its path is not a file system
 * path: on Windows it reads {@code /C:/ws/}, everywhere else {@code /home/u/ws}
 * - so neither taking the path as is nor cutting its first character works on
 * both. {@link URIUtil#toURI(URL)} is the platform's own conversion and handles
 * the encoding (spaces, umlauts) on top.
 */
public final class InstanceLocationUtil {

	private InstanceLocationUtil() {
	}

	/**
	 * @return the workspace directory, without a trailing separator
	 */
	public static Path getDirectory() {
		URL url = Platform.getInstanceLocation().getURL();
		try {
			return Paths.get(URIUtil.toURI(url));
		} catch (URISyntaxException e) {
			Platform.getLog(InstanceLocationUtil.class).error("Unusable instance location: " + url, e);
			return Paths.get(url.getPath());
		}
	}

	/**
	 * @param relativePath path of an object relative to the workspace, an absolute
	 *                     path is taken as is
	 * @return the file the given path denotes
	 */
	public static Path resolve(String relativePath) {
		return getDirectory().resolve(relativePath);
	}
}
