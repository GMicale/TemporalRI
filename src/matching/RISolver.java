package matching;

import com.google.common.collect.TreeMultiset;
import graph.*;
import io.FileManager;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.TreeSet;

public class RISolver
{
	private final Graph targetGraph;
	private final Graph queryGraph;
	private final boolean dump;
	private final FileManager fm;
	private MatchingMachine mama;

	public RISolver(Graph targetGraph, Graph queryGraph, boolean dump, FileManager fm)
	{
		this.targetGraph=targetGraph;
		this.queryGraph=queryGraph;
		this.dump=dump;
		this.fm=fm;
	}
	
	public long match(int delta)
	{
		long numTotalOccs=0;
		int i;

		IntOpenHashSet[] domains=computeDomains(queryGraph,delta);
		/*for(i=0;i<domains.length;i++)
			System.out.println("node "+i+" --> |domain|="+domains[i].size());*/

		int[] domainSizes=new int[domains.length];
		for(int u=0;u< domains.length;u++)
			domainSizes[u]=domains[u].size();
		mama = new MatchingMachine(queryGraph, domainSizes);
		ObjectArrayList<int[]> timeBounds=getBounds(mama.map_state_to_edge);
		int[] lowerBounds=timeBounds.get(0);
		int[] upperBounds=timeBounds.get(1);
		/*for(i=0; i< mama.map_state_to_node.length; i++)
			System.out.println("state "+i+" --> node "+ mama.map_state_to_node[i]);
		for(i=0; i< mama.map_state_to_edge.length; i++)
			System.out.println("state "+i+" --> edge "+ mama.map_state_to_edge[i]);*/

		Int2IntOpenHashMap targetNodes=targetGraph.getNodeLabs();
		ObjectArrayList<int[]> targetEdges=targetGraph.getEdgeProps();
		int numQueryNodes=mama.map_state_to_node.length;
		int numQueryEdges=mama.map_state_to_edge.length;
		int startCandNodeIndex=0;
		if(domains[mama.map_state_to_node[0]].size()==0)
			return 0;
		//System.out.println(domains[mama.map_state_to_node[0]]);
		int[] listInitNodes=domains[mama.map_state_to_node[0]].toIntArray();
		int[] candidatesIT=new int[numQueryEdges];
		for(i=0;i<numQueryEdges;i++)
			candidatesIT[i]=-1;
		IntArrayList[] setCandidates=new IntArrayList[numQueryEdges];
		for(i=0;i<numQueryEdges;i++)
			setCandidates[i]=new IntArrayList();
        int[] solutionNodes=new int[numQueryNodes];
        for(i=0;i<solutionNodes.length;i++)
        	solutionNodes[i]=-1;
		int[] solutionEdges = new int[numQueryEdges];
		for(i=0;i<solutionEdges.length;i++)
			solutionEdges[i]=-1;
		IntOpenHashSet matchedNodes = new IntOpenHashSet();
		IntOpenHashSet matchedEdges = new IntOpenHashSet();
		int[] mapQueryToTargetTimes=new int[mama.map_state_to_edge.length];
		for(i=0;i<mapQueryToTargetTimes.length;i++)
			mapQueryToTargetTimes[i]=-1;
		TreeMultiset<Integer> targetTemporalMap=TreeMultiset.create();

		//Find initial candidates
		setCandidates[0]=findStartCandidates(listInitNodes[startCandNodeIndex],mama.map_state_to_edge[0],domains);

		//Start matching process
		int psi = -1;
		int si = 0;
		int sip1;
		while(si != -1)
		{
			//Backtracking on edge
			if(psi>=si)
			{
				//Restore time map
				if(si<numQueryEdges-1)
				{
					int targetEdgeId=solutionEdges[si];
					int[] targetProps=targetEdges.get(targetEdgeId);
					targetTemporalMap.remove(targetProps[0]);
					mapQueryToTargetTimes[si]=-1;
				}

				//Remove current mapping for edge and (eventually) for node
				matchedEdges.remove(solutionEdges[si]);
				solutionEdges[si]=-1;
				if(si==0)
				{
					matchedNodes.remove(solutionNodes[0]);
					matchedNodes.remove(solutionNodes[1]);
					solutionNodes[0]=-1;
					solutionNodes[1]=-1;
				}
				else
				{
					int prevNodeState=mama.map_edgeState_to_sourceState[si-1];
					int currNodeState=mama.map_edgeState_to_sourceState[si];
					if(currNodeState!=prevNodeState)
					{
						matchedNodes.remove(solutionNodes[currNodeState]);
						solutionNodes[currNodeState]=-1;
					}
				}
			}

			//Find next candidate
			candidatesIT[si]++;
			boolean backtrack=false;
			if(candidatesIT[si]==setCandidates[si].size())
			{
				if(si==0)
				{
					while(true)
					{
						startCandNodeIndex++;
						if(startCandNodeIndex==listInitNodes.length)
						{
							backtrack=true;
							break;
						}
						else
						{
							candidatesIT[si]=0;
							setCandidates[0]=findStartCandidates(listInitNodes[startCandNodeIndex],mama.map_state_to_edge[0],domains);
							if(setCandidates[0].size()>0)
								break;
						}
					}
				}
				else
					backtrack=true;
			}
			if(backtrack)
			{
				psi = si;
				si--;
			}
			else
			{
				//Update matching
				solutionEdges[si]=setCandidates[si].getInt(candidatesIT[si]);
				if(si==0)
				{
					candidatesIT[si]++;
					solutionNodes[0]=setCandidates[si].getInt(candidatesIT[si]);
					candidatesIT[si]++;
					solutionNodes[1]=setCandidates[si].getInt(candidatesIT[si]);
				}
				else
				{
					int sourceNodeState=mama.map_edgeState_to_sourceState[si];
					if(solutionNodes[sourceNodeState]==-1)
					{
						candidatesIT[si]++;
						solutionNodes[sourceNodeState]=setCandidates[si].getInt(candidatesIT[si]);
					}
				}

				if(si==numQueryEdges-1)
				{
					//New occurrence found
					numTotalOccs++;
					//if(numTotalOccs%10000000==0) System.out.println("Found "+numTotalOccs+" occurrences...");
					if(dump)
					{
						Graph occ=new Graph(targetGraph.isDirected());
						for(i=0;i<solutionNodes.length;i++)
						{
							int idNode=solutionNodes[i];
							occ.addNode(idNode,targetNodes.get(idNode));
						}
						for(i=0;i<solutionEdges.length;i++)
						{
							int idEdge=solutionEdges[i];
							int[] props=targetEdges.get(idEdge);
							MaMaEdge queryEdge=mama.map_state_to_edge[i];
							int source=solutionNodes[queryEdge.getSource()];
							int dest=solutionNodes[queryEdge.getDest()];
							EdgeType type=queryEdge.getType();
							if(type==EdgeType.OUT || type==EdgeType.RECIP)
								occ.addEdge(source,dest,props[0],props[1]);
							else
								occ.addEdge(dest,source,props[0],props[1]);
						}
						fm.printOcc(occ);
					}
					psi = si;
				}
				else
				{
					//Update auxiliary info
					int targetEdgeId=solutionEdges[si];
					int[] targetProps=targetEdges.get(targetEdgeId);
					mapQueryToTargetTimes[si]=targetProps[0];
					targetTemporalMap.add(targetProps[0]);
					int minTime=targetTemporalMap.firstEntry().getElement();
					int maxTime=targetTemporalMap.lastEntry().getElement();
					int diffTime;
					if(delta==Integer.MAX_VALUE)
						diffTime=Integer.MAX_VALUE;
					else
						diffTime=delta-maxTime+minTime;
					matchedEdges.add(solutionEdges[si]);
					if(si==0)
					{
						matchedNodes.add(solutionNodes[0]);
						matchedNodes.add(solutionNodes[1]);
					}
					else
					{
						int prevNodeState=mama.map_edgeState_to_sourceState[si-1];
						int currNodeState=mama.map_edgeState_to_sourceState[si];
						if(currNodeState!=prevNodeState)
							matchedNodes.add(solutionNodes[currNodeState]);
					}
					sip1=si+1;

					MaMaEdge queryEdge=mama.map_state_to_edge[sip1];
					setCandidates[sip1]=findCandidates(queryEdge,solutionNodes,domains,minTime,maxTime,diffTime,
							matchedNodes,matchedEdges,mapQueryToTargetTimes,lowerBounds[sip1],upperBounds[sip1]);
					candidatesIT[si+1]=-1;
					psi = si;
					si++;
				}
			}
		}
		return numTotalOccs;
	}

	private IntOpenHashSet[] computeDomains(Graph queryGraph, int delta)
	{
		int j, k;
		boolean directed=targetGraph.isDirected();
		Int2IntOpenHashMap queryNodes=queryGraph.getNodeLabs();
		int numQueryEdges=queryGraph.getNumEdges();
		int numQueryNodes=queryNodes.size();
		IntOpenHashSet[] domains=new IntOpenHashSet[numQueryNodes];
		for(j=0;j<domains.length;j++)
			domains[j]=new IntOpenHashSet();

		Int2IntOpenHashMap targetNodes=targetGraph.getNodeLabs();
		int[][] nodeDegreesQuery=new int[numQueryNodes][3];
		int[][] flowDegreesQuery;
		if(directed)
			flowDegreesQuery=new int[numQueryNodes][7];
		else
			flowDegreesQuery=new int[numQueryNodes][2];
		int[] nodeLimits=new int[3];
		int[] flowLimits;
		if(directed)
			flowLimits=new int[12];
		else
			flowLimits=new int[3];

		for(j=0;j<numQueryNodes;j++)
		{
			nodeDegreesQuery[j]=queryGraph.getNodeDegrees(j,null);
			for(k=0;k<nodeDegreesQuery[j].length;k++)
			{
				if(nodeDegreesQuery[j][k]>nodeLimits[k])
					nodeLimits[k]=nodeDegreesQuery[j][k];
			}
			flowDegreesQuery[j]=queryGraph.getFlowDegrees(j,null, numQueryEdges);
			for(k=0;k<flowDegreesQuery[j].length;k++)
			{
				if(flowDegreesQuery[j][k]>flowLimits[k])
					flowLimits[k]=flowDegreesQuery[j][k];
			}
		}

		for(int idTarget : targetNodes.keySet())
		{
			int[] nodeDegreesTarget=targetGraph.getNodeDegrees(idTarget,nodeLimits);
			int[] flowDegreesTarget=targetGraph.getFlowDegrees(idTarget,flowLimits,delta);
			int targetNodeLab=targetNodes.get(idTarget);

			for(j=0;j<numQueryNodes;j++)
			{
				int queryNodeLab=queryNodes.get(j);
				if(targetNodeLab==queryNodeLab
						&& containsDegrees(nodeDegreesTarget,nodeDegreesQuery[j])
						&& containsDegrees(flowDegreesTarget,flowDegreesQuery[j])
				)
				{
					domains[j].add(idTarget);
				}
			}
		}

		return domains;
	}

	private boolean containsDegrees(int[] degreesTarget,
										int[] degreesQuery)
	{
		for(int i=0;i<degreesTarget.length;i++)
		{
			if(degreesTarget[i]<degreesQuery[i])
				return false;
		}
		return true;
	}

	private IntArrayList findCandidates(MaMaEdge queryEdge, int[] solutionNodes, IntOpenHashSet[] domains, int minTime, int maxTime,
								int diffTime, IntOpenHashSet matchedNodes, IntOpenHashSet matchedEdges,
										int[] mapQueryToTargetTimes, int lowerBound, int upperBound)
	{
		IntArrayList listCandidates=new IntArrayList();
		Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> outAdjLists=targetGraph.getOutAdjLists();
		Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> inAdjLists=targetGraph.getInAdjLists();
		Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> recipAdjLists=targetGraph.getRecipAdjLists();
		ObjectArrayList<int[]> targetEdgeProps=targetGraph.getEdgeProps();
		int[] qProps=queryGraph.getEdgeProps().get(queryEdge.getId());
		int sourceState=queryEdge.getSource();
		int source=solutionNodes[sourceState];
		int sourceQuery=mama.map_state_to_node[sourceState];
		int destState=queryEdge.getDest();
		int dest=solutionNodes[destState];
		EdgeType edgeType=queryEdge.getType();
		Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes;
		Int2ObjectSortedMap<Int2IntOpenHashMap> subMapTimes;
		if(edgeType==EdgeType.OUT)
			mapTimes=inAdjLists.get(dest);
		else if(edgeType==EdgeType.IN)
			mapTimes=outAdjLists.get(dest);
		else
			mapTimes=recipAdjLists.get(dest);
		if(lowerBound!=-1 && upperBound!=-1)
		{
			if(lowerBound==upperBound)
				subMapTimes=mapTimes.subMap(mapQueryToTargetTimes[lowerBound],mapQueryToTargetTimes[upperBound]+1);
			else
				subMapTimes=mapTimes.subMap(mapQueryToTargetTimes[lowerBound]+1,mapQueryToTargetTimes[upperBound]);
		}
		else if(lowerBound==-1)
		{
			if(diffTime==Integer.MAX_VALUE)
				subMapTimes=mapTimes.headMap(mapQueryToTargetTimes[upperBound]);
			else
				subMapTimes=mapTimes.subMap(minTime-diffTime,mapQueryToTargetTimes[upperBound]);
		}
		else
		{
			if(diffTime==Integer.MAX_VALUE)
				subMapTimes=mapTimes.tailMap(mapQueryToTargetTimes[lowerBound]+1);
			else
				subMapTimes=mapTimes.subMap(mapQueryToTargetTimes[lowerBound]+1,maxTime+diffTime+1);
		}
		if(source==-1)
		{
			if(!subMapTimes.isEmpty())
			{
				for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e : subMapTimes.int2ObjectEntrySet())
				{
					Int2IntOpenHashMap mapAdiacs=e.getValue();
					for (Int2IntMap.Entry adiac : mapAdiacs.int2IntEntrySet())
					{
						int idAdiac=adiac.getIntKey();
						if(!matchedNodes.contains(idAdiac) && domains[sourceQuery].contains(idAdiac))
						{
							int idEdge=adiac.getIntValue();
							int[] tProps=targetEdgeProps.get(idEdge);
							if(!matchedEdges.contains(idEdge) && qProps[1]==tProps[1])
							{
								listCandidates.add(idEdge);
								listCandidates.add(idAdiac);
							}
						}
					}
				}
			}
		}
		else
		{
			if(!subMapTimes.isEmpty())
			{
				for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e : subMapTimes.int2ObjectEntrySet())
				{
					Int2IntOpenHashMap mapAdiacs=e.getValue();
					if(mapAdiacs.containsKey(source))
					{
						int idEdge=mapAdiacs.get(source);
						int[] tProps=targetEdgeProps.get(idEdge);
						if(!matchedEdges.contains(idEdge) && qProps[1]==tProps[1])
							listCandidates.add(idEdge);
					}
				}
			}
		}

		return listCandidates;
	}

	private IntArrayList findStartCandidates(int startNode, MaMaEdge queryEdge, IntOpenHashSet[] domains)
	{
		IntArrayList listCandidates=new IntArrayList();
		Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> outAdjLists=targetGraph.getOutAdjLists();
		Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> inAdjLists=targetGraph.getInAdjLists();
		Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> recipAdjLists=targetGraph.getRecipAdjLists();
		ObjectArrayList<int[]> targetEdgeProps=targetGraph.getEdgeProps();
		int[] qProps=queryGraph.getEdgeProps().get(queryEdge.getId());
		int sourceState=queryEdge.getSource();
		int sourceQuery=mama.map_state_to_node[sourceState];
		EdgeType edgeType=queryEdge.getType();
		Int2ObjectSortedMap<Int2IntOpenHashMap> mapTimes;
		if(edgeType==EdgeType.OUT)
			mapTimes=inAdjLists.get(startNode);
		else if(edgeType==EdgeType.IN)
			mapTimes=outAdjLists.get(startNode);
		else
			mapTimes=recipAdjLists.get(startNode);
		if(mapTimes!=null)
		{
			for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e : mapTimes.int2ObjectEntrySet())
			{
				Int2IntOpenHashMap mapAdiacs=e.getValue();
				for (Int2IntMap.Entry adiac : mapAdiacs.int2IntEntrySet())
				{
					int idAdiac=adiac.getIntKey();
					if(domains[sourceQuery].contains(idAdiac))
					{
						int idEdge=adiac.getIntValue();
						int[] tProps=targetEdgeProps.get(idEdge);
						if(qProps[1]==tProps[1])
						{
							listCandidates.add(idEdge);
							listCandidates.add(startNode);
							listCandidates.add(idAdiac);
						}
					}
				}
			}
		}
		return listCandidates;
	}

	private ObjectArrayList<int[]> getBounds(MaMaEdge[] mapStatesToEdges)
	{
		int[] lower=new int[mapStatesToEdges.length];
		int[] upper=new int[mapStatesToEdges.length];
		ObjectArrayList<int[]> timeBounds=new ObjectArrayList<>();
		ObjectArrayList<int[]> edgeProps=queryGraph.getEdgeProps();
		TreeSet<Integer> queryTemporalMap=new TreeSet<>();
		for(int i=0;i<mapStatesToEdges.length;i++)
		{
			int idEdge=mapStatesToEdges[i].getId();
			int time=edgeProps.get(idEdge)[0];
			if(!queryTemporalMap.contains(time))
			{
				queryTemporalMap.add(time);
				Integer pred=queryTemporalMap.lower(time);
				if(pred!=null)
				{
					for(int j=0;j<i;j++)
					{
						int idEdgeRef=mapStatesToEdges[j].getId();
						int timeRef=edgeProps.get(idEdgeRef)[0];
						if(timeRef==pred)
							lower[i]=j;
					}
				}
				else
					lower[i]=-1;
				Integer succ=queryTemporalMap.higher(time);
				if(succ!=null)
				{
					for(int j=0;j<i;j++)
					{
						int idEdgeRef=mapStatesToEdges[j].getId();
						int timeRef=edgeProps.get(idEdgeRef)[0];
						if(timeRef==succ)
							upper[i]=j;
					}
				}
				else
					upper[i]=-1;
			}
			else
			{
				for(int j=0;j<i;j++)
				{
					int idEdgeRef=mapStatesToEdges[j].getId();
					int timeRef=edgeProps.get(idEdgeRef)[0];
					if(timeRef==time)
					{
						lower[i]=j;
						upper[i]=j;
					}
				}
			}
		}
		timeBounds.add(lower);
		timeBounds.add(upper);
		return timeBounds;
	}
}