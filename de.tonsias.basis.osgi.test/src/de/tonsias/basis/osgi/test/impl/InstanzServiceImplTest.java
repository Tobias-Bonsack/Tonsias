package de.tonsias.basis.osgi.test.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyOrNullString;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasItems;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.util.concurrent.CompletionException;

import org.eclipse.e4.core.services.events.IEventBroker;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IKeyService;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants;
import de.tonsias.basis.osgi.intf.non.service.InstanzEventConstants.InstanzEvent;
import de.tonsias.basis.osgi.util.OsgiUtil;

public class InstanzServiceImplTest {
	final String _parentKey = "0";

	IInstanzService _inse;

	@BeforeEach
	void beforeEach() {
		_inse = OsgiUtil.getService(IInstanzService.class);
	}

	@AfterEach
	void afterEach() {
		try {
			OsgiUtil.getService(IDeltaService.class).saveDeltas();
		} catch (CompletionException e) {
		}
	}

	@Test
	void testDeleteInstanz_propageteAllChildren()
			throws NoSuchFieldException, SecurityException, IllegalArgumentException, IllegalAccessException {
		IInstanz instanz = _inse.createInstanz(_parentKey, Type.SEND);
		IInstanz instanz2 = _inse.createInstanz(instanz.getOwnKey(), Type.SEND);

		_inse.removeSubtreeInstanz(instanz.getOwnKey(), Type.SEND);

		assertThat(instanz.getParentKey(), is(emptyOrNullString()));
		assertThat(instanz2.getParentKey(), is(instanz.getOwnKey()));
		assertThat(instanz.getChildren(), hasItem(instanz2.getOwnKey()));
		assertThat(instanz2.getChildren(), hasSize(0));
	}

	@Test
	void testDeleteInstanz_inputNotValid() {
		assertThat(_inse.removeChild("invalid", "1", Type.SEND), is(false));
		assertThat(_inse.removeChild("0", null, Type.SEND), is(false));
		assertThat(_inse.removeChild("0", "", Type.SEND), is(false));
		assertThat(_inse.removeChild("0", "   ", Type.SEND), is(false));
	}

	@Test
	void testDeleteInstanz_nothingRemoved() {
		assertThat(_inse.removeChild("0", "invalid", Type.SEND), is(false));
	}

	@Test
	void testDeleteInstanz_validRemoved() {
		IInstanz toDelete = _inse.createInstanz(_parentKey, Type.SEND);
		EventHandler eventSpy = spy(new EventHandler() {
			@Override
			public void handleEvent(Event event) {
				InstanzEventConstants.InstanzEvent property = (InstanzEvent) event.getProperty(IEventBroker.DATA);
				assertThat(property._key(), is(toDelete.getOwnKey()));
				assertThat(property._parentKey(), is(null));
			}
		});
		OsgiUtil.getService(IEventBrokerBridge.class).subscribe(InstanzEventConstants.DELETE, eventSpy, true);
		IInstanz notToRemove = _inse.createInstanz(_parentKey, Type.SEND);

		boolean isRemoved = _inse.removeChild(_parentKey, toDelete.getOwnKey(), Type.SEND);
		assertThat(isRemoved, is(true));
		assertThat(_inse.resolveKey(_parentKey).get().getChildren(), hasItem(notToRemove.getOwnKey()));
		assertThat(_inse.resolveKey(_parentKey).get().getChildren(), not(hasItem(toDelete.getOwnKey())));
		verify(eventSpy, times(1)).handleEvent(any());
	}

	@Test
	void testRemoveChild_validRemoved() {
		IInstanz instanz = _inse.createInstanz(_parentKey, Type.SEND);

		_inse.removeChild(_parentKey, instanz.getOwnKey(), Type.SEND);

		assertThat(_inse.resolveKey(_parentKey).get().getChildren(), not(hasItem(instanz.getOwnKey())));
		assertThat(instanz.getParentKey(), is(nullValue()));
	}

	@Test
	void testRemoveChild_notHisChild_nothingHappens() {
		IInstanz instanz = _inse.createInstanz(_parentKey, Type.SEND);
		IInstanz instanz2 = _inse.createInstanz(_parentKey, Type.SEND);

		boolean removeChild = _inse.removeChild(instanz.getOwnKey(), instanz2.getOwnKey(), Type.SEND);

		assertThat(removeChild, is(false));
		assertThat(_inse.resolveKey(_parentKey).get().getChildren(),
				hasItems(instanz.getOwnKey(), instanz2.getOwnKey()));
		assertThat(instanz.getParentKey(), is(_parentKey));
		assertThat(instanz2.getParentKey(), is(_parentKey));
	}

	@Test
	void testCreateInstanz_parentKeyInvalid() {
		assertThat(_inse.createInstanz(null, IEventBrokerBridge.Type.SEND), is(nullValue()));
		assertThat(_inse.createInstanz("", IEventBrokerBridge.Type.SEND), is(nullValue()));
		assertThat(_inse.createInstanz("  ", IEventBrokerBridge.Type.SEND), is(nullValue()));
	}

	@Test
	void testCreateInstanz_validParentKey() throws InterruptedException {
		String previewNextKey = OsgiUtil.getService(IKeyService.class).previewNextKey();
		IInstanz instanz = _inse.createInstanz(_parentKey, IEventBrokerBridge.Type.SEND);
		assertThat(instanz.getOwnKey(), is(previewNextKey));
		assertThat(instanz.getParentKey(), is(_parentKey));
		assertThat(_inse.resolveKey(_parentKey).get().getChildren(), hasItem(previewNextKey));
	}

	@Test
	void testPutChild_parentKeyNotResolvable() {
		assertThat(_inse.putChild("RandomKey", "test", IEventBrokerBridge.Type.SEND), is(false));
	}

	@Test
	void testPutChild_childKeyNotValid() {
		assertThat(_inse.putChild(_parentKey, null, IEventBrokerBridge.Type.SEND), is(false));
		assertThat(_inse.putChild(_parentKey, "", IEventBrokerBridge.Type.SEND), is(false));
		assertThat(_inse.putChild(_parentKey, "  ", IEventBrokerBridge.Type.SEND), is(false));
	}

	@Test
	void testPutChild_alreadyChild() {
		_inse.resolveKey(_parentKey).get().addChildKeys("child");

		assertThat(_inse.putChild(_parentKey, "child", IEventBrokerBridge.Type.SEND), is(false));
	}

	@Test
	void testPutChild_moveChild() throws InterruptedException {
		IInstanz oldParent = _inse.resolveKey(_parentKey).get();
		IInstanz newParent = _inse.createInstanz(_parentKey, IEventBrokerBridge.Type.SEND);
		IInstanz toMove = _inse.createInstanz(_parentKey, IEventBrokerBridge.Type.SEND);

		assertThat(_inse.putChild(newParent.getOwnKey(), toMove.getOwnKey(), IEventBrokerBridge.Type.SEND), is(true));

		assertThat(newParent.getChildren(), contains(toMove.getOwnKey()));
		assertThat(toMove.getParentKey(), is(newParent.getOwnKey()));
		assertThat(oldParent.getChildren(), not(hasItem(toMove.getOwnKey())));
	}
}
