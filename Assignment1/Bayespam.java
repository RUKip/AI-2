import java.io.*;
import java.lang.reflect.Array;
import java.util.*;
import java.util.regex.Pattern;

public class Bayespam
{
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

    
    private static final int tuningParameter = 1;
    private static double probRegular;
	private static double probSpam;
    private static int totalRegularWords = 0;
	private static int totalSpamWords = 0;
    
    
    // Add a word to the vocabulary
    private static void addWord(String word, MessageType type)
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

        listing_regular = dir_listing[0].listFiles();
        listing_spam    = dir_listing[1].listFiles();
    }

    
    // Print the current content of the vocabulary
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


    // Read the words from messages and add them to your vocabulary. The boolean type determines whether the messages are regular or not  
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
            
            while ((line = in.readLine()) != null)                      // read a line
            {

            	line = cleanLine(line);
            	
                StringTokenizer st = new StringTokenizer(line);         // parse it into words
       
            	
                while (st.hasMoreTokens())                  // while there are stille words left..
                {
                    addWord(st.nextToken(), type);                  // add them to the vocabulary
                }
            }

            in.close();
        }
    }
    
    
    /// Print the current content of the spam CCL
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
    
    ///New methods below
    private static String cleanLine(String line){
    	///Below line removes all non alphabet characters, using regex
    	line = " " + line.replaceAll("[^a-zA-Z\\s]", "") + " ";            	
    	///Below line removes all words that are now smaller then 4, using regex
    	line = line.replaceAll("(\\s+([a-zA-Z]{1,3}\\s+)+)+"," ");
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
             
             cclSpamValue = Math.log10(cclSpamValue);	///TODO: is this the point of 2.3? we take log 10
             cclRegularValue = Math.log10(cclRegularValue);
             
             cclRegular.put(word, cclRegularValue);
             cclSpam.put(word, cclSpamValue);
             
         }
    }
    
    //TODO: finish, after asking about details
    private static void classifyMessage(File f) throws IOException{ 
    	FileInputStream i_s = new FileInputStream(f);
         BufferedReader in = new BufferedReader(new InputStreamReader(i_s));
         String line;
         String word;
         String tag;
         
         double posteriRegular = probRegular;
         double posteriSpam = probSpam;
         
         while ((line = in.readLine()) != null)                      // read a line
         {

         	line = cleanLine(line);
         	
             StringTokenizer st = new StringTokenizer(line);         // parse it into words
    
         	
             while (st.hasMoreTokens())                  // while there are still words left..
             {
            	word = st.nextToken();
                posteriRegular += cclRegular.get(word);                  // add them to the vocabulary
                posteriSpam += cclSpam.get(word);
             }
         } 

         if(posteriRegular > posteriSpam){
        	 tag = "Regular";
         }else{
        	 tag = "Spam";
         }
         
         System.out.println("Our file is tagged: " + tag + " Where spamposteri = " + posteriSpam + " and Where regularposteri = " + posteriRegular);
         
         in.close();
    }
    
    
    /// List the regular and spam messages as one giant set of test messages to be classified
    private static void listTest(File dir_location)
    {
        // List all files in the directory passed
        File[] dir_listing = dir_location.listFiles();

        // Check that there are 2 subdirectories
        if ( dir_listing.length != 2 )
        {
            System.out.println( "- Error: specified directory does not contain two subdirectories.\n" );
            Runtime.getRuntime().exit(0);
        }
        
        File[] listing1 = dir_listing[0].listFiles();
        File[] listing2 = dir_listing[1].listFiles();
        
        
        int aLen = listing1.length;
        int bLen = listing2.length;

        File[] c = (File[]) Array.newInstance(listing1.getClass().getComponentType(), aLen+bLen);
        System.arraycopy(listing1, 0, c, 0, aLen);
        System.arraycopy(listing2, 0, c, aLen, bLen);
        
        listing_test = c;
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

        // Print out the hash table
        //printVocab();
        
        // Now all students must continue from here:
        //
        // 1) A priori class probabilities must be computed from the number of regular and spam messages
        // 2) The vocabulary must be clean: punctuation and digits must be removed, case insensitive
        // 3) Conditional probabilities must be computed for every word
        // 4) A priori probabilities must be computed for every word
        // 5) Zero probabilities must be replaced by a small estimated value
        // 6) Bayes rule must be applied on new messages, followed by argmax classification
        // 7) Errors must be computed on the test set (FAR = false accept rate (misses), FRR = false reject rate (false alarms))
        // 8) Improve the code and the performance (speed, accuracy)
        //
        // Use the same steps to create a class BigramBayespam which implements a classifier using a vocabulary consisting of bigrams
        
        ///Below takes the regular.length and spam length and calls probability
        calcPReg(listing_regular.length, listing_spam.length);
        calcPSpam(listing_regular.length, listing_spam.length);

        countAllWords();
        
        calcCCL();
        
        printCCLSpam();
        printCCLRegular();
        
        
        // Location of the directory (the path) taken from the cmd line (second arg)
        File dir_messages = new File( args[1] );
        
        // Check if the cmd line arg is a directory
        if ( !dir_messages.isDirectory() )
        {
            System.out.println( "- Error: cmd line arg not a directory.\n" );
            Runtime.getRuntime().exit(0);
        }

        listTest(dir_messages);
        
        ///TODO: loop all messages to the classifyMessage method
        
        classifyMessage(listing_test[43]);
        
    }
}
