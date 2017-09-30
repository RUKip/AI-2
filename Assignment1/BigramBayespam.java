import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Pattern;

public class BigramBayespam {

	    // This defines the two types of messages we have.
	    static enum MessageType
	    {
	        NORMAL, SPAM
	    }

	    // This a class with two counters (for regular and for spam)
	    static class Multiple_Counter
	    {
	        int counter_spam    = 0;
	        int counter_regular = 0;

	        // Increase one of the counters by one
	        public void incrementCounter(MessageType type)
	        {
	            if ( type == MessageType.NORMAL ){
	                ++counter_regular;
	            } else {
	                ++counter_spam;
	            }
	        }
	    }

	    // Listings of the two subdirectories (regular/ and spam/)
	    private static File[] listing_regular = new File[0];
	    private static File[] listing_spam = new File[0];
	    private static File[] listing_test = new File[0];
	    
	    // A hash table for the vocabulary (word searching is very fast in a hash table)
	    private static Hashtable <String, Multiple_Counter> vocab = new Hashtable <String, Multiple_Counter> ();
	    private static Hashtable <String, Double> cclRegular = new Hashtable <String, Double> ();
	    private static Hashtable <String, Double> cclSpam = new Hashtable <String, Double> ();

	    ///These are our constant values
	    private static final int sizeOfRegularFolder = 28;	///We know this is the regular/spam folder based on size(this is the max size of regular)
	    private static final double tuningParameter = 0.02;
	    private static final int minimalWordSize = 3;
	    private static final int minimumCount = 4;
	    
	    private static double probRegular;
		private static double probSpam;
	    private static int totalRegularWords = 0;
		private static int totalSpamWords = 0;
	    
		private static int truePositive = 0;	///Regular and classified regular
		private static int falsePositive = 0;	///Spam and classified regular
		private static int trueNegative = 0;	///Spam and classified spam
		private static int falseNegative = 0;	///Regular and classified spam
		
		///Filters words from the vocab if they occur below the minimum count
		private static void filterBigramCount(){

			Multiple_Counter counter = new Multiple_Counter();
			int totalCount;
			
			for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
		        {   
		            String word;
		            
		            word = e.nextElement();
		            counter  = vocab.get(word);
		          
		            totalCount = counter.counter_regular + counter.counter_spam;
		            if(totalCount < minimumCount) vocab.remove(word);
		        }
		}
		
	    // Add a word to the vocabulary
	    private static void addBigram(String word, MessageType type)
	    {
	        Multiple_Counter counter = new Multiple_Counter();

	        if ( vocab.containsKey(word) ){                  // if word exists already in the vocabulary..
	            counter = vocab.get(word);                  // get the counter from the hashtable
	        }
	        counter.incrementCounter(type);                 // increase the counter appropriately

	        vocab.put(word, counter);                       // put the word with its counter into the hashtable
	    }

	    // List the regular and spam messages
	    private static void listDirs(File dir_location)
	    {
	        // List all files in the directory passed
	        File[] dir_listing = dir_location.listFiles();

	        // Check that there are 2 subdirectories
	        if ( dir_listing.length != 2 )
	        {
	            System.out.println( "- Error: specified directory does not contain two subdirectories.\n" );
	            Runtime.getRuntime().exit(0);
	        }

	        if(dir_listing[0].listFiles().length<sizeOfRegularFolder){
	        	listing_regular = dir_listing[0].listFiles();
	        	listing_spam    = dir_listing[1].listFiles();
	        }else{
	        	listing_spam = dir_listing[0].listFiles();
	        	listing_regular    = dir_listing[1].listFiles();
	        }
	    }

	    
	    // Print the current content of the vocabulary (debug)
	    private static void printVocab()
	    {
	        Multiple_Counter counter = new Multiple_Counter();

	        for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
	        {   
	            String word;
	            
	            word = e.nextElement();
	            counter  = vocab.get(word);
	            
	            System.out.println( word + " | in regular: " + counter.counter_regular + 
	                                " in spam: "    + counter.counter_spam);
	        }
	    }


	    ///Read the words from messages and add them to your vocabulary, however now also remember the read word so we can create bigrams.
	    //The boolean type determines whether the messages are regular or not  
	    private static void readMessages(MessageType type)
	    throws IOException
	    {
	        File[] messages = new File[0];

	        if (type == MessageType.NORMAL){
	            messages = listing_regular;
	        } else {
	            messages = listing_spam;
	        }
	        
	        for (int i = 0; i < messages.length; ++i)
	        {
	            FileInputStream i_s = new FileInputStream( messages[i] );
	            BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
	            String line;
	            String word;
	            String lastWord = "";
	            String currentWord;

	            
	            while ((line = in.readLine()) != null)                      // read a line
	            {

	            	line = cleanLine(line);
	            	
	            	
	                StringTokenizer st = new StringTokenizer(line);         // parse it into words
	       
	                while (st.hasMoreTokens())                  // while there are stille words left..
	                {
	                	currentWord = st.nextToken();				
	                    if(!lastWord.equals("")) addBigram(lastWord + " " + currentWord, type);                  // add them to the vocabulary
	                    lastWord = currentWord;
	                }
	                lastWord = "";
	         
	            }

	            in.close();
	        }
	    }
	    
	   
		/// Print the current content of the spam CCL (debug)
	    private static void printCCLSpam()
	    {
	        Double counter;

	        for (Enumeration<String> e = cclSpam.keys() ; e.hasMoreElements() ;)
	        {   
	            String word;
	            
	            word = e.nextElement();
	            counter  = cclSpam.get(word);
	            
	            System.out.println( word + " | CCL for spam : " + counter );
	        }
	    }

		/// Print the current content of the regular CCL (debug)
	    private static void printCCLRegular()
	    {
	        Double counter;

	        for (Enumeration<String> e = cclRegular.keys() ; e.hasMoreElements() ;)
	        {   
	            String word;
	            
	            word = e.nextElement();
	            counter  = cclRegular.get(word);
	            
	            System.out.println( word + " | CCL for Regular : " + counter );
	        }
	    }
	    
	    private static String cleanLine(String line){
	    	///Below line removes all non alphabet characters, using regex
	    	line = " " + line.replaceAll("[^a-zA-Z\\s]", "") + " ";            	
	    	///Below line removes all words that are now smaller then 4, using regex
	    	line = line.replaceAll("(\\s+([a-zA-Z]{0,"+ minimalWordSize +"}\\s+)+)+"," ");
	    	///Below line converts remaining string to lowercase
	    	line = line.toLowerCase();
	    	return line;
	    }
	    
	    private static void calcPReg(int regular, int spam){
	    	int totalNr = regular + spam;
	    	probRegular = (double) regular/totalNr;
	    }
	    
	    private static void calcPSpam(int regular, int spam){
	    	int totalNr = regular + spam;
	    	probSpam = (double) spam/totalNr;
	    }
	    
	    private static void countAllWords(){
	    	 Multiple_Counter counter = new Multiple_Counter();
	    	 
	         for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
	         {   
	             String word;
	             
	             word = e.nextElement();
	             counter  = vocab.get(word);
	             
	             totalRegularWords += counter.counter_regular;
	             totalSpamWords += counter.counter_spam;
	         }
	    }
	    
	    private static void calcCCL(){
	    	Multiple_Counter counter = new Multiple_Counter();
	    	 int regular, spam;
	         Double cclRegularValue, cclSpamValue;
	    	 for (Enumeration<String> e = vocab.keys() ; e.hasMoreElements() ;)
	         {   
	             String word;
	             
	             word = e.nextElement();
	             counter  = vocab.get(word);
	             
	             regular = counter.counter_regular;
	             spam = counter.counter_spam;
	             
	             if(regular != 0){
	            	cclRegularValue = (double) regular/totalRegularWords;
	             }else{
	            	cclRegularValue = (double) tuningParameter/totalRegularWords;
	             }
	             if(spam != 0){
	            	 cclSpamValue = (double) spam/totalSpamWords;
	             }else{
	            	 cclSpamValue = (double) tuningParameter/totalSpamWords;
	             }	
	             
	             cclSpamValue = Math.log10(cclSpamValue);
	             cclRegularValue = Math.log10(cclRegularValue);
	            
	             cclRegular.put(word, cclRegularValue);
	             cclSpam.put(word, cclSpamValue);
	             
	         }
	    }
	    
	    private static void classifyMessage(File f, MessageType type) throws IOException{ 
	    	FileInputStream i_s = new FileInputStream(f);
	         BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
	         String line;
	         String word, lastWord = "";
	         MessageType tag;
	         
	         ///Posteri of regular and spam are initialized with the probabilities
	         double posteriRegular = probRegular;	
	         double posteriSpam = probSpam;
	         
	         while ((line = in.readLine()) != null)                      // read a line
	         {

	         	line = cleanLine(line);
	         	
	             StringTokenizer st = new StringTokenizer(line);         // parse it into words
	             while (st.hasMoreTokens())                  // while there are still words left..
	             {
	            	word = st.nextToken();
	            	if(!lastWord.equals("")){
		            	if(cclRegular.get(lastWord + " " + word) != null){
		            		 posteriRegular += cclRegular.get(lastWord + " " + word); 
		            	 }
		            	 if(cclSpam.get(lastWord + " " + word) != null){
		            		 posteriSpam += cclSpam.get(lastWord + " " + word);
		            	 }
		             }
	            	lastWord = word;
	             }
	             lastWord = "";
	         } 

	         if(posteriRegular > posteriSpam){
	        	 tag = MessageType.NORMAL;
	        	 if(type.equals(tag)){
	        		 truePositive++;
	        	 }else{
	        		 falsePositive++;
	        	 }
	         }else{
	        	 tag = MessageType.SPAM;
	        	 if(type.equals(tag)){
	        		 trueNegative++;
	        	 }else{
	        		 falseNegative++;
	        	 }
	         }	         
	         in.close();
	    }
	    
	    
	    /// List the regular and spam messages as one giant set of test messages to be classified
	    private static void listTest(File dir_location) throws IOException
	    {
	        // List all files in the directory passed
	        File[] dir_listing = dir_location.listFiles();

	        // Check that there are 2 subdirectories
	        if ( dir_listing.length != 2 )
	        {
	            System.out.println( "- Error: specified directory does not contain two subdirectories.\n" );
	            Runtime.getRuntime().exit(0);
	        }
	        
	        File[] regularListing = dir_listing[0].listFiles(); 	///We know this is the regular folder
	        File[] spamListing = dir_listing[1].listFiles();	///We know this is the spam folder
	        
	        for(File listing : regularListing){
	        	classifyMessage(listing, MessageType.NORMAL);
	        }        
	      
	        for(File listing : spamListing){
	        	classifyMessage(listing, MessageType.SPAM);
	        }        
	    }


	   
	    public static void main(String[] args)
	    throws IOException
	    {
	        // Location of the directory (the path) taken from the cmd line (first arg)
	        File dir_location = new File( args[0] );
	        
	        // Check if the cmd line arg is a directory
	        if ( !dir_location.isDirectory() )
	        {
	            System.out.println( "- Error: cmd line arg not a directory.\n" );
	            Runtime.getRuntime().exit(0);
	        }

	        // Initialize the regular and spam lists
	        listDirs(dir_location);

	        // Read the e-mail messages
	        readMessages(MessageType.NORMAL);
	        readMessages(MessageType.SPAM);
	        
	        ///At this point we have read the bigrams, now we filter out the words that don't occur enough.
	        filterBigramCount();
	        
	        ///Below 4 method calls are the same as for bayespam, essential steps for calculating CCL and probablity
	        calcPReg(listing_regular.length, listing_spam.length);
	        calcPSpam(listing_regular.length, listing_spam.length);
	        
	        countAllWords();
	        
	        calcCCL();
	
	        // Location of the directory (the path) taken from the cmd line (second arg)
	        File dir_messages = new File( args[1] );
	        
	        // Check if the cmd line arg is a directory
	        if ( !dir_messages.isDirectory() )
	        {
	            System.out.println( "- Error: cmd line arg not a directory.\n" );
	            Runtime.getRuntime().exit(0);
	        }

	        ///Tests all files and creates the confusion matrix
	        listTest(dir_messages); 
	        
	        ///output the confusion matrix
	        System.out.println("True Positvie count: " + truePositive);
	        System.out.println("False Postive count: " + falsePositive);
	        System.out.println("True Negative count: " + trueNegative);
	        System.out.println("False Negative count: " + falseNegative);
	     
	        
	        int total = truePositive + falseNegative + trueNegative + falsePositive;
	        double incorrectClassified = (double) (falsePositive+falseNegative)/total;
	        double correctClassified = (double) (truePositive+trueNegative)/total;
	        double correctClassifiedSpam = (double) (trueNegative)/(trueNegative+falsePositive);
	        double correctClassifiedRegular = (double) (truePositive)/(truePositive+falseNegative);
	        System.out.println("Total Correct classified: " + correctClassified*100 + "%");
	        System.out.println("Total Incorrect classified: " + incorrectClassified*100 + "%");
	        System.out.println("Correct classified spam: " + correctClassifiedSpam*100 + "%");
	        System.out.println("Correct classified regular: " + correctClassifiedRegular*100 + "%");
	    }
}
