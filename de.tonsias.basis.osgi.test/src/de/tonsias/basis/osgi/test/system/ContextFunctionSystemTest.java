package de.tonsias.basis.osgi.test.system;

import static de.tonsias.basis.osgi.test.ProductRuntime.ROOT;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.sameInstance;

import java.util.ArrayList;
import java.util.List;

import org.eclipse.e4.core.contexts.EclipseContextFactory;
import org.eclipse.e4.core.contexts.IEclipseContext;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.framework.BundleContext;
import org.osgi.framework.FrameworkUtil;
import org.osgi.framework.InvalidSyntaxException;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.test.ProductRuntime;
import de.tonsias.basis.osgi.util.OsgiUtil;

/**
 * What the two {@code IContextFunction}s hand out, asked the way the workbench
 * asks.
 * <p>
 * An {@code IEclipseContext} caches a context function's result per asking
 * context, and every part gets a context of its own - so a function is asked
 * once per part, not once per application. What comes back has to be the
 * instance the service registry holds, or a part would work on a delta log of
 * its own: it would render changes it happened to be present for, and a save on
 * one log would leave the other standing.
 * </p>
 *
 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/52">#52</a>
 */
public class ContextFunctionSystemTest {

	IEclipseContext _serviceContext;

	final List<IEclipseContext> _partContexts = new ArrayList<>();

	IInstanzService _inse;

	@BeforeEach
	void beforeEach() {
		ProductRuntime.start();
		_inse = ProductRuntime.instanzService();
		_serviceContext = EclipseContextFactory.getServiceContext(bundleContext());
		ProductRuntime.flushDeltas();
	}

	@AfterEach
	void afterEach() {
		_partContexts.forEach(IEclipseContext::dispose);
		_partContexts.clear();
		ProductRuntime.flushDeltas();
	}

	private static BundleContext bundleContext() {
		return FrameworkUtil.getBundle(ContextFunctionSystemTest.class).getBundleContext();
	}

	/** a context of the kind e4 creates for a part, and asks the injection from */
	private IEclipseContext partContext(String name) {
		IEclipseContext partContext = _serviceContext.createChild(name);
		_partContexts.add(partContext);
		return partContext;
	}

	private <T> T injectedInto(IEclipseContext context, Class<T> type) {
		return type.cast(context.get(type.getName()));
	}

	private int registrationsOf(Class<?> type) throws InvalidSyntaxException {
		return bundleContext().getServiceReferences(type, null).size();
	}

	@Test
	void testDeltaService_aPartContextIsGivenTheRegisteredInstance() {
		IDeltaService fromPart = injectedInto(partContext("DeltaView"), IDeltaService.class);

		assertThat(fromPart, is(notNullValue()));
		assertThat(fromPart, is(sameInstance(OsgiUtil.getService(IDeltaService.class))));
	}

	@Test
	void testEventBrokerBridge_aPartContextIsGivenTheRegisteredInstance() {
		IEventBrokerBridge fromPart = injectedInto(partContext("InstanzView"), IEventBrokerBridge.class);

		assertThat(fromPart, is(notNullValue()));
		assertThat(fromPart, is(sameInstance(OsgiUtil.getService(IEventBrokerBridge.class))));
	}

	@Test
	void testDeltaService_twoPartContextsShareOneInstance() {
		IDeltaService first = injectedInto(partContext("first"), IDeltaService.class);
		IDeltaService second = injectedInto(partContext("second"), IDeltaService.class);

		assertThat(first, is(sameInstance(second)));
	}

	/**
	 * Asking is what runs the function, so asking often used to register often -
	 * and nothing ever unregistered those instances again.
	 */
	@Test
	void testCompute_askingFromSeveralContextsRegistersTheServiceOnlyOnce() throws InvalidSyntaxException {
		injectedInto(partContext("first"), IDeltaService.class);
		injectedInto(partContext("second"), IDeltaService.class);
		injectedInto(partContext("third"), IEventBrokerBridge.class);

		assertThat(registrationsOf(IDeltaService.class), is(1));
		assertThat(registrationsOf(IEventBrokerBridge.class), is(1));
	}

	/**
	 * The point of it all: a part that opens after the fact sees the changes that
	 * happened before it, because there is only ever the one log.
	 */
	@Test
	void testDeltaService_aPartOpenedAfterAChangeSeesIt() {
		_inse.createInstanz(ROOT, Type.SEND);

		IDeltaService openedAfterwards = injectedInto(partContext("late"), IDeltaService.class);

		assertThat(openedAfterwards.getDeltas(), hasSize(3));
	}

	/** And the other way round: a save on the shared log empties it for everyone. */
	@Test
	void testDeltaService_aSaveThroughOneContextEmptiesTheLogForTheOther() {
		IDeltaService fromPart = injectedInto(partContext("part"), IDeltaService.class);
		_inse.createInstanz(ROOT, Type.SEND);

		ProductRuntime.deltaService().saveDeltas();

		assertThat(fromPart.getDeltas(), contains(IDeltaService.START_EVENT));
	}

	/**
	 * A part context dies with its part, and disposing a context un-injects
	 * everything that was made from it. The shared instance must not be one of
	 * those, or closing a view would leave the registry handing out a service whose
	 * fields are null.
	 */
	@Test
	void testCompute_theSharedInstanceOutlivesTheContextThatAskedForIt() {
		IEclipseContext partContext = partContext("closed again");
		injectedInto(partContext, IEventBrokerBridge.class);
		injectedInto(partContext, IDeltaService.class);

		partContext.dispose();
		_partContexts.remove(partContext);

		assertThat(ProductRuntime.broker().send("de/tonsias/test/afterDispose", null), is(true));
		assertThat(ProductRuntime.deltaService().getDeltas(), contains(IDeltaService.START_EVENT));
	}
}
