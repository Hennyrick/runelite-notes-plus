package com.notesplus;

import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;
import javax.swing.DropMode;
import javax.swing.JComponent;
import javax.swing.JTree;
import javax.swing.TransferHandler;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;

class NotesTreeDragDropController
{
	private static final DataFlavor NODE_FLAVOR = new DataFlavor(DefaultMutableTreeNode.class, "NotesNode");
	private static final String FILTER_MODE_MESSAGE = "Drag-and-drop is unavailable while search/filter is active.";

	private final JTree tree;
	private final NotesTreeManager treeManager;
	private final Supplier<Boolean> filterActiveSupplier;
	private final Consumer<DefaultMutableTreeNode> selectionConsumer;
	private final Consumer<String> messageConsumer;

	NotesTreeDragDropController(
		JTree tree,
		NotesTreeManager treeManager,
		Supplier<Boolean> filterActiveSupplier,
		Consumer<DefaultMutableTreeNode> selectionConsumer,
		Consumer<String> messageConsumer)
	{
		this.tree = tree;
		this.treeManager = treeManager;
		this.filterActiveSupplier = filterActiveSupplier;
		this.selectionConsumer = selectionConsumer;
		this.messageConsumer = messageConsumer;
	}

	void install()
	{
		tree.setDragEnabled(true);
		tree.setDropMode(DropMode.ON_OR_INSERT);
		tree.setTransferHandler(new NotesTreeTransferHandler());
	}

	private final class NotesTreeTransferHandler extends TransferHandler
	{
		@Override
		public int getSourceActions(JComponent c)
		{
			return MOVE;
		}

		@Override
		protected Transferable createTransferable(JComponent component)
		{
			DefaultMutableTreeNode source = getSelectedTreeNode();
			return source == null ? null : new NodeTransferable(source);
		}

		@Override
		public boolean canImport(TransferSupport support)
		{
			if (!support.isDataFlavorSupported(NODE_FLAVOR) || !support.isDrop())
			{
				return false;
			}
			if (filterActiveSupplier.get())
			{
				return false;
			}

			DropTarget dropTarget = resolveDropTarget(support);
			if (dropTarget == null)
			{
				return false;
			}

			DefaultMutableTreeNode source = extractSourceNode(support.getTransferable());
			if (source == null)
			{
				return false;
			}

			return treeManager.validateMove(source, dropTarget.parent) == NotesTreeManager.MoveResult.SUCCESS;
		}

		@Override
		public boolean importData(TransferSupport support)
		{
			if (!support.isDrop())
			{
				return false;
			}
			if (filterActiveSupplier.get())
			{
				messageConsumer.accept(FILTER_MODE_MESSAGE);
				return false;
			}

			DropTarget dropTarget = resolveDropTarget(support);
			DefaultMutableTreeNode source = extractSourceNode(support.getTransferable());
			if (dropTarget == null || source == null)
			{
				return false;
			}

			List<TreePath> expandedPaths = captureExpandedPaths(source);
			NotesTreeManager.MoveResult result = treeManager.moveNode(source, dropTarget.parent, dropTarget.index);
			if (result != NotesTreeManager.MoveResult.SUCCESS)
			{
				showMoveError(result);
				return false;
			}

			restoreExpandedPaths(source, expandedPaths);
			selectionConsumer.accept(source);
			return true;
		}
	}

	private DefaultMutableTreeNode getSelectedTreeNode()
	{
		TreePath path = tree.getSelectionPath();
		if (path == null)
		{
			return null;
		}
		Object node = path.getLastPathComponent();
		return node instanceof DefaultMutableTreeNode ? (DefaultMutableTreeNode) node : null;
	}

	private DropTarget resolveDropTarget(TransferHandler.TransferSupport support)
	{
		JTree.DropLocation dropLocation = (JTree.DropLocation) support.getDropLocation();
		TreePath path = dropLocation.getPath();
		if (path == null)
		{
			return null;
		}

		Object node = path.getLastPathComponent();
		if (!(node instanceof DefaultMutableTreeNode))
		{
			return null;
		}

		DefaultMutableTreeNode targetNode = (DefaultMutableTreeNode) node;
		int childIndex = dropLocation.getChildIndex();
		if (childIndex >= 0)
		{
			return new DropTarget(targetNode, childIndex);
		}

		NotesNodeData targetData = NotesTreeManager.getData(targetNode);
		if (targetData == null || targetData.isNote())
		{
			return null;
		}
		return new DropTarget(targetNode, targetNode.getChildCount());
	}

	private DefaultMutableTreeNode extractSourceNode(Transferable transferable)
	{
		if (transferable == null)
		{
			return null;
		}
		try
		{
			Object data = transferable.getTransferData(NODE_FLAVOR);
			return data instanceof DefaultMutableTreeNode ? (DefaultMutableTreeNode) data : null;
		}
		catch (Exception ex)
		{
			return null;
		}
	}

	private List<TreePath> captureExpandedPaths(DefaultMutableTreeNode source)
	{
		List<TreePath> expandedPaths = new ArrayList<>();
		collectExpandedPaths(source, expandedPaths);
		return expandedPaths;
	}

	private void collectExpandedPaths(DefaultMutableTreeNode node, List<TreePath> expandedPaths)
	{
		TreePath nodePath = new TreePath(node.getPath());
		if (tree.isExpanded(nodePath))
		{
			expandedPaths.add(nodePath);
		}

		for (int i = 0; i < node.getChildCount(); i++)
		{
			collectExpandedPaths((DefaultMutableTreeNode) node.getChildAt(i), expandedPaths);
		}
	}

	private void restoreExpandedPaths(DefaultMutableTreeNode movedNode, List<TreePath> expandedPaths)
	{
		TreePath movedPath = new TreePath(movedNode.getPath());
		tree.expandPath(movedPath);
		for (TreePath expandedPath : expandedPaths)
		{
			Object node = expandedPath.getLastPathComponent();
			if (node instanceof DefaultMutableTreeNode)
			{
				tree.expandPath(new TreePath(((DefaultMutableTreeNode) node).getPath()));
			}
		}
	}

	private void showMoveError(NotesTreeManager.MoveResult result)
	{
		switch (result)
		{
			case SOURCE_IS_ROOT:
				messageConsumer.accept("The root folder cannot be moved.");
				break;
			case TARGET_IS_NOTE:
				messageConsumer.accept("Items can only be dropped into folders.");
				break;
			case CYCLE_DETECTED:
				messageConsumer.accept("A folder cannot be moved into itself or one of its descendants.");
				break;
			default:
				messageConsumer.accept("That move is not allowed.");
		}
	}

	private static final class NodeTransferable implements Transferable
	{
		private final DefaultMutableTreeNode node;

		private NodeTransferable(DefaultMutableTreeNode node)
		{
			this.node = node;
		}

		@Override
		public DataFlavor[] getTransferDataFlavors()
		{
			return new DataFlavor[]{NODE_FLAVOR};
		}

		@Override
		public boolean isDataFlavorSupported(DataFlavor flavor)
		{
			return NODE_FLAVOR.equals(flavor);
		}

		@Override
		public Object getTransferData(DataFlavor flavor)
		{
			return isDataFlavorSupported(flavor) ? node : null;
		}
	}

	private static final class DropTarget
	{
		private final DefaultMutableTreeNode parent;
		private final int index;

		private DropTarget(DefaultMutableTreeNode parent, int index)
		{
			this.parent = parent;
			this.index = index;
		}
	}
}
