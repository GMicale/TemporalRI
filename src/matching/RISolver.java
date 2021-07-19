package matching;

import com.google.common.collect.TreeMultiset;
import graph.*;
import io.FileManager;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.util.TreeSet;

public class RISolver
{
	//Target temporal network
	private final Graph targetGraph;
	//Query temporal network
	private final Graph queryGraph;
	//Should query occurrences be stored?
	private final boolean dump;
	//Object that eventually handles the storage of query occurrences into output file
	private final FileManager fm;
	//Matching state machine
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

		//Compute node compatibility domains
		IntOpenHashSet[] domains=computeDomains(queryGraph,delta);
		/*for(i=0;i<domains.length;i++)
			System.out.println("node "+i+" --> |domain|="+domains[i].size());*/

		//Cardinalities of node domains
		int[] domainSizes=new int[domains.length];
		for(int u=0;u< domains.length;u++)
			domainSizes[u]=domains[u].size();
		mama = new MatchingMachine(queryGraph, domainSizes);

		//Get lower and upper bounds of timestamp at each step of matching
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
		//Position of currently examined candidate in the set of target candidate starting nodes
		int startCandNodeIndex=0;
		//If starting node has no candidates stop searching
		if(domains[mama.map_state_to_node[0]].size()==0)
			return 0;
		//System.out.println(domains[mama.map_state_to_node[0]]);
		//List of candidate nodes for matching to the first query node in the ordering
		int[] listInitNodes=domains[mama.map_state_to_node[0]].toIntArray();
		//Iterators for the position of the currently examined candidate edge at each step of matching process
		int[] candidatesIT=new int[numQueryEdges];
		for(i=0;i<numQueryEdges;i++)
			candidatesIT[i]=-1;
		//Set of target edges candidate for matching at each step of the matching process
		IntArrayList[] setCandidates=new IntArrayList[numQueryEdges];
		for(i=0;i<numQueryEdges;i++)
			setCandidates[i]=new IntArrayList();
		//Node mapping function, storing target nodes mapped to each query node
        int[] solutionNodes=new int[numQueryNodes];
        for(i=0;i<solutionNodes.length;i++)
        	solutionNodes[i]=-1;
		//Edge mapping function, storing target edges mapped to each query edge
		int[] solutionEdges = new int[numQueryEdges];
		for(i=0;i<solutionEdges.length;i++)
			solutionEdges[i]=-1;
		//Set of currently mapped target nodes
		IntOpenHashSet matchedNodes = new IntOpenHashSet();
		//Set of currently mapped target edges
		IntOpenHashSet matchedEdges = new IntOpenHashSet();
		//Mapping query edge timestamps to target edge timestamps
		int[] mapQueryToTargetTimes=new int[mama.map_state_to_edge.length];
		for(i=0;i<mapQueryToTargetTimes.length;i++)
			mapQueryToTargetTimes[i]=-1;
		//Set of timestamps of edges that have been already mapped
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
				//Remove the edge timestamp from the set of timestamps of already mapped edges
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
					//Removing the target edge mapped to the first query edge in the ordering implies also removing both its endpoints
					matchedNodes.remove(solutionNodes[0]);
					matchedNodes.remove(solutionNodes[1]);
					solutionNodes[0]=-1;
					solutionNodes[1]=-1;
				}
				else
				{
					//Check if, by doing backtracking, we are also removing the mapping for a new target node
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
				//We have no more candidate edges to scan
				if(si==0)
				{
					//We are matching the first query edge in the ordering
					while(true)
					{
						//Try scanning candidate edges from a different starting node
						startCandNodeIndex++;
						if(startCandNodeIndex==listInitNodes.length)
						{
							//There are no more candidate edges to scan, even from a different starting node
							//Do backtrack
							backtrack=true;
							break;
						}
						else
						{
							//Get the set of candidate target edges from this new starting node
							candidatesIT[si]=0;
							setCandidates[0]=findStartCandidates(listInitNodes[startCandNodeIndex],mama.map_state_to_edge[0],domains);
							if(setCandidates[0].size()>0)
								//There are new candidates to examine, continue the search from this new list
								break;
						}
					}
				}
				else
					backtrack=true;
			}
			if(backtrack)
			{
				//Flag backtracking
				psi = si;
				si--;
			}
			else
			{
				//Update node and edge mapping information
				solutionEdges[si]=setCandidates[si].getInt(candidatesIT[si]);
				if(si==0)
				{
					//This is the first edge we are mapping, we have also to save mapping for both endpoints
					candidatesIT[si]++;
					solutionNodes[0]=setCandidates[si].getInt(candidatesIT[si]);
					candidatesIT[si]++;
					solutionNodes[1]=setCandidates[si].getInt(candidatesIT[si]);
				}
				else
				{
					//If the new mapped edge involves a new unmapped node, we have to save mapping for this node.
					int sourceNodeState=mama.map_edgeState_to_sourceState[si];
					if(solutionNodes[sourceNodeState]==-1)
					{
						candidatesIT[si]++;
						solutionNodes[sourceNodeState]=setCandidates[si].getInt(candidatesIT[si]);
					}
				}

				if(si==numQueryEdges-1)
				{
					//We have mapped all edges, so a new occurrence has been found
					numTotalOccs++;
					//if(numTotalOccs%10000000==0) System.out.println("Found "+numTotalOccs+" occurrences...");
					//Print new occurrence found, if required
					if(dump)
					{
						//Create occurrence temporal graph
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
						//Print the occurrence to standard output
						fm.printOcc(occ);
					}
					psi = si;
				}
				else
				{
					//Update auxiliary info
					//Update edge mapping
					int targetEdgeId=solutionEdges[si];
					//Update set of timestamps of already mapped edges
					int[] targetProps=targetEdges.get(targetEdgeId);
					mapQueryToTargetTimes[si]=targetProps[0];
					targetTemporalMap.add(targetProps[0]);
					//Calculate the residual difference, i.e. how much we can extend the temporal window of the partial occurrence found, given delta
					int minTime=targetTemporalMap.firstEntry().getElement();
					int maxTime=targetTemporalMap.lastEntry().getElement();
					int diffTime;
					//If delta is not defined, there is no limit to the extension of the temporal window
					if(delta==Integer.MAX_VALUE)
						diffTime=Integer.MAX_VALUE;
					else
						diffTime=delta-maxTime+minTime;
					//Update the set of currently matched edges and/or nodes
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
						//Check if the newly mapped edge includes an unmapped target node
						if(currNodeState!=prevNodeState)
							matchedNodes.add(solutionNodes[currNodeState]);
					}
					sip1=si+1;

					//Find candidates for the next query edge in the ordering
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

	//Compute node compatibility domains
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
		//Query node degrees
		int[][] nodeDegreesQuery=new int[numQueryNodes][3];
		//Counts of temporal flows centered in each query node
		int[][] flowDegreesQuery;
		if(directed)
			flowDegreesQuery=new int[numQueryNodes][7];
		else
			flowDegreesQuery=new int[numQueryNodes][2];
		//Maximum in-, out- and reciprocal degrees of query nodes
		int[] nodeLimits=new int[3];
		//Maximum counts of temporal flows of query nodes
		int[] flowLimits;
		if(directed)
			flowLimits=new int[12];
		else
			flowLimits=new int[3];

		for(j=0;j<numQueryNodes;j++)
		{
			//Compute degrees for query node j
			nodeDegreesQuery[j]=queryGraph.getNodeDegrees(j,null);
			for(k=0;k<nodeDegreesQuery[j].length;k++)
			{
				if(nodeDegreesQuery[j][k]>nodeLimits[k])
					nodeLimits[k]=nodeDegreesQuery[j][k];
			}
			//Compute counts of temporal flows centered in query node j
			flowDegreesQuery[j]=queryGraph.getFlowDegrees(j,null, numQueryEdges);
			for(k=0;k<flowDegreesQuery[j].length;k++)
			{
				if(flowDegreesQuery[j][k]>flowLimits[k])
					flowLimits[k]=flowDegreesQuery[j][k];
			}
		}

		for(int idTarget : targetNodes.keySet())
		{
			//Compute in-,out-,reciprocal degrees of target node
			int[] nodeDegreesTarget=targetGraph.getNodeDegrees(idTarget,nodeLimits);
			//Compute counts of temporal flows centered in target node
			int[] flowDegreesTarget=targetGraph.getFlowDegrees(idTarget,flowLimits,delta);
			int targetNodeLab=targetNodes.get(idTarget);

			for(j=0;j<numQueryNodes;j++)
			{
				int queryNodeLab=queryNodes.get(j);
				//Check if the target and query nodes have the same label, query degrees are <= target degrees and counts of query flows are <= counts of target flows
				if(targetNodeLab==queryNodeLab
						&& containsDegrees(nodeDegreesTarget,nodeDegreesQuery[j])
						&& containsDegrees(flowDegreesTarget,flowDegreesQuery[j])
				)
				{
					//Add target node to the compatibility domain of query node j
					domains[j].add(idTarget);
				}
			}
		}

		return domains;
	}

	//Check if for each degree/flow type, target degree/flow is >= query degree/flow
	private boolean containsDegrees(int[] degreesTarget, int[] degreesQuery)
	{
		for(int i=0;i<degreesTarget.length;i++)
		{
			if(degreesTarget[i]<degreesQuery[i])
				return false;
		}
		return true;
	}

	//Find candidates for a given query edge
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
			//Parent node in the query edge is connected by an outgoing edge
			mapTimes=inAdjLists.get(dest);
		else if(edgeType==EdgeType.IN)
			//Parent node in the query edge is connected by an ingoing edge
			mapTimes=outAdjLists.get(dest);
		else
			mapTimes=recipAdjLists.get(dest);
		if(lowerBound!=-1 && upperBound!=-1)
		{
			//Lower bound and upper bound for the temporal window in which to search are both defined at this step of matching
			if(lowerBound==upperBound)
				subMapTimes=mapTimes.subMap(mapQueryToTargetTimes[lowerBound],mapQueryToTargetTimes[upperBound]+1);
			else
				subMapTimes=mapTimes.subMap(mapQueryToTargetTimes[lowerBound]+1,mapQueryToTargetTimes[upperBound]);
		}
		else if(lowerBound==-1)
		{
			//No lower bound is defined at this step of matching
			if(diffTime==Integer.MAX_VALUE)
				//Delta is undefined, so there is no lower limit for the temporal window
				subMapTimes=mapTimes.headMap(mapQueryToTargetTimes[upperBound]);
			else
				//We can overcome lower limit, but only by a diffTime quantity
				subMapTimes=mapTimes.subMap(minTime-diffTime,mapQueryToTargetTimes[upperBound]);
		}
		else
		{
			//No upper bound is defined at this step of matching
			if(diffTime==Integer.MAX_VALUE)
				//Delta is undefined, so there is no upper limit for the temporal window
				subMapTimes=mapTimes.tailMap(mapQueryToTargetTimes[lowerBound]+1);
			else
				//We can overcome upper limit, but only by a diffTime quantity
				subMapTimes=mapTimes.subMap(mapQueryToTargetTimes[lowerBound]+1,maxTime+diffTime+1);
		}
		if(source==-1)
		{
			//One of the two endpoints of the query edge have never been mapped before
			//Search candidate edges (if any) in the temporal window defined above starting from all possible nodes in the domain of the source node of the query edge
			if(!subMapTimes.isEmpty())
			{
				for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e : subMapTimes.int2ObjectEntrySet())
				{
					Int2IntOpenHashMap mapAdiacs=e.getValue();
					for (Int2IntMap.Entry adiac : mapAdiacs.int2IntEntrySet())
					{
						int idAdiac=adiac.getIntKey();
						//Check if node has not been matched before and is in the compatibility domain of the source node of the query edge
						if(!matchedNodes.contains(idAdiac) && domains[sourceQuery].contains(idAdiac))
						{
							int idEdge=adiac.getIntValue();
							int[] tProps=targetEdgeProps.get(idEdge);
							//Check if the candidate edge has not been mapped yet and has the same label of the query edge
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
			//Both endpoints of the query edge have been already mapped
			//Search candidate edges (if any) in the temporal window defined above connecting the already mapped target nodes
			if(!subMapTimes.isEmpty())
			{
				for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e : subMapTimes.int2ObjectEntrySet())
				{
					Int2IntOpenHashMap mapAdiacs=e.getValue();
					if(mapAdiacs.containsKey(source))
					{
						int idEdge=mapAdiacs.get(source);
						int[] tProps=targetEdgeProps.get(idEdge);
						//Check if the candidate edge has not been mapped yet and has the same label of the query edge
						if(!matchedEdges.contains(idEdge) && qProps[1]==tProps[1])
							listCandidates.add(idEdge);
					}
				}
			}
		}

		return listCandidates;
	}

	//Compute the list of candidate edges for the first query edge in the ordering
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
			//Search candidates among the nodes that are adjacent to the start node and compatible to the source of the query edge
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
						//Check if the candidate target edge and the query edge have the same label
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

	//Pre-calculate positions of lower and upper bounds of the temporal window where to search candidates at each step of matching process
	//These arrays are used to fast lookup at the lower and upper bounds of timestamps of already mapped target edges
	//and quickly define the temporal window in which we have to look for candidate edges at each step of matching process
	private ObjectArrayList<int[]> getBounds(MaMaEdge[] mapStatesToEdges)
	{
		//For each query edge, which is the state of the query edge with the immediately lower value of timestamp?
		int[] lower=new int[mapStatesToEdges.length];
		//For each query edge, which is the state of the query edge with the immediately higher value of timestamp?
		int[] upper=new int[mapStatesToEdges.length];
		ObjectArrayList<int[]> timeBounds=new ObjectArrayList<>();
		ObjectArrayList<int[]> edgeProps=queryGraph.getEdgeProps();
		//Ordered set of timestamps of query edges
		TreeSet<Integer> queryTemporalMap=new TreeSet<>();
		//Scan query edges according to the processing ordering for the matching
		for(int i=0;i<mapStatesToEdges.length;i++)
		{
			int idEdge=mapStatesToEdges[i].getId();
			int time=edgeProps.get(idEdge)[0];
			if(!queryTemporalMap.contains(time))
			{
				queryTemporalMap.add(time);
				//Get the previous timestamp (if any) in the ordered set of query timestamps
				Integer pred=queryTemporalMap.lower(time);
				//Find the state of the predecessor edge with that timestamp
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
					//Timestamp is the minimum observed one until then, so there is no lower position
					lower[i]=-1;
				//Get the next timestamp (if any) in the ordered set of query timestamps
				Integer succ=queryTemporalMap.higher(time);
				//Find the state of the predecessor edge with that timestamp
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
					//Timestamp is the maximum observed one until then, so there is no upper position
					upper[i]=-1;
			}
			else
			{
				//Timestamp has been already observed
				//Find the state of the predecessor edge with the same timestamp
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