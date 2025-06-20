package de.tonsias.basis.logic.dialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Optional;

import org.eclipse.core.runtime.jobs.JobGroup;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;

public class CreateInstanzDialogLogic {

	Collection<TableRecord> _tableInput = new ArrayList<>();

	IInstanzService _ins;

	ISingleValueService _sin;

	JobGroup _jobGroup;

	private IBasicPreferenceService _basic;

	private IInstanz _iParent;

	public CreateInstanzDialogLogic(IInstanzService service, ISingleValueService service2,
			IBasicPreferenceService basic) {
		_ins = service;
		_sin = service2;
		_basic = basic;
		_jobGroup = new JobGroup("CreateInstanzDialogLogic JobGroup", 1, 0);

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
	};

}
