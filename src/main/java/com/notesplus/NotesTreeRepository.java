package com.notesplus;

import com.google.gson.Gson;
import com.google.gson.JsonParseException;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import lombok.extern.slf4j.Slf4j;

@Slf4j
class NotesTreeRepository
{
	private static final Path DEFAULT_FILE = Paths.get(
		System.getProperty("user.home"),
		".runelite",
		"notes-plus",
		"notes-tree.json"
	);

	private final Gson gson = new Gson();
	private final Path filePath;

	NotesTreeRepository()
	{
		this(DEFAULT_FILE);
	}

	NotesTreeRepository(Path filePath)
	{
		this.filePath = filePath;
	}

	NotesTreeSnapshot load()
	{
		if (!Files.exists(filePath))
		{
			return defaultSnapshot();
		}

		try (Reader reader = Files.newBufferedReader(filePath))
		{
			NotesTreeSnapshot snapshot = gson.fromJson(reader, NotesTreeSnapshot.class);
			if (snapshot == null || snapshot.getRoot() == null)
			{
				log.warn("Notes Plus save file is empty or missing root: {}", filePath);
				return defaultSnapshot();
			}
			return snapshot;
		}
		catch (IOException | JsonParseException ex)
		{
			log.warn("Failed to read Notes Plus save file at {}. Using default tree.", filePath, ex);
			return defaultSnapshot();
		}
	}

	void save(NotesTreeSnapshot snapshot)
	{
		try
		{
			Path parent = filePath.getParent();
			if (parent != null)
			{
				Files.createDirectories(parent);
			}
			try (Writer writer = Files.newBufferedWriter(filePath))
			{
				gson.toJson(snapshot, writer);
			}
		}
		catch (IOException ex)
		{
			log.warn("Failed to save Notes Plus tree to {}", filePath, ex);
		}
	}

	Path getFilePath()
	{
		return filePath;
	}

	private NotesTreeSnapshot defaultSnapshot()
	{
		return new NotesTreeSnapshot(new NotesTreeSnapshot.Node(NotesNodeData.Type.FOLDER, NotesTreeManager.ROOT_FOLDER_NAME));
	}
}
