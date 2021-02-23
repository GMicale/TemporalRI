package matching;

import graph.EdgeType;

public class MaMaEdge
{
	private final int id;
	private final int source;
	private final int dest;
	private final EdgeType type;
	public MaMaEdge(int id, int source, int dest, EdgeType type)
	{
		this.id=id;
		this.source=source;
		this.dest=dest;
		this.type=type;
	}
	public int getId()
	{
		return id;
	}
	public int getSource()
	{
		return source;
	}
	public int getDest()
	{
		return dest;
	}
	public EdgeType getType()
	{
		return type;
	}
	public String toString()
	{
		return "ID: "+id+"\tSOURCE: "+source+"\tDEST: "+dest+"\tTYPE: "+type;
	}
}