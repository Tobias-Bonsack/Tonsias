package de.tonsias.basis.ui.widget;

import java.util.List;
import java.util.Locale;

import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.ITreeContentProvider;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.StructuredSelection;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.jface.viewers.Viewer;
import org.eclipse.jface.viewers.ViewerFilter;
import org.eclipse.jface.widgets.TextFactory;
import org.eclipse.swt.SWT;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Text;

import de.tonsias.basis.logic.part.InstanzChoices.Choice;

/**
 * Picks one instanz out of the model: a filter field over the tree the model
 * is.
 * <p>
 * It used to be a {@code Combo} holding every instanz, flattened. That is
 * unusable as soon as a model grows - no search, and none of the structure the
 * user navigates by, see
 * <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/76">#76</a>. The
 * source is still {@code InstanzChoices}, which now hands the tree over instead
 * of flattening it.
 * </p>
 * <p>
 * A widget rather than a dialog, because the three places that choose an
 * instanz want it differently: the value dialog of a relation shows it inline,
 * the Instanz View and the create dialog open {@link InstanzSelectionDialog}
 * around it.
 * </p>
 */
public class InstanzChooser extends Composite {

	private final TreeViewer _viewer;

	private final LabelFilter _filter = new LabelFilter();

	private final Text _filterText;

	/**
	 * @param parent    to build into
	 * @param root      the model as {@code InstanzChoices.tree()} hands it over
	 * @param hintLabel what the filter field says while it is empty
	 */
	public InstanzChooser(Composite parent, Choice root, String hintLabel) {
		super(parent, SWT.NONE);
		GridLayoutFactory.fillDefaults().applyTo(this);

		_filterText = TextFactory.newText(SWT.SEARCH | SWT.ICON_CANCEL)//
				.message(hintLabel)//
				.create(this);
		GridDataFactory.fillDefaults().grab(true, false).applyTo(_filterText);

		_viewer = new TreeViewer(this, SWT.SINGLE | SWT.BORDER | SWT.V_SCROLL | SWT.H_SCROLL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(_viewer.getTree());
		_viewer.setContentProvider(new ChoiceContentProvider());
		_viewer.setLabelProvider(LabelProvider.createTextProvider(element -> ((Choice) element)._label()));
		_viewer.addFilter(_filter);
		_viewer.setInput(List.of(root));
		_viewer.expandAll();

		_filterText.addModifyListener(event -> refilter());
	}

	/**
	 * Every keystroke narrows the tree, and everything left is opened - a match
	 * hidden inside a collapsed branch would be no answer to a filter.
	 */
	private void refilter() {
		_filter.setNeedle(_filterText.getText());
		_viewer.refresh();
		_viewer.expandAll();
	}

	/** @return the key of the chosen instanz, empty while nothing is chosen */
	public String getSelectedKey() {
		return selected() == null ? "" : selected()._key();
	}

	/** @return the label of the chosen instanz, empty while nothing is chosen */
	public String getSelectedLabel() {
		return selected() == null ? "" : selected()._label();
	}

	/**
	 * Preselects the instanz a stored relation points at, so the user is not asked
	 * to choose again to keep what is already there. A key the tree does not hold
	 * leaves the selection empty - which is what a relation pointing nowhere is.
	 */
	public void setSelectedKey(String key) {
		root().find(key).ifPresent(choice -> _viewer.setSelection(new StructuredSelection(choice), true));
	}

	/**
	 * @param listener run whenever the chosen instanz changes - the OK button of
	 *                 whoever holds this widget hangs on it
	 */
	public void onSelectionChanged(Runnable listener) {
		_viewer.addSelectionChangedListener(event -> listener.run());
	}

	/** for the tests and for {@link #setSelectedKey}, which searches it */
	public Choice root() {
		return ((List<?>) _viewer.getInput()).stream().map(Choice.class::cast).findFirst().orElseThrow();
	}

	public TreeViewer getViewer() {
		return _viewer;
	}

	public Text getFilterText() {
		return _filterText;
	}

	private Choice selected() {
		Object first = _viewer.getStructuredSelection().getFirstElement();
		return first instanceof Choice choice ? choice : null;
	}

	private static class ChoiceContentProvider implements ITreeContentProvider {

		@Override
		public Object[] getElements(Object inputElement) {
			return ((List<?>) inputElement).toArray();
		}

		@Override
		public Object[] getChildren(Object parentElement) {
			return ((Choice) parentElement)._children().toArray();
		}

		@Override
		public Object getParent(Object element) {
			// the tree is walked downwards only; JFace uses this for reveal, and
			// setSelection(.., true) works off the path it builds itself
			return null;
		}

		@Override
		public boolean hasChildren(Object element) {
			return !((Choice) element)._children().isEmpty();
		}
	}

	/**
	 * Keeps what the filter names, and every instanz on the way down to it: hiding
	 * a parent would hide the match below it, and the tree would answer the filter
	 * with nothing.
	 */
	private static class LabelFilter extends ViewerFilter {

		private String _needle = "";

		void setNeedle(String needle) {
			_needle = needle.toLowerCase(Locale.ROOT);
		}

		@Override
		public boolean select(Viewer viewer, Object parentElement, Object element) {
			return matches((Choice) element);
		}

		private boolean matches(Choice choice) {
			if (_needle.isEmpty() || choice._label().toLowerCase(Locale.ROOT).contains(_needle)) {
				return true;
			}
			return choice._children().stream().anyMatch(this::matches);
		}
	}
}
