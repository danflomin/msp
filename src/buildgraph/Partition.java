package buildgraph;

import sun.reflect.generics.reflectiveObjects.NotImplementedException;

import java.io.*;
import java.sql.Array;
import java.util.*;
import java.util.Map.Entry;


public class Partition{
	
	private int k;
	private String inputfile;
	private int numOfBlocks;
	private int pivotLen;
	private int bufSize;
	
	private FileReader frG;
	private BufferedReader bfrG;
	private FileWriter[] fwG;
	private BufferedWriter[] bfwG;
	
	private int readLen;
	private Hashtable<Long, Long> pmerDistribution;

	private Hashtable<Long, Integer> pmerToBinId;

	private static int[] valTable = new int[]{0,-1,1,-1,-1,-1,2,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,3};
	private static char[] twinTable = new char[]{'T','0','G','0','0','0','C','0','0','0','0','0','0','0','0','0','0','0','0','A'};

	public Partition(int bufferSize, Hashtable<Long, Long> pmerDistribution){
		this.inputfile = "Nodes/raw_nodes";
		this.bufSize = bufferSize;
		this.pmerDistribution = pmerDistribution;
		this.pmerToBinId = new Hashtable<>();
	}
	

	
	private long DistributeNodes() throws IOException{
		numOfBlocks = buildBinMapping();

		frG = new FileReader(inputfile);
		bfrG = new BufferedReader(frG, bufSize);
		fwG = new FileWriter[numOfBlocks];
		bfwG = new BufferedWriter[numOfBlocks];
		
		int binIndex;
		long id = 0;
		String payload;
		Long rawPmerValue;

		String line;
		
		File dir = new File("Nodes");
		if(!dir.exists())
			dir.mkdir();
	
		for(int i=0;i<numOfBlocks;i++){
			fwG[i] = new FileWriter("Nodes/nodes"+i);
			bfwG[i] = new BufferedWriter(fwG[i], bufSize);
		}
		
		while((line = bfrG.readLine()) != null){

			String[] strs = line.split("\t");
			payload = strs[0];
			id = Long.parseLong(strs[1]);
			rawPmerValue = Long.parseLong(strs[2]);

			binIndex = pmerToBinId.get(rawPmerValue);

			bfwG[binIndex].write(payload);
			bfwG[binIndex].write("\t" + id);
			bfwG[binIndex].newLine();
		}
		
		for(int i=0;i<numOfBlocks;i++){
			bfwG[i].close();
			fwG[i].close();
		}
		
		bfrG.close();
		frG.close();
		
		return id;
	}

	private int buildBinMapping() {
		LinkedList<Bin> bins = new LinkedList<>();
		long maxBinSize = Collections.max(pmerDistribution.values());
		bins.add(new Bin(maxBinSize));

		List<Entry<Long, Long>> pmerDistributionList = new ArrayList<>(pmerDistribution.entrySet());
		pmerDistributionList.sort(Entry.comparingByValue());
		Collections.reverse(pmerDistributionList);


		for (Entry<Long, Long> entry : pmerDistributionList) {
			long pmerValue = entry.getKey();
			long pmerCapacity = entry.getValue();
			boolean tryAccomodate = true;
			for (Bin bin : bins) {
				if (tryAccomodate && bin.TryAccomodate(pmerValue, pmerCapacity)) {
					tryAccomodate = false;
				}
			}
			if (tryAccomodate) {
				Bin newBin = new Bin(maxBinSize);
				newBin.TryAccomodate(pmerValue, pmerCapacity);
				bins.add(newBin);
			}

		}

		int binId = 0;
		for(Bin bin : bins) {
			Long[] pmersInBin = bin.getPmersInBin();
			for(Long pmerValue : pmersInBin){
				pmerToBinId.put(pmerValue, binId);
			}
			binId += 1;
		}
		return binId;
	}
	
	public long Run() throws Exception{
		
		long time1=0;
		
		long t1 = System.currentTimeMillis();
		System.out.println("Distribute Nodes Begin!");	
		long maxID = DistributeNodes();		
		long t2 = System.currentTimeMillis();
		time1 = (t2-t1)/1000;
		System.out.println("Time used for distributing nodes: " + time1 + " seconds!");	
		return maxID;
		
	}

	public int getNumOfBlocks(){
		return numOfBlocks;
	}
	
	
//    public static void main(String[] args){
//
//    	String infile = "E:\\test.txt";
//    	int k = 15, numBlocks = 256, pivot_len = 12, bufferSize = 8192, readLen = 101;
//
//    	if(args[0].equals("-help")){
//    		System.out.print("Usage: java -jar Partition.jar -in InputPath -k k -L readLength[options]\n" +
//	        			       "Options Available: \n" +
//	        			       "[-NB numOfBlocks] : (Integer) Number Of Kmer Blocks. Default: 256" + "\n" +
//	        			       "[-p pivotLength] : (Integer) Pivot Length. Default: 12" + "\n" +
//	        			       "[-b bufferSize] : (Integer) Read/Writer Buffer Size. Default: 8192" + "\n");
//    		return;
//    	}
//
//    	for(int i=0; i<args.length; i+=2){
//    		if(args[i].equals("-in"))
//    			infile = args[i+1];
//    		else if(args[i].equals("-k"))
//    			k = new Integer(args[i+1]);
//    		else if(args[i].equals("-NB"))
//    			numBlocks = new Integer(args[i+1]);
//    		else if(args[i].equals("-p"))
//    			pivot_len = new Integer(args[i+1]);
//    		else if(args[i].equals("-b"))
//    			bufferSize = new Integer(args[i+1]);
//    		else if(args[i].equals("-L"))
//    			readLen = new Integer(args[i+1]);
//    		else{
//    			System.out.println("Wrong with arguments. Abort!");
//    			return;
//    		}
//    	}
//
//
//		Partition bdgraph = new Partition(k, numBlocks, pivot_len, bufferSize, readLen);
//
//		try{
//
//			System.out.println("Program Configuration:");
//	    	System.out.print("Input File: " + infile + "\n" +
//	    					 "Kmer Length: " + k + "\n" +
//	    					 "Read Length: " + readLen + "\n" +
//	    					 "# Of Blocks: " + numBlocks + "\n" +
//	    					 "Pivot Length: " + pivot_len + "\n" +
//	    					 "R/W Buffer Size: " + bufferSize + "\n");
//
//			bdgraph.Run();
//
//		}
//		catch(Exception E){
//			System.out.println("Exception caught!");
//			E.printStackTrace();
//		}
//
//	}
}
	
	