package buildgraph;

import java.io.*;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CountDownLatch;


public class Map{
	
	private int k;
	private int numOfBlocks;
	private int bufSize;
	
	private Object lock_blocks = new Object();
	
	private int capacity;
	
	private int blockID;
	
	private long forAndVal;
	private long forAndVal32;
	private StringUtils stringUtils;

	private static int[] valTable = StringUtils.valTable;

	public Map(int kk, int numberOfBlocks, int bufferSize, int HScapacity){
		this.k = kk;
		this.numOfBlocks = numberOfBlocks;
		this.bufSize = bufferSize;
		this.capacity = HScapacity;
		this.blockID = 0;
		this.forAndVal = (long)Math.pow(2, 2*(k-32)) - 1;
		this.forAndVal32 = (long)Math.pow(2, 2*k) - 1;
		stringUtils = new StringUtils();

	}
	
	public class MyThread extends Thread{
		private CountDownLatch threadsSignal;
		private HashSet<String> fileNames;
		private ConcurrentHashMap<Long, Long> distinctKmersPerPartition;

		public MyThread(CountDownLatch threadsSignal, HashSet<String> fileNames, ConcurrentHashMap<Long, Long> distinctKmersPerPartition){
			super();
			this.threadsSignal = threadsSignal;
			this.fileNames = fileNames;
			this.distinctKmersPerPartition = distinctKmersPerPartition;
		}
		
		@Override
		public void run(){
			System.out.println(Thread.currentThread().getName() + "Start..."); 
			
			FileReader fr;
			BufferedReader bfr;

			String line;
			
			int p,j;
			
			try{
				File dir = new File("Maps");
				if(!dir.exists())
					dir.mkdir();
				
				while(blockID<numOfBlocks){
					
					synchronized (lock_blocks){
						p = blockID;
						blockID++;
					}

					String filename = "nodes" + p;
					if(!fileNames.contains(filename)){
						continue;
					}

								
					fr = new FileReader("Nodes/nodes"+p);
					bfr = new BufferedReader(fr, bufSize);

					
					HashSet<String> nodes = new HashSet<String>(capacity);
					
					while((line = bfr.readLine()) != null){
						
						String[] strs = line.split("\t");


						char[] lineCharArray = strs[0].toCharArray();
						String revLine = new String(stringUtils.getReversedRead(lineCharArray));

						int bound = strs[0].length() - k + 1;
						
						for(j = 0; j < bound; j++){
							String reg = strs[0].substring(j, j + k);
							String rev = new String(stringUtils.getReversedRead(reg.toCharArray()));
							if(reg.equals(rev))
							{
								nodes.add(rev);
							}
							else{
								boolean didAdd = false;
								for (int i = 0; i < k; i++) {
									if(rev.charAt(i) < reg.charAt(i))
									{
										nodes.add(rev);
										didAdd = true;
										break;
									}
									else if(reg.charAt(i) < rev.charAt(i))
									{
										nodes.add(rev);
										didAdd = true;
										break;
									}
								}
								if(!didAdd)
									nodes.add(reg);
							}
						}

					}

					if(p%100 == 0) System.out.println(p);

					distinctKmersPerPartition.put((long)p, (long)nodes.size());

					nodes.clear();
					nodes = null;
					

					bfr.close();
					fr.close();
					bfr = null;
					fr = null;

					File myObj = new File("Nodes/nodes"+p);
					if (!myObj.delete())
						System.out.println("Failed to delete the file." + p);
				}
							
			}catch(Exception E){
				System.out.println("Exception caught!");
				E.printStackTrace();
			}
			
			threadsSignal.countDown();  
			System.out.println(Thread.currentThread().getName() + "End. Remaining" + threadsSignal.getCount() + " threads");  
			
		}
	}
	
	
	private AbstractMap<Long, Long> BuildMap(int threadNum, HashSet<String> fileNames) throws Exception{
		CountDownLatch threadSignal = new CountDownLatch(threadNum);

		ConcurrentHashMap<Long, Long> distinctKmersPerPartition = new ConcurrentHashMap<>();
		
		for(int i=0;i<threadNum;i++){
			Thread t = new MyThread(threadSignal, fileNames, distinctKmersPerPartition);
			t.start();
		}
		threadSignal.await();
		System.out.println(Thread.currentThread().getName() + "End.");
		return distinctKmersPerPartition;
	}
	
	public AbstractMap<Long, Long> Run(int numThreads) throws Exception{
		long time1=0;

		HashSet<String> fileNames =  getNodesFileNames();
		
		long t1 = System.currentTimeMillis();
		System.out.println("Build Maps Begin!");	
		AbstractMap<Long, Long> distinctKmersPerPartition= BuildMap(numThreads, fileNames);
		long t2 = System.currentTimeMillis();
		time1 = (t2-t1)/1000;
		System.out.println("Time used for building maps: " + time1 + " seconds!");

		return distinctKmersPerPartition;
		
	}

	private HashSet<String> getNodesFileNames(){
		File[] files = (new File("./Nodes")).listFiles();
		List<String> fileNames = new LinkedList<>();
		for(File file : files){
			if(file.isFile()){
				fileNames.add(file.getName());
			}
		}
		return new HashSet<>(fileNames);
	}
	
	public static void main(String[] args){
    	
    	int k = 15, numBlocks = 256, numThreads = 1, bufferSize = 8192, hsmapCapacity = 1000000;
    	
    	if(args[0].equals("-help")){
    		System.out.print("Usage: java -jar Map.jar -k k -NB numOfBlocks [options]\n" +
	        			       "Options Available: \n" + 
	        			       "[-t numOfThreads] : (Integer) Number Of Threads. Default: 1" + "\n" +
	        			       "[-b bufferSize] : (Integer) Read/Writer Buffer Size. Default: 8192" + "\n" +
	        			       "[-c capacity] : (Integer) Hashmap Capacity. Default: 1000000" + "\n");
    		return;
    	}
    	
    	for(int i=0; i<args.length; i+=2){
    		if(args[i].equals("-k"))
    			k = new Integer(args[i+1]);
    		else if(args[i].equals("-NB"))
    			numBlocks = new Integer(args[i+1]);
    		else if(args[i].equals("-t"))
    			numThreads = new Integer(args[i+1]);
    		else if(args[i].equals("-b"))
    			bufferSize = new Integer(args[i+1]);
    		else if(args[i].equals("-c"))
    			hsmapCapacity = new Integer(args[i+1]);
    		else{
    			System.out.println("Wrong with arguments. Abort!");
    			return;
    		}
    	}
    	
		
		Map bdgraph = new Map(k, numBlocks, bufferSize, hsmapCapacity);
	
		try{
			System.out.println("Program Configuration:");
	    	System.out.print("Kmer Length: " + k + "\n" +
	    					 "# Of Blocks: " + numBlocks + "\n" +
	    					 "# Of Threads: " + numThreads + "\n" +
	    					 "R/W Buffer Size: " + bufferSize + "\n" +
	    					 "Hashmap Capacity: " + hsmapCapacity + "\n");
		
			bdgraph.Run(numThreads);	
			
		}
		catch(Exception E){
			System.out.println("Exception caught!");
			E.printStackTrace();
		}
		
	}	

}