package de.tonsias.basis.osgi.test.impl;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.osgi.service.event.Event;

import de.tonsias.basis.osgi.impl.DeltaServiceImpl;
import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;

@ExtendWith(MockitoExtension.class)
public class DeltaServiceImplTest {

	private IDeltaService _service;
	private IInstanzService mock3;
	private ISingleValueService mock4;

	@BeforeEach
	void beforeEach() {
		DeltaServiceImplTestee testee = new DeltaServiceImplTestee();
		mock3 = mock(IInstanzService.class);
		mock4 = mock(ISingleValueService.class);

		testee.set_instanzService(mock3);
		testee.set_singleValueService(mock4);

		_service = testee;
	}

	@Test
	void testSaveDeltas() {
		Event mock = mock(Event.class);
		when(mock.getTopic()).thenReturn("test");
		_service.handleEvent(mock);

		_service.saveDeltas();
		verify(mock3, times(1)).saveAll(any());
		verify(mock3, times(1)).deleteAll(any());
		verify(mock4, times(1)).saveAll(any());
		verify(mock4, times(1)).deleteAll(any());
	}

	private static class DeltaServiceImplTestee extends DeltaServiceImpl {
		public void set_instanzService(IInstanzService _instanzService) {
			this._instanzService = _instanzService;
		}

		public void set_singleValueService(ISingleValueService _singleValueService) {
			this._singleValueService = _singleValueService;
		}
	}
}
