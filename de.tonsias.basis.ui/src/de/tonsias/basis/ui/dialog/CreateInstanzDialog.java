package de.tonsias.basis.ui.dialog;

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
import org.eclipse.jface.viewers.DialogCellEditor;
import org.eclipse.jface.viewers.EditingSupport;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.viewers.TextCellEditor;
import org.eclipse.jface.widgets.ButtonFactory;
import org.eclipse.jface.widgets.CompositeFactory;
import org.eclipse.jface.window.Window;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Point;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.logic.dialog.CreateInstanzDialogLogic;
import de.tonsias.basis.logic.dialog.CreateInstanzDialogLogic.TableRecord;
import de.tonsias.basis.model.enums.ValueContentType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IMultiValueService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.util.OsgiUtil;
import de.tonsias.basis.ui.i18n.Messages;
import de.tonsias.basis.ui.util.MessagesUtil;
import de.tonsias.basis.ui.widget.InstanzSelectionDialog;

public class CreateInstanzDialog extends Dialog {

	private CreateInstanzDialogLogic _logic = new CreateInstanzDialogLogic(OsgiUtil.getService(IInstanzService.class),
			OsgiUtil.getService(ISingleValueService.class), OsgiUtil.getService(IMultiValueService.class),
			OsgiUtil.getService(IBasicPreferenceService.class));

	private TableViewer _viewer;

	private final Messages _messages;

	public CreateInstanzDialog(Shell parentShell, IInstanz iParent, Messages messages) {
		super(parentShell);
		_messages = messages;
		_logic.setInstanzParent(iParent);
	}

	@Override
	protected void configureShell(Shell newShell) {
		super.configureShell(newShell);
		newShell.setText(_messages.dialog_createInstanz_title);
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
		ButtonFactory.newButton(SWT.PUSH).text(_messages.constant_add).onSelect(this::addNewEntry).create(buttonParent);
		ButtonFactory.newButton(SWT.PUSH).text(_messages.constant_remove).onSelect(this::removeSelectedEntry)
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

		// the combo shows both families at once, so its selection indexes into the
		// list the logic keeps rather than into an enum's ordinals
		createColumn(_messages.constant_type, 200, tRec -> MessagesUtil.getValueTypeLabel(_messages, tRec.type), //
				getEditingSupport(//
						(element, value) -> {
							// the logic drops a value the new type would not take with it, so the row
							// never carries text under a type that stores a key or a list
							_logic.setType(element, _logic.typeAt((int) value));
							_viewer.update(element, null);
						}, //
						element -> _logic.indexOf(element), //
						element -> {
							String[] array = CreateInstanzDialogLogic.TYPE_CHOICES.stream()
									.map(type -> MessagesUtil.getValueTypeLabel(_messages, type))
									.collect(Collectors.toList()).toArray(String[]::new);
							return new ComboBoxCellEditor(_viewer.getTable(), array);
						}));

		createColumn(_messages.constant_parameterName, 200, tRec -> tRec.parameterName, //
				getEditingSupport(//
						(element, value) -> {
							element.parameterName = (String) value;
							_viewer.update(element, null);
						}, //
						element -> element.parameterName, //
						element -> new TextCellEditor(_viewer.getTable())));

		createColumn(_messages.constant_value, 200, tRec -> _logic.valueLabel(tRec), //
				getEditingSupport(//
						(element, value) -> {
							element.value = value;
							_viewer.update(element, null);
						}, //
						element -> element.value, //
						this::valueCellEditor));


		_viewer.setContentProvider(ArrayContentProvider.getInstance());
		_viewer.setInput(_logic.getInput());
	}

	/**
	 * The one column whose editor depends on the row. Two of the ten types are no
	 * text: a relation is chosen from the tree the value dialog offers rather than
	 * typed as a raw key, and a list is not one line at all - a cell is.
	 * <p>
	 * A {@link TextCellEditor} takes a {@code String} and nothing else, so handing
	 * one a row holding a list is not a wrong label but an assertion inside JFace,
	 * thrown the moment the cell is clicked.
	 * </p>
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/75">#75</a>
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/88">#88</a>
	 */
	public CellEditor valueCellEditor(TableRecord row) {
		if (row.type.isMulti()) {
			return multiCellEditor(row);
		}
		if (row.type.getContentType() == ValueContentType.INSTANZ) {
			return instanzCellEditor();
		}
		return new TextCellEditor(_viewer.getTable());
	}

	/**
	 * The cell editor of a list: a cell that opens {@link MultiElementListDialog}.
	 * What it keeps is the list itself, in the shape the model would hold it, and
	 * what it shows is the row's own label - the same one the column shows when
	 * nobody is editing it.
	 */
	private CellEditor multiCellEditor(TableRecord row) {
		return new DialogCellEditor(_viewer.getTable()) {

			@Override
			protected Object openDialogBox(Control cellEditorWindow) {
				var dialog = new MultiElementListDialog(cellEditorWindow.getShell(), row.type.getContentType(),
						CreateInstanzDialogLogic.elementsOf(getValue()), _messages, _logic::instanzChoices);
				// cancelling keeps what the cell held - the editor writes back whatever comes
				// out of here, so handing back null would empty the cell
				return dialog.open() == Window.OK ? dialog.getElements() : getValue();
			}

			@Override
			protected void updateContents(Object value) {
				super.updateContents(_logic.valueLabel(row.type, value));
			}
		};
	}

	/**
	 * The cell editor of a relation: a cell that opens {@link InstanzSelectionDialog}
	 * rather than one that is typed into. What it keeps is the key of the chosen
	 * instanz, and what it shows is the label - the same pair the Instanz View
	 * shows on its button.
	 */
	private CellEditor instanzCellEditor() {
		return new DialogCellEditor(_viewer.getTable()) {

			@Override
			protected Object openDialogBox(Control cellEditorWindow) {
				var dialog = new InstanzSelectionDialog(cellEditorWindow.getShell(), _logic.instanzChoices().tree(),
						_messages, String.valueOf(getValue()));
				// cancelling keeps what the cell held - the editor writes back whatever comes
				// out of here, so handing back null would empty the cell
				return dialog.open() == Window.OK ? dialog.getSelectedKey() : getValue();
			}

			@Override
			protected void updateContents(Object value) {
				super.updateContents(_logic.instanzChoices().labelOf(String.valueOf(value)).orElse(""));
			}
		};
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

	/**
	 * The rows behind the table, so a test can set one up the way the type column
	 * does rather than reach past the dialog into the viewer.
	 */
	public CreateInstanzDialogLogic getLogic() {
		return _logic;
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
