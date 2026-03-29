package com.notesplus;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.TreePath;
import net.runelite.client.ui.PluginPanel;

class NotesPlusPanel extends PluginPanel
{
	private static final String FOLDER_EMPTY_STATE = "Select a note to view and edit its content.";

	private final NotesTreeManager treeManager;
	private final JTree notesTree;
	private final JTextArea editor = new JTextArea();
	private boolean editorSyncInProgress;

	NotesPlusPanel(NotesTreeManager treeManager)
	{
		this.treeManager = treeManager;
		this.notesTree = new JTree(treeManager.getTreeModel());

		setLayout(new BorderLayout());
		add(buildTopControls(), BorderLayout.NORTH);
		add(buildEditorArea(), BorderLayout.CENTER);

		wireSelectionListener();
		wireEditorListener();
		selectRoot();
	}

	private JPanel buildTopControls()
	{
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		controlPanel.add(new JButton(new AbstractAction("New Folder")
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				DefaultMutableTreeNode created = treeManager.createFolder(getSelectedNode());
				expandParentOf(created);
				selectNode(created);
			}
		}));

		controlPanel.add(new JButton(new AbstractAction("New Note")
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				DefaultMutableTreeNode created = treeManager.createNote(getSelectedNode());
				expandParentOf(created);
				selectNode(created);
				editor.requestFocusInWindow();
			}
		}));

		controlPanel.add(new JButton(new AbstractAction("Rename")
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				handleRename();
			}
		}));

		controlPanel.add(new JButton(new AbstractAction("Delete")
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				handleDelete();
			}
		}));
		return controlPanel;
	}

	private JSplitPane buildEditorArea()
	{
		notesTree.setRootVisible(true);
		editor.setLineWrap(true);
		editor.setWrapStyleWord(true);

		JSplitPane splitPane = new JSplitPane(
			JSplitPane.HORIZONTAL_SPLIT,
			new JScrollPane(notesTree),
			new JScrollPane(editor)
		);
		splitPane.setResizeWeight(0.3);
		return splitPane;
	}

	private void wireSelectionListener()
	{
		notesTree.addTreeSelectionListener(this::onTreeSelectionChanged);
	}

	private void wireEditorListener()
	{
		editor.getDocument().addDocumentListener(new DocumentListener()
		{
			@Override
			public void insertUpdate(DocumentEvent e)
			{
				syncEditorToSelectedNote();
			}

			@Override
			public void removeUpdate(DocumentEvent e)
			{
				syncEditorToSelectedNote();
			}

			@Override
			public void changedUpdate(DocumentEvent e)
			{
				syncEditorToSelectedNote();
			}
		});
	}

	private void onTreeSelectionChanged(TreeSelectionEvent event)
	{
		DefaultMutableTreeNode selectedNode = getSelectedNode();
		NotesNodeData data = NotesTreeManager.getData(selectedNode);

		if (data != null && data.isNote())
		{
			setEditorState(data.getContent(), true);
			return;
		}

		setEditorState(FOLDER_EMPTY_STATE, false);
	}

	private void syncEditorToSelectedNote()
	{
		if (editorSyncInProgress)
		{
			return;
		}

		DefaultMutableTreeNode selectedNode = getSelectedNode();
		NotesNodeData data = NotesTreeManager.getData(selectedNode);
		if (data != null && data.isNote())
		{
			treeManager.updateNoteContent(selectedNode, editor.getText());
		}
	}

	private void handleRename()
	{
		DefaultMutableTreeNode selectedNode = getSelectedNode();
		NotesNodeData data = NotesTreeManager.getData(selectedNode);
		if (data == null)
		{
			return;
		}

		String updatedName = JOptionPane.showInputDialog(this, "Enter new name", data.getName());
		if (updatedName == null)
		{
			return;
		}

		NotesTreeManager.RenameResult result = treeManager.rename(selectedNode, updatedName);
		if (result == NotesTreeManager.RenameResult.INVALID_NAME)
		{
			showMessage("Name cannot be empty.");
		}
		else if (result == NotesTreeManager.RenameResult.DUPLICATE_NAME)
		{
			showMessage(data.isFolder()
				? "A folder with that name already exists in this location."
				: "A note with that name already exists in this location.");
		}
	}

	private void handleDelete()
	{
		DefaultMutableTreeNode selectedNode = getSelectedNode();
		NotesNodeData data = NotesTreeManager.getData(selectedNode);
		if (data == null)
		{
			return;
		}

		if (data.isFolder() && treeManager.hasChildren(selectedNode))
		{
			int confirmation = JOptionPane.showConfirmDialog(
				this,
				"This folder contains notes/folders. Delete it and all children?",
				"Confirm delete",
				JOptionPane.YES_NO_OPTION,
				JOptionPane.WARNING_MESSAGE
			);
			if (confirmation != JOptionPane.YES_OPTION)
			{
				return;
			}
		}

		DefaultMutableTreeNode fallbackNode = determineDeleteFallback(selectedNode);
		if (!treeManager.delete(selectedNode))
		{
			showMessage("That item cannot be deleted.");
			return;
		}

		if (fallbackNode == null)
		{
			fallbackNode = treeManager.getRoot();
		}
		selectNode(fallbackNode);
	}

	private DefaultMutableTreeNode determineDeleteFallback(DefaultMutableTreeNode deletingNode)
	{
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) deletingNode.getParent();
		if (parent == null)
		{
			return treeManager.getRoot();
		}

		int index = parent.getIndex(deletingNode);
		if (index >= 0 && index + 1 < parent.getChildCount())
		{
			return (DefaultMutableTreeNode) parent.getChildAt(index + 1);
		}
		if (index > 0 && index - 1 < parent.getChildCount())
		{
			return (DefaultMutableTreeNode) parent.getChildAt(index - 1);
		}
		return parent;
	}

	private DefaultMutableTreeNode getSelectedNode()
	{
		TreePath selectedPath = notesTree.getSelectionPath();
		if (selectedPath == null)
		{
			return treeManager.getRoot();
		}

		Object node = selectedPath.getLastPathComponent();
		return node instanceof DefaultMutableTreeNode ? (DefaultMutableTreeNode) node : treeManager.getRoot();
	}

	private void setEditorState(String text, boolean enabled)
	{
		editorSyncInProgress = true;
		try
		{
			editor.setEnabled(enabled);
			editor.setText(text == null ? "" : text);
			editor.setCaretPosition(0);
		}
		finally
		{
			editorSyncInProgress = false;
		}
	}

	private void selectRoot()
	{
		selectNode(treeManager.getRoot());
		notesTree.expandRow(0);
	}

	private void selectNode(DefaultMutableTreeNode node)
	{
		TreePath path = new TreePath(node.getPath());
		notesTree.setSelectionPath(path);
		notesTree.scrollPathToVisible(path);
	}

	private void expandParentOf(DefaultMutableTreeNode node)
	{
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) node.getParent();
		if (parent != null)
		{
			notesTree.expandPath(new TreePath(parent.getPath()));
		}
	}

	private void showMessage(String message)
	{
		JOptionPane.showMessageDialog(this, message, "Notes Plus", JOptionPane.INFORMATION_MESSAGE);
	}
}
