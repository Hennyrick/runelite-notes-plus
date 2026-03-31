package com.notesplus;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.nio.file.Path;
import javax.swing.AbstractAction;
import javax.swing.JButton;
import javax.swing.JFileChooser;
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
import javax.swing.filechooser.FileNameExtensionFilter;
import net.runelite.client.ui.PluginPanel;

class NotesPlusPanel extends PluginPanel
{
	private static final String FOLDER_EMPTY_STATE = "Select a note to view and edit its content.";

	private final NotesTreeManager treeManager;
	private final NotesTreeTransferService transferService;
	private final JTree notesTree;
	private final NotesTreeDragDropController dragDropController;
	private final JTextArea editor = new JTextArea();
	private boolean editorSyncInProgress;
	private boolean filterActive;

	NotesPlusPanel(NotesTreeManager treeManager, NotesTreeRepository repository)
	{
		this.treeManager = treeManager;
		this.transferService = new NotesTreeTransferService(repository);
		this.notesTree = new JTree(treeManager.getTreeModel());
		this.dragDropController = new NotesTreeDragDropController(
			notesTree,
			treeManager,
			this::isFilterActive,
			this::selectNode,
			this::showMessage);

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
			public void actionPerformed(ActionEvent e)
			{
				DefaultMutableTreeNode created = treeManager.createFolder(getSelectedNode());
				expandParentOf(created);
				selectNode(created);
			}
		}));

		controlPanel.add(new JButton(new AbstractAction("New Note")
		{
			@Override
			public void actionPerformed(ActionEvent e)
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
			public void actionPerformed(ActionEvent e)
			{
				handleRename();
			}
		}));

		controlPanel.add(new JButton(new AbstractAction("Delete")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				handleDelete();
			}
		}));

		controlPanel.add(new JButton(new AbstractAction("Import")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				handleImport();
			}
		}));

		controlPanel.add(new JButton(new AbstractAction("Export")
		{
			@Override
			public void actionPerformed(ActionEvent e)
			{
				handleExport();
			}
		}));
		return controlPanel;
	}

	private JSplitPane buildEditorArea()
	{
		notesTree.setRootVisible(true);
		dragDropController.install();
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

	private void handleImport()
	{
		int confirmation = JOptionPane.showConfirmDialog(
			this,
			"Importing will replace all current notes. Continue?",
			"Confirm import",
			JOptionPane.YES_NO_OPTION,
			JOptionPane.WARNING_MESSAGE
		);
		if (confirmation != JOptionPane.YES_OPTION)
		{
			return;
		}

		JFileChooser chooser = createJsonChooser("Import Notes Tree");
		if (chooser.showOpenDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}

		Path path = chooser.getSelectedFile().toPath();
		NotesTreeRepository.LoadResult result = transferService.importSnapshot(path);
		if (!result.isSuccess())
		{
			showErrorMessage("Import failed: " + result.getError());
			return;
		}

		if (!treeManager.replaceTree(result.getSnapshot()))
		{
			showErrorMessage("Import failed: root folder is invalid.");
			return;
		}

		selectAfterImport();
		showMessage("Import successful.");
	}

	private void handleExport()
	{
		JFileChooser chooser = new ConfirmOverwriteFileChooser();
		chooser.setDialogTitle("Export Notes Tree");
		chooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
		if (chooser.showSaveDialog(this) != JFileChooser.APPROVE_OPTION)
		{
			return;
		}

		Path targetPath = chooser.getSelectedFile().toPath();
		if (!targetPath.getFileName().toString().toLowerCase().endsWith(".json"))
		{
			targetPath = targetPath.resolveSibling(targetPath.getFileName().toString() + ".json");
		}

		String error = transferService.exportSnapshot(treeManager.toSnapshot(), targetPath);
		if (error != null)
		{
			showErrorMessage("Export failed: " + error);
			return;
		}
		showMessage("Exported notes to " + targetPath);
	}

	private JFileChooser createJsonChooser(String title)
	{
		JFileChooser chooser = new JFileChooser();
		chooser.setDialogTitle(title);
		chooser.setFileFilter(new FileNameExtensionFilter("JSON Files", "json"));
		return chooser;
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

	private void selectAfterImport()
	{
		DefaultMutableTreeNode root = treeManager.getRoot();
		if (root.getChildCount() > 0)
		{
			selectNode((DefaultMutableTreeNode) root.getChildAt(0));
		}
		else
		{
			selectNode(root);
		}
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

	private void showErrorMessage(String message)
	{
		JOptionPane.showMessageDialog(this, message, "Notes Plus", JOptionPane.ERROR_MESSAGE);
	}

	private boolean isFilterActive()
	{
		return filterActive;
	}

	private static class ConfirmOverwriteFileChooser extends JFileChooser
	{
		@Override
		public void approveSelection()
		{
			if (getDialogType() == SAVE_DIALOG && getSelectedFile() != null && getSelectedFile().exists())
			{
				int choice = JOptionPane.showConfirmDialog(
					this,
					"The selected file already exists. Overwrite it?",
					"Confirm overwrite",
					JOptionPane.YES_NO_OPTION,
					JOptionPane.WARNING_MESSAGE
				);
				if (choice != JOptionPane.YES_OPTION)
				{
					return;
				}
			}
			super.approveSelection();
		}
	}
}
