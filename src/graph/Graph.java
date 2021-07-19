package graph;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.*;

import java.util.*;

//Class Graph: implements temporal graphs

public class Graph
{
    private final boolean directed;  //Is the network directed?
    private final Int2IntOpenHashMap nodeLabs; //Map node id to its corresponding label (Labels
    private final ObjectArrayList<int[]> edgeProps;  //Map each edge id (ids starting from 0) to its properties, i.e. the pair (label, timestamp)
    private final Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> outAdjLists;
    //Adjacency list of out-adiacent nodes.
    //Adjacent nodes are grouped by timestamps, so the first map is sorted and maps each node id to a second map, indexed by timestamp.
    //The second map maps each timestamp to the a third map, which maps each adjacent node in that timestamp to the corresponding edge id
    private final Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> inAdjLists; //Adjacency list of in-adiacent nodes
    private final Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> recipAdjLists;
    //Adjacency list of reciprocal-adiacent nodes. This list is used only for undirected networks

    public Graph(boolean dir)
    {
        directed=dir;
        nodeLabs=new Int2IntOpenHashMap();
        edgeProps=new ObjectArrayList<>();
        outAdjLists=new Int2ObjectOpenHashMap<>();
        //If the key is not present in the map, return null
        outAdjLists.defaultReturnValue(null);
        inAdjLists=new Int2ObjectOpenHashMap<>();
        inAdjLists.defaultReturnValue(null);
        recipAdjLists=new Int2ObjectOpenHashMap<>();
        recipAdjLists.defaultReturnValue(null);
    }

    //Is the network directed?
    public boolean isDirected()
    {
        return directed;
    }

    //Number of network nodes
    public int getNumNodes()
    {
        return nodeLabs.size();
    }

    //Number of network edges
    public int getNumEdges()
    {
        return edgeProps.size();
    }

    public Int2IntOpenHashMap getNodeLabs()
    {
        return nodeLabs;
    }

    public ObjectArrayList<int[]> getEdgeProps()
    {
        return edgeProps;
    }

    public Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> getOutAdjLists()
    {
        return outAdjLists;
    }

    public Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> getInAdjLists()
    {
        return inAdjLists;
    }

    public Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> getRecipAdjLists()
    {
        return recipAdjLists;
    }

    //Add a new node to the network with ID id and label lab
    public void addNode(int id, int lab)
    {
        //If present, the node is not added
        nodeLabs.putIfAbsent(id,lab);
        //Initialize adjacency lists of the node
        if(directed)
        {
            outAdjLists.putIfAbsent(id,new Int2ObjectAVLTreeMap<>());
            inAdjLists.putIfAbsent(id,new Int2ObjectAVLTreeMap<>());
        }
        else
            recipAdjLists.putIfAbsent(id,new Int2ObjectAVLTreeMap<>());
    }

    //Add a new edge to the network from source node to dest node having a certain timestamp and label
    public void addEdge(int idSource, int idDest, int timestamp, int lab)
    {
        //Assign a progressive number as id for the edge (starting from 0)
        int idEdge=edgeProps.size();
        //Store edge timestamp and label
        int[] props=new int[2];
        props[0]=timestamp;
        props[1]=lab;
        edgeProps.add(props);
        //Network is directed, update both in- and out- adjacency lists
        if(directed)
        {
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes=outAdjLists.get(idSource);
            Int2IntOpenHashMap mapAdiacs=mapTimes.get(timestamp);
            if(mapAdiacs==null)
            {
                //This is a new timestamp for source node adjacency list
                mapAdiacs=new Int2IntOpenHashMap();
                //Map id of destination node to edge id
                mapAdiacs.put(idDest,idEdge);
                //Map timestamp to dest node and associated edge id
                mapTimes.put(timestamp,mapAdiacs);
            }
            else
                //Timestamp exists. Just map timestamp to dest node and associated edge id
                mapAdiacs.put(idDest,idEdge);
            //Do the same for in-adjacency list of destination node
            mapTimes=inAdjLists.get(idDest);
            mapAdiacs=mapTimes.get(timestamp);
            if(mapAdiacs==null)
            {
                mapAdiacs=new Int2IntOpenHashMap();
                mapAdiacs.put(idSource,idEdge);
                mapTimes.put(timestamp,mapAdiacs);
            }
            else
                mapAdiacs.put(idSource,idEdge);
        }
        else
        {
            //Update only reciprocal adjacency list
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes=recipAdjLists.get(idSource);
            Int2IntOpenHashMap mapAdiacs=mapTimes.get(timestamp);
            if(mapAdiacs==null)
            {
                mapAdiacs=new Int2IntOpenHashMap();
                mapAdiacs.put(idDest,idEdge);
                mapTimes.put(timestamp,mapAdiacs);
            }
            else
                mapAdiacs.put(idDest,idEdge);
            mapTimes=recipAdjLists.get(idDest);
            mapAdiacs=mapTimes.get(timestamp);
            if(mapAdiacs==null)
            {
                mapAdiacs=new Int2IntOpenHashMap();
                mapAdiacs.put(idSource,idEdge);
                mapTimes.put(timestamp,mapAdiacs);
            }
            else
                mapAdiacs.put(idSource,idEdge);
        }
    }

    //Compute in-, out- and reciprocal degrees for target or query node idNode
    //Limits array store the maximum in-, out- and reciprocal degrees of query nodes (applied only on target nodes)
    //If the method is called on a query node, limits is null
    public int[] getNodeDegrees(int idNode, int[] limits)
    {
        //Store target node in, out and reciprocal degrees
        int[] degrees=new int[3];
        //For each type of degree, check if we can stop computing that degree for target node
        boolean[] finish=new boolean[3];
        //Number of degree types for which we stopped the evaluation
        int numEnded=0;
        if(limits!=null)
        {
            //Node idNode is a target node. Check if limits are reached for some (or all) degree types
            for(int i=0;i<limits.length;i++)
            {
                if(limits[i]==0)
                {
                    //Maximum in, out or reciprocal degree for query nodes is 0, so we can avoid evaluating that degree type for target node
                    numEnded++;
                    finish[i]=true;
                }
            }
            //If all limits are reached we can stop the evaluation and return the current partial degree counts
            if(numEnded==3)
                return degrees;
        }
        Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimesOut=outAdjLists.get(idNode);
        Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimesIn=inAdjLists.get(idNode);
        Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimesRecip=recipAdjLists.get(idNode);
        if(!finish[0] || !finish[1] || !finish[2])
        {
            //We still have to evaluate one of the three types of degree
            //Compute out-degree until necessary
            if(mapTimesOut!=null)
            {
                for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e : mapTimesOut.int2ObjectEntrySet())
                {
                    Int2IntOpenHashMap mapAdiacs=e.getValue();
                    //Count the number of adjacent nodes reachable with edges with that timestamp and update the total degree count
                    degrees[0]+=mapAdiacs.size();
                    if(limits!=null && degrees[0]>=limits[0])
                    {
                        //We have to check limits and the current degree count reaches or overcomes the limit. In this case, we stop counting
                        finish[0]=true;
                        numEnded++;
                        if(numEnded==3)
                            //All limits have been reached, so we stop and return the current partial degree counts
                            return degrees;
                        break;
                    }
                }
            }
            //Compute in-degree until necessary as before
            if(mapTimesIn!=null)
            {
                for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e : mapTimesIn.int2ObjectEntrySet())
                {
                    Int2IntOpenHashMap mapAdiacs=e.getValue();
                    degrees[1]+=mapAdiacs.size();
                    if(limits!=null && degrees[1]>=limits[1])
                    {
                        finish[1]=true;
                        numEnded++;
                        if(numEnded==3)
                            return degrees;
                        break;
                    }
                }
            }
            //Compute reciprocal-degree until necessary as before
            if(mapTimesRecip!=null)
            {
                for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e : mapTimesRecip.int2ObjectEntrySet())
                {
                    Int2IntOpenHashMap mapAdiacs=e.getValue();
                    degrees[2]+=mapAdiacs.size();
                    if(limits!=null && degrees[2]>=limits[2])
                    {
                        finish[2]=true;
                        numEnded++;
                        if(numEnded==3)
                            return degrees;
                        break;
                    }
                }
            }
        }
        return degrees;
    }

    //Compute counts of temporal flows centered in node with id idNode where the maximum difference between timestamps is within threshold delta
    //Limits array store the maximum counts of temporal flows for query nodes (applied on for computing counts of temporal flows of target nodes)
    //If the method is called on a query node, limits is null
    //The different types of flows are defined as described in the main paper
    public int[] getFlowDegrees(int idNode, int[] limits, int delta)
    {
        if(directed)
        {
            //Network is directed, so we have 12 different types of flows
            //Store count for each temporal flow
            int[] flowDegrees=new int[12];
            //For each type of flow, check if we can stop computing flow count for target node
            boolean[] finish=new boolean[12];
            //Number of flow types for which we stopped the evaluation
            int numEnded=0;
            if(limits!=null)
            {
                //Node idNode is a target node. Check if limits are reached for some (or all) flow types
                for(int i=0;i<limits.length;i++)
                {
                    if(limits[i]==0)
                    {
                        //Maximum count for this flow type in query nodes is 0, so we can avoid evaluating that flow type for target node
                        numEnded++;
                        finish[i]=true;
                    }
                }
                if(numEnded==12)
                    //If all limits are reached we can stop the evaluation and return the current partial flow counts
                    return flowDegrees;
            }
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes1In=inAdjLists.get(idNode);
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes1Out=outAdjLists.get(idNode);
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes2Out=outAdjLists.get(idNode);
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes2In=inAdjLists.get(idNode);

            if(!finish[0] || !finish[1] || !finish[2] || !finish[3] || !finish[4] ||
                    !finish[7] || !finish[8] || !finish[9] || !finish[10])
            {
                //There is still some flow type to count involving at least one in-adjacency map
                for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e1 : mapTimes1In.int2ObjectEntrySet())
                {
                    int time1 = e1.getIntKey();
                    Int2IntOpenHashMap mapAdiacs1=e1.getValue();
                    if(!finish[0] || !finish[7])
                    {
                        //Search for forward-IO flows (with 2 or 3 nodes)
                        //Restrict search to flows for which difference between timestamps is <= delta
                        Int2ObjectSortedMap<Int2IntOpenHashMap> subMapTimes;
                        if(delta==Integer.MAX_VALUE)
                            subMapTimes=mapTimes2Out.tailMap(time1+1);
                        else
                            subMapTimes=mapTimes2Out.subMap(time1+1,time1+delta+1);
                        for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e2 : subMapTimes.int2ObjectEntrySet())
                        {
                            Int2IntOpenHashMap mapAdiacs2=e2.getValue();
                            if(mapAdiacs2!=null)
                            {
                                //Get the set of adjacent nodes in the given timestamp
                                IntOpenHashSet commonAdiacs=new IntOpenHashSet(mapAdiacs2.keySet());
                                //Count the number of cases in which the first and the third node of the flow are identical, i.e. the flows of type A->B->A
                                commonAdiacs.retainAll(mapAdiacs1.keySet());
                                int numCommons=commonAdiacs.size();
                                if(!finish[0])
                                {
                                    //Count the number of forward-IO flows with 3 nodes centered in node idNode with difference of timestamps within delta
                                    //Count is done excluding flows of type A->B->A
                                    flowDegrees[0]+=(mapAdiacs1.size()*mapAdiacs2.size())-numCommons;
                                    if(limits!=null && flowDegrees[0]>=limits[0])
                                    {
                                        //We have to check limits and the current degree count reaches or overcomes the limit. In this case, we stop counting
                                        finish[0]=true;
                                        numEnded++;
                                        if(numEnded==12)
                                            //Limits for all flow types have been reached. Stop search
                                            return flowDegrees;
                                    }
                                }
                                if(!finish[7])
                                {
                                    //Count the number of forward-IO flows with 2 nodes centered in node idNode with difference of timestamps within delta
                                    //Count is done considering only flows of type A->B->A
                                    flowDegrees[7]+=numCommons;
                                    if(limits!=null && flowDegrees[7]>=limits[7])
                                    {
                                        finish[7]=true;
                                        numEnded++;
                                        if(numEnded==12)
                                            return flowDegrees;
                                    }
                                }
                                if(finish[0] && finish[7])
                                    //We don't have to look anymore for forward IO-flows
                                    break;
                            }
                        }
                    }
                    if(!finish[1] || !finish[8])
                    {
                        //Search for backward-IO flows (with 2 or 3 nodes)
                        //Restrict search to flows for which difference between timestamps is <= delta
                        Int2ObjectSortedMap<Int2IntOpenHashMap> subMapTimes;
                        if(delta==Integer.MAX_VALUE)
                            subMapTimes=mapTimes2Out.headMap(time1);
                        else
                            subMapTimes=mapTimes2Out.subMap(time1-delta,time1);
                        for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e2 : subMapTimes.int2ObjectEntrySet())
                        {
                            Int2IntOpenHashMap mapAdiacs2=e2.getValue();
                            if(mapAdiacs2!=null)
                            {
                                //Count the number of cases in which the first and the third node of the flow are identical, i.e. the flows of type A->B->A
                                IntOpenHashSet commonAdiacs=new IntOpenHashSet(mapAdiacs2.keySet());
                                commonAdiacs.retainAll(mapAdiacs1.keySet());
                                int numCommons=commonAdiacs.size();
                                if(!finish[1])
                                {
                                    //Update counts for backward-IO flow with 3 nodes
                                    flowDegrees[1]+=(mapAdiacs1.size()*mapAdiacs2.size())-numCommons;
                                    if(limits!=null && flowDegrees[1]>=limits[1])
                                    {
                                        finish[1]=true;
                                        numEnded++;
                                        if(numEnded==12)
                                            return flowDegrees;
                                    }
                                }
                                if(!finish[8])
                                {
                                    //Update counts for backward-IO flow with 2 nodes
                                    flowDegrees[8]+=numCommons;
                                    if(limits!=null && flowDegrees[8]>=limits[8])
                                    {
                                        finish[8]=true;
                                        numEnded++;
                                        if(numEnded==12)
                                            return flowDegrees;
                                    }
                                }
                                if(finish[1] && finish[8])
                                    //We don't have to look anymore for backward IO-flows
                                    break;
                            }
                        }
                    }
                    if(!finish[2] || !finish[9])
                    {
                        //Search for synchronous-IO flow (with 2 or 3 nodes)
                        //In this case time is fixed, we do not need to search in a time interval
                        Int2IntOpenHashMap mapAdiacs2=mapTimes2Out.get(time1);
                        if(mapAdiacs2!=null)
                        {
                            //Count the number of cases in which the first and the third node of the flow are identical, i.e. the flows of type A->B->A
                            IntOpenHashSet commonAdiacs=new IntOpenHashSet(mapAdiacs2.keySet());
                            commonAdiacs.retainAll(mapAdiacs1.keySet());
                            int numCommons=commonAdiacs.size();
                            if(!finish[2])
                            {
                                //Update counts for synchronous-IO flow with 3 nodes
                                flowDegrees[2]+=(mapAdiacs1.size()*mapAdiacs2.size())-numCommons;
                                if(limits!=null && flowDegrees[2]>=limits[2])
                                {
                                    finish[2]=true;
                                    numEnded++;
                                    if(numEnded==12)
                                        return flowDegrees;
                                }
                            }
                            if(!finish[9])
                            {
                                //Update counts for synchronous-IO flow with 2 nodes
                                flowDegrees[9]+=numCommons;
                                if(limits!=null && flowDegrees[9]>=limits[9])
                                {
                                    finish[9]=true;
                                    numEnded++;
                                    if(numEnded==12)
                                        return flowDegrees;
                                }
                            }
                        }
                    }
                    if(!finish[3] || !finish[10])
                    {
                        //Search for asynchronous-II flows (with 2 or 3 nodes)
                        //Restrict search to flows for which difference between timestamps is <= delta
                        Int2ObjectSortedMap<Int2IntOpenHashMap> subMapTimes;
                        if(delta==Integer.MAX_VALUE)
                            subMapTimes=mapTimes2Out.tailMap((time1+1));
                        else
                            subMapTimes=mapTimes2Out.subMap(time1+1,time1+delta+1);
                        for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e2 : subMapTimes.int2ObjectEntrySet())
                        {
                            Int2IntOpenHashMap mapAdiacs2=e2.getValue();
                            if(mapAdiacs2!=null)
                            {
                                //Count the number of cases in which the first and the third node of the flow are identical, i.e. the flows of type A->B<-A
                                IntOpenHashSet commonAdiacs=new IntOpenHashSet(mapAdiacs2.keySet());
                                commonAdiacs.retainAll(mapAdiacs1.keySet());
                                int numCommons=commonAdiacs.size();
                                if(!finish[3])
                                {
                                    //Update counts for asynchronous-II flow with 3 nodes
                                    flowDegrees[3]+=(mapAdiacs1.size()*mapAdiacs2.size())-numCommons;
                                    if(limits!=null && flowDegrees[3]>=limits[3])
                                    {
                                        finish[3]=true;
                                        numEnded++;
                                        if(numEnded==12)
                                            return flowDegrees;
                                    }
                                }
                                if(!finish[10])
                                {
                                    //Update counts for asynchronous-II flow with 2 nodes
                                    flowDegrees[10]+=numCommons;
                                    if(limits!=null && flowDegrees[10]>=limits[10])
                                    {
                                        finish[10]=true;
                                        numEnded++;
                                        if(numEnded==12)
                                            return flowDegrees;
                                    }
                                }
                                if(finish[3] && finish[10])
                                    break;
                            }
                        }
                    }
                    if(!finish[4])
                    {
                        //Search for synchronous-II flows. By definition of temporal network, these flows MUST have 3 nodes.
                        Int2IntOpenHashMap mapAdiacs2=mapTimes2In.get(time1);
                        if(mapAdiacs2!=null)
                        {
                            flowDegrees[4]+=mapAdiacs1.size()*(mapAdiacs2.size()-1);
                            if(limits!=null && flowDegrees[4]>=limits[4])
                            {
                                finish[4]=true;
                                numEnded++;
                                if(numEnded==12)
                                    return flowDegrees;
                            }
                        }
                    }
                    if(finish[0] && finish[1] && finish[2] && finish[3] && finish[4] &&
                            finish[7] && finish[8] && finish[9] && finish[10])
                        break;
                }
            }

            if(!finish[5] || !finish[6] || !finish[11])
            {
                for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e1 : mapTimes1Out.int2ObjectEntrySet())
                {
                    int time1 = e1.getIntKey();
                    Int2IntOpenHashMap mapAdiacs1=e1.getValue();
                    if(!finish[5] || !finish[11])
                    {
                        //Search for asynchronous-OO flows (with 2 or 3 nodes)
                        //Restrict search to flows for which the difference between the timestamps of the two edges is within delta
                        Int2ObjectSortedMap<Int2IntOpenHashMap> subMapTimes;
                        if(delta==Integer.MAX_VALUE)
                            subMapTimes=mapTimes2Out.tailMap((time1+1));
                        else
                            subMapTimes=mapTimes2Out.subMap(time1+1,time1+delta+1);
                        for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e2 : subMapTimes.int2ObjectEntrySet())
                        {
                            Int2IntOpenHashMap mapAdiacs2=e2.getValue();
                            if(mapAdiacs2!=null)
                            {
                                //Count the number of cases in which the first and the third node of the flow are identical, i.e. the flows of type A<-B->A
                                IntOpenHashSet commonAdiacs=new IntOpenHashSet(mapAdiacs2.keySet());
                                commonAdiacs.retainAll(mapAdiacs1.keySet());
                                int numCommons=commonAdiacs.size();
                                if(!finish[5])
                                {
                                    //Update counts for asynchronous-OO flows with 3 nodes
                                    flowDegrees[5]+=(mapAdiacs1.size()*mapAdiacs2.size())-numCommons;
                                    if(limits!=null && flowDegrees[5]>=limits[5])
                                    {
                                        finish[5]=true;
                                        numEnded++;
                                        if(numEnded==12)
                                            return flowDegrees;
                                    }
                                }
                                if(!finish[11])
                                {
                                    //Update counts for asynchronous-OO flows with 2 nodes
                                    flowDegrees[11]+=numCommons;
                                    if(limits!=null && flowDegrees[11]>=limits[11])
                                    {
                                        finish[11]=true;
                                        numEnded++;
                                        if(numEnded==12)
                                            return flowDegrees;
                                    }
                                }
                                if(finish[5] && finish[11])
                                    break;
                            }
                        }
                    }
                    if(!finish[6])
                    {
                        //Search for synchronous-OO flows. These flows MUST have 3 nodes
                        Int2IntOpenHashMap mapAdiacs2=mapTimes2Out.get(time1);
                        if(mapAdiacs2!=null)
                        {
                            flowDegrees[6]+=mapAdiacs1.size()*(mapAdiacs2.size()-1);
                            if(limits!=null && flowDegrees[6]>=limits[6])
                            {
                                finish[6]=true;
                                numEnded++;
                                if(numEnded==12)
                                    return flowDegrees;
                            }
                        }
                    }
                    if(finish[5] && finish[6] && finish[11])
                        break;
                }
            }
            return flowDegrees;
        }
        else
        {
            //Network is undirected, so we have 3 different types of flows
            //Store count for each temporal flow
            int[] flowDegrees=new int[3];
            //For each type of flow, check if we can stop computing flow count for target node
            boolean[] finish=new boolean[3];
            //Number of flow types for which we stopped the evaluation
            int numEnded=0;
            if(limits!=null)
            {
                //Node idNode is a target node. Check if limits are reached for some (or all) flow types
                for(int i=0;i<limits.length;i++)
                {
                    if(limits[i]==0)
                    {
                        //Maximum count for this flow type in query nodes is 0, so we can avoid evaluating that flow type for target node
                        numEnded++;
                        finish[i]=true;
                    }
                }
                if(numEnded==3)
                    //If all limits are reached we can stop the evaluation and return the current partial flow counts
                    return flowDegrees;
            }
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes1=recipAdjLists.get(idNode);
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes2=recipAdjLists.get(idNode);
            if(!finish[0] || !finish[1] || !finish[2])
            {
                for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e1 : mapTimes1.int2ObjectEntrySet())
                {
                    int time1 = e1.getIntKey();
                    Int2IntOpenHashMap mapAdiacs1=e1.getValue();
                    if(!finish[0] || !finish[2])
                    {
                        //Search for asynchronous flows (with 2 or 3 nodes)
                        //Restrict search to flows for which difference between timestamps is <= delta
                        Int2ObjectSortedMap<Int2IntOpenHashMap> subMapTimes;
                        if(delta==Integer.MAX_VALUE)
                            subMapTimes=mapTimes2.tailMap((time1+1));
                        else
                            subMapTimes=mapTimes2.subMap(time1+1,time1+delta+1);
                        for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e2 : subMapTimes.int2ObjectEntrySet())
                        {
                            Int2IntOpenHashMap mapAdiacs2=e2.getValue();
                            if(mapAdiacs2!=null)
                            {
                                //Count the number of cases in which the first and the third node of the flow are identical, i.e. the flows of type A-B-A
                                IntOpenHashSet commonAdiacs=new IntOpenHashSet(mapAdiacs2.keySet());
                                commonAdiacs.retainAll(mapAdiacs1.keySet());
                                int numCommons=commonAdiacs.size();
                                if(!finish[0])
                                {
                                    //Update counts for asynchronous flows with 3 nodes
                                    flowDegrees[0]+=(mapAdiacs1.size()*mapAdiacs2.size())-numCommons;
                                    if(limits!=null && flowDegrees[0]>=limits[0])
                                    {
                                        finish[0]=true;
                                        numEnded++;
                                        if(numEnded==3)
                                            return flowDegrees;
                                        break;
                                    }
                                }
                                if(!finish[2])
                                {
                                    //Update counts for asynchronous flows with 2 nodes
                                    flowDegrees[2]+=numCommons;
                                    if(limits!=null && flowDegrees[2]>=limits[2])
                                    {
                                        finish[2]=true;
                                        numEnded++;
                                        if(numEnded==3)
                                            return flowDegrees;
                                        break;
                                    }
                                }
                            }
                        }
                    }
                    if(!finish[1])
                    {
                        //Search for synchronous flows. By definition of temporal network, these flows MUST have 3 nodes
                        Int2IntOpenHashMap mapAdiacs2=mapTimes2.get(time1);
                        if(mapAdiacs2!=null)
                        {
                            flowDegrees[1]+=mapAdiacs1.size()*(mapAdiacs2.size()-1);
                            if(limits!=null && flowDegrees[1]>=limits[1])
                            {
                                finish[1]=true;
                                numEnded++;
                                if(numEnded==3)
                                    return flowDegrees;
                            }
                        }
                    }
                    if(finish[0] && finish[1] && finish[2])
                        break;
                }
            }
            return flowDegrees;
        }
    }

    //Print network information
    public String toString()
    {
        String str="NODES:\n";
        for(int idNode : nodeLabs.keySet())
            str+=idNode+":"+nodeLabs.get(idNode)+"\n";
        str+="\nEDGES:\n";
        Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> adjLists=outAdjLists;
        if(!directed)
            adjLists=recipAdjLists;
        for (Int2ObjectMap.Entry<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> source : adjLists.int2ObjectEntrySet())
        {
            int idSource=source.getIntKey();
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes=source.getValue();
            for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e : mapTimes.int2ObjectEntrySet())
            {
                Int2IntOpenHashMap mapAdiacs=e.getValue();
                for (Int2IntMap.Entry dest : mapAdiacs.int2IntEntrySet())
                {
                    int idDest=dest.getIntKey();
                    if(directed || idSource<idDest)
                    {
                        int edge=dest.getIntValue();
                        int[] props=edgeProps.get(edge);
                        str+="("+idSource+","+idDest+","+props[0]+":"+props[1]+")\n";
                    }
                }
            }
        }
        str+="\n";
        return str;
    }

    //Check if two nodes are neighbors (i.e. they are connected by an edge with any timestamp)
    public boolean areNeighbors(int idNode1, int idNode2)
    {
        Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes=outAdjLists.get(idNode1);
        if(mapTimes!=null)
        {
            for(Int2ObjectMap.Entry<Int2IntOpenHashMap> e : mapTimes.int2ObjectEntrySet())
            {
                Int2IntOpenHashMap mapAdiacs=e.getValue();
                if(mapAdiacs.containsKey(idNode2))
                    return true;
            }
        }
        mapTimes=inAdjLists.get(idNode1);
        if(mapTimes!=null)
        {
            for(Int2ObjectMap.Entry<Int2IntOpenHashMap> e : mapTimes.int2ObjectEntrySet())
            {
                Int2IntOpenHashMap mapAdiacs=e.getValue();
                if(mapAdiacs.containsKey(idNode2))
                    return true;
            }
        }
        mapTimes=recipAdjLists.get(idNode1);
        if(mapTimes!=null)
        {
            for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e : mapTimes.int2ObjectEntrySet())
            {
                Int2IntOpenHashMap mapAdiacs=e.getValue();
                if(mapAdiacs.containsKey(idNode2))
                    return true;
            }
        }
        return false;
    }

    //Get the S-relative degree, i.e. the number of nodes in a set S that are connected to node n
    public int getDegree(int node, IntOpenHashSet setRefNodes)
    {
        int deg=0;
        for(int refNode : setRefNodes)
        {
            if(areNeighbors(node,refNode))
                deg++;
        }
        return deg;
    }

    //Extract randomly a subgraph with numFinalEdges edges from the temporal network
    //THIS TEMPORARILY WORKS ONLY ON DIRECTED GRAPHS
    public Graph extractRandomSubgraph(int numFinalEdges)
    {
        Graph subgraph=new Graph(this.directed);
        Random r=new Random();
        //List of ids of sampled edges of the subgraph
        IntOpenHashSet setSampledEdges=new IntOpenHashSet();
        //List of all possible starting nodes from which the subgraph should be extracted
        int[] listStartNodes=nodeLabs.keySet().toArray(new int[0]);
        //Sample randomly the starting node and add it to the final extracted subgraph
        int startNode=listStartNodes[r.nextInt(listStartNodes.length)];
        subgraph.addNode(startNode,nodeLabs.get(startNode));
        int numSampledEdges=0;
        //Maximum number of attempts done to sample a new edge and add it to the extracted subgraph
        //This is done to avoid being trapped in the case the sampled subgraph cannot be further extended
        int numAttempts=1000;
        int indexAttempt=0;
        //List of sources of extracted edges
        int[] sources=new int[numFinalEdges];
        //List of destinations of extracted edges
        int[] dests=new int[numFinalEdges];
        while(numSampledEdges<numFinalEdges && indexAttempt<numAttempts)
        {
            //Get list of candidate incident edges for sampling
            //Candidate edges are all edges incident in one of the nodes already present in the current sampled subgraph
            //List of sources of candidate edges
            IntArrayList candSources=new IntArrayList();
            //List of destinations of candidate edges
            IntArrayList candDests=new IntArrayList();
            //List of ids of candidate edges
            IntArrayList candEdges=new IntArrayList();
            for(int start : subgraph.getNodeLabs().keySet())
            {
                Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes=outAdjLists.get(start);
                if(mapTimes!=null)
                {
                    for(int time : mapTimes.keySet())
                    {
                        Int2IntOpenHashMap mapAdiacs=mapTimes.get(time);
                        for(int adiac : mapAdiacs.keySet())
                        {
                            int edgeId=mapAdiacs.get(adiac);
                            //Check if candidate edges has been already sampled before
                            if(!setSampledEdges.contains(edgeId))
                            {
                                //Update candidate sets
                                candSources.add(start);
                                candDests.add(adiac);
                                candEdges.add(edgeId);
                            }
                        }
                    }
                }
                mapTimes=inAdjLists.get(start);
                if(mapTimes!=null)
                {
                    for(int time : mapTimes.keySet())
                    {
                        Int2IntOpenHashMap mapAdiacs=mapTimes.get(time);
                        for(int adiac : mapAdiacs.keySet())
                        {
                            int edgeId=mapAdiacs.get(adiac);
                            //Check if candidate edges has been already sampled before
                            if(!setSampledEdges.contains(edgeId))
                            {
                                //Update candidate sets
                                candSources.add(adiac);
                                candDests.add(start);
                                candEdges.add(edgeId);
                            }
                        }
                    }
                }
            }
            if(!candSources.isEmpty())
            {
                //There are one or more candidate edges to sample
                //Sample randomly one edge from the list of candidate incident edges
                int sampledPos=r.nextInt(candSources.size());
                int sampledSource=candSources.getInt(sampledPos);
                int sampledDest=candDests.getInt(sampledPos);
                int sampledEdge=candEdges.getInt(sampledPos);
                //System.out.println(sampledSource+"\t"+sampledDest+"\t"+sampledEdge);
                //Add source and destination nodes of the sampled edge if necessary
                subgraph.addNode(sampledSource,nodeLabs.get(sampledSource));
                subgraph.addNode(sampledDest,nodeLabs.get(sampledDest));
                //Add sampled edge to subgraph
                int[] props=edgeProps.get(sampledEdge);
                subgraph.addEdge(sampledSource,sampledDest,props[0],props[1]);
                //Update auxiliary information
                sources[numSampledEdges]=sampledSource;
                dests[numSampledEdges]=sampledDest;
                numSampledEdges++;
                setSampledEdges.add(sampledEdge);
            }
            else
                //No valid candidate edge has been sampled. Try resampling
                indexAttempt++;
        }
        if(numSampledEdges==numFinalEdges)
        {
            //Subgraph has been fully extracted
            //Sample edge timestamps with replacement
            ObjectOpenHashSet<String> setEdges=new ObjectOpenHashSet<>();
            ObjectArrayList<int[]> subgraphEdgeProps=subgraph.getEdgeProps();
            int i=0;
            while(i<numSampledEdges)
            {
                int sampledTime=r.nextInt(numSampledEdges)+1;
                //If an edge between the two nodes with the same sampled timestamp already exists, sample a new timestamp
                if(!setEdges.contains(sources[i]+"-"+dests[i]+"-"+sampledTime))
                {
                    //Record sampled edge with its timestamp and update its timestamp in the final extracted subgraph
                    setEdges.add(sources[i]+"-"+dests[i]+"-"+sampledTime);
                    int[] props=subgraphEdgeProps.get(i);
                    props[0]=sampledTime;
                    i++;
                }
            }
            return subgraph;
        }
        else
            return null;
    }

}
