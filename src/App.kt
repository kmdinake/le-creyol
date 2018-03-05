/* Imports */
import pso.*

fun main(args: Array<String>) {
	if (args.size < 2) {
		println("$ lecreyol -help\tFor help on using Le-Creyol.")
		exitProcess(0)
	}
	
	if (args[2] == "-help") {
		usage()
		exitProcess(0)
	}

	info()

	println("-Please enter the data filename (e.g. data1.csv)")
	print("> ")
	var filename: String = readline().toString()
	if (filename == "") {
		do {
			println("-Please provide a valid data filename (e.g. data1.csv)")
			print("> ")
			filename = readline().toString()
		} while (filename == "") 
	}

	var nrClusters: Int = 0
	if (args[2] == "-c") {
		do {
			println("-Please enter the number of cluster centroids usually greater than 1 (e.g. 3)")
			print("> ")
			nrClusters = readline().toInt()
		} while (nrClusters < 1)
	}

	val pso: MultiObjectivePSO = MultiObjectivePSO(nrClusters)
	pso.readDataFrom(filename)  // expecting some CSV file
	pso.swarmify()  // run the multi-objective pso to cluster the data. 
	pso.graphify()  // produce a pca or tnse reduced 2D or maybe 3D graph of cluster centroids, and data samples / particles. 
	pso.stringify()  // write results to file, such as the cluster centroids, the amount data samples per centroid, etc.
}

fun info() {
	println("---Welcome to Le-Creyol---")
	println("-MultiObjective PSO for Clustering Static Data")
	println("-Version: 0.1.0")
	println("-Developed by Keoagile M. Dinake, 2018")
}

fun usage() {
	println("---Guidelines to using Le-Creyol---")
	println("")
	println("$ ./lecreyol <option>")
	prinln("where <option> is: ")
	println("-c\tTurn off dynamic optimal number of cluster centroid determination")
	println("")
	println("---Description of Program---")
	println("")
	println("-The Le-Creyol Multi-Objective PSO Algorithm by default attempts to cluster the provided data set into an optimal number of cluster centroids, by applying a meta-heuristic approach to data clustering instead of a predefined number of clusters. However, the number of clusters can be specified, as the program prompts the user to enter an optional number of clusters to cluster towards.")
	println("-Once the algorithm has completed generating clusters, and grouping data samples according to those clusters, a graph displaying the results is shown to the user for inspection. Along with this graphical display of the results is a textual representation of the results. These results can be found in the file 'Le-Creyol-results.txt'.")
}
