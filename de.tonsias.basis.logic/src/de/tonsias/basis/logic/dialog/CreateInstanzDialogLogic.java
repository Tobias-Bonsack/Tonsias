package de.tonsias.basis.logic.dialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.tonsias.basis.logic.part.InstanzChoices;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;

public class CreateInstanzDialogLogic {

	Collection<TableRecord> _tableInput = new ArrayList<>();

	IInstanzService _ins;

	ISingleValueService _sin;

	private IBasicPreferenceService _basic;

	private IInstanz _iParent;

	public CreateInstanzDialogLogic(IInstanzService service, ISingleValueService service2,
			IBasicPreferenceService basic) {
		_ins = service;
		_sin = service2;
		_basic = basic;

		Optional<String> parameterName = _basic.getValue(IBasicPreferenceService.Key.MODEL_VIEW_TEXT.getKey(),
				String.class);
		if (parameterName.isPresent()) {
			_tableInput.add(new TableRecord(SingleValueType.SINGLE_STRING, parameterName.get(), "Model View Name"));
		}
	}

	public void addNewEntry() {
		_tableInput.add(new TableRecord(SingleValueType.SINGLE_STRING, "parameterName", "Value"));
	}

	public void removeSelectedEntry(Object object) {
		_tableInput.remove(object);
	}

	public Collection<TableRecord> getInput() {
		return _tableInput;
	}

	/**
	 * A row of the table: what kind of attribute, under what name, holding what.
	 * The value is whatever the cell editor of the column left there - text for the
	 * types that are typed into, and the key of the target for a
	 * {@code SingleValueType.SINGLE_INSTANZ} row, which is chosen from the same
	 * tree the value dialog offers.
	 */
	public class TableRecord {
		public SingleValueType type;
		public String parameterName;
		public Object value;

		public TableRecord(SingleValueType type, String parameterName, Object value) {
			this.type = type;
			this.parameterName = parameterName;
			this.value = value;

		}
	}

	/**
	 * Changes what a row is, and drops a value the new type would not take with it.
	 * A relation stores a key and nothing else, so text typed under another type is
	 * no starting point for it - and the key of an instanz is no text anybody meant
	 * to write either.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/75">#75</a>
	 */
	public void setType(TableRecord row, SingleValueType type) {
		boolean wasRelation = row.type == SingleValueType.SINGLE_INSTANZ;
		row.type = type;

		if (wasRelation != (type == SingleValueType.SINGLE_INSTANZ)) {
			row.value = "";
		}
	}

	/**
	 * What the value column shows for a row. A relation stores the key of its
	 * target, which is no text to put in front of anybody - it reads as the target
	 * does everywhere else.
	 *
	 * @return the label of the target, and an empty string for a relation pointing
	 *         nowhere or at an instanz the walk did not reach
	 */
	public String valueLabel(TableRecord row) {
		if (row.type != SingleValueType.SINGLE_INSTANZ) {
			return String.valueOf(row.value);
		}
		return instanzChoices().labelOf(String.valueOf(row.value)).orElse("");
	}

	/**
	 * The instanzen a relation can be pointed at. Built per call rather than kept:
	 * the dialog stays open while the model can change under it.
	 */
	public InstanzChoices instanzChoices() {
		return new InstanzChoices(_ins, _sin, _basic);
	}

	public void setInstanzParent(IInstanz iParent) {
		_iParent = iParent;
	}

	public void okPressed(IEventBrokerBridge broker) {
		new Job("Create new instanz with values") {

			@Override
			protected IStatus run(IProgressMonitor monitor) {
				broker.send(EventConstants.OPEN_OPERATION, null);

				var newInstanz = _ins.createInstanz(_iParent.getOwnKey(), Type.SEND);
				for (TableRecord tableRecord : _tableInput) {
					_sin.createNew(tableRecord.type.getClazz(), newInstanz.getOwnKey(), tableRecord.parameterName,
							tableRecord.value, Type.SEND);
				}

				broker.send(EventConstants.CLOSE_OPERATION, null);
				return Status.OK_STATUS;
			}
		}.schedule();
	};

}
