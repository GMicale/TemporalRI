package matching;

import graph.EdgeType;
import graph.Graph;
import it.unimi.dsi.fastutil.ints.*;

public class MatchingMachine
{
	public int[] map_node_to_state;
	public int[] map_state_to_node;
	public MaMaEdge[] map_state_to_edge;
	public int[] map_edgeState_to_sourceState;
	public int[] map_edgeState_to_destState;
	public int[] parent_state;
	public MamaParentType[] parent_type;
	
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
	
	public void build(Graph query, int[] domainSizes)
	{
		//Initialize data structures
		Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> outAdiacs=query.getOutAdjLists();
		Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> inAdiacs=query.getInAdjLists();
		Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> recipAdiacs=query.getRecipAdjLists();
		IntOpenHashSet setOrderedNodes=new IntOpenHashSet();
		IntOpenHashSet setNeighborNodes=new IntOpenHashSet();
		IntOpenHashSet setOtherNodes=new IntOpenHashSet();

		int numNodes = query.getNumNodes();
		int i, j;

		int[] t_parent_node = new int[numNodes];
		MamaParentType[] t_parent_type = new MamaParentType[numNodes];
		for(i=0; i<numNodes; i++)
		{
			setOtherNodes.add(i);
			t_parent_node[i] = -1;
			t_parent_type[i] = MamaParentType.PARENTTYPE_NULL;
		}

		//Start ordering
		int si=0;
		while(si < numNodes)
		{
			int bestNode=-1;
			if(si==0)
			{
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
				t_parent_type[bestNode] = MamaParentType.PARENTTYPE_NULL;
				t_parent_node[bestNode] = -1;
			}
			else
			{
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

			//Update data structures
			map_state_to_node[si] = bestNode;
			map_node_to_state[bestNode] = si;
			setOrderedNodes.add(bestNode);
			if(si==0)
				setOtherNodes.remove(bestNode);
			setNeighborNodes.remove(bestNode);
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
							t_parent_node[adiac] = bestNode;
							t_parent_type[adiac] = MamaParentType.PARENTTYPE_OUT;
						}
					}
				}
			}
			si++;
		}

		//Get the order of query edges
		int siEdge=0;
		for(i=0;i<numNodes;i++)
		{
			int source=map_state_to_node[i];
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
						Int2IntOpenHashMap mapAdiacs=t.getValue();
						if(mapAdiacs.containsKey(dest))
						{
							int edge=mapAdiacs.get(dest);
							map_state_to_edge[siEdge]=new MaMaEdge(edge,i,j,EdgeType.OUT);
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
						Int2IntOpenHashMap mapAdiacs=t.getValue();
						if(mapAdiacs.containsKey(dest))
						{
							int edge=mapAdiacs.get(dest);
							map_state_to_edge[siEdge]=new MaMaEdge(edge,i,j,EdgeType.IN);
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
						Int2IntOpenHashMap mapAdiacs=t.getValue();
						if(mapAdiacs.containsKey(dest))
						{
							int edge=mapAdiacs.get(dest);
							map_state_to_edge[siEdge]=new MaMaEdge(edge,i,j,EdgeType.RECIP);
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
