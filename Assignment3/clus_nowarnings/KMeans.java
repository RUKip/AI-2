import java.util.*;
import java.util.Random;

public class KMeans extends ClusteringAlgorithm
{
	
	private static final int nrOfClients = 70;
	
	// Number of clusters
	private int k;

	// Dimensionality of the vectors
	private int dim;
	
	// Threshold above which the corresponding html is prefetched
	private double prefetchThreshold;
	
	// Array of k clusters, class cluster is used for easy bookkeeping
	private Cluster[] clusters;
	
	// This class represents the clusters, it contains the prototype (the mean of all it's members)
	// and memberlists with the ID's (which are Integer objects) of the datapoints that are member of that cluster.
	// You also want to remember the previous members so you can check if the clusters are stable.
	static class Cluster
	{
		float[] prototype;

		Set<Integer> currentMembers;
		Set<Integer> previousMembers;
		  
		public Cluster()
		{
			currentMembers = new HashSet<Integer>();
			previousMembers = new HashSet<Integer>();
		}
	}
	// These vectors contains the feature vectors you need; the feature vectors are float arrays.
	// Remember that you have to cast them first, since vectors return objects.
	private Vector<float[]> trainData;
	private Vector<float[]> testData;

	// Results of test()
	private double hitrate;
	private double accuracy;
	
	public KMeans(int k, Vector<float[]> trainData, Vector<float[]> testData, int dim)
	{
		this.k = k;
		this.trainData = trainData;
		this.testData = testData; 
		this.dim = dim;
		prefetchThreshold = 0.5;
		
		// Here k new cluster are initialized
		clusters = new Cluster[k];
		for (int ic = 0; ic < k; ic++) clusters[ic] = new Cluster();
	}

	///The cluster prototypes are recalculated based on the mean of all the values of the members of that cluster
	private void calcPrototypes() {
		for (int i=0; i<k; i++) {
			Cluster cluster = clusters[i];
			cluster.prototype = new float[dim];
			for(int j=0; j<dim;j++) {
				float value = 0;
				for(Integer client : cluster.currentMembers){
					float[] clientVector = trainData.get(client);
					value += clientVector[j];
				}
				cluster.prototype[j] = (float) value/cluster.currentMembers.size();
			}
		}
	}
	
	///small check to see if our member set has changed or that we can stop
	private boolean checkStable(){
		for(int i=0; i<k; i++){
			Cluster cluster = clusters[i];
			if (!cluster.currentMembers.equals(cluster.previousMembers)) return false;	
		}
		return true;
	}
	
	
	private void reassignClusters(){
		double smallestDifference;
		int winnerCluster;
		///Here we reset the clusters current members and prevous, and store the new previous members
		for(Cluster cluster : clusters){
			cluster.previousMembers.clear();
			for(Integer client : cluster.currentMembers) cluster.previousMembers.add(client);
			cluster.currentMembers.clear();
		}
		///Below we calculate the winners
		for(int i=0; i<nrOfClients; i++){
			smallestDifference = dim*dim;
			winnerCluster = -1;
			float[] datapoint = trainData.get(i);
			for(int j=0; j<k; j++){
				Cluster cluster = clusters[j];
				double difference = 0;
				///Here we decide winner cluster based on euclidan distance
				for(int dimension=0; dimension<dim; dimension++){
					difference += Math.pow(datapoint[dimension] - cluster.prototype[dimension], 2);	
				}
				difference = Math.sqrt(difference);
				if(smallestDifference>difference){
					smallestDifference = difference;
					winnerCluster = j;
				}
			}
			if(winnerCluster == -1){
				System.out.println("Something went wrong, with deciding winning cluster");
				System.exit(-1);
			}
			///new members are added
			clusters[winnerCluster].currentMembers.add(i);
		}
	}

	public boolean train()
	{
		Random random = new Random();
		// Step 1: Select an initial random partioning with k clusters
		for(int i=0; i<nrOfClients; i++){
			int classNr = random.nextInt(this.k);
			clusters[classNr].currentMembers.add(i);
		}
		
		calcPrototypes();
		
		// Step 2: Generate a new partition by assigning each datapoint to its closest cluster center
		reassignClusters();
		
		// Step 3: recalculate cluster centers
		calcPrototypes();
		
		// Step 4: repeat until clustermembership stabilizes
		while(!checkStable()){
			reassignClusters();
			calcPrototypes();
			//showMembers(); for testing purposes
		}
		return false;
	}

	public boolean test()
	{
		int prefetchedHTMLCount = 0;
		int hits = 0;
		int requests = 0;		
		for(int j=0; j<k; j++){ ///Take each cluster
			Cluster cluster = clusters[j];
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
		
		this.hitrate = (double) hits/requests;
		this.accuracy = (double) hits/prefetchedHTMLCount;
		return true;
	}


	// The following members are called by RunClustering, in order to present information to the user
	public void showTest()
	{
		System.out.println("Prefetch threshold=" + this.prefetchThreshold);
		System.out.println("Hitrate: " + this.hitrate);
		System.out.println("Accuracy: " + this.accuracy);
		System.out.println("Hitrate+Accuracy=" + (this.hitrate + this.accuracy));
	}
	
	public void showMembers()
	{
		for (int i = 0; i < k; i++)
			System.out.println("\nMembers cluster["+i+"] :" + clusters[i].currentMembers);
	}
	
	public void showPrototypes()
	{
		for (int ic = 0; ic < k; ic++) {
			System.out.print("\nPrototype cluster["+ic+"] :");
			
			for (int ip = 0; ip < dim; ip++)
				System.out.print(clusters[ic].prototype[ip] + " ");
			
			System.out.println();
		 }
	}

	// With this function you can set the prefetch threshold.
	public void setPrefetchThreshold(double prefetchThreshold)
	{
		this.prefetchThreshold = prefetchThreshold;
	}
}
