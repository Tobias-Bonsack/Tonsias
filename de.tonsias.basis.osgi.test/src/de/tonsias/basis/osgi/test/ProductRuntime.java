package de.tonsias.basis.osgi.test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.concurrent.CompletionException;

import org.eclipse.core.runtime.Platform;
import org.eclipse.core.runtime.URIUtil;

import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.IValue;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.IMultiValueService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.util.OsgiUtil;

/**
 * The services of the running product, resolved the way the product resolves
 * them.
 * <p>
 * Everything a system test talks to comes from here: the components are the
 * real {@code @Component}s out of the OSGi registry, the broker is the real e4
 * broker, and the files land in the real instance location. Nothing is
 * substituted, so a test that passes here describes behaviour the shipped
 * application actually has.
 * </p>
 * <p>
 * The runtime is shared by every test in a bundle - one Equinox, one root
 * instanz, one key sequence, one delta log. Tests therefore build their own
 * subtree below the root instead of assuming an empty model, and hand the delta
 * log back empty via {@link #flushDeltas()} so the next test starts from a
 * known state.
 * </p>
 */
public final class ProductRuntime {

	/** by convention the root instanz, see {@code KeyServiceImpl} */
	public static final String ROOT = "0";

	public static final String INSTANZ_PATH = "instanz/";

	/** keeps a {@link #block(Path) blocked} directory non-empty */
	private static final String IN_THE_WAY = "in-the-way.txt";

	private ProductRuntime() {
	}

	/**
	 * Brings the runtime up and hands back the root instanz - in the product
	 * {@code ModelView} creates it at start-up, in a fresh test workspace nothing
	 * has yet.
	 */
	public static IInstanz start() {
		E4ServiceContext.prime();
		return instanzService().getRoot();
	}

	public static IInstanzService instanzService() {
		return service(IInstanzService.class);
	}

	public static ISingleValueService singleValueService() {
		return service(ISingleValueService.class);
	}

	public static IMultiValueService multiValueService() {
		return service(IMultiValueService.class);
	}

	public static IDeltaService deltaService() {
		return service(IDeltaService.class);
	}

	public static IEventBrokerBridge broker() {
		return service(IEventBrokerBridge.class);
	}

	public static IKeyService keyService() {
		return service(IKeyService.class);
	}

	public static IBasicPreferenceService preferenceService() {
		return service(IBasicPreferenceService.class);
	}

	public static LoadService loadService() {
		return service(LoadService.class);
	}

	private static <T> T service(Class<T> type) {
		T service = OsgiUtil.getService(type);
		assertThat("service not registered: " + type.getName(), service, is(not(nullValue())));
		return service;
	}

	/**
	 * Writes out whatever is pending and empties the delta log. A test that leaves
	 * the log filled would otherwise have its events folded into the next test's
	 * save.
	 * <p>
	 * {@code saveDeltas()} empties its log whatever the save ran into, so a test
	 * that fabricated a delete for something that was never written does not drag
	 * the rest of the bundle down with it. What it threw is of no interest here -
	 * the test that cares about it asserts on it itself.
	 * </p>
	 */
	public static void flushDeltas() {
		try {
			deltaService().saveDeltas();
		} catch (CompletionException e) {
			// the log is empty again either way
		}
	}

	// ---------- making a delete fail ----------

	/**
	 * Puts something in the way of {@code path}: a directory with a file in it,
	 * where the services expect a JSON file. Deleting that fails with
	 * {@code DirectoryNotEmptyException} however often it is tried - a delete that
	 * merely finds nothing has reached its goal and is no failure at all, so it is
	 * no use for testing what a save does when a step of it goes wrong.
	 * <p>
	 * Whoever blocks a path {@link #unblock(Path) unblocks} it again, the workspace
	 * is shared by every test in the bundle.
	 * </p>
	 */
	public static void block(Path path) {
		try {
			Files.createDirectories(path);
			Files.writeString(path.resolve(IN_THE_WAY), "");
		} catch (IOException e) {
			throw new AssertionError("could not block " + path, e);
		}
	}

	public static void unblock(Path path) {
		try {
			Files.deleteIfExists(path.resolve(IN_THE_WAY));
			Files.deleteIfExists(path);
		} catch (IOException e) {
			throw new AssertionError("could not unblock " + path, e);
		}
	}

	// ---------- reading the model back off disk ----------

	/**
	 * @return the instanz as it is stored, not the cached one - the only way to see
	 *         what a save actually wrote
	 */
	public static Instanz reloadInstanz(String key) {
		Instanz loaded = loadService().loadFromGson(INSTANZ_PATH + key, Instanz.class);
		assertThat("no file for instanz " + key, loaded, is(not(nullValue())));
		return loaded;
	}

	/**
	 * Takes an {@link IValueType}, so the same call reads a single value and a list
	 * back - every existing caller keeps compiling, a {@code SingleValueType} being
	 * one.
	 */
	public static <T extends IValue> T reloadValue(IValueType type, String key, Class<T> clazz) {
		T loaded = loadService().loadFromGson(type.getPath() + key, clazz);
		assertThat("no file for value " + key, loaded, is(not(nullValue())));
		return loaded;
	}

	public static boolean instanzFileExists(String key) {
		return Files.exists(instanzFile(key));
	}

	public static boolean valueFileExists(IValueType type, String key) {
		return Files.exists(valueFile(type, key));
	}

	public static Path instanzFile(String key) {
		return workspace().resolve(INSTANZ_PATH + key + ".json");
	}

	public static Path valueFile(IValueType type, String key) {
		return workspace().resolve(type.getPath() + key + ".json");
	}

	/**
	 * the directory the persistence services write to, resolved the same way
	 * {@code InstanceLocationUtil} does
	 */
	public static Path workspace() {
		URL url = Platform.getInstanceLocation().getURL();
		try {
			return Paths.get(URIUtil.toURI(url));
		} catch (URISyntaxException e) {
			throw new AssertionError("unusable instance location: " + url, e);
		}
	}
}
