package de.tonsias.basis.osgi.test.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.isNotNull;

import java.util.concurrent.CompletionException;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.util.OsgiUtil;

public class SingleValueServiceImplTest {
	final String _originKey = "0";

	IInstanzService _ins;

	ISingleValueService _svs;

	IInstanz _instanz;

	@BeforeEach
	void beforeEach() {
		_ins = OsgiUtil.getService(IInstanzService.class);
		_instanz = _ins.createInstanz(_originKey, Type.SEND);

		_svs = OsgiUtil.getService(ISingleValueService.class);
	}

	@AfterEach
	void afterEach() {
		try {
			OsgiUtil.getService(IDeltaService.class).saveDeltas();
		} catch (CompletionException e) {
		}
	}

	@Test
	void testCreateNew_test() {
		SingleStringValue stringValue = _svs.createNew(SingleStringValue.class, _instanz.getOwnKey(), "parName",
				"value", Type.SEND);

		assertThat(stringValue, is(not(nullValue())));
		assertThat(stringValue.getConnectedInstanzKeys(), hasSize(1));
		assertThat(stringValue.getConnectedInstanzKeys(), hasItem(_instanz.getOwnKey()));
		assertThat(_instanz.getSingleValues(SingleValueType.SINGLE_STRING).size(), is(1));
	}

}
