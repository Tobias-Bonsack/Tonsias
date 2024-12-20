package de.tonsias.delta.view.ui;

import org.eclipse.swt.widgets.Composite;

import jakarta.annotation.PostConstruct;

public class DeltaView {

	@PostConstruct
	public void postConstruct(Composite parent) {
		System.out.println("Hello World!");
	}
}
