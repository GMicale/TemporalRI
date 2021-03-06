package graph;

import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.*;

import java.util.*;

public class Graph
{
    private final boolean directed;
    private final Int2IntOpenHashMap nodeLabs;
    private final ObjectArrayList<int[]> edgeProps;
    private final Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> outAdjLists;
    private final Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> inAdjLists;
    private final Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> recipAdjLists;

    public Graph(boolean dir)
    {
        directed=dir;
        nodeLabs=new Int2IntOpenHashMap();
        edgeProps=new ObjectArrayList<>();
        outAdjLists=new Int2ObjectOpenHashMap<>();
        outAdjLists.defaultReturnValue(null);
        inAdjLists=new Int2ObjectOpenHashMap<>();
        inAdjLists.defaultReturnValue(null);
        recipAdjLists=new Int2ObjectOpenHashMap<>();
        recipAdjLists.defaultReturnValue(null);
    }

    public boolean isDirected()
    {
        return directed;
    }

    public int getNumNodes()
    {
        return nodeLabs.size();
    }

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

    public void addNode(int id, int lab)
    {
        nodeLabs.putIfAbsent(id,lab);
        if(directed)
        {
            outAdjLists.putIfAbsent(id,new Int2ObjectAVLTreeMap<>());
            inAdjLists.putIfAbsent(id,new Int2ObjectAVLTreeMap<>());
        }
        else
            recipAdjLists.putIfAbsent(id,new Int2ObjectAVLTreeMap<>());
    }

    public void addEdge(int idSource, int idDest, int timestamp, int lab)
    {
        int idEdge=edgeProps.size();
        int[] props=new int[2];
        props[0]=timestamp;
        props[1]=lab;
        edgeProps.add(props);
        if(directed)
        {
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes=outAdjLists.get(idSource);
            Int2IntOpenHashMap mapAdiacs=mapTimes.get(timestamp);
            if(mapAdiacs==null)
            {
                mapAdiacs=new Int2IntOpenHashMap();
                mapAdiacs.put(idDest,idEdge);
                mapTimes.put(timestamp,mapAdiacs);
            }
            else
                mapAdiacs.put(idDest,idEdge);
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

    public int[] getNodeDegrees(int idNode, int[] limits)
    {
        int[] degrees=new int[3];
        boolean[] finish=new boolean[3];
        int numEnded=0;
        if(limits!=null)
        {
            for(int i=0;i<limits.length;i++)
            {
                if(limits[i]==0)
                {
                    numEnded++;
                    finish[i]=true;
                }
            }
            if(numEnded==3)
                return degrees;
        }
        Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimesOut=outAdjLists.get(idNode);
        Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimesIn=inAdjLists.get(idNode);
        Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimesRecip=recipAdjLists.get(idNode);
        if(!finish[0] || !finish[1] || !finish[2])
        {
            //Out-degree
            if(mapTimesOut!=null)
            {
                for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e : mapTimesOut.int2ObjectEntrySet())
                {
                    Int2IntOpenHashMap mapAdiacs=e.getValue();
                    degrees[0]+=mapAdiacs.size();
                    if(limits!=null && degrees[0]>=limits[0])
                    {
                        finish[0]=true;
                        numEnded++;
                        if(numEnded==3)
                            return degrees;
                        break;
                    }
                }
            }
            //In-degree
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
            //Recip-degree
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

    public int[] getFlowDegrees(int idNode, int[] limits, int delta)
    {
        if(directed)
        {
            int[] flowDegrees=new int[12];
            boolean[] finish=new boolean[12];
            int numEnded=0;
            if(limits!=null)
            {
                for(int i=0;i<limits.length;i++)
                {
                    if(limits[i]==0)
                    {
                        numEnded++;
                        finish[i]=true;
                    }
                }
                if(numEnded==12)
                    return flowDegrees;
            }
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes1In=inAdjLists.get(idNode);
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes1Out=outAdjLists.get(idNode);
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes2Out=outAdjLists.get(idNode);
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes2In=inAdjLists.get(idNode);

            if(!finish[0] || !finish[1] || !finish[2] || !finish[3] || !finish[4] ||
                    !finish[7] || !finish[8] || !finish[9] || !finish[10])
            {
                for (Int2ObjectMap.Entry<Int2IntOpenHashMap> e1 : mapTimes1In.int2ObjectEntrySet())
                {
                    int time1 = e1.getIntKey();
                    Int2IntOpenHashMap mapAdiacs1=e1.getValue();
                    if(!finish[0] || !finish[7])
                    {
                        //Forward-IO flow
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
                                IntOpenHashSet commonAdiacs=new IntOpenHashSet(mapAdiacs2.keySet());
                                commonAdiacs.retainAll(mapAdiacs1.keySet());
                                int numCommons=commonAdiacs.size();
                                if(!finish[0])
                                {
                                    //Forward-IO flow with 3 nodes
                                    flowDegrees[0]+=(mapAdiacs1.size()*mapAdiacs2.size())-numCommons;
                                    if(limits!=null && flowDegrees[0]>=limits[0])
                                    {
                                        finish[0]=true;
                                        numEnded++;
                                        if(numEnded==12)
                                            return flowDegrees;
                                    }
                                }
                                if(!finish[7])
                                {
                                    //Forward-IO flow with 2 nodes
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
                                    break;
                            }
                        }
                    }
                    if(!finish[1] || !finish[8])
                    {
                        //Backward-IO flow
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
                                IntOpenHashSet commonAdiacs=new IntOpenHashSet(mapAdiacs2.keySet());
                                commonAdiacs.retainAll(mapAdiacs1.keySet());
                                int numCommons=commonAdiacs.size();
                                if(!finish[1])
                                {
                                    //Backward-IO flow with 3 nodes
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
                                    //Backward-IO flow with 2 nodes
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
                                    break;
                            }
                        }
                    }
                    if(!finish[2] || !finish[9])
                    {
                        //Synchronous-IO flow
                        Int2IntOpenHashMap mapAdiacs2=mapTimes2Out.get(time1);
                        if(mapAdiacs2!=null)
                        {
                            IntOpenHashSet commonAdiacs=new IntOpenHashSet(mapAdiacs2.keySet());
                            commonAdiacs.retainAll(mapAdiacs1.keySet());
                            int numCommons=commonAdiacs.size();
                            if(!finish[2])
                            {
                                //Synchronous-IO flow with 3 nodes
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
                                //Synchronous-IO flow with 2 nodes
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
                        //Asynchronous-II flow
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
                                IntOpenHashSet commonAdiacs=new IntOpenHashSet(mapAdiacs2.keySet());
                                commonAdiacs.retainAll(mapAdiacs1.keySet());
                                int numCommons=commonAdiacs.size();
                                if(!finish[3])
                                {
                                    //Asynchronous-II flow with 3 nodes
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
                                    //Asynchronous-II flow with 2 nodes
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
                        //Synchronous-II flow
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
                        //Asynchronous-OO flow
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
                                IntOpenHashSet commonAdiacs=new IntOpenHashSet(mapAdiacs2.keySet());
                                commonAdiacs.retainAll(mapAdiacs1.keySet());
                                int numCommons=commonAdiacs.size();
                                if(!finish[5])
                                {
                                    //Asynchronous-OO flow with 3 nodes
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
                                    //Asynchronous-OO flow with 2 nodes
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
                        //Synchronous-OO flow
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
            int[] flowDegrees=new int[3];
            boolean[] finish=new boolean[3];
            int numEnded=0;
            if(limits!=null)
            {
                for(int i=0;i<limits.length;i++)
                {
                    if(limits[i]==0)
                    {
                        numEnded++;
                        finish[i]=true;
                    }
                }
                if(numEnded==3)
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
                        //Asynchronous flow
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
                                IntOpenHashSet commonAdiacs=new IntOpenHashSet(mapAdiacs2.keySet());
                                commonAdiacs.retainAll(mapAdiacs1.keySet());
                                int numCommons=commonAdiacs.size();
                                if(!finish[0])
                                {
                                    //Asynchronous flow with 3 nodes
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
                                    //Asynchronous flow with 2 nodes
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
                        //Synchronous flow
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

    public Graph extractRandomSubgraph(int numFinalEdges)
    {
        Graph subgraph=new Graph(this.directed);
        Random r=new Random();
        IntOpenHashSet setSampledEdges=new IntOpenHashSet();
        int[] listStartNodes=nodeLabs.keySet().toArray(new int[0]);
        int startNode=listStartNodes[r.nextInt(listStartNodes.length)];
        subgraph.addNode(startNode,nodeLabs.get(startNode));
        int numSampledEdges=0;
        int numAttempts=1000;
        int indexAttempt=0;
        int[] sources=new int[numFinalEdges];
        int[] dests=new int[numFinalEdges];
        while(numSampledEdges<numFinalEdges && indexAttempt<numAttempts)
        {
            listStartNodes=subgraph.getNodeLabs().keySet().toArray(new int[0]);
            int sampledNode=listStartNodes[r.nextInt(listStartNodes.length)];
            int dirType=r.nextInt(2);
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes;
            if(dirType==0)
                mapTimes=outAdjLists.get(sampledNode);
            else
                mapTimes=inAdjLists.get(sampledNode);
            if(mapTimes!=null && !mapTimes.isEmpty())
            {
                int[] listTimes=mapTimes.keySet().toArray(new int[0]);
                int sampledTime=listTimes[r.nextInt(listTimes.length)];
                Int2IntOpenHashMap mapAdiacs=mapTimes.get(sampledTime);
                int[] listAdiacs=mapAdiacs.keySet().toArray(new int[0]);
                int sampledAdiac=listAdiacs[r.nextInt(listAdiacs.length)];
                int edge=mapAdiacs.get(sampledAdiac);
                if(!setSampledEdges.contains(edge))
                {
                    setSampledEdges.add(edge);
                    subgraph.addNode(sampledAdiac,nodeLabs.get(sampledAdiac));
                    int[] props=edgeProps.get(edge);
                    if(dirType==1)
                    {
                        subgraph.addEdge(sampledAdiac,sampledNode,props[0],props[1]);
                        sources[numSampledEdges]=sampledAdiac;
                        dests[numSampledEdges]=sampledNode;
                    }
                    else
                    {
                        subgraph.addEdge(sampledNode,sampledAdiac,props[0],props[1]);
                        sources[numSampledEdges]=sampledNode;
                        dests[numSampledEdges]=sampledAdiac;
                    }
                    numSampledEdges++;
                }
                else
                    indexAttempt++;
            }
            else
                indexAttempt++;
        }
        if(numSampledEdges==numFinalEdges)
        {
            int[] sampledTimes=new int[numSampledEdges];
            TreeSet<Integer> setTimes;
            while(true)
            {
                setTimes=new TreeSet<>();
                int numDistinct=r.nextInt(numSampledEdges)+1;
                ObjectOpenHashSet<String> setEdges=new ObjectOpenHashSet<>();
                int i=0;
                while(i<sampledTimes.length)
                {
                    sampledTimes[i]=r.nextInt(numDistinct)+1;
                    if(!setEdges.contains(sources[i]+"-"+dests[i]+"-"+sampledTimes[i]))
                    {
                        setEdges.add(sources[i]+"-"+dests[i]+"-"+sampledTimes[i]);
                        setTimes.add(sampledTimes[i]);
                        i++;
                    }
                    else
                        break;
                }
                if(i==sampledTimes.length && setTimes.size()==numDistinct)
                    break;
            }
            ObjectArrayList<int[]> subgraphEdgeProps=subgraph.getEdgeProps();
            int i=0;
            for(int[] props : subgraphEdgeProps)
            {
                int rank=setTimes.headSet(sampledTimes[i],false).size()+1;
                props[0]=rank;
                i++;
            }
            return subgraph;
        }
        else
            return null;
    }

}
