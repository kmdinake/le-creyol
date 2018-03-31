/* Imports */
import kotlin.system.exitProcess
import pso.*
import java.io.FileNotFoundException
import java.io.IOException
import kotlin.math.abs

data class OptimalClusterSize(val clusterId: Int) {
    var silhouetteScore: Double = 0.0
    var centroids: MutableList<DoubleArray> = mutableListOf()
    var averageSamplesPerCluster = mutableMapOf<Int, Int>()
}

fun main(args: Array<String>) {
   try {
        if (args.isEmpty()) {
            println("$ lecreyol -help\tFor help on using Le-Creyol.")
            exitProcess(0)
        }

        if (args[0] == "-help") {
            usage()
            exitProcess(0)
        }

        info()

        println("-Please enter the data filename (e.g. data1.csv)")
        print("> ")
        var filename: String = readLine().toString()
        if (filename == "") {
            do {
                println("-Please provide a valid data filename (e.g. data1.csv)")
                print("> ")
                filename = readLine().toString()
            } while (filename == "")
        }

        var nrClusters = 0
        if (args.size == 1 && args[0] == "-c") {
            do {
                println("-Please enter the number of cluster centroids usually greater than 1 (e.g. 3)")
                print("> ")
                try {
                    nrClusters = readLine().toString().toInt()
                } catch (e: NumberFormatException) {
                    println("-Invalid input, ensure that the number of cluster centroids is a discrete number greater than 1.")
                }
            } while (nrClusters < 1)
            val pso = MultiObjectivePSO(nrClusters)
            pso.readDataFrom(filename)  // expecting some CSV file
            val averageSamplesPerCluster = mutableMapOf<Int, Int>()
            (0 until nrClusters).forEach {
                averageSamplesPerCluster[it] = 0
            }
            (0 until 50).forEach {
                println("RUN $it")
                pso.swarmify()  // run the multi-objective pso to cluster the data.
                //pso.graphify()  // produce a pca or tnse reduced 2D or maybe 3D graph of cluster centroids, and data samples / particles.
                pso.stringify()  // write results to file, such as the cluster centroids, the amount data samples per centroid, etc.
                (0 until nrClusters).forEach {
                    averageSamplesPerCluster[it] = averageSamplesPerCluster[it]!! + pso.samplesPerCluster[it]!!
                }
            }
            println("After 50 Independent Runs the Average number of Samples Per Cluster is as follows:")
            (0 until nrClusters).forEach {
                println("Cluster $it has ${averageSamplesPerCluster[it]!! / 50} samples")
            }
        } else {
            val nrClustersRange = listOf<Int>(2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15)
            val optimals: MutableList<OptimalClusterSize> = mutableListOf()
            for (i in nrClustersRange.indices) {
                println("Trying ${nrClustersRange[i]} clusters")
                // Initialize Possible Optimal Cluster Size
                optimals.add(OptimalClusterSize(nrClustersRange[i]))
                optimals[i].silhouetteScore = 0.0
                (0 until nrClustersRange[i]).forEach {
                    optimals[i].averageSamplesPerCluster[it] = 0
                }
                // Initialize MOPSO
                nrClusters = nrClustersRange[i]
                val pso = MultiObjectivePSO(nrClusters)
                pso.readDataFrom(filename)
                // Run 50 Independent MOPSO Cycles
                (0 until 50).forEach {
                    println("RUN $it")
                    pso.swarmify()
                    //pso.graphify()
                    pso.stringify()
                    (0 until nrClusters).forEach {
                        optimals[i].averageSamplesPerCluster[it] = optimals[i].averageSamplesPerCluster[it]!! + pso.samplesPerCluster[it]!!
                    }
                }
                optimals[i].centroids = pso.optimalCentroids
                optimals[i].silhouetteScore = pso.calcSilhouetteScore()
            }
            val silhouetteDistances = DoubleArray(optimals.size - 1)
            (0 until (optimals.size - 1)).forEach { i ->
                silhouetteDistances[i] = abs(optimals[i].silhouetteScore - optimals[i + 1].silhouetteScore)
            }
            var highest = Double.MIN_VALUE
            var hIndex = -1
            (0 until silhouetteDistances.size -1 ).forEach { i ->
                val dist = abs(silhouetteDistances[i] - silhouetteDistances[i + 1])
                if (dist > highest) {
                    highest = dist
                    hIndex = i
                }
            }
            println("The optimal number of clusters is ${optimals[hIndex].clusterId}")
            println("After 50 Independent Runs the Silhouette Score for using ${optimals[hIndex].clusterId} clusters is: ${optimals[hIndex].silhouetteScore}")
            println("After 50 Independent Runs the Average number of Samples Per Cluster is as follows:")
            (0 until optimals[hIndex].centroids.size).forEach {
                println("Cluster $it has ${optimals[hIndex].averageSamplesPerCluster[it]!! / 50} samples")
            }
        }
    } catch (e: FileNotFoundException) {
        println("ERROR => File ${e.message}")
    } catch (e: IOException) {
        println("ERROR => IO ${e.message}")
    }
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
	println("where <option> is: ")
    println("-c -1\tTurn ON dynamic optimal number of cluster centroid determination")
	println("-c\tTurn OFF dynamic optimal number of cluster centroid determination")
	println("")
	println("---Description of Program---")
	println("")
	println("-The Le-Creyol Multi-Objective PSO Algorithm by default attempts to cluster the provided data set into an optimal number of cluster centroids, by applying a meta-heuristic approach to data clustering instead of a predefined number of clusters. However, the number of clusters can be specified, as the program prompts the user to enter an optional number of clusters to cluster towards.")
	println("-Once the algorithm has completed generating clusters, and grouping data samples according to those clusters, a graph displaying the results is shown to the user for inspection. Along with this graphical display of the results is a textual representation of the results. These results can be found in the file 'Le-Creyol-results.txt'.")
}
