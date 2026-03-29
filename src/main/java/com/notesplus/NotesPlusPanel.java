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
				selectNode(created);
			}
		}));
		controlPanel.add(new JButton(new AbstractAction("New Note")
		{
			@Override
			public void actionPerformed(java.awt.event.ActionEvent e)
			{
				DefaultMutableTreeNode created = treeManager.createNote(getSelectedNode());
				selectNode(created);
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
		setEditorState("", false);
	}

	private void syncEditorToSelectedNote()
	{
		if (editorSyncInProgress)
		{
			return;
		}
		treeManager.updateNoteContent(getSelectedNode(), editor.getText());
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
		if (updatedName != null)
		{
			treeManager.rename(selectedNode, updatedName);
		}
	}

	private void handleDelete()
	{
		DefaultMutableTreeNode selectedNode = getSelectedNode();
		DefaultMutableTreeNode parent = (DefaultMutableTreeNode) selectedNode.getParent();
		if (!treeManager.delete(selectedNode))
		{
			return;
		}
		selectNode(parent == null ? treeManager.getRoot() : parent);
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
			editor.setText(text);
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
}
