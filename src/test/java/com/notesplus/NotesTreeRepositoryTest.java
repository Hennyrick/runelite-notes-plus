package com.notesplus;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

public class NotesTreeRepositoryTest
{
	@Rule
	public TemporaryFolder temporaryFolder = new TemporaryFolder();

	@Test
	public void saveThenLoadRoundTrip() throws Exception
	{
		Path file = temporaryFolder.newFile("tree.json").toPath();
		NotesTreeRepository repository = new NotesTreeRepository(file);

		NotesTreeSnapshot snapshot = buildSampleSnapshot();

		repository.save(snapshot);
		NotesTreeSnapshot loaded = repository.load();

		assertEquals(snapshot, loaded);
	}

	@Test
	public void exportImportRoundTrip() throws Exception
	{
		Path dataFile = temporaryFolder.newFile("tree.json").toPath();
		Path exportFile = temporaryFolder.newFile("export.json").toPath();
		NotesTreeRepository repository = new NotesTreeRepository(dataFile);

		NotesTreeSnapshot snapshot = buildSampleSnapshot();
		assertEquals(null, repository.exportTo(snapshot, exportFile));

		NotesTreeRepository.LoadResult imported = repository.importFrom(exportFile);

		assertTrue(imported.isSuccess());
		assertEquals(snapshot, imported.getSnapshot());
	}

	@Test
	public void missingFileFallsBackToDefaultRoot()
	{
		Path file = temporaryFolder.getRoot().toPath().resolve("missing.json");
		NotesTreeRepository repository = new NotesTreeRepository(file);

		NotesTreeSnapshot loaded = repository.load();

		assertEquals(NotesNodeData.Type.FOLDER, loaded.getRoot().getType());
		assertEquals(NotesTreeManager.ROOT_FOLDER_NAME, loaded.getRoot().getName());
	}

	@Test
	public void invalidJsonFallsBackToDefaultRoot() throws Exception
	{
		Path file = temporaryFolder.newFile("broken.json").toPath();
		Files.write(file, "{ definitely invalid json".getBytes(StandardCharsets.UTF_8));
		NotesTreeRepository repository = new NotesTreeRepository(file);

		NotesTreeSnapshot loaded = repository.load();

		assertEquals(NotesNodeData.Type.FOLDER, loaded.getRoot().getType());
		assertEquals(NotesTreeManager.ROOT_FOLDER_NAME, loaded.getRoot().getName());
	}

	@Test
	public void importRejectsInvalidJson() throws Exception
	{
		Path file = temporaryFolder.newFile("invalid-import.json").toPath();
		Files.write(file, "{ not-json".getBytes(StandardCharsets.UTF_8));
		NotesTreeRepository repository = new NotesTreeRepository(file);

		NotesTreeRepository.LoadResult result = repository.importFrom(file);

		assertFalse(result.isSuccess());
		assertNotNull(result.getError());
	}

	@Test
	public void importRejectsMissingRoot() throws Exception
	{
		Path file = temporaryFolder.newFile("missing-root.json").toPath();
		Files.write(file, "{}".getBytes(StandardCharsets.UTF_8));
		NotesTreeRepository repository = new NotesTreeRepository(file);

		NotesTreeRepository.LoadResult result = repository.importFrom(file);

		assertFalse(result.isSuccess());
		assertEquals("Missing required field: root.", result.getError());
	}

	private NotesTreeSnapshot buildSampleSnapshot()
	{
		NotesTreeSnapshot.Node root = new NotesTreeSnapshot.Node(NotesNodeData.Type.FOLDER, "My Notes");
		NotesTreeSnapshot.Node folder = new NotesTreeSnapshot.Node(NotesNodeData.Type.FOLDER, "Bossing");
		NotesTreeSnapshot.Node note = new NotesTreeSnapshot.Node(NotesNodeData.Type.NOTE, "Gear");
		note.setContent("Bring potions");
		folder.getChildren().add(note);
		root.getChildren().add(folder);
		return new NotesTreeSnapshot(root);
	}
}
