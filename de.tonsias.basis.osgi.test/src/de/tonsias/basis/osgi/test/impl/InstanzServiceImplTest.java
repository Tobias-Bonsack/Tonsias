package de.tonsias.basis.osgi.test.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;

import java.time.Duration;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.model.interfaces.IInstanz;
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
	void testCreateInstanz_parentKeyInvalid() {
		assertThat(_inse.createInstanz(null), is(nullValue()));
		assertThat(_inse.createInstanz(""), is(nullValue()));
		assertThat(_inse.createInstanz("  "), is(nullValue()));
	}

	@Test
	void testCreateInstanz_validParentKey() throws InterruptedException {
		IInstanz instanz = _inse.createInstanz(_parentKey);
		assertThat(instanz.getOwnKey(), is("1"));
		assertThat(instanz.getParentKey(), is(_parentKey));

		// Wait for events
		Thread.sleep(Duration.ofSeconds(1));

		assertThat(_inse.resolveKey(_parentKey).get().getChildren(), contains(instanz.getOwnKey()));
	}

	@Test
	void testPutChild_parentKeyNotResolvable() {
		assertThat(_inse.putChild("RandomKey", "test"), is(false));
	}

	@Test
	void testPutChild_childKeyNotValid() {
		assertThat(_inse.putChild(_parentKey, null), is(false));
		assertThat(_inse.putChild(_parentKey, ""), is(false));
		assertThat(_inse.putChild(_parentKey, "  "), is(false));
	}

	@Test
	void testPutChild_alreadyChild() {
		_inse.resolveKey(_parentKey).get().addChildKeys("child");

		assertThat(_inse.putChild(_parentKey, "child"), is(false));
	}

	@Test
	void testPutChild_newChild() throws InterruptedException {
		IInstanz oldParent = _inse.resolveKey(_parentKey).get();
		IInstanz newParent = _inse.createInstanz(_parentKey);
		IInstanz toMove = _inse.createInstanz(_parentKey);

		assertThat(_inse.putChild(newParent.getOwnKey(), toMove.getOwnKey()), is(true));

		Thread.sleep(Duration.ofSeconds(2));

		assertThat(newParent.getChildren(), contains(toMove.getOwnKey()));
		assertThat(toMove.getParentKey(), is(newParent.getOwnKey()));
		assertThat(oldParent.getChildren(), not(hasItem(toMove.getOwnKey())));
	}
}
