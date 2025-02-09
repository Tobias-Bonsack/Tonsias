package de.tonsias.delta.view.ui;

import org.eclipse.e4.core.services.nls.Translation;
import org.eclipse.jface.layout.GridDataFactory;
import org.eclipse.jface.layout.GridLayoutFactory;
import org.eclipse.jface.viewers.LabelProvider;
import org.eclipse.jface.viewers.TreeViewer;
import org.eclipse.swt.SWT;
import org.eclipse.swt.events.MouseAdapter;
import org.eclipse.swt.events.MouseEvent;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Tree;

import de.tonsias.basis.osgi.intf.IDeltaService;
import de.tonsias.delta.view.ui.i18n.Messages;
import de.tonsias.delta.view.ui.tree.EventTreeContentProvider;
import de.tonsias.delta.view.ui.tree.EventTreeNodeWrapper;
import jakarta.annotation.PostConstruct;
import jakarta.inject.Inject;

public class DeltaView {

	@Inject
	@Translation
	Messages _messages;

	@Inject
	IDeltaService _deltaService;

	private Composite _parent;

	private TreeViewer _treeViewer;

	@PostConstruct
	public void postConstruct(Composite parent) {
		EventTreeNodeWrapper._deltaService = _deltaService;

		GridLayoutFactory.fillDefaults().numColumns(2).applyTo(parent);
		_parent = parent;

		createDeltaTree();

	}

	private void createDeltaTree() {
		Tree tree = new Tree(_parent, SWT.BORDER | SWT.VIRTUAL);
		GridDataFactory.fillDefaults().grab(true, true).applyTo(tree);
		tree.addMouseListener(new MouseAdapter() {
			@Override
			public void mouseDown(MouseEvent e) {
				// TODO Auto-generated method stub
			}
		});

		_treeViewer = new TreeViewer(tree);
		_treeViewer.setContentProvider(new EventTreeContentProvider(_treeViewer));
		_treeViewer.setLabelProvider(new LabelProvider());
		_treeViewer.setUseHashlookup(true);

		EventTreeNodeWrapper root = new EventTreeNodeWrapper(IDeltaService.START_EVENT, null);
		_treeViewer.setInput(root);
		_treeViewer.setChildCount(root, root.getChildCount());

		_treeViewer.refresh();
	}

	public void updateTree() {
		_treeViewer.refresh();

	}
}
