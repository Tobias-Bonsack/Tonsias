package de.tonsias.basis.logic.part;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import de.tonsias.basis.model.enums.SingleValueType;
import de.tonsias.basis.model.impl.value.SingleStringValue;
import de.tonsias.basis.model.interfaces.IInstanz;
import de.tonsias.basis.osgi.intf.IBasicPreferenceService;
import de.tonsias.basis.osgi.intf.IInstanzService;
import de.tonsias.basis.osgi.intf.ISingleValueService;

/**
 * The instanzen a relation can point at, as the tree the model is, each with
 * the label it carries in the Model View.
 * <p>
 * This is what a {@code SingleInstanzValue} is chosen from, and it lives here
 * rather than next to the widget so it can be tested without SWT. The target is
 * not filtered: a reference to the instanz itself is no parent-child edge and so
 * cannot build a cycle in the tree.
 * </p>
 * <p>
 * The shape is a tree and not a list on purpose. A flat list of every instanz is
 * unusable as soon as a model grows, and it throws away the one thing the user
 * navigates by - where an instanz sits. The widget adds a filter on top of it,
 * see <a href="https://github.com/Tobias-Bonsack/Tonsias/issues/76">#76</a>.
 * </p>
 */
public class InstanzChoices {

	/**
	 * @param _key      of the instanz, which is what a reference stores
	 * @param _label    how it reads in the Model View
	 * @param _children the same, for everything below it
	 */
	public record Choice(String _key, String _label, List<Choice> _children) {

		/**
		 * This choice and everything below it, depth first - the order the Model View
		 * draws its nodes in.
		 */
		public List<Choice> flatten() {
			List<Choice> result = new ArrayList<>();
			result.add(this);
			_children.forEach(child -> result.addAll(child.flatten()));
			return result;
		}

		/**
		 * @param key of the instanz to look for, the stored side of a relation
		 * @return the choice for it, empty for a key this subtree does not hold - a
		 *         relation pointing nowhere included, whose key is the empty string
		 */
		public Optional<Choice> find(String key) {
			if (_key.equals(key)) {
				return Optional.of(this);
			}
			return _children.stream().map(child -> child.find(key)).filter(Optional::isPresent).map(Optional::get)
					.findFirst();
		}
	}

	private final IInstanzService _instanzService;

	private final ISingleValueService _singleValueService;

	private final IBasicPreferenceService _preferenceService;

	public InstanzChoices(IInstanzService instanzService, ISingleValueService singleValueService,
			IBasicPreferenceService preferenceService) {
		_instanzService = instanzService;
		_singleValueService = singleValueService;
		_preferenceService = preferenceService;
	}

	/**
	 * Walks the tree from the root down. Children that do not resolve are skipped
	 * rather than reported - the same thing the Model View does when it builds its
	 * nodes.
	 *
	 * @return the root, carrying every reachable instanz below it
	 */
	public Choice tree() {
		IInstanz root = _instanzService.getRoot();
		// a child key claiming an ancestor would otherwise walk forever. The services
		// keep parent and child in sync, but nothing stops a hand-edited file
		Set<String> seen = new LinkedHashSet<>();
		seen.add(root.getOwnKey());
		return collect(root, seen);
	}

	private Choice collect(IInstanz instanz, Set<String> seen) {
		List<Choice> children = new ArrayList<>();
		for (String childKey : List.copyOf(instanz.getChildren())) {
			_instanzService.resolveKey(childKey)//
					.filter(child -> seen.add(child.getOwnKey()))//
					.ifPresent(child -> children.add(collect(child, seen)));
		}
		return new Choice(instanz.getOwnKey(), labelOf(instanz), List.copyOf(children));
	}

	/**
	 * How an instanz reads: the content of the string value the
	 * {@code MODEL_VIEW_TEXT} preference names, and its {@code toString()} when the
	 * preference is unset or names nothing this instanz carries. The Model View
	 * labels its nodes by the same rule, and this is the one place it is written -
	 * a reference that read differently there and here would be hard to follow.
	 */
	public String labelOf(IInstanz instanz) {
		Optional<String> parameterName = _preferenceService
				.getValue(IBasicPreferenceService.Key.MODEL_VIEW_TEXT.getKey(), String.class);
		if (parameterName.isEmpty()) {
			return instanz.toString();
		}

		String valueKey = instanz.getValues(SingleValueType.SINGLE_STRING).inverse().get(parameterName.get());
		return _singleValueService
				.resolveKey(SingleValueType.SINGLE_STRING.getPath(), valueKey, SingleStringValue.class)
				.map(SingleStringValue::getValue)//
				.orElseGet(instanz::toString);
	}

	/**
	 * How the instanz behind a stored relation reads, for the places that show
	 * where a reference points without offering the choice again.
	 *
	 * @param instanzKey the stored side of a relation, empty when it points nowhere
	 * @return the label, or an empty {@link Optional} for a key the tree does not
	 *         hold - which is what a relation whose target was deleted would be
	 */
	public Optional<String> labelOf(String instanzKey) {
		return tree().find(instanzKey).map(Choice::_label);
	}
}
