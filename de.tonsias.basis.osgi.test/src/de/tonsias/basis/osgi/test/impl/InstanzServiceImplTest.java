package de.tonsias.basis.osgi.test.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.mockito.ArgumentMatchers.isNotNull;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;

import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.util.OsgiUtil;

@ExtendWith(MockitoExtension.class)
public class InstanzServiceImplTest {

	@BeforeAll
	static void beforeAll() {
		IInstanzService service = OsgiUtil.getService(IInstanzService.class);
		assertThat(service, isNotNull());
	}

	@Test
	void test() {

	}

}
