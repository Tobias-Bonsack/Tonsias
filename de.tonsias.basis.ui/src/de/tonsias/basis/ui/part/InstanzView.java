package de.tonsias.basis.ui.part;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Map.Entry;

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

import com.google.common.collect.BiMap;

import de.tonsias.basis.model.enums.SingleValueTypes;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.model.interfaces.ISingleValue;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.InstanzEventConstants;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class InstanzView {

	private IInstanz _shownInstanz = null;

	private Text _ownKeyText;

	private Collection<Group> _groups = new ArrayList<Group>();

	private Composite _parent;

	@Inject
	private IInstanzService _instanzService;

	@Inject
	private ISingleValueService _singleService;

	@PostConstruct
	public void postConstruct(Composite parent) {
		GridLayoutFactory.fillDefaults().applyTo(parent);
		_parent = parent;

		if (_shownInstanz == null) {
			return;
		}
		createInstanzInfos();
		createSingleValueGroup();
		createChildren();
		createParent();
	}

	private void createParent() {
		Group parent = new Group(_parent, SWT.None);
		parent.setText("Parent");
		GridDataFactory.fillDefaults().applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(parent);
		_groups.add(parent);

		LabelFactory.newLabel(SWT.None)//
				.text(_shownInstanz.getParentKey())//
				.data(GridDataFactory.fillDefaults().create())//
				.create(parent);
	}

	private void createChildren() {
		Group parent = new Group(_parent, SWT.None);
		parent.setText("Children");
		GridDataFactory.fillDefaults().applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(1).applyTo(parent);
		_groups.add(parent);

		Collection<String> children = _shownInstanz.getChildren();
		for (String key : children) {
			LabelFactory.newLabel(SWT.None)//
					.text(key)//
					.data(GridDataFactory.fillDefaults().create())//
					.create(parent);
		}
	}

	private void updateView() {
		if (_ownKeyText == null) {
			createInstanzInfos();
		}
		_ownKeyText.setText(_shownInstanz.getOwnKey());

		_groups.forEach(group -> group.dispose());
		createSingleValueGroup();
		createParent();
		createChildren();

		_parent.layout();
	}

	private void createInstanzInfos() {
		Group parent = new Group(_parent, SWT.None);
		parent.setText("Instanz");
		GridDataFactory.fillDefaults().applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(parent);

		LabelFactory.newLabel(SWT.None)//
				.text("Key")//
				.data(GridDataFactory.fillDefaults().create())//
				.create(parent);
		_ownKeyText = TextFactory.newText(SWT.None)//
				.enabled(false)//
				.text(_shownInstanz.getOwnKey())//
				.data(GridDataFactory.fillDefaults().create())//
				.create(parent);

	}

	private void createSingleValueGroup() {
		Group parent = new Group(_parent, SWT.None);
		parent.setText("SingleValues");
		GridDataFactory.fillDefaults().applyTo(parent);
		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(parent);
		_groups.add(parent);

		SingleValueTypes[] values = SingleValueTypes.values();
		for (SingleValueTypes type : values) {
			Group typeGroup = new Group(parent, SWT.None);
			typeGroup.setText(type.name());
			GridDataFactory.fillDefaults().applyTo(typeGroup);
			GridLayoutFactory.fillDefaults().numColumns(3).applyTo(typeGroup);

			BiMap<String, String> singleValues = _shownInstanz.getSingleValues(type);
			for (Entry<String, String> attribute : singleValues.entrySet()) {
				LabelFactory.newLabel(SWT.None)//
						.text(attribute.getValue())//
						.data(GridDataFactory.fillDefaults().create())//
						.create(typeGroup);

				java.util.Optional<? extends ISingleValue<?>> singleValue = _singleService.resolveKey(type.getPath(),
						attribute.getKey(), type.getClazz());
				if (singleValue.isPresent()) {
					TextFactory.newText(SWT.None)//
							.text(singleValue.get().getValue().toString())//
							.onModify(event -> {
								String text = ((Text) event.widget).getText();
								singleValue.get().tryToSetValue(text);
							}).data(GridDataFactory.fillDefaults().create())//
							.create(typeGroup);

					TextFactory.newText(SWT.None)//
							.enabled(false)//
							.text("Key: " + singleValue.get().getOwnKey())//
							.data(GridDataFactory.fillDefaults().create())//
							.create(typeGroup);
				}
			}
		}
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
