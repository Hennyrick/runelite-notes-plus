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

		assertEquals(NotesTreeManager.RenameResult.SUCCESS, treeManager.rename(folder, "Quests"));
		assertEquals(NotesTreeManager.RenameResult.SUCCESS, treeManager.rename(note, "Todo"));

		assertEquals("Quests", NotesTreeManager.getData(folder).getName());
		assertEquals("Todo", NotesTreeManager.getData(note).getName());
	}

	@Test
	public void renameRejectsBlankAndWhitespaceOnlyNames()
	{
		DefaultMutableTreeNode folder = treeManager.createFolder(treeManager.getRoot());

		assertEquals(NotesTreeManager.RenameResult.INVALID_NAME, treeManager.rename(folder, ""));
		assertEquals(NotesTreeManager.RenameResult.INVALID_NAME, treeManager.rename(folder, "   "));
	}

	@Test
	public void renameRejectsDuplicateFolderNameWithinSameParent()
	{
		DefaultMutableTreeNode parent = treeManager.getRoot();
		DefaultMutableTreeNode first = treeManager.createFolder(parent);
		DefaultMutableTreeNode second = treeManager.createFolder(parent);
		treeManager.rename(first, "Quests");

		assertEquals(NotesTreeManager.RenameResult.DUPLICATE_NAME, treeManager.rename(second, "Quests"));
	}

	@Test
	public void renameRejectsDuplicateNoteNameWithinSameParent()
	{
		DefaultMutableTreeNode folder = treeManager.createFolder(treeManager.getRoot());
		DefaultMutableTreeNode first = treeManager.createNote(folder);
		DefaultMutableTreeNode second = treeManager.createNote(folder);
		treeManager.rename(first, "Checklist");

		assertEquals(NotesTreeManager.RenameResult.DUPLICATE_NAME, treeManager.rename(second, "Checklist"));
	}

	@Test
	public void updateNoteContentIncludedInSnapshot()
	{
		DefaultMutableTreeNode note = treeManager.createNote(treeManager.getRoot());
		assertTrue(treeManager.updateNoteContent(note, "Farm runs every 90 mins"));

		NotesTreeSnapshot snapshot = treeManager.toSnapshot();
		NotesTreeSnapshot.Node savedNote = snapshot.getRoot().getChildren().get(0);
		assertEquals("Farm runs every 90 mins", savedNote.getContent());
	}
}
