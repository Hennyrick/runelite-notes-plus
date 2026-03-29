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

	@Test
	public void moveNoteIntoFolder()
	{
		DefaultMutableTreeNode sourceFolder = treeManager.createFolder(treeManager.getRoot());
		DefaultMutableTreeNode destinationFolder = treeManager.createFolder(treeManager.getRoot());
		DefaultMutableTreeNode note = treeManager.createNote(sourceFolder);

		assertEquals(NotesTreeManager.MoveResult.SUCCESS, treeManager.moveNode(note, destinationFolder, 0));
		assertEquals(destinationFolder, note.getParent());
		assertEquals(1, destinationFolder.getChildCount());
	}

	@Test
	public void moveFolderIntoFolder()
	{
		DefaultMutableTreeNode sourceFolder = treeManager.createFolder(treeManager.getRoot());
		DefaultMutableTreeNode destinationFolder = treeManager.createFolder(treeManager.getRoot());

		assertEquals(NotesTreeManager.MoveResult.SUCCESS, treeManager.moveNode(sourceFolder, destinationFolder, 0));
		assertEquals(destinationFolder, sourceFolder.getParent());
	}

	@Test
	public void rejectDropIntoNote()
	{
		DefaultMutableTreeNode folder = treeManager.createFolder(treeManager.getRoot());
		DefaultMutableTreeNode note = treeManager.createNote(treeManager.getRoot());

		assertEquals(NotesTreeManager.MoveResult.TARGET_IS_NOTE, treeManager.moveNode(folder, note, 0));
		assertEquals(treeManager.getRoot(), folder.getParent());
	}

	@Test
	public void rejectMovingRoot()
	{
		DefaultMutableTreeNode folder = treeManager.createFolder(treeManager.getRoot());

		assertEquals(NotesTreeManager.MoveResult.SOURCE_IS_ROOT, treeManager.moveNode(treeManager.getRoot(), folder, 0));
	}

	@Test
	public void rejectMovingFolderIntoOwnDescendant()
	{
		DefaultMutableTreeNode folder = treeManager.createFolder(treeManager.getRoot());
		DefaultMutableTreeNode childFolder = treeManager.createFolder(folder);

		assertEquals(NotesTreeManager.MoveResult.CYCLE_DETECTED, treeManager.moveNode(folder, childFolder, 0));
		assertEquals(treeManager.getRoot(), folder.getParent());
	}

	@Test
	public void reorderAmongSiblings()
	{
		DefaultMutableTreeNode parent = treeManager.getRoot();
		DefaultMutableTreeNode first = treeManager.createFolder(parent);
		DefaultMutableTreeNode second = treeManager.createFolder(parent);

		assertEquals(NotesTreeManager.MoveResult.SUCCESS, treeManager.moveNode(second, parent, 0));
		assertEquals(second, parent.getChildAt(0));
		assertEquals(first, parent.getChildAt(1));
	}
}
