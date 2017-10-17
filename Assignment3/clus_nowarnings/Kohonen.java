import java.util.*;

public class Kohonen extends ClusteringAlgorithm
{
	/// Nr of clients constant
	private static final int nrOfClients = 70;

	// Size of clustersmap
	private int n;

	// Number of epochs
	private int epochs;
	
	// Dimensionality of the vectors
	private int dim;
	
	// Threshold above which the corresponding html is prefetched
	private double prefetchThreshold;

	private double initialLearningRate; 
	private double learningRate;
	
	private int initialNeigbourSize;
	private int neigbourSize;
	
	// This class represents the clusters, it contains the prototype (the mean of all it's members)
	// and a memberlist with the ID's (Integer objects) of the datapoints that are member of that cluster.  
	private Cluster[][] clusters;

	// Vector which contains the train/test data
	private Vector<float[]> trainData;
	private Vector<float[]> testData;
	
	// Results of test()
	private double hitrate;
	private double accuracy;
	
	static class Cluster
	{
			float[] prototype;

			Set<Integer> currentMembers;

			public Cluster()
			{
				currentMembers = new HashSet<Integer>();
			}
	}
	
	public Kohonen(int n, int epochs, Vector<float[]> trainData, Vector<float[]> testData, int dim)
	{
		///Initializing parameters
		this.n = n;
		this.epochs = epochs;
		prefetchThreshold = 0.5;
		initialLearningRate = 0.8;
		learningRate = initialLearningRate;
		initialNeigbourSize = n/2;
		neigbourSize = initialNeigbourSize;
		this.trainData = trainData;
		this.testData = testData; 
		this.dim = dim;       
		
		Random rnd = new Random();

		// Here n*n new cluster are initialized
		clusters = new Cluster[n][n];
		for (int i = 0; i < n; i++)  {
			for (int i2 = 0; i2 < n; i2++) {
				clusters[i][i2] = new Cluster();
				clusters[i][i2].prototype = new float[dim];
				// Step 1: initialize map with random vectors (A good place to do this, is in the initialisation of the clusters)	
				float[] ourProto = clusters[i][i2].prototype;
				for(int j=0; j<dim; j++){
					ourProto[j] = rnd.nextFloat();
				}
			}
		}
	}

	
	public boolean train()
	{
		CustomProgressBar progressBar = new CustomProgressBar(this.epochs);
			
		int epoch;
		// Repeat 'epochs' times:
		for(epoch = 0; epoch<this.epochs; epoch++){
			// Step 2: Calculate the squareSize and the learningRate, these decrease lineary with the number of epochs.	
			double updateVariable = (1-((double)epoch/epochs));
			///η = 0.8(1−t/tmax)
			neigbourSize = (int) (initialNeigbourSize*updateVariable);
			///r = N/2(1−t/tmax)
			learningRate = initialLearningRate*updateVariable;
			///Empty the memberlist of the clusters for new epoch
			for(Cluster[] clusterRow : clusters){
				for(Cluster cluster : clusterRow) cluster.currentMembers.clear();
			}
			
			// Step 3: Every input vector is presented to the map (always in the same order)
			for(int client=0; client<nrOfClients; client++){ 
				double smallestDifference = dim*dim;
				int winnerClusterX = -1;
				int winnerClusterY = -1;
				float[] inputVector = trainData.get(client);		
				for(int i=0; i<n; i++){
					for(int j=0; j<n; j++){
						float[] unit = clusters[i][j].prototype;
						double difference = 0;
						for(int dimension=0; dimension<dim; dimension++){
							///Calculate Euclidean distance part 1
							difference += Math.pow(inputVector[dimension] - unit[dimension], 2);	
						}
						///Calculate Euclidean distance part 2
						difference = Math.sqrt(difference);
						// For each vector its Best Matching Unit is found, and :
						if(smallestDifference>difference){
							smallestDifference = difference;
							winnerClusterX = i; ///Our BMU X
							winnerClusterY = j; ///Our BMU Y
						}
					}
				}			
				if(winnerClusterX == -1 || winnerClusterY == -1){
					System.out.println("Something went wrong, with deciding winning cluster");
					System.exit(-1);
				}
				// Step 4: All nodes within the neighbourhood of the BMU are changed, you don't have to use distance relative learning.
				clusters[winnerClusterX][winnerClusterY].currentMembers.add(client);
				///Loop over neighbouring clusters
				for(int i=(winnerClusterX-neigbourSize); i<(winnerClusterX+neigbourSize+1); i++){
					///make sure you're not out of bounds
					if(i<n && i>=0){
						for(int j=(winnerClusterY-neigbourSize); j<(winnerClusterY+neigbourSize+1); j++){
							///make sure you're not out of bounds
							if(j<n && j>=0){
								float[] myBeautifulPrototype = clusters[i][j].prototype;
								for(int dimension=0; dimension<dim; dimension++) {
									///P_NEW = (1−η)∗P_OLD+η∗X
									myBeautifulPrototype[dimension] = (float) (((1-learningRate)*myBeautifulPrototype[dimension]) + (learningRate*inputVector[dimension]));
								}
							}
						}
					}	
				}
						
			}
			// Since training kohonen maps can take quite a while, presenting the user with a progress bar would be nice
			progressBar.setEpoch(epoch);
		}
		progressBar.setEpoch(epoch);
		progressBar.setVisible(false); ///you can't see me!
		progressBar.dispose();
		
		return true;
	}
	
	public boolean test()
	{
		int prefetchedHTMLCount = 0;
		int hits = 0;
		int requests = 0;		
		for(int i=0; i<n; i++){
			for(int j=0; j<n; j++){ ///Take each cluster
				Cluster cluster = clusters[i][j];
				Set<Integer> members = cluster.currentMembers;
				float[] proto = cluster.prototype;
				for(Integer member : members){
					float[] values = testData.get(member);
					for(int dimension=0; dimension<dim; dimension++){
						if(proto[dimension]>prefetchThreshold){ ///test if they pass the threshold, if so the should be prefetched
							prefetchedHTMLCount++;
							if(values[dimension]==1){ ///Test if they are requested, if so we have a hit
								hits++;
							}
						}
						if(values[dimension]==1){ ///Test if they are requested, we don't care here about prefetched or not
							requests++;
						}
					}
				}
			}
		}
		
		this.hitrate = (double) hits/requests;
		this.accuracy = (double) hits/prefetchedHTMLCount;
		return true;
	}


	public void showTest()
	{
		System.out.println("Initial learning Rate=" + initialLearningRate);
		System.out.println("Prefetch threshold=" + prefetchThreshold);
		System.out.println("Hitrate: " + hitrate);
		System.out.println("Accuracy: " + accuracy);
		System.out.println("Hitrate+Accuracy=" + (hitrate + accuracy));
	}
 
 
	public void showMembers()
	{
		for (int i = 0; i < n; i++)
			for (int i2 = 0; i2 < n; i2++)
				System.out.println("\nMembers cluster["+i+"]["+i2+"] :" + clusters[i][i2].currentMembers);
	}

	public void showPrototypes()
	{
		for (int i = 0; i < n; i++) {
			for (int i2 = 0; i2 < n; i2++) {
				System.out.print("\nPrototype cluster["+i+"]["+i2+"] :");
				
				for (int i3 = 0; i3 < dim; i3++)
					System.out.print(" " + clusters[i][i2].prototype[i3]);
				
				System.out.println();
			}
		}
	}

	public void setPrefetchThreshold(double prefetchThreshold)
	{
		this.prefetchThreshold = prefetchThreshold;
	}
}

