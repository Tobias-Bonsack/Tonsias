package de.tonsias.basis.osgi.test.system;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.test.E4ServiceContext;
import de.tonsias.basis.osgi.util.OsgiUtil;

/**
 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/22">#22</a>
 */
public class OsgiUtilSystemTest {

	@BeforeEach
	void beforeEach() {
		E4ServiceContext.prime();
	}

	@Test
	void testGetService_registeredService_resolves() {
		assertThat(OsgiUtil.getService(IKeyService.class), is(notNullValue()));
	}

	/**
	 * {@code FrameworkUtil.getBundle(..)} returns null for a class that was not
	 * loaded by a bundle class loader. That has to yield null rather than an NPE,
	 * so callers can fall back on their own null handling.
	 */
	@Test
	void testGetService_classNotFromABundle_returnsNull() {
		assertThat(OsgiUtil.getService(String.class), is(nullValue()));
	}

	@Test
	void testGetService_nullContext_returnsNull() {
		assertThat(OsgiUtil.getService(IKeyService.class, null), is(nullValue()));
	}

	/** A well-formed context with nothing registered for the type is not an error. */
	@Test
	void testGetService_noServiceRegistered_returnsNull() {
		var context = org.osgi.framework.FrameworkUtil.getBundle(OsgiUtilSystemTest.class).getBundleContext();
		assertThat(OsgiUtil.getService(java.util.RandomAccess.class, context), is(nullValue()));
	}
}
