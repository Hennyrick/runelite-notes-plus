package com.notesplus;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
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
	private final Gson prettyGson = new GsonBuilder().setPrettyPrinting().create();
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
			LoadResult result = parseSnapshot(reader);
			if (!result.isSuccess())
			{
				log.warn("Notes Plus save file is invalid at {}: {}", filePath, result.getError());
				return defaultSnapshot();
			}
			return result.getSnapshot();
		}
		catch (IOException ex)
		{
			log.warn("Failed to read Notes Plus save file at {}. Using default tree.", filePath, ex);
			return defaultSnapshot();
		}
	}

	void save(NotesTreeSnapshot snapshot)
	{
		writeSnapshot(snapshot, filePath, gson);
	}

	String exportTo(NotesTreeSnapshot snapshot, Path exportPath)
	{
		return writeSnapshot(snapshot, exportPath, prettyGson);
	}

	LoadResult importFrom(Path importPath)
	{
		if (!Files.exists(importPath))
		{
			return LoadResult.error("Selected file does not exist.");
		}

		try (Reader reader = Files.newBufferedReader(importPath))
		{
			return parseSnapshot(reader);
		}
		catch (IOException ex)
		{
			return LoadResult.error("Failed to read file: " + ex.getMessage());
		}
	}

	Path getFilePath()
	{
		return filePath;
	}

	private String writeSnapshot(NotesTreeSnapshot snapshot, Path targetPath, Gson targetGson)
	{
		try
		{
			Path parent = targetPath.getParent();
			if (parent != null)
			{
				Files.createDirectories(parent);
			}
			try (Writer writer = Files.newBufferedWriter(targetPath))
			{
				targetGson.toJson(snapshot, writer);
			}
			return null;
		}
		catch (IOException ex)
		{
			log.warn("Failed to save Notes Plus tree to {}", targetPath, ex);
			return ex.getMessage();
		}
	}

	private LoadResult parseSnapshot(Reader reader)
	{
		final NotesTreeSnapshot snapshot;
		try
		{
			snapshot = gson.fromJson(reader, NotesTreeSnapshot.class);
		}
		catch (JsonParseException ex)
		{
			return LoadResult.error("Malformed JSON: " + ex.getMessage());
		}

		if (snapshot == null)
		{
			return LoadResult.error("JSON file is empty.");
		}
		if (snapshot.getRoot() == null)
		{
			return LoadResult.error("Missing required field: root.");
		}
		if (snapshot.getRoot().getType() != NotesNodeData.Type.FOLDER)
		{
			return LoadResult.error("The root node must be a folder.");
		}
		if (snapshot.getRoot().getName() == null || snapshot.getRoot().getName().trim().isEmpty())
		{
			return LoadResult.error("The root folder must have a name.");
		}

		return LoadResult.success(snapshot);
	}

	private NotesTreeSnapshot defaultSnapshot()
	{
		return new NotesTreeSnapshot(new NotesTreeSnapshot.Node(NotesNodeData.Type.FOLDER, NotesTreeManager.ROOT_FOLDER_NAME));
	}

	static class LoadResult
	{
		private final boolean success;
		private final NotesTreeSnapshot snapshot;
		private final String error;

		private LoadResult(boolean success, NotesTreeSnapshot snapshot, String error)
		{
			this.success = success;
			this.snapshot = snapshot;
			this.error = error;
		}

		static LoadResult success(NotesTreeSnapshot snapshot)
		{
			return new LoadResult(true, snapshot, null);
		}

		static LoadResult error(String error)
		{
			return new LoadResult(false, null, error);
		}

		boolean isSuccess()
		{
			return success;
		}

		NotesTreeSnapshot getSnapshot()
		{
			return snapshot;
		}

		String getError()
		{
			return error;
		}
	}
}
