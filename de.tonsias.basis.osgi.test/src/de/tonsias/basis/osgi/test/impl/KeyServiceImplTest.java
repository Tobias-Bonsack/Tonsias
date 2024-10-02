package de.tonsias.basis.osgi.test.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.eclipse.core.runtime.preferences.IEclipsePreferences;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import de.tonsias.basis.osgi.impl.KeyServiceImpl;

public class KeyServiceImplTest {

	private KeyServiceImplTestee _keyService;

	private IEclipsePreferences _pref;

	@BeforeEach
	void setup() {
		_pref = mock(IEclipsePreferences.class);

		_keyService = new KeyServiceImplTestee(_pref);
	}

	@Test
	void testInitKey_blankNode() {
		when(_pref.get(anyString(), anyString())).thenReturn("");

		String key = _keyService.initKey();

		assertThat(key, is("0"));
	}

	@Test
	void testInitKey_validNode() {
		when(_pref.get(anyString(), anyString())).thenReturn("ZZ");

		String key = _keyService.initKey();

		assertThat(key, is("ZZ"));
	}

	@Test
	void testPreviewKey_notRealChange() {
		_keyService.setKey("aa");
		var key = _keyService.initKey();
		var previewNextKey = _keyService.previewNextKey();
		var key2 = _keyService.initKey();

		assertThat(key2, is(key));
		assertThat(previewNextKey, is("ba"));
	}

	@Test
	void testInitKey_withKey() {
		_keyService.setKey("a");

		String key = _keyService.initKey();

		assertThat(key, is("a"));
	}

	@ParameterizedTest
	@CsvSource({ //
			"a, b", //
			"z, 00", //
			"aA, bA", //
			"z0, 01", //
			"zz, 000", //
			"2z0, 3z0", //
			"zzT, 00U", //
			"zzzzz, 000000" //
	})
	void testGeneratekey_inArrayChange(String key, String nextKey) {
		_keyService.setKey(key);

		String newkey = _keyService.generateKey();

		assertThat(newkey, is(nextKey));
	}

	private static class KeyServiceImplTestee extends KeyServiceImpl {

		private IEclipsePreferences _node;

		KeyServiceImplTestee(IEclipsePreferences node) {
			this._node = node;
		}

		private void setKey(String key) {
			this._key = key;
		}

		@Override
		public String generateKey() {
			return super.generateKey();
		}

		@Override
		protected void flush(IEclipsePreferences node) {
		}

		@Override
		public IEclipsePreferences getNode() {
			return _node;
		}
	}
}
