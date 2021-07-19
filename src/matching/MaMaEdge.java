package matching;

import graph.EdgeType;

//Query edge as represented in the state matching machine
public class MaMaEdge
{
	private final int id; //Edge id
	private final int source; //Source node of the edge
	private final int dest; //Destination node of the edge
	private final EdgeType type; //Edge direction (IN <-, OUT ->, or RECIPROCAL <->)
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