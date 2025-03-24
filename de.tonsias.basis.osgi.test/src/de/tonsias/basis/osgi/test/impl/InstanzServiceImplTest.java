package de.tonsias.basis.osgi.test.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tonsias.basis.data.access.osgi.intf.DeleteService;
import de.tonsias.basis.data.access.osgi.intf.LoadService;
import de.tonsias.basis.data.access.osgi.intf.SaveService;
import de.tonsias.basis.model.impl.Instanz;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.impl.InstanzServiceImpl;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;

@ExtendWith(MockitoExtension.class)
public class InstanzServiceImplTest {

	@Mock
	private IKeyService _keyService;

	@Mock
	private LoadService _loadService;

	@Mock
	private SaveService _saveService;

	@Mock
	private DeleteService _deleteService;

	@Mock
	private IEventBrokerBridge _broker;

	private Map<String, IInstanz> _cache;

	private InstanzServiceImpl _inse;

	@BeforeEach
	void beforeEach() {
		_cache = new HashMap<>();
		_inse = new InstanzServiceImpl();

		injectDependencies(_inse, "_keyService", _keyService);
		injectDependencies(_inse, "_loadService", _loadService);
		injectDependencies(_inse, "_saveService", _saveService);
		injectDependencies(_inse, "_deleteService", _deleteService);
		injectDependencies(_inse, "_broker", _broker);
		injectDependencies(_inse, "_cache", _cache);
	}

	@Test
	void testCreateInstanz_parentKeyInvalid() {
		assertThat(_inse.createInstanz(null), is(nullValue()));
		assertThat(_inse.createInstanz(""), is(nullValue()));
		assertThat(_inse.createInstanz("  "), is(nullValue()));
	}

	@Test
	void testCreateInstanz_validParentKey() {
		when(_keyService.generateKey()).thenReturn("KEY");
		when(_broker.post(any(), any())).thenReturn(true);

		IInstanz instanz = _inse.createInstanz("parentKey");
		assertThat(instanz.getOwnKey(), is("KEY"));
		assertThat(instanz.getParentKey(), is("parentKey"));
		verify(_broker, times(1)).post(eq(InstanzEventConstants.NEW), any());

	}

	@Test
	void testPutChild_parentKeyNotResolvable() {
		when(_loadService.loadFromGson(any(), any())).thenReturn(null);

		assertThat(_inse.putChild(null, "test"), is(false));

		verify(_broker, times(0)).post(eq(InstanzEventConstants.CHILD_LIST_CHANGE), any());
	}

	@Test
	void testPutChild_childKeyNotValid() {
		when(_loadService.loadFromGson(any(), any())).thenReturn(new Instanz("test"));

		assertThat(_inse.putChild("test", null), is(false));
		assertThat(_inse.putChild("test", ""), is(false));
		assertThat(_inse.putChild("test", "  "), is(false));

		verify(_broker, times(0)).post(eq(InstanzEventConstants.CHILD_LIST_CHANGE), any());
	}

	@Test
	void testPutChild_alreadyChild() {
		Instanz value = spy(new Instanz("test"));
		value.addChildKeys("child");
		when(_loadService.loadFromGson(any(), any())).thenReturn(value);

		assertThat(_inse.putChild("test", "child"), is(false));

		verify(value, times(2)).addChildKeys(anyString());
		verify(_broker, times(0)).post(eq(InstanzEventConstants.CHILD_LIST_CHANGE), any());
	}

	@Test
	void testPutChild_newChild() {
		Instanz value = spy(new Instanz("test"));
		when(_loadService.loadFromGson(any(), any())).thenReturn(value);
		when(_broker.post(any(), any())).thenReturn(true);

		assertThat(_inse.putChild("test", "child"), is(true));

		verify(value, times(1)).addChildKeys(any());
		verify(_broker, times(1)).post(eq(InstanzEventConstants.CHILD_LIST_CHANGE), any());
	}

	private void injectDependencies(Object target, String fieldName, Object value) {
		try {
			var field = target.getClass().getDeclaredField(fieldName);
			field.setAccessible(true);
			field.set(target, value);
			field.setAccessible(false);
		} catch (NoSuchFieldException | IllegalAccessException e) {
			throw new RuntimeException("Inject not possible: " + fieldName, e);
		}
	}

}
