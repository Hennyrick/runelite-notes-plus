package com.notesplus;

import java.nio.file.Path;

class NotesTreeTransferService
{
	private final NotesTreeRepository repository;

	NotesTreeTransferService(NotesTreeRepository repository)
	{
		this.repository = repository;
	}

	NotesTreeRepository.LoadResult importSnapshot(Path importPath)
	{
		return repository.importFrom(importPath);
	}

	String exportSnapshot(NotesTreeSnapshot snapshot, Path exportPath)
	{
		return repository.exportTo(snapshot, exportPath);
	}
}
