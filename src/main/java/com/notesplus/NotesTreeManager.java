package com.notesplus;

import java.util.Enumeration;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

class NotesTreeManager
{
	static final String ROOT_FOLDER_NAME = "My Notes";

	enum RenameResult
	{
		SUCCESS,
		INVALID_NAME,
		DUPLICATE_NAME,
		INVALID_NODE
	}

	enum MoveResult
	{
		SUCCESS,
		INVALID_SOURCE,
		INVALID_TARGET,
		SOURCE_IS_ROOT,
		TARGET_IS_NOTE,
		CYCLE_DETECTED
	}

	private final DefaultMutableTreeNode root;
	private final DefaultTreeModel treeModel;
	private final AtomicInteger folderSequence = new AtomicInteger(1);
	private final AtomicInteger noteSequence = new AtomicInteger(1);
	private Consumer<NotesTreeManager> changeListener;

	NotesTreeManager()
	{
		this(new NotesTreeSnapshot(new NotesTreeSnapshot.Node(NotesNodeData.Type.FOLDER, ROOT_FOLDER_NAME)));
	}

	NotesTreeManager(NotesTreeSnapshot snapshot)
	{
		NotesTreeSnapshot.Node snapshotRoot = snapshot != null ? snapshot.getRoot() : null;
		if (snapshotRoot == null || snapshotRoot.getType() != NotesNodeData.Type.FOLDER)
		{
			snapshotRoot = new NotesTreeSnapshot.Node(NotesNodeData.Type.FOLDER, ROOT_FOLDER_NAME);
		}

		root = fromSnapshot(snapshotRoot);
		treeModel = new DefaultTreeModel(root);
		recalculateSequences();
	}

	void setChangeListener(Consumer<NotesTreeManager> listener)
	{
		this.changeListener = listener;
	}

	DefaultTreeModel getTreeModel()
	{
		return treeModel;
	}

	DefaultMutableTreeNode getRoot()
	{
		return root;
	}

	DefaultMutableTreeNode createFolder(DefaultMutableTreeNode selectedNode)
	{
		DefaultMutableTreeNode parent = resolveTargetFolder(selectedNode);
		String name = nextDefaultName(parent, true, "New Folder ", folderSequence);
		DefaultMutableTreeNode folder = createFolderNode(name);
		treeModel.insertNodeInto(folder, parent, parent.getChildCount());
		notifyChanged();
		return folder;
	}

	DefaultMutableTreeNode createNote(DefaultMutableTreeNode selectedNode)
	{
		DefaultMutableTreeNode parent = resolveTargetFolder(selectedNode);
		String name = nextDefaultName(parent, false, "New Note ", noteSequence);
		DefaultMutableTreeNode note = createNoteNode(name);
		treeModel.insertNodeInto(note, parent, parent.getChildCount());
		notifyChanged();
		return note;
	}

	RenameResult rename(DefaultMutableTreeNode node, String newName)
	{
		if (node == null || newName == null)
		{
			return RenameResult.INVALID_NODE;
		}

		String trimmed = newName.trim();
		if (trimmed.isEmpty())
		{
			return RenameResult.INVALID_NAME;
		}

		NotesNodeData data = getData(node);
		if (data == null)
		{
			return RenameResult.INVALID_NODE;
		}

		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
		if (parent != null && hasSiblingWithName(parent, node, trimmed, data.getType()))
		{
			return RenameResult.DUPLICATE_NAME;
		}

		if (trimmed.equals(data.getName()))
		{
			return RenameResult.SUCCESS;
		}

		data.setName(trimmed);
		treeModel.nodeChanged(node);
		notifyChanged();
		return RenameResult.SUCCESS;
	}

	boolean delete(DefaultMutableTreeNode node)
	{
		if (node == null || node == root)
		{
			return false;
		}

		treeModel.removeNodeFromParent(node);
		notifyChanged();
		return true;
	}

	boolean updateNoteContent(DefaultMutableTreeNode node, String content)
	{
		NotesNodeData data = getData(node);
		if (data == null || !data.isNote())
		{
			return false;
		}

		String safeContent = content == null ? "" : content;
		if (Objects.equals(data.getContent(), safeContent))
		{
			return true;
		}

		data.setContent(safeContent);
		notifyChanged();
		return true;
	}

	boolean hasChildren(DefaultMutableTreeNode node)
	{
		return node != null && node.getChildCount() > 0;
	}

	MoveResult validateMove(DefaultMutableTreeNode sourceNode, DefaultMutableTreeNode targetParent)
	{
		if (sourceNode == null)
		{
			return MoveResult.INVALID_SOURCE;
		}
		if (targetParent == null)
		{
			return MoveResult.INVALID_TARGET;
		}
		if (sourceNode == root)
		{
			return MoveResult.SOURCE_IS_ROOT;
		}

		NotesNodeData targetData = getData(targetParent);
		if (targetData == null)
		{
			return MoveResult.INVALID_TARGET;
		}
		if (targetData.isNote())
		{
			return MoveResult.TARGET_IS_NOTE;
		}
		if (isSameNodeOrDescendant(sourceNode, targetParent))
		{
			return MoveResult.CYCLE_DETECTED;
		}
		return MoveResult.SUCCESS;
	}

	MoveResult moveNode(DefaultMutableTreeNode sourceNode, DefaultMutableTreeNode targetParent, int targetIndex)
	{
		MoveResult validation = validateMove(sourceNode, targetParent);
		if (validation != MoveResult.SUCCESS)
		{
			return validation;
		}

		DefaultMutableTreeNode currentParent = (DefaultMutableTreeNode) sourceNode.getParent();
		if (currentParent == null)
		{
			return MoveResult.INVALID_SOURCE;
		}

		int oldIndex = currentParent.getIndex(sourceNode);
		int boundedIndex = Math.max(0, Math.min(targetIndex, targetParent.getChildCount()));
		if (currentParent == targetParent && boundedIndex > oldIndex)
		{
			boundedIndex--;
		}
		if (currentParent == targetParent && boundedIndex == oldIndex)
		{
			return MoveResult.SUCCESS;
		}

		treeModel.removeNodeFromParent(sourceNode);
		treeModel.insertNodeInto(sourceNode, targetParent, boundedIndex);
		notifyChanged();
		return MoveResult.SUCCESS;
	}

	NotesTreeSnapshot toSnapshot()
	{
		return new NotesTreeSnapshot(toSnapshotNode(root));
	}

	DefaultMutableTreeNode resolveTargetFolder(DefaultMutableTreeNode selectedNode)
	{
		if (selectedNode == null)
		{
			return root;
		}

		NotesNodeData selectedData = getData(selectedNode);
		if (selectedData == null)
		{
			return root;
		}

		if (selectedData.isFolder())
		{
			return selectedNode;
		}

		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
		return parent == null ? root : parent;
	}

	static NotesNodeData getData(DefaultMutableTreeNode node)
	{
		if (node == null)
		{
			return null;
		}

		Object userObject = node.getUserObject();
		return userObject instanceof NotesNodeData ? (NotesNodeData) userObject : null;
	}

	private DefaultMutableTreeNode createFolderNode(String name)
	{
		return new DefaultMutableTreeNode(NotesNodeData.folder(name));
	}

	private DefaultMutableTreeNode createNoteNode(String name)
	{
		return new DefaultMutableTreeNode(NotesNodeData.note(name));
	}

	private boolean hasSiblingWithName(DefaultMutableTreeNode parent, DefaultMutableTreeNode self, String name, NotesNodeData.Type type)
	{
		for (int i = 0; i < parent.getChildCount(); i++)
		{
			DefaultMutableTreeNode child = (DefaultMutableTreeNode) parent.getChildAt(i);
			if (child == self)
			{
				continue;
			}

			NotesNodeData childData = getData(child);
			if (childData != null && childData.getType() == type && name.equals(childData.getName()))
			{
				return true;
			}
		}
		return false;
	}

	private String nextDefaultName(DefaultMutableTreeNode parent, boolean folder, String prefix, AtomicInteger sequence)
	{
		NotesNodeData.Type type = folder ? NotesNodeData.Type.FOLDER : NotesNodeData.Type.NOTE;
		while (true)
		{
			String candidate = prefix + sequence.getAndIncrement();
			if (!hasSiblingWithName(parent, null, candidate, type))
			{
				return candidate;
			}
		}
	}

	private void notifyChanged()
	{
		if (changeListener != null)
		{
			changeListener.accept(this);
		}
	}

	private void recalculateSequences()
	{
		int nextFolder = 1;
		int nextNote = 1;
		Enumeration<?> nodes = root.preorderEnumeration();
		while (nodes.hasMoreElements())
		{
			DefaultMutableTreeNode node = (DefaultMutableTreeNode) nodes.nextElement();
			NotesNodeData data = getData(node);
			if (data == null)
			{
				continue;
			}
			if (data.isFolder() && data.getName().startsWith("New Folder "))
			{
				nextFolder = Math.max(nextFolder, parseSuffix(data.getName(), "New Folder ") + 1);
			}
			if (data.isNote() && data.getName().startsWith("New Note "))
			{
				nextNote = Math.max(nextNote, parseSuffix(data.getName(), "New Note ") + 1);
			}
		}
		folderSequence.set(nextFolder);
		noteSequence.set(nextNote);
	}

	private int parseSuffix(String name, String prefix)
	{
		try
		{
			return Integer.parseInt(name.substring(prefix.length()).trim());
		}
		catch (RuntimeException ex)
		{
			return 0;
		}
	}

	private boolean isSameNodeOrDescendant(DefaultMutableTreeNode ancestor, DefaultMutableTreeNode node)
	{
		DefaultMutableTreeNode cursor = node;
		while (cursor != null)
		{
			if (cursor == ancestor)
			{
				return true;
			}
			cursor = (DefaultMutableTreeNode) cursor.getParent();
		}
		return false;
	}

	private DefaultMutableTreeNode fromSnapshot(NotesTreeSnapshot.Node snapshotNode)
	{
		NotesNodeData data = snapshotNode.getType() == NotesNodeData.Type.NOTE
			? NotesNodeData.note(snapshotNode.getName())
			: NotesNodeData.folder(snapshotNode.getName());
		if (data.isNote())
		{
			data.setContent(snapshotNode.getContent() == null ? "" : snapshotNode.getContent());
		}

		DefaultMutableTreeNode node = new DefaultMutableTreeNode(data);
		if (snapshotNode.getChildren() != null)
		{
			for (NotesTreeSnapshot.Node child : snapshotNode.getChildren())
			{
				node.add(fromSnapshot(child));
			}
		}
		return node;
	}

	private NotesTreeSnapshot.Node toSnapshotNode(DefaultMutableTreeNode node)
	{
		NotesNodeData data = getData(node);
		NotesTreeSnapshot.Node snapshotNode = new NotesTreeSnapshot.Node(data.getType(), data.getName());
		if (data.isNote())
		{
			snapshotNode.setContent(data.getContent());
		}

		for (int i = 0; i < node.getChildCount(); i++)
		{
			snapshotNode.getChildren().add(toSnapshotNode((DefaultMutableTreeNode) node.getChildAt(i)));
		}
		return snapshotNode;
	}
}
