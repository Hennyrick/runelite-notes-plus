package com.notesplus;

import java.util.ArrayList;
import java.util.List;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

class NotesTreeManager
{
	static final String ROOT_FOLDER_NAME = "My Notes";

	private final DefaultMutableTreeNode root;
	private final DefaultTreeModel treeModel;
	private int folderSequence = 1;
	private int noteSequence = 1;
	private Runnable changeListener;

	NotesTreeManager()
	{
		this(null);
	}

	NotesTreeManager(NotesTreeSnapshot snapshot)
	{
		root = hydrateRoot(snapshot);
		treeModel = new DefaultTreeModel(root);
		refreshSequences(root);
	}

	DefaultTreeModel getTreeModel()
	{
		return treeModel;
	}

	DefaultMutableTreeNode getRoot()
	{
		return root;
	}

	void setChangeListener(Runnable changeListener)
	{
		this.changeListener = changeListener;
	}

	DefaultMutableTreeNode createFolder(DefaultMutableTreeNode selectedNode)
	{
		DefaultMutableTreeNode parent = resolveTargetFolder(selectedNode);
		DefaultMutableTreeNode folder = createFolderNode("New Folder " + folderSequence++);
		treeModel.insertNodeInto(folder, parent, parent.getChildCount());
		notifyChanged();
		return folder;
	}

	DefaultMutableTreeNode createNote(DefaultMutableTreeNode selectedNode)
	{
		DefaultMutableTreeNode parent = resolveTargetFolder(selectedNode);
		DefaultMutableTreeNode note = createNoteNode("New Note " + noteSequence++);
		treeModel.insertNodeInto(note, parent, parent.getChildCount());
		notifyChanged();
		return note;
	}

	boolean rename(DefaultMutableTreeNode node, String newName)
	{
		if (node == null || newName == null)
		{
			return false;
		}

		String trimmed = newName.trim();
		if (trimmed.isEmpty())
		{
			return false;
		}

		NotesNodeData data = getData(node);
		if (data == null)
		{
			return false;
		}

		data.setName(trimmed);
		treeModel.nodeChanged(node);
		notifyChanged();
		return true;
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

		String updated = content == null ? "" : content;
		if (updated.equals(data.getContent()))
		{
			return false;
		}

		data.setContent(updated);
		notifyChanged();
		return true;
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

	NotesTreeSnapshot toSnapshot()
	{
		return new NotesTreeSnapshot(toSnapshotNode(root));
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

	private void notifyChanged()
	{
		if (changeListener != null)
		{
			changeListener.run();
		}
	}

	private DefaultMutableTreeNode hydrateRoot(NotesTreeSnapshot snapshot)
	{
		if (snapshot == null || snapshot.getRoot() == null)
		{
			return createFolderNode(ROOT_FOLDER_NAME);
		}

		DefaultMutableTreeNode loadedRoot = fromSnapshotNode(snapshot.getRoot());
		NotesNodeData rootData = getData(loadedRoot);
		if (rootData == null || !rootData.isFolder())
		{
			return createFolderNode(ROOT_FOLDER_NAME);
		}

		if (rootData.getName() == null || rootData.getName().trim().isEmpty())
		{
			rootData.setName(ROOT_FOLDER_NAME);
		}

		return loadedRoot;
	}

	private void refreshSequences(DefaultMutableTreeNode node)
	{
		NotesNodeData data = getData(node);
		if (data != null)
		{
			if (data.isFolder())
			{
				folderSequence = Math.max(folderSequence, extractSequence(data.getName(), "New Folder ") + 1);
			}
			if (data.isNote())
			{
				noteSequence = Math.max(noteSequence, extractSequence(data.getName(), "New Note ") + 1);
			}
		}

		for (int i = 0; i < node.getChildCount(); i++)
		{
			refreshSequences((DefaultMutableTreeNode) node.getChildAt(i));
		}
	}

	private int extractSequence(String name, String prefix)
	{
		if (name == null || !name.startsWith(prefix))
		{
			return 0;
		}
		try
		{
			return Integer.parseInt(name.substring(prefix.length()).trim());
		}
		catch (NumberFormatException ignored)
		{
			return 0;
		}
	}

	private NotesTreeSnapshot.Node toSnapshotNode(DefaultMutableTreeNode node)
	{
		NotesNodeData data = getData(node);
		NotesTreeSnapshot.Node snapshotNode = new NotesTreeSnapshot.Node(data.getType(), data.getName());
		if (data.isNote())
		{
			snapshotNode.setContent(data.getContent());
		}

		List<NotesTreeSnapshot.Node> children = new ArrayList<>();
		for (int i = 0; i < node.getChildCount(); i++)
		{
			children.add(toSnapshotNode((DefaultMutableTreeNode) node.getChildAt(i)));
		}
		snapshotNode.setChildren(children);
		return snapshotNode;
	}

	private DefaultMutableTreeNode fromSnapshotNode(NotesTreeSnapshot.Node snapshotNode)
	{
		NotesNodeData data = snapshotNode.getType() == NotesNodeData.Type.NOTE
			? NotesNodeData.note(defaultName(snapshotNode.getName(), "Untitled Note"))
			: NotesNodeData.folder(defaultName(snapshotNode.getName(), "Untitled Folder"));

		if (data.isNote())
		{
			data.setContent(snapshotNode.getContent() == null ? "" : snapshotNode.getContent());
		}

		DefaultMutableTreeNode node = new DefaultMutableTreeNode(data);
		List<NotesTreeSnapshot.Node> children = snapshotNode.getChildren();
		if (children != null)
		{
			for (NotesTreeSnapshot.Node child : children)
			{
				if (child != null)
				{
					node.add(fromSnapshotNode(child));
				}
			}
		}
		return node;
	}

	private String defaultName(String name, String fallback)
	{
		if (name == null || name.trim().isEmpty())
		{
			return fallback;
		}
		return name;
	}

	private DefaultMutableTreeNode createFolderNode(String name)
	{
		return new DefaultMutableTreeNode(NotesNodeData.folder(name));
	}

	private DefaultMutableTreeNode createNoteNode(String name)
	{
		return new DefaultMutableTreeNode(NotesNodeData.note(name));
	}
}
