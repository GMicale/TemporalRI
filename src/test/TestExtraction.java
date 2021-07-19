package test;

import graph.Graph;
import io.FileManager;

import java.io.File;

//Test extraction of temporal subgraphs from target graph
public class TestExtraction
{
    public static void main(String[] args) throws Exception
    {
        String[] networks={"SFHH-conf-sensor","tech-as-topology","ia-contacts_dublin","ia-enron-email-dynamic",
                "digg-friends","ia-yahoo-messages","ia-prosper-loans"};
        int[] querySizes={3,6,9};
        int numQueries=100;
        FileManager fm=new FileManager();
        for(int j=0;j<querySizes.length;j++)
        {
            for(int i=0;i<networks.length;i++)
            {
                //int delta=querySizes[j]*deltaValues[i];
                String targetFile="C:/Ricerca/TemporalRI/NetworksRI/"+networks[i]+".txt";
                Graph target=fm.readGraph(targetFile,null,true);
                File f=new File("C:/Ricerca/TemporalRI/QueriesRI/"+networks[i]);
                if(!f.exists())
                    f.mkdir();
                String queryFolder="C:/Ricerca/TemporalRI/QueriesRI/"+networks[i]+"/"+querySizes[j];
                f=new File("C:/Ricerca/TemporalRI/QueriesRI/"+networks[i]+"/"+querySizes[j]);
                if(!f.exists())
                    f.mkdir();
                //System.out.println(target+"\n");
                System.out.println("Extracting queries of size "+querySizes[j]+" for "+networks[i]+"...");
                int l=1;
                while(l<=numQueries)
                {
                    Graph subgraph=target.extractRandomSubgraph(querySizes[j]);
                    if(subgraph!=null)
                    {
                        fm.writeQuery(subgraph,queryFolder+"/query_"+l+".txt");
                        l++;
                    }
                }
            }
        }
    }
}