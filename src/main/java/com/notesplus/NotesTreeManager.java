package com.notesplus;

import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

class NotesTreeManager
{
	private static final String ROOT_FOLDER_NAME = "My Notes";

	private final DefaultMutableTreeNode root;
	private final DefaultTreeModel treeModel;
	private int folderSequence = 1;
	private int noteSequence = 1;

	NotesTreeManager()
	{
		root = createFolderNode(ROOT_FOLDER_NAME);
		treeModel = new DefaultTreeModel(root);
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
		DefaultMutableTreeNode folder = createFolderNode("New Folder " + folderSequence++);
		treeModel.insertNodeInto(folder, parent, parent.getChildCount());
		return folder;
	}

	DefaultMutableTreeNode createNote(DefaultMutableTreeNode selectedNode)
	{
		DefaultMutableTreeNode parent = resolveTargetFolder(selectedNode);
		DefaultMutableTreeNode note = createNoteNode("New Note " + noteSequence++);
		treeModel.insertNodeInto(note, parent, parent.getChildCount());
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
		return true;
	}

	boolean delete(DefaultMutableTreeNode node)
	{
		if (node == null || node == root)
		{
			return false;
		}

		treeModel.removeNodeFromParent(node);
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
}
