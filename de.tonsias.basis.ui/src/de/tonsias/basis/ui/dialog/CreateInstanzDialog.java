package de.tonsias.basis.ui.dialog;

import java.util.Arrays;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.CellEditor;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.ComboBoxCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.widgets.ButtonFactory;
import org.eclipse.jface.widgets.CompositeFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.logic.dialog.CreateInstanzDialogLogic;
import de.tonsias.basis.logic.dialog.CreateInstanzDialogLogic.TableRecord;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.util.OsgiUtil;

public class CreateInstanzDialog extends Dialog {

	private CreateInstanzDialogLogic _logic = new CreateInstanzDialogLogic(OsgiUtil.getService(IInstanzService.class),
			OsgiUtil.getService(ISingleValueService.class), OsgiUtil.getService(IBasicPreferenceService.class));

	private TableViewer _viewer;

	public CreateInstanzDialog(Shell parentShell, IInstanz iParent) {
		super(parentShell);
		_logic.setInstanzParent(iParent);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText("Create new Instanz");
	}

	@Override
	protected Point getInitialLocation(Point initialSize) {
		Point point = new Point(600, 600);
		getShell().setMinimumSize(point);
		return point;
	}

	@Override
	protected boolean isResizable() {
		return true;
	}

	@Override
	protected Control createDialogArea(Composite composite) {
		Composite parent = (Composite) super.createDialogArea(composite);

		createTableButtons(parent);
		createTable(parent);

		return parent;
	}

	private void createTableButtons(Composite parent) {
		var buttonParent = CompositeFactory.newComposite(SWT.None)
				.layout(GridLayoutFactory.fillDefaults().numColumns(2).create()).create(parent);
		ButtonFactory.newButton(SWT.PUSH).text("ADD").onSelect(this::addNewEntry).create(buttonParent);
		ButtonFactory.newButton(SWT.PUSH).text("REMOVE").onSelect(this::removeSelectedEntry)
				.layoutData(GridDataFactory.swtDefaults().create()).create(buttonParent);
	}

	void addNewEntry(SelectionEvent event) {
		_logic.addNewEntry();
		_viewer.refresh();

	}

	void removeSelectedEntry(SelectionEvent event) {
		_logic.removeSelectedEntry(_viewer.getStructuredSelection().getFirstElement());
		_viewer.refresh();
	}

	private void createTable(Composite parent) {
		_viewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewer.getTable());
		_viewer.getTable().setHeaderVisible(true);
		_viewer.getTable().setLinesVisible(true);

		createColumn("Single Value Type", 200, tRec -> tRec.type.getClazz().getSimpleName(), //
				getEditingSupport(//
						(element, value) -> {
							element.type = SingleValueType.values()[(int) value];
							_viewer.update(element, null);
						}, //
						element -> Arrays.asList(SingleValueType.values()).indexOf(element.type), //
						element -> {
							String[] array = Arrays.stream(SingleValueType.values())
									.map(i -> i.getClazz().getSimpleName()).collect(Collectors.toList())
									.toArray(String[]::new);
							var editor = new ComboBoxCellEditor(_viewer.getTable(), array);

							return editor;
						}));

		createColumn("Parameter Name", 200, tRec -> tRec.parameterName, //
				getEditingSupport(//
						(element, value) -> {
							element.parameterName = (String) value;
							_viewer.update(element, null);
						}, //
						element -> element.parameterName, //
						element -> new TextCellEditor(_viewer.getTable())));

		createColumn("Value", 200, tRec -> tRec.value.toString(), //
				getEditingSupport(//
						(element, value) -> {
							element.value = value;
							_viewer.update(element, null);
						}, //
						element -> element.value, //
						element -> new TextCellEditor(_viewer.getTable())));


		_viewer.setContentProvider(ArrayContentProvider.getInstance());
		_viewer.setInput(_logic.getInput());
	}

	private void createColumn(String title, int width, IColumnLabelProvider provider, EditingSupport editingSupport) {
		TableViewerColumn column = new TableViewerColumn(_viewer, SWT.NONE);
		column.getColumn().setText(title);
		column.getColumn().setWidth(width);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return provider.getText((TableRecord) element);
			}
		});
		column.setEditingSupport(editingSupport);
	}

	@Override
	protected void okPressed() {
		_logic.okPressed(OsgiUtil.getService(IEventBrokerBridge.class));
		super.okPressed();
	}

	private interface IColumnLabelProvider {
		String getText(TableRecord person);
	}

	public EditingSupport getEditingSupport(BiConsumer<TableRecord, Object> setValue,
			Function<TableRecord, Object> getValue, Function<TableRecord, CellEditor> getCellEditor) {
		return new EditingSupport(_viewer) {

			@Override
			protected void setValue(Object element, Object value) {
				setValue.accept((TableRecord) element, value);
			}

			@Override
			protected Object getValue(Object element) {
				return getValue.apply((TableRecord) element);
			}

			@Override
			protected CellEditor getCellEditor(Object element) {
				return getCellEditor.apply((TableRecord) element);
			}

			@Override
			protected boolean canEdit(Object element) {
				return true;
			}
		};
	}

}
