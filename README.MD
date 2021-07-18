Java implementation of TemporalRI algorithm for subgraph matching in temporal networks.
<br/><br/>

<b>References:</b>

- Locicero G, Micale G, Pulvirenti A, Ferro A (2021). <i>TemporalRI: A Subgraph Isomorphism Algorithm for Temporal Networks.</i> International Conference on Complex Networks and Their Applications, (Complex Networks 2020), pp. 675-687.
- Micale G, Locicero G, Pulvirenti A, Ferro A (2021). <i>TemporalRI: subgraph isomorphism in temporal networks with multiple contacts</i>. Applied Network Science 6, 55 (2021), doi:10.1007/s41109-021-00397-0.
<br/><br/>

<b>Requirements:</b>

Java, version 14.0.1 or higher (https://www.oracle.com/it/java/technologies/javase-downloads.html)
<br/><br/>

<b>Usage:</b>

<pre>
java -jar TemporalRI.jar -t targetFile -q queryFile [-d deltaThresh -u -o dumpOccFile]
</pre>

Required parameters:
<pre>
-t     Target network file
-q     Query network file
</pre>

Optional parameters:
<pre>
-d      Delta threshold for time window of events (by default delta is infinite)
-u      Treat target and query as undirected (by default networks are directed)
-o      Save query occurrences to specified output file (by default do not save, just count)
</pre>
<br/>

<b>Input files format:</b>

Target network is specified as a text file. 
The first row contains the number n of nodes in the network. 
The following n rows contain the id of a node followed by its label. 
Both strings and numbers can be specified as labels. 
Fields are separated by tab character (\t). The remaining rows contain the list of edges.

Each edge is specified by the id of the source node, followed by the id of the destination node, the timestamp of the edge and the edge label, according to the following format:

<pre>
idSource    idDest    timestamp:label
</pre>

'idSource', 'idDest' and 'timestamp:label' fields are separated by tab characters (\t).
Both strings and numbers can be specified as labels.
Multiple edges between two nodes but with different timestamps can be specified.
<br/>

Example:

<pre>
4
1	1
2	1
3	2
4	2
1	3	1245:5
1	4	1244:5
2	3	1245:5
1	3	1248:4
2	4	1246:4
</pre>

Query network file has the same format, but node ids range from 0 to n-1, where n is the number of nodes.
<br/>

Example:

<pre>
3
0	1
1	1
2	1
0	1	1:1
0	2	2:1
0	1	2:2
</pre>
<br/>

<b>Output file format:</b>

By default the count of occurrences of the query in the target is returned.
If an output file is specified, also the occurrences are returned and collected in the output file.

Each row contains an occurrence specified according to the following format:

<pre>
(idNode1:lab1),(idNode2:lab2),...,(idNodeN:labN)    (idSource1,idDest1,timestamp1:lab1),(idSource2,idDest2,timestamp2:lab2),...,(idSourceM,idDestM,timestampM:labM)
</pre>

where N and M are the number of nodes and edges of the occurrence, respectively.
<br/><br/>


<b>Benchmark dataset:</b>

<a href="https://dmiunictit-my.sharepoint.com/:u:/g/personal/gmicale_dmi_unict_it/ETDXZnJqcQhPgsL_2nQafNUBaJI4AU3KGB52g0fGAkMKPg?e=wyp7Xo">Temporal networks</a>: a dataset composed by 7 real temporal networks of different sizes. 

<a href="https://dmiunictit-my.sharepoint.com/:u:/g/personal/gmicale_dmi_unict_it/ERma4KO8QEBPqDTVeXbqQNgBi3ZMRW5ExgVs340vUm32Ug?e=Bu7C3C">Temporal queries</a>: a dataset of queries with 3, 6, and 9 edges randomly extracted from the above 7 real temporal networks.

More details about the networks and how the queries were extracted are reported in:

- Micale G, Locicero G, Pulvirenti A, Ferro A (2021). <i>TemporalRI: subgraph isomorphism in temporal networks with multiple contacts</i>. Submitted to Applied Network Science (2021).
