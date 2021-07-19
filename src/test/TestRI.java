package test;

import graph.Graph;
import io.FileManager;
import matching.RISolver;

//Test TemporalRI algorithm with pre-defined targets and queries
public class TestRI
{
    public static void main(String[] args) throws Exception
    {
        String targetFile="Networks/target4.txt";
        String queryfile="Queries/query4.txt";
        int delta=2;
        boolean directed=true;
        boolean dump=true;
        FileManager fm=new FileManager();
        System.out.println("Reading query graph...");
        Graph query=fm.readGraph(queryfile,null,directed);

        double inizio=System.currentTimeMillis();
        System.out.println("Reading target graph...");
        Graph target=fm.readGraph(targetFile,query,directed);
        //System.out.println(target+"\n");
        double fine=System.currentTimeMillis();
        double totalTimeReading=(fine-inizio)/1000;
        inizio=System.currentTimeMillis();
        System.out.println("Matching query to target...");
        RISolver ri=new RISolver(target,query,dump,fm);
        if(dump)
        {
            System.out.println("\nOCCURRENCES FOUND:\n");
            System.out.println("Nodes\tEdges");
        }
        long numOccs=ri.match(delta);
        fine=System.currentTimeMillis();
        double totalTimeMatching=(fine-inizio)/1000;
        System.out.println("\nDone! Found "+numOccs+" occurrences\n");
        System.out.println("Time for reading: "+totalTimeReading+" secs");
        System.out.println("Time for matching: "+totalTimeMatching+" secs");
        System.out.println("Total time: "+(totalTimeReading+totalTimeMatching)+" secs");

    }
}
