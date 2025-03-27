package de.tonsias.basis.osgi.test.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.util.OsgiUtil;

public class InstanzServiceImplTest {
	final String _parentKey = "0";

	IInstanzService _inse;

	@BeforeEach
	void beforeEach() {
		_inse = OsgiUtil.getService(IInstanzService.class);
	}

	@Test
	void testRemoveChild_inputNotValid() {
		assertThat(_inse.removeChild("invalid", "1", Type.SEND), is(false));
		assertThat(_inse.removeChild("0", null, Type.SEND), is(false));
		assertThat(_inse.removeChild("0", "", Type.SEND), is(false));
		assertThat(_inse.removeChild("0", "   ", Type.SEND), is(false));
	}

	@Test
	void testRemoveChild_nothingRemoved() {
		assertThat(_inse.removeChild("0", "invalid", Type.SEND), is(false));
	}

	@Test
	void testRemovChild_validRemoved() {
		IInstanz toRemove = _inse.createInstanz(_parentKey, Type.SEND);
		IInstanz notToRemove = _inse.createInstanz(_parentKey, Type.SEND);

		boolean isRemoved = _inse.removeChild(_parentKey, toRemove.getOwnKey(), Type.SEND);
		assertThat(isRemoved, is(true));
		assertThat(_inse.resolveKey(_parentKey).get().getChildren(), hasItem(notToRemove.getOwnKey()));
		assertThat(_inse.resolveKey(_parentKey).get().getChildren(), not(hasItem(toRemove.getOwnKey())));
	}

	@Test
	void testCreateInstanz_parentKeyInvalid() {
		assertThat(_inse.createInstanz(null, IEventBrokerBridge.Type.SEND), is(nullValue()));
		assertThat(_inse.createInstanz("", IEventBrokerBridge.Type.SEND), is(nullValue()));
		assertThat(_inse.createInstanz("  ", IEventBrokerBridge.Type.SEND), is(nullValue()));
	}

	@Test
	void testCreateInstanz_validParentKey() throws InterruptedException {
		IInstanz instanz = _inse.createInstanz(_parentKey, IEventBrokerBridge.Type.SEND);
		assertThat(instanz.getOwnKey(), is("1"));
		assertThat(instanz.getParentKey(), is(_parentKey));
		assertThat(_inse.resolveKey(_parentKey).get().getChildren(), hasItem("1"));
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
