package buildgraph;

public class BuildDeBruijnGraph {
	
	public static void main(String[] args){
    	
//    	String infile = "/specific/netapp5/gaga/data-scratch/yaelbenari/datas/smalldata.fastq";
		String infile = "/specific/netapp5/gaga/data-scratch/yaelbenari/datas/chr14.fastq";
    	int k = 60, numBlocks = 256, pivot_len = 8, bufferSize = 8192, readLen = 101, numThreads = 1, hsmapCapacity = 10000000;
    	boolean readable = false;
    	
//    	if(args[0].equals("-help")){
//    		System.out.print("Usage: java -jar BuildDeBruijnGraph.jar -in InputPath -k k -L readLength[options]\n" +
//	        			       "Options Available: \n" +
//	        			       "[-NB numOfBlocks] : (Integer) Number Of Kmer Blocks. Default: 256" + "\n" +
//	        			       "[-p pivotLength] : (Integer) Pivot Length. Default: 12" + "\n" +
//	        			       "[-t numOfThreads] : (Integer) Number Of Threads. Default: 1" + "\n" +
//	        			       "[-b bufferSize] : (Integer) Read/Writer Buffer Size. Default: 8192" + "\n" +
//	        			       "[-r readable] : (Boolean) Output Format: true means readable text, false means binary. Default: false" + "\n");
//    		return;
//    	}
    	
    	for(int i=0; i<args.length; i+=2){
    		if(args[i].equals("-in"))
    			infile = args[i+1];
    		else if(args[i].equals("-k"))
    			k = new Integer(args[i+1]);
    		else if(args[i].equals("-NB"))
    			numBlocks = new Integer(args[i+1]);
    		else if(args[i].equals("-p"))
    			pivot_len = new Integer(args[i+1]);
    		else if(args[i].equals("-b"))
    			bufferSize = new Integer(args[i+1]);
    		else if(args[i].equals("-L"))
    			readLen = new Integer(args[i+1]);
    		else if(args[i].equals("-t"))
    			numThreads = new Integer(args[i+1]);
    		else if(args[i].equals("-r"))
    			readable = new Boolean(args[i+1]);
    		else{
    			System.out.println("Wrong with arguments. Abort!");
    			return;
    		}
    	}
    	
		
		Partition partition = new Partition(k, infile, numBlocks, pivot_len, bufferSize, readLen);
		Map map = new Map(k, numBlocks, bufferSize, hsmapCapacity);
	
		try{
			
			System.out.println("Program Configuration:");
	    	System.out.print("Input File: " + infile + "\n" +
	    					 "Kmer Length: " + k + "\n" +
	    					 "Read Length: " + readLen + "\n" +
	    					 "# Of Blocks: " + numBlocks + "\n" +
	    					 "Pivot Length: " + pivot_len + "\n" +
	    					 "# Of Threads: " + numThreads + "\n" +
	    					 "R/W Buffer Size: " + bufferSize + "\n" +
	    					 "Output Format: " + (readable==true?"Text":"Binary") + "\n");
		
			long maxID = partition.Run();
			map.Run(numThreads);
			
//			long time1=0;
//			long t1 = System.currentTimeMillis();
//			System.out.println("Merge IDReplaceTables Begin!");
//			String sortcmd = "sort -t $\'\t\' -o IDReplaceTable +0 -1 -n -m Maps/maps*";
//			Runtime.getRuntime().exec(new String[]{"/bin/sh","-c",sortcmd},null,null).waitFor();
//			long t2 = System.currentTimeMillis();
//			time1 = (t2-t1)/1000;
//			System.out.println("Time used for merging: " + time1 + " seconds!");
//
//			Replace replace = new Replace("IDReplaceTable", "OutGraph", k, bufferSize, readLen, maxID);
//			replace.Run(readable);
			
		
		}
		catch(Exception E){
			System.out.println("Exception caught!");
			E.printStackTrace();
		}
		
	}	

}
