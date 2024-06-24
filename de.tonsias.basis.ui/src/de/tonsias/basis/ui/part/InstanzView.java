package de.tonsias.basis.ui.part;

import org.eclipse.e4.core.di.annotations.Optional;
import org.eclipse.e4.ui.di.UIEventTopic;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.widgets.LabelFactory;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Group;
import org.eclipse.swt.widgets.Text;

import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.InstanzEventConstants;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class InstanzView {

	private IInstanz _shownInstanz = null;
	private Text ownKeyText;

	@PostConstruct
	public void postConstruct(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);

		createInstanzInfos(parent);
	}

	private void updateView() {
		ownKeyText.setText(_shownInstanz.getOwnKey());
	}

	private void createInstanzInfos(Composite com) {
		Group parent = new Group(com, SWT.None);
		parent.setText("Instanz");
		GridDataFactory.fillDefaults().applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(parent);

		LabelFactory.newLabel(SWT.None)//
				.text("Key")//
				.data(GridDataFactory.fillDefaults().create())//
				.create(parent);
		ownKeyText = TextFactory.newText(SWT.None)//
				.text(_shownInstanz != null ? _shownInstanz.getOwnKey() : "")//
				.data(GridDataFactory.fillDefaults().create())//
				.create(parent);

	}

	@Inject
	@Optional
	private void selectionEventListener(@UIEventTopic(InstanzEventConstants.SELECTED) IInstanz instanz) {
		if (instanz == null || instanz.equals(_shownInstanz)) {
			return;
		}
		_shownInstanz = instanz;
		updateView();
	}
}
