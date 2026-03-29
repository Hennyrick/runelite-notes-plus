package com.notesplus;

import java.util.Objects;

class NotesNodeData
{
	enum Type
	{
		FOLDER,
		NOTE
	}

	private final Type type;
	private String name;
	private String content;

	private NotesNodeData(Type type, String name)
	{
		this.type = Objects.requireNonNull(type);
		this.name = Objects.requireNonNull(name);
	}

	static NotesNodeData folder(String name)
	{
		return new NotesNodeData(Type.FOLDER, name);
	}

	static NotesNodeData note(String name)
	{
		NotesNodeData nodeData = new NotesNodeData(Type.NOTE, name);
		nodeData.content = "";
		return nodeData;
	}

	Type getType()
	{
		return type;
	}

	String getName()
	{
		return name;
	}

	void setName(String name)
	{
		this.name = Objects.requireNonNull(name);
	}

	String getContent()
	{
		return content;
	}

	void setContent(String content)
	{
		this.content = Objects.requireNonNull(content);
	}

	boolean isFolder()
	{
		return type == Type.FOLDER;
	}

	boolean isNote()
	{
		return type == Type.NOTE;
	}

	@Override
	public String toString()
	{
		return name;
	}
}
