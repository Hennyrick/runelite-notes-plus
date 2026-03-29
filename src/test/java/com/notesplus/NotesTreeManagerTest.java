package com.notesplus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import javax.swing.tree.DefaultMutableTreeNode;
import org.junit.Before;
import org.junit.Test;

public class NotesTreeManagerTest
{
	private NotesTreeManager treeManager;

	@Before
	public void setUp()
	{
		treeManager = new NotesTreeManager();
	}

	@Test
	public void createFolderUnderRootWhenNoSelection()
	{
		DefaultMutableTreeNode folder = treeManager.createFolder(null);
		DefaultMutableTreeNode root = treeManager.getRoot();

		assertEquals(root, folder.getParent());
		assertTrue(NotesTreeManager.getData(folder).isFolder());
	}

	@Test
	public void createNoteUnderParentWhenNoteSelected()
	{
		DefaultMutableTreeNode folder = treeManager.createFolder(treeManager.getRoot());
		DefaultMutableTreeNode firstNote = treeManager.createNote(folder);
		DefaultMutableTreeNode secondNote = treeManager.createNote(firstNote);

		assertEquals(folder, secondNote.getParent());
		assertTrue(NotesTreeManager.getData(secondNote).isNote());
	}

	@Test
	public void deleteRootIsPrevented()
	{
		assertFalse(treeManager.delete(treeManager.getRoot()));
	}

	@Test
	public void renameFolderAndNote()
	{
		DefaultMutableTreeNode folder = treeManager.createFolder(treeManager.getRoot());
		DefaultMutableTreeNode note = treeManager.createNote(folder);

		assertTrue(treeManager.rename(folder, "Quests"));
		assertTrue(treeManager.rename(note, "Todo"));

		assertEquals("Quests", NotesTreeManager.getData(folder).getName());
		assertEquals("Todo", NotesTreeManager.getData(note).getName());
	}
}
