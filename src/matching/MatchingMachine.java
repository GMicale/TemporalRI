package matching;

import graph.EdgeType;
import graph.Graph;
import it.unimi.dsi.fastutil.ints.*;

//State machine for matching process
public class MatchingMachine
{
	//Map node id to correponding state id in the machine. State 0 corresponds to the first node in the ordering, state 1 to the second node, etc...
	public int[] map_node_to_state;
	//Map state id to the corresponding node id
	public int[] map_state_to_node;
	//Map edge state id to its corresponding id. State 0 corresponds to the first edge in the ordering, state 1 to the second edge, etc...
	public MaMaEdge[] map_state_to_edge;
	//Map edge state id to the state id of its source node
	public int[] map_edgeState_to_sourceState;
	//Map edge state id to the state id of its destination node
	public int[] map_edgeState_to_destState;
	//State of the parent node (i.e. predecessor node in the ordering) of each query node
	public int[] parent_state;
	//Type of the edge (IN or OUT, or NULL if no edge) connecting a query node to its parent in the ordering
	public MamaParentType[] parent_type;

	//Initialize matching machine
	public MatchingMachine(Graph query, int[] domainSizes)
	{
		int numNodes = query.getNumNodes();
		int numEdges = query.getNumEdges();
		map_node_to_state = new int[numNodes];
		map_state_to_node = new int[numNodes];
		map_edgeState_to_sourceState = new int[numEdges];
		map_edgeState_to_destState = new int[numEdges];
		map_state_to_edge = new MaMaEdge[numEdges];
		parent_state = new int[numNodes];
		parent_type = new MamaParentType[numNodes];
		build(query,domainSizes);
	}

	//Build the matching machine for the query graph
	//domainSizes are the cardinalities of compatibility domains for each query node
	public void build(Graph query, int[] domainSizes)
	{

		Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> outAdiacs=query.getOutAdjLists();
		Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> inAdiacs=query.getInAdjLists();
		Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> recipAdiacs=query.getRecipAdjLists();
		//Current set of nodes already in the ordering
		IntOpenHashSet setOrderedNodes=new IntOpenHashSet();
		//Current set of nodes that are neighbors of nodes already in the ordering
		IntOpenHashSet setNeighborNodes=new IntOpenHashSet();
		//Current set of all nodes that are neither in the ordering nor neighbors of nodes in the ordering
		IntOpenHashSet setOtherNodes=new IntOpenHashSet();

		int numNodes = query.getNumNodes();
		int i, j;

		//Initialize parent info
		int[] t_parent_node = new int[numNodes];
		MamaParentType[] t_parent_type = new MamaParentType[numNodes];
		for(i=0; i<numNodes; i++)
		{
			setOtherNodes.add(i);
			t_parent_node[i] = -1;
			t_parent_type[i] = MamaParentType.PARENTTYPE_NULL;
		}

		//Start node ordering
		int si=0;
		while(si < numNodes)
		{
			int bestNode=-1;
			if(si==0)
			{
				//Find the first node of the ordering, which is the one with highest degree and, in case of tie, lowest domain cardinality
				int highestDeg=-Integer.MAX_VALUE;
				for(int node : setOtherNodes)
				{
					int deg=query.getDegree(node,setOtherNodes);
					if(deg>highestDeg)
					{
						bestNode=node;
						highestDeg=deg;
					}
					else if(deg==highestDeg && domainSizes[node]<domainSizes[bestNode])
						bestNode=node;
				}
				//Update parent information
				t_parent_type[bestNode] = MamaParentType.PARENTTYPE_NULL;
				t_parent_node[bestNode] = -1;
			}
			else
			{
				//Find next node in the ordering, which is the one connected to the highest number of nodes in the ordering
				//In case of tie, choose node with lowest domain cardinality
				int highestDeg=-Integer.MAX_VALUE;
				for(int neigh : setNeighborNodes)
				{
					int deg=query.getDegree(neigh,setOrderedNodes);
					if(deg>highestDeg)
					{
						bestNode=neigh;
						highestDeg=deg;
					}
					else if(deg==highestDeg && domainSizes[neigh]<domainSizes[bestNode])
						bestNode=neigh;
				}
			}

			//Update state for the found node
			map_state_to_node[si] = bestNode;
			map_node_to_state[bestNode] = si;
			//Add node to the ordering
			setOrderedNodes.add(bestNode);
			//Remove node from the other sets
			if(si==0)
				setOtherNodes.remove(bestNode);
			setNeighborNodes.remove(bestNode);
			//Move nodes that are neighbors to the found node and are not in the current ordering to set of neighbor nodes
			if(outAdiacs.containsKey(bestNode))
			{
				Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes=outAdiacs.get(bestNode);
				for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e : mapTimes.int2ObjectEntrySet())
				{
					Int2IntOpenHashMap mapAdiacs=e.getValue();
					for(int adiac : mapAdiacs.keySet())
					{
						if(setOtherNodes.contains(adiac) && !setOrderedNodes.contains(adiac))
						{
							setNeighborNodes.add(adiac);
							setOtherNodes.remove(adiac);
							//Update parent node info
							t_parent_node[adiac] = bestNode;
							t_parent_type[adiac] = MamaParentType.PARENTTYPE_OUT;
						}
					}
				}
			}
			if(inAdiacs.containsKey(bestNode))
			{
				Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes=inAdiacs.get(bestNode);
				for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e : mapTimes.int2ObjectEntrySet())
				{
					Int2IntOpenHashMap mapAdiacs=e.getValue();
					for(int adiac : mapAdiacs.keySet())
					{
						if(setOtherNodes.contains(adiac) && !setOrderedNodes.contains(adiac))
						{
							setNeighborNodes.add(adiac);
							setOtherNodes.remove(adiac);
							//Update parent node info
							t_parent_node[adiac] = bestNode;
							t_parent_type[adiac] = MamaParentType.PARENTTYPE_IN;
						}
					}
				}
			}
			if(recipAdiacs.containsKey(bestNode))
			{
				Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes=recipAdiacs.get(bestNode);
				for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e : mapTimes.int2ObjectEntrySet())
				{
					Int2IntOpenHashMap mapAdiacs=e.getValue();
					for(int adiac : mapAdiacs.keySet())
					{
						if(setOtherNodes.contains(adiac) && !setOrderedNodes.contains(adiac))
						{
							setNeighborNodes.add(adiac);
							setOtherNodes.remove(adiac);
							//Update parent node info
							t_parent_node[adiac] = bestNode;
							t_parent_type[adiac] = MamaParentType.PARENTTYPE_OUT;
						}
					}
				}
			}
			si++;
		}

		//Get the ordering of query edges, induced by the ordering of query nodes
		int siEdge=0;
		for(i=0;i<numNodes;i++)
		{
			int source=map_state_to_node[i];
			//Set parent state of source node
			if(t_parent_node[source] != -1)
				parent_state[i] = map_node_to_state[t_parent_node[source]];
			else
				parent_state[i] = -1;
			parent_type[i] = t_parent_type[source];
			Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimesOut=outAdiacs.get(source);
			Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimesIn=inAdiacs.get(source);
			Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimesRecip=recipAdiacs.get(source);
			for(j=0;j<i;j++)
			{
				int dest=map_state_to_node[j];
				if(mapTimesOut!=null)
				{
					for(Int2ObjectMap.Entry<Int2IntOpenHashMap> t : mapTimesOut.int2ObjectEntrySet())
					{
						//Add edges to final ordering one by one
						Int2IntOpenHashMap mapAdiacs=t.getValue();
						if(mapAdiacs.containsKey(dest))
						{
							int edge=mapAdiacs.get(dest);
							//Create edge representation for matching machine
							map_state_to_edge[siEdge]=new MaMaEdge(edge,i,j,EdgeType.OUT);
							//Map edge state to corresponding source and destination nodes
							map_edgeState_to_sourceState[siEdge]=i;
							map_edgeState_to_destState[siEdge]=j;
							siEdge++;
						}
					}
				}
				if(mapTimesIn!=null)
				{
					for(Int2ObjectMap.Entry<Int2IntOpenHashMap> t : mapTimesIn.int2ObjectEntrySet())
					{
						//Add edges to final ordering one by one
						Int2IntOpenHashMap mapAdiacs=t.getValue();
						if(mapAdiacs.containsKey(dest))
						{
							int edge=mapAdiacs.get(dest);
							//Create edge representation for matching machine
							map_state_to_edge[siEdge]=new MaMaEdge(edge,i,j,EdgeType.IN);
							//Map edge state to corresponding source and destination nodes
							map_edgeState_to_sourceState[siEdge]=i;
							map_edgeState_to_destState[siEdge]=j;
							siEdge++;
						}
					}
				}
				if(mapTimesRecip!=null)
				{
					for(Int2ObjectMap.Entry<Int2IntOpenHashMap> t : mapTimesRecip.int2ObjectEntrySet())
					{
						//Add edges to final ordering one by one
						Int2IntOpenHashMap mapAdiacs=t.getValue();
						if(mapAdiacs.containsKey(dest))
						{
							int edge=mapAdiacs.get(dest);
							//Create edge representation for matching machine
							map_state_to_edge[siEdge]=new MaMaEdge(edge,i,j,EdgeType.RECIP);
							//Map edge state to corresponding source and destination nodes
							map_edgeState_to_sourceState[siEdge]=i;
							map_edgeState_to_destState[siEdge]=j;
							siEdge++;
						}
					}
				}
			}
		}
	}
}
