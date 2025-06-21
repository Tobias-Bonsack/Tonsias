package de.tonsias.basis.logic.dialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

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
