package buildgraph;

import java.io.*;
import java.util.*;

public class PrePartition {

	private int k;
	private String inputfile;
	private int numOfBlocks;
	private int pivotLen;
	private int bufSize;

	private FileReader frG;
	private BufferedReader bfrG;
	private FileWriter fwG;
	private BufferedWriter bfwG;

	private int readLen;

	private Hashtable<Long, Long> charsPerMSP;

	private static int[] valTable = new int[]{0,-1,1,-1,-1,-1,2,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,-1,3};
	private static char[] twinTable = new char[]{'T','0','G','0','0','0','C','0','0','0','0','0','0','0','0','0','0','0','0','A'};

	public PrePartition(int kk, String infile, int pivotLength, int bufferSize, int readLen){
		this.k = kk;
		this.inputfile = infile;
		this.numOfBlocks = 1;
		this.pivotLen = pivotLength;
		this.bufSize = bufferSize;
		this.readLen = readLen;
		this.charsPerMSP = new Hashtable<>();
	}
	
	private boolean isReadLegal(char[] line){
		int Len = line.length;
		for(int i=0; i<Len; i++){
			if(line[i]!='A' && line[i]!='C' && line[i]!='G' && line[i]!='T')
				return false;
		}
		return true;
	}
	
	private int strcmp(char[] a, char[] b, int froma, int fromb, int len){
		for(int i = 0; i < len; i++){
			if(a[froma+i] < b[fromb+i])
				return -1;
			else if(a[froma+i] > b[fromb+i])
				return 1;
		}
		return 0;
	}
	
	private int findSmallest(char[] a, int from, int to){
		
		int min_pos = from;
		
		for(int i=from+1; i<=to-pivotLen; i++){
			if(strcmp(a, a, min_pos, i, pivotLen)>0)
				min_pos = i;
		}
		
		return min_pos;
	}
	
	private int findPosOfMin(char[] a, char[] b, int from, int to, int[] flag){
		
		int len = a.length;
		int pos1 = findSmallest(a,from,to);
		int pos2 = findSmallest(b,len - to, len - from);
		
		if(strcmp(a,b,pos1,pos2,pivotLen)<0){
			flag[0] = 0;
			return pos1;
		}
		else{
			flag[0] = 1;
			return pos2;
		}	
	}
	
	private int calPosNew(char[] a, int from, int to){
		return (int)(calRawPosNew(a, from, to) % numOfBlocks);
	}

	private long calRawPosNew(char[] a, int from, int to){

		long val=0L;

		for(int i=from; i<to; i++){
			val = val<<2;
			val += valTable[a[i]-'A'];
		}

		return val;
	}


	
	private long createRawNodes() throws IOException{
		frG = new FileReader(inputfile);
		bfrG = new BufferedReader(frG, bufSize);

		File dir = new File("Nodes");
		if(!dir.exists())
			dir.mkdir();
		fwG = new FileWriter("Nodes/raw_nodes");
		bfwG = new BufferedWriter(fwG, bufSize);

		
		String describeline;
		
		int prepos, substart = 0, subend, min_pos = -1;
		
		char[] lineCharArray = new char[readLen];
		
		int[] flag = new int[1];
		
		long cnt = 0, outcnt = 0;
		

	



		while((describeline = bfrG.readLine()) != null){
			
			bfrG.read(lineCharArray, 0, readLen);
			bfrG.read();
			
			prepos = -1;
			if(isReadLegal(lineCharArray)){
				
				substart = 0;
				
				outcnt = cnt;
				
				int len = readLen;
				
				char[] revCharArray = new char[len];
				
				for(int i=0; i<len; i++){
					revCharArray[i] = twinTable[lineCharArray[len-1-i]-'A'];
				}
				
				min_pos = findPosOfMin(lineCharArray, revCharArray, 0, k, flag);
				
				cnt += 2;
				
				int bound = len - k + 1;
				
				for(int i = 1; i < bound; i++){
					
					if(i > (flag[0]==0?min_pos:len-min_pos-pivotLen)){
						
						int temp = (flag[0]==0 ? calPosNew(lineCharArray,min_pos,min_pos+pivotLen):calPosNew(revCharArray,min_pos,min_pos+pivotLen));
						long rawPmerValue = (flag[0]==0 ? calRawPosNew(lineCharArray,min_pos,min_pos+pivotLen):calRawPosNew(revCharArray,min_pos,min_pos+pivotLen));

						min_pos = findPosOfMin(lineCharArray, revCharArray, i, i+k, flag);
						
						if(rawPmerValue != (flag[0]==0 ? calRawPosNew(lineCharArray,min_pos,min_pos+pivotLen):calRawPosNew(revCharArray,min_pos,min_pos+pivotLen))){
							subend = i - 1 + k;

							writeToFile(substart, lineCharArray, outcnt, bfwG, subend - substart, rawPmerValue);
							updateCharsPerMSP(rawPmerValue, subend-substart);

							substart = i;
							outcnt = cnt;
						}
						
					}
					
					else{
						
						if(strcmp(lineCharArray, revCharArray, k + i - pivotLen, len - i - k, pivotLen)<0){
							if(strcmp(lineCharArray, flag[0]==0?lineCharArray:revCharArray, k + i - pivotLen, min_pos, pivotLen)<0){
								
								int temp = (flag[0]==0 ? calPosNew(lineCharArray,min_pos,min_pos+pivotLen):calPosNew(revCharArray,min_pos,min_pos+pivotLen));
								long rawPmerValue = (flag[0]==0 ? calRawPosNew(lineCharArray,min_pos,min_pos+pivotLen):calRawPosNew(revCharArray,min_pos,min_pos+pivotLen));
								min_pos = k + i - pivotLen;
								
								if(rawPmerValue != calRawPosNew(lineCharArray, min_pos, min_pos+pivotLen)){
									subend = i - 1 + k;

									writeToFile(substart, lineCharArray, outcnt, bfwG, subend-substart, rawPmerValue);
									updateCharsPerMSP(rawPmerValue, subend-substart);

									substart = i;
									outcnt = cnt;
								}
								
								flag[0]=0;
	
							}
						}
						else{
							if(strcmp(revCharArray, flag[0]==0?lineCharArray:revCharArray, len - i - k, min_pos, pivotLen)<0){
								
								int temp = (flag[0]==0 ? calPosNew(lineCharArray,min_pos,min_pos+pivotLen):calPosNew(revCharArray,min_pos,min_pos+pivotLen));
								long rawPmerValue = (flag[0]==0 ? calRawPosNew(lineCharArray,min_pos,min_pos+pivotLen):calRawPosNew(revCharArray,min_pos,min_pos+pivotLen));

								min_pos = -k - i + len;
								
								if(rawPmerValue != calRawPosNew(revCharArray, min_pos, min_pos+pivotLen)){
									subend = i - 1 + k;

									writeToFile(substart, lineCharArray, outcnt, bfwG, subend-substart, rawPmerValue);
									updateCharsPerMSP(rawPmerValue, subend-substart);

									substart = i;
									outcnt = cnt;
								}
								
								flag[0]=1;
								
								
							}
						}
					}
					
					cnt += 2;
				}
				subend = len;

				long rawPmerValue = (flag[0]==0 ? calRawPosNew(lineCharArray,min_pos,min_pos+pivotLen):calRawPosNew(revCharArray,min_pos,min_pos+pivotLen));
				writeToFile(substart, lineCharArray, outcnt, bfwG, subend-substart, rawPmerValue);
				updateCharsPerMSP(rawPmerValue, subend-substart);
			}
		}
		
		System.out.println("Largest ID is " + cnt);
		

		bfwG.close();
		fwG.close();

		bfrG.close();
		frG.close();
		
		return cnt;
	}

	private void writeToFile(int substart, char[] lineCharArray, long outcnt, BufferedWriter bufferedWriter, int writeLength, long rawPmerValue) throws IOException {
		bufferedWriter.write(lineCharArray, substart, writeLength);
		bufferedWriter.write("\t" + outcnt + "\t"+rawPmerValue);
		bufferedWriter.newLine();
	}

	private void updateCharsPerMSP(Long pmerValue, int numChars){
		if(!charsPerMSP.containsKey(pmerValue)){
			charsPerMSP.put(pmerValue, 0L);
		}
		charsPerMSP.put(pmerValue, charsPerMSP.get(pmerValue) + numChars);
	}

	public Hashtable<Long, Long> Run() throws Exception{
		
		long time1=0;
		
		long t1 = System.currentTimeMillis();
		System.out.println("Distribute Nodes Begin!");	
		long maxID = createRawNodes();
		long t2 = System.currentTimeMillis();
		time1 = (t2-t1)/1000;
		System.out.println("Time used for distributing nodes: " + time1 + " seconds!");
		return new Hashtable<>(charsPerMSP);

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
//		PrePartition bdgraph = new PrePartition(k, infile, numBlocks, pivot_len, bufferSize, readLen);
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
	
	