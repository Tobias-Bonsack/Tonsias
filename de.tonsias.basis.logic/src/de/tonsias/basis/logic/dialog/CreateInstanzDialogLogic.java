package de.tonsias.basis.logic.dialog;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.IStatus;
import org.eclipse.core.runtime.Status;
import org.eclipse.core.runtime.jobs.Job;

import de.tonsias.basis.logic.part.InstanzChoices;
import de.tonsias.basis.model.enums.IValueType;
import de.tonsias.basis.model.enums.MultiValueType;
import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.enums.ValueContentType;
import de.tonsias.basis.model.enums.ValueTypes;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge;
import de.tonsias.basis.osgi.intf.IEventBrokerBridge.Type;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.IMultiValueService;
import de.tonsias.basis.osgi.intf.ISingleValueService;
import de.tonsias.basis.osgi.intf.non.service.EventConstants;

public class CreateInstanzDialogLogic {

	/**
	 * The order the type combo offers, and the list its selection indexes into.
	 * Written down here rather than read off an enum ordinal: the combo shows both
	 * families at once, and no single enum can number them.
	 */
	public static final List<IValueType> TYPE_CHOICES = ValueTypes.valuesList();

	Collection<TableRecord> _tableInput = new ArrayList<>();

	IInstanzService _ins;

	ISingleValueService _sin;

	IMultiValueService _min;

	private IBasicPreferenceService _basic;

	private IInstanz _iParent;

	public CreateInstanzDialogLogic(IInstanzService service, ISingleValueService service2, IMultiValueService service3,
			IBasicPreferenceService basic) {
		_ins = service;
		_sin = service2;
		_min = service3;
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

	/** where the combo's selection index comes from and goes back to */
	public int indexOf(TableRecord row) {
		return TYPE_CHOICES.indexOf(row.type);
	}

	public IValueType typeAt(int index) {
		return TYPE_CHOICES.get(index);
	}

	/**
	 * A row of the table: what kind of attribute, under what name, holding what. The
	 * value is whatever the cell editor of the column left there - text for the
	 * types that are typed into, the key of the target for a relation row, and a
	 * {@link Collection} for every row whose type holds a list.
	 */
	public class TableRecord {
		public IValueType type;
		public String parameterName;
		public Object value;

		public TableRecord(IValueType type, String parameterName, Object value) {
			this.type = type;
			this.parameterName = parameterName;
			this.value = value;

		}
	}

	/**
	 * Changes what a row is, and drops a value the new type would not take with it.
	 * A relation stores a key and nothing else, so text typed under another type is
	 * no starting point for it - and the key of an instanz is no text anybody meant
	 * to write either. A list and a single value are the same again: a list is no
	 * text and a text is no list.
	 *
	 * @see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/75">#75</a>
	 */
	public void setType(TableRecord row, IValueType type) {
		boolean contentChanged = relationChanged(row.type, type);
		boolean kindChanged = row.type.isMulti() != type.isMulti();
		row.type = type;

		if (contentChanged || kindChanged) {
			row.value = type.isMulti() ? new ArrayList<>() : "";
		}
	}

	private boolean relationChanged(IValueType from, IValueType to) {
		return (from.getContentType() == ValueContentType.INSTANZ) != (to.getContentType() == ValueContentType.INSTANZ);
	}

	/**
	 * The elements a value holds, for the rows whose type is a list. Everything the
	 * value column leaves in a row goes through here, so a row nobody has edited yet
	 * - or one just switched over from a single value - reads as the empty list
	 * rather than as the text that stood there.
	 */
	public static Collection<?> elementsOf(Object value) {
		return value instanceof Collection<?> collection ? collection : List.of();
	}

	public String valueLabel(TableRecord row) {
		return valueLabel(row.type, row.value);
	}

	/**
	 * What the value column shows for a row. A relation stores the key of its
	 * target, which is no text to put in front of anybody - it reads as the target
	 * does everywhere else, and a list of them reads as all of its targets.
	 * <p>
	 * Takes the two halves of a row rather than the row, because the cell editor of
	 * a list has to label what it is about to hand back - which is not in the row
	 * yet at the moment it shows it.
	 * </p>
	 *
	 * @return the label of the target, and an empty string for a relation pointing
	 *         nowhere or at an instanz the walk did not reach
	 */
	public String valueLabel(IValueType type, Object value) {
		boolean isRelation = type.getContentType() == ValueContentType.INSTANZ;

		if (!type.isMulti()) {
			return isRelation ? instanzChoices().labelOf(String.valueOf(value)).orElse("") : String.valueOf(value);
		}

		Collection<?> elements = elementsOf(value);
		if (!isRelation) {
			return elements.stream().map(String::valueOf).collect(Collectors.joining(", "));
		}

		InstanzChoices choices = instanzChoices();
		return elements.stream()//
				.map(element -> choices.labelOf(String.valueOf(element)).orElse(""))//
				.filter(label -> !label.isEmpty())//
				.collect(Collectors.joining(", "));
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
					createValue(tableRecord, newInstanz.getOwnKey());
				}

				broker.send(EventConstants.CLOSE_OPERATION, null);
				return Status.OK_STATUS;
			}
		}.schedule();
	};

	// not create(..): inside the anonymous Job above that would resolve to the
	// static Job.create the compiler finds first
	private void createValue(TableRecord row, String instanzKey) {
		if (row.type instanceof MultiValueType multi) {
			_min.createNew(multi.getClazz(), instanzKey, row.parameterName, elementsOf(row.value), Type.SEND);
			return;
		}
		_sin.createNew(((SingleValueType) row.type).getClazz(), instanzKey, row.parameterName, row.value, Type.SEND);
	}
}
