package com.notesplus;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

class NotesTreeSnapshot
{
	private Node root;

	NotesTreeSnapshot()
	{
	}

	NotesTreeSnapshot(Node root)
	{
		this.root = root;
	}

	Node getRoot()
	{
		return root;
	}

	void setRoot(Node root)
	{
		this.root = root;
	}

	static class Node
	{
		private NotesNodeData.Type type;
		private String name;
		private String content;
		private List<Node> children = new ArrayList<>();

		Node()
		{
		}

		Node(NotesNodeData.Type type, String name)
		{
			this.type = type;
			this.name = name;
		}

		NotesNodeData.Type getType()
		{
			return type;
		}

		void setType(NotesNodeData.Type type)
		{
			this.type = type;
		}

		String getName()
		{
			return name;
		}

		void setName(String name)
		{
			this.name = name;
		}

		String getContent()
		{
			return content;
		}

		void setContent(String content)
		{
			this.content = content;
		}

		List<Node> getChildren()
		{
			return children;
		}

		void setChildren(List<Node> children)
		{
			this.children = children;
		}

		@Override
		public boolean equals(Object other)
		{
			if (this == other)
			{
				return true;
			}
			if (!(other instanceof Node))
			{
				return false;
			}
			Node node = (Node) other;
			return type == node.type
				&& Objects.equals(name, node.name)
				&& Objects.equals(content, node.content)
				&& Objects.equals(children, node.children);
		}

		@Override
		public int hashCode()
		{
			return Objects.hash(type, name, content, children);
		}
	}

	@Override
	public boolean equals(Object other)
	{
		if (this == other)
		{
			return true;
		}
		if (!(other instanceof NotesTreeSnapshot))
		{
			return false;
		}
		NotesTreeSnapshot that = (NotesTreeSnapshot) other;
		return Objects.equals(root, that.root);
	}

	@Override
	public int hashCode()
	{
		return Objects.hash(root);
	}
}
