package de.tonsias.basis.osgi.test.impl;

import org.junit.jupiter.api.Test;

import de.tonsias.basis.osgi.impl.util.ChangePropagationListener;

public class ChangePropagationListenerTest {

	@Test
	void test() {
		ChangePropagationListener changePropagationListener = new ChangePropagationListener();
		changePropagationListener.loadServices();
	}
}
