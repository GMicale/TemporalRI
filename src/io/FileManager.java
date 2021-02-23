package io;

import graph.*;
import it.unimi.dsi.fastutil.ints.*;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import it.unimi.dsi.fastutil.objects.ObjectArrayList;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;

public class FileManager
{
    private final Object2IntOpenHashMap<String> mapNodeLabelsToIds;
    private final ObjectArrayList<String> setNodeLabels;
    private final Object2IntOpenHashMap<String> mapEdgeLabelsToIds;
    private final ObjectArrayList<String> setEdgeLabels;

    public FileManager()
    {
        mapNodeLabelsToIds=new Object2IntOpenHashMap<>();
        setNodeLabels=new ObjectArrayList<>();
        mapEdgeLabelsToIds=new Object2IntOpenHashMap<>();
        setEdgeLabels=new ObjectArrayList<>();
    }

    public Graph readGraph(String file, Graph pattern, boolean directed) throws Exception
    {
        Graph g=new Graph(directed);
        Int2IntOpenHashMap queryNodeLabs=null;
        ObjectArrayList<int[]> queryEdgeProps=null;
        if(pattern!=null)
        {
            queryNodeLabs=pattern.getNodeLabs();
            queryEdgeProps=pattern.getEdgeProps();
        }
        BufferedReader br=new BufferedReader(new FileReader(file));
        String str;
        int numNodes=Integer.parseInt(br.readLine());
        for(int i=0;i<numNodes;i++)
        {
            str=br.readLine();
            String[] split=str.split("\t");
            int nodeId=Integer.parseInt(split[0]);
            String label=split[1];
            if(!mapNodeLabelsToIds.containsKey(label))
            {
                mapNodeLabelsToIds.put(label,setNodeLabels.size());
                setNodeLabels.add(label);
            }
            int labId=mapNodeLabelsToIds.getInt(label);
            if(pattern!=null)
            {
                for(int queryNode : queryNodeLabs.keySet())
                {
                    if(labId==queryNodeLabs.get(queryNode))
                    {
                        g.addNode(nodeId,labId);
                        break;
                    }
                }
            }
            else
                g.addNode(nodeId,labId);
        }
        while((str=br.readLine())!=null)
        {
            String[] split=str.split("\t");
            int idSource=Integer.parseInt(split[0]);
            int idDest=Integer.parseInt(split[1]);
            if(idSource!=idDest)
            {
                String[] split2=split[2].split(",");
                for(int i=0;i<split2.length;i++)
                {
                    String[] split3=split2[i].split(":");
                    int timestamp=Integer.parseInt(split3[0]);
                    String edgeLab=split3[1];
                    if(!mapEdgeLabelsToIds.containsKey(edgeLab))
                    {
                        mapEdgeLabelsToIds.put(edgeLab,setEdgeLabels.size());
                        setEdgeLabels.add(edgeLab);
                    }
                    int labId=mapEdgeLabelsToIds.getInt(edgeLab);
                    if(pattern!=null)
                    {
                        for(int[] queryProps : queryEdgeProps)
                        {
                            if(labId==queryProps[1])
                            {
                                g.addEdge(idSource,idDest,timestamp,labId);
                                break;
                            }
                        }
                    }
                    else
                        g.addEdge(idSource,idDest,timestamp,labId);
                }
            }
        }
        br.close();
        return g;
    }

    public void printOcc(Graph occ)
    {
        Int2IntOpenHashMap nodeLabs=occ.getNodeLabs();
        ObjectArrayList<int[]> edgeProps=occ.getEdgeProps();
        Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> outAdjLists=occ.getOutAdjLists();
        IntIterator it=nodeLabs.keySet().iterator();
        int idNode=it.nextInt();
        System.out.print("("+idNode+":"+nodeLabs.get(idNode)+")");
        while(it.hasNext())
        {
            idNode=it.nextInt();
            System.out.print(","+"("+idNode+":"+nodeLabs.get(idNode)+")");
        }
        System.out.print("\t");
        String edgeStr="";
        for (Int2ObjectMap.Entry<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> source : outAdjLists.int2ObjectEntrySet())
        {
            int idSource= source.getIntKey();
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes=source.getValue();
            for (Int2ObjectMap.Entry<Int2IntOpenHashMap> time : mapTimes.int2ObjectEntrySet())
            {
                Int2IntOpenHashMap mapAdiacs=time.getValue();
                for (Int2IntMap.Entry dest : mapAdiacs.int2IntEntrySet())
                {
                    int idDest=dest.getIntKey();
                    int edge=dest.getIntValue();
                    int[] props=edgeProps.get(edge);
                    edgeStr+="("+idSource+","+idDest+","+props[0]+":"+props[1]+"),";
                }
            }
        }
        edgeStr=edgeStr.substring(0,edgeStr.length()-1);
        System.out.println(edgeStr);
    }

    public void writeQuery(Graph g, String file) throws Exception
    {
        Int2IntOpenHashMap nodeLabs=g.getNodeLabs();
        boolean directed=g.isDirected();
        int[] listIdNodes=nodeLabs.keySet().toArray(new int[0]);
        ObjectArrayList<int[]> edgeProps=g.getEdgeProps();
        Int2ObjectOpenHashMap<Int2ObjectAVLTreeMap<Int2IntOpenHashMap>> adjLists=g.getOutAdjLists();
        if(!directed)
            adjLists=g.getRecipAdjLists();
        BufferedWriter bw=new BufferedWriter(new FileWriter(file));
        bw.write(nodeLabs.size()+"\n");
        for(int i=0;i<listIdNodes.length;i++)
            bw.write(i+"\t"+setNodeLabels.get(nodeLabs.get(listIdNodes[i]))+"\n");
        for(int i=0;i<listIdNodes.length;i++)
        {
            Int2ObjectAVLTreeMap<Int2IntOpenHashMap> mapTimes=adjLists.get(listIdNodes[i]);
            for(int time : mapTimes.keySet())
            {
                Int2IntOpenHashMap mapAdiacs=mapTimes.get(time);
                for(int j=0;j<listIdNodes.length;j++)
                {
                    if(directed || i<j)
                    {
                        if(mapAdiacs.containsKey(listIdNodes[j]))
                        {
                            int edge=mapAdiacs.get(listIdNodes[j]);
                            int[] props=edgeProps.get(edge);
                            bw.write(i+"\t"+j+"\t"+props[0]+":"+setEdgeLabels.get(props[1])+"\n");
                        }
                    }
                }
            }

        }
        bw.close();
    }

}
