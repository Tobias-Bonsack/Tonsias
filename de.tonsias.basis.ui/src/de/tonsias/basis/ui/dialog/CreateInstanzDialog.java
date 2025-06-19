package de.tonsias.basis.ui.dialog;

import org.eclipse.jface.dialogs.Dialog;
import org.eclipse.jface.viewers.ArrayContentProvider;
import org.eclipse.jface.viewers.ColumnLabelProvider;
import org.eclipse.jface.viewers.TableViewer;
import org.eclipse.jface.viewers.TableViewerColumn;
import org.eclipse.jface.widgets.ButtonFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Control;
import org.eclipse.swt.widgets.Shell;

import de.tonsias.basis.logic.dialog.CreateInstanzDialogLogic;
import de.tonsias.basis.logic.dialog.CreateInstanzDialogLogic.TableRecord;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
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
	protected Control createDialogArea(Composite composite) {
		Composite parent = (Composite) super.createDialogArea(composite);

		createTableButtons(parent);
		createTable(parent);

		return parent;
	}

	private void createTableButtons(Composite parent) {
		ButtonFactory.newButton(SWT.PUSH).text("ADD").onSelect(_logic::addNewEntry).create(parent);

		ButtonFactory.newButton(SWT.PUSH).text("REMOVE").onSelect(_logic::removeSelectedEntry).create(parent);
	}

	private void createTable(Composite parent) {
		_viewer = new TableViewer(parent, SWT.BORDER | SWT.FULL_SELECTION);

		createColumn(_viewer, "SingleValueType", 100, tRec -> tRec.type().getClazz().getSimpleName());
		createColumn(_viewer, "SingleValueType", 100, tRec -> tRec.parameterName());
		createColumn(_viewer, "SingleValueType", 100, tRec -> tRec.value().toString());

		_viewer.setContentProvider(ArrayContentProvider.getInstance());
		_viewer.setInput(_logic.getInput());
	}

	private static void createColumn(TableViewer viewer, String title, int width, IColumnLabelProvider provider) {
		TableViewerColumn column = new TableViewerColumn(viewer, SWT.NONE);
		column.getColumn().setText(title);
		column.getColumn().setWidth(width);
		column.setLabelProvider(new ColumnLabelProvider() {
			@Override
			public String getText(Object element) {
				return provider.getText((TableRecord) element);
			}
		});
	}

	@Override
	protected void okPressed() {
		// TODO Auto-generated method stub
		super.okPressed();
	}

	private interface IColumnLabelProvider {
		String getText(TableRecord person);
	}

}
