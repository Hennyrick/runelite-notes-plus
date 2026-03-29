package com.notesplus;

import java.awt.BorderLayout;
import java.awt.FlowLayout;
import javax.swing.JButton;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTextArea;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import net.runelite.client.ui.PluginPanel;

class NotesPlusPanel extends PluginPanel
{
	private static final String ROOT_FOLDER_NAME = "My Notes";

	NotesPlusPanel()
	{
		setLayout(new BorderLayout());

		add(buildTopControls(), BorderLayout.NORTH);
		add(buildEditorArea(), BorderLayout.CENTER);
	}

	private JPanel buildTopControls()
	{
		JPanel controlPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
		controlPanel.add(new JButton("New Folder"));
		controlPanel.add(new JButton("New Note"));
		controlPanel.add(new JButton("Rename"));
		controlPanel.add(new JButton("Delete"));
		return controlPanel;
	}

	private JSplitPane buildEditorArea()
	{
		DefaultMutableTreeNode root = new DefaultMutableTreeNode(ROOT_FOLDER_NAME);
		JTree folderTree = new JTree(root);
		JTextArea editor = new JTextArea();

		JSplitPane splitPane = new JSplitPane(
			JSplitPane.HORIZONTAL_SPLIT,
			new JScrollPane(folderTree),
			new JScrollPane(editor)
		);
		splitPane.setResizeWeight(0.3);
		return splitPane;
	}
}
