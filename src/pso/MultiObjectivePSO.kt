package pso
import java.io.File
import java.util.Random
import kotlin.math.pow

class MultiObjectivePSO (private val nrClusters: Int) {
    /* Constants */
    private val nrEpochs: Int = 100
    private val w: Double = 0.729844
    private val c1: Double = 1.49618
    private val c2: Double = 1.49618
    private val nrParticles: Int = 30
    private val nrObjectives: Int = 2
    
    /* Mutables */
    private var nrDimensions: Int = 0
    private var minimizeSwarm: Swarm = Swarm(this.nrObjectives, this.nrDimensions)
    private var maximizeSwarm: Swarm = Swarm(this.nrObjectives, this.nrDimensions)
    private var r1: DoubleArray = DoubleArray(0)
    private var r2: DoubleArray = DoubleArray(0)
    private var samples: MutableList<DoubleArray> = mutableListOf()

    /* Methods */
    fun readDataFrom(filename: String) {
        var fileData: List<String> = emptyList()
        println("---Reading File---")
        val bufReader = File(filename).bufferedReader()
        fileData = bufReader.readLines()
        val features = fileData[0].split(",")
        this.nrDimensions = features.subList(1, features.lastIndex).size
        this.samples = MutableList(fileData.size - 1, { DoubleArray(this.nrDimensions)})
        // convert each string to double
        (1 until fileData.size).forEach { i ->
            var row = fileData[i].split(",")
            row = row.subList(1, row.lastIndex)
            (0 until this.nrDimensions).forEach { j ->
                this.samples[(i - 1)][j] = row[j].toDouble()
            }
        }
        println("Read ${this.samples.size} sample(s) from $filename")
    }

    fun swarmify() {
        initialize()
        println("---Optimizing Swarms---")
        var epoch = 0
        val sampleParticleCentroidMinSwarm: MutableList<IntArray> = MutableList(this.samples.size, { IntArray(this.nrParticles) })
        val sampleParticleCentroidMaxSwarm: MutableList<IntArray> = MutableList(this.samples.size, { IntArray(this.nrParticles) })
        while (epoch < this.nrEpochs) {
            println("Epoch $epoch")
            // Perform KMeans steps to cluster data samples
            cluster(this.minimizeSwarm.particles, sampleParticleCentroidMinSwarm)
            cluster(this.maximizeSwarm.particles, sampleParticleCentroidMaxSwarm)
            // Perform MOPSO Steps to find objectives
            (0 until this.nrParticles).forEach { i ->
                // Update the Personal best -> MIN
                if (this.minimizeSwarm.particles[i].x[0] < this.minimizeSwarm.particles[i].y[0]) {
                    this.minimizeSwarm.particles[i].y = this.minimizeSwarm.particles[i].x
                }
                // Update the Personal best -> MAX
                if (this.maximizeSwarm.particles[i].x[1] > this.maximizeSwarm.particles[i].y[1]) {
                    this.maximizeSwarm.particles[i].y = this.maximizeSwarm.particles[i].x
                }
                // Update the Global Best -> MIN
                if (this.minimizeSwarm.particles[i].y[0] < this.minimizeSwarm.globalBest[0]) {
                    this.minimizeSwarm.globalBest = this.minimizeSwarm.particles[i].y
                    this.minimizeSwarm.globalBestCentroids = this.minimizeSwarm.particles[i].centroids
                }
                // Update the Global Best -> MAX
                if (this.maximizeSwarm.particles[i].y[1] > this.maximizeSwarm.globalBest[1]) {
                    this.maximizeSwarm.globalBest = this.maximizeSwarm.particles[i].y
                    this.maximizeSwarm.globalBestCentroids = this.maximizeSwarm.particles[i].centroids
                }
            }
            (0 until this.nrParticles).forEach { i ->
                // Update Velocity
                for (j in this.minimizeSwarm.particles[i].v.indices) {
                    val cognitiveComponent: Double = (this.c1 * this.r1[j]) * (this.minimizeSwarm.globalBest[j] - this.minimizeSwarm.particles[i].x[j])
                    val socialComponent: Double = (this.c2 * this.r2[j]) * (this.maximizeSwarm.globalBest[j] - this.minimizeSwarm.particles[i].x[j])
                    this.minimizeSwarm.particles[i].v[j] = (this.w * this.minimizeSwarm.particles[i].v[j]) + cognitiveComponent + socialComponent
                }
                for (j in this.maximizeSwarm.particles[i].v.indices) {
                    val cognitiveComponent: Double = (this.c1 * this.r1[j]) * (this.maximizeSwarm.globalBest[j] - this.maximizeSwarm.particles[i].x[j])
                    val socialComponent: Double = (this.c2 * this.r2[j]) * (this.minimizeSwarm.globalBest[j] - this.maximizeSwarm.particles[i].x[j])
                    this.maximizeSwarm.particles[i].v[j] = (this.w * this.maximizeSwarm.particles[i].v[j]) + cognitiveComponent + socialComponent
                }
                // Update Position
                for (j in this.minimizeSwarm.particles[i].x.indices) {
                    this.minimizeSwarm.particles[i].x[j] += this.minimizeSwarm.particles[i].v[j]
                    this.maximizeSwarm.particles[i].x[j] += this.maximizeSwarm.particles[i].v[j]
                }
            }
            randomizeRs()
            this.samples.shuffle() // Shuffle data samples to remove bias found in the order of presentation of data samples
            this.minimizeSwarm.stringify()
            this.maximizeSwarm.stringify()
            epoch += 1
        }
    }

    fun graphify() {
        TODO("not implemented")
    }

    fun stringify() {
        println("---Printing Optimal Results---")
        println("After ${this.nrEpochs} epochs, the results of the Le-Creyol Multi Objective PSO are as follows:")
        val distMinMax: Boolean = optimalDistance(this.minimizeSwarm.globalBest, this.maximizeSwarm.globalBest)
        val optimalCentroids = if (distMinMax) {
            this.minimizeSwarm.stringify()
            this.minimizeSwarm.globalBestCentroids
        } else {  // defaults to maximizeSwarm with a higher InterCluster distance
            this.maximizeSwarm.stringify()
            this.maximizeSwarm.globalBestCentroids
        }
        println("---Sample Classifications---")
        var samplesPerCluster = mutableMapOf<String, Int>()
        (0 until this.nrClusters).forEach { i ->
            samplesPerCluster.put(i.toString(), 0)
        }
        for (i in this.samples.indices) {
            val c: Int = findClosestCentroid(optimalCentroids, samples[i])
            var count = samplesPerCluster.get(c.toString())!!.toInt()
            count += 1
            samplesPerCluster.replace(c.toString(), count)
            println("Sample $i belongs to Cluster $c")
        }
        println("---Sample Per Cluster---")
        (0 until this.nrClusters).forEach { i ->
            println("Cluster $i has ${samplesPerCluster.get(i.toString())} samples")
        }
    }

    /* Helpers */
    private fun initialize() {
        println("---Initializing Swarms---")
        this.minimizeSwarm = Swarm(this.nrObjectives, this.nrDimensions)
        this.maximizeSwarm = Swarm(this.nrObjectives, this.nrDimensions)
        this.r1 = DoubleArray(this.nrObjectives)
        this.r2 = DoubleArray(this.nrObjectives)
        val extremes = getExtremes()
        (0 until this.nrParticles).forEach { i ->
            val p1 = Particle(i, this.nrDimensions, this.nrObjectives)
            val p2 = Particle(i, this.nrDimensions, this.nrObjectives)
            (0 until this.nrClusters).forEach { j ->
                p1.centroids.add(j, getRandomCentroid(extremes))
                p2.centroids.add(j, getRandomCentroid(extremes))
            }
            (0 until this.nrObjectives).forEach { j ->
                p1.v[j] = uniformDist(0.0, 1.0)
                p2.v[j] = uniformDist(0.0, 1.0)
                if (j == 0){
                    p1.x[j] = Double.MAX_VALUE
                    p2.x[j] = Double.MAX_VALUE
                } else if (j == 1){
                    p1.x[j] = Double.MIN_VALUE
                    p2.x[j] = Double.MIN_VALUE
                }
            }
            p1.y = p1.x
            p2.y = p2.x
            this.minimizeSwarm.particles.add(i, p1)
            this.maximizeSwarm.particles.add(i, p2)
        }
        (0 until this.nrObjectives).forEach { j ->
            if (j == 0) {
                this.minimizeSwarm.globalBest[j] = Double.MAX_VALUE
                this.maximizeSwarm.globalBest[j] = Double.MAX_VALUE
            } else if (j == 1) {
                this.minimizeSwarm.globalBest[j] = Double.MIN_VALUE
                this.maximizeSwarm.globalBest[j] = Double.MIN_VALUE
            }
        }
        randomizeRs()
        println("Both swarms have ${this.nrParticles} particles, which have been initialized.")
    }

    private fun cluster(particles: MutableList<Particle>, sampleParticleCentroid: MutableList<IntArray>) {
        println("---Clustering Data using KMeans---")
        for (s in this.samples.indices) {
            for (p in particles.indices) {
                sampleParticleCentroid[s][p] = findClosestCentroid(particles[p].centroids, samples[s])
            }
        }
        for (p in particles.indices) {
            val sampleParticleCentroidColumn = getColumnFromArray(p, sampleParticleCentroid)
            for (c in particles[p].centroids.indices) {
                val centroidSamples = findSamplesForCentroid(c, sampleParticleCentroidColumn)
                particles[p].centroids[c] = updateCentroid(centroidSamples)
                particles[p].x[0] = calcIntraClusterDistance(particles[p].centroids[c], centroidSamples)
            }
            particles[p].x[1] = calcInterClusterDistance(particles[p].centroids)
        }
    }

    private fun findClosestCentroid(centroids: MutableList<DoubleArray>, sample: DoubleArray): Int {
        /*  
            Find the centroid closest to the data sample and return it's index
        */
        var idx: Int = -1
        var lowest: Double = Double.MAX_VALUE
        for (i in centroids.indices) {
            val distance: Double = euclideanDistance(sample, centroids[i])
            if (distance < lowest){
                lowest = distance
                idx = i
            }
        }
        return idx
    }

    private fun findSamplesForCentroid(centroidIndex: Int, sampleParticleCentroidColumn: IntArray): MutableList<DoubleArray> {
        /*
            From the sampleParticleCentroidColumn, find those that match the centroidIndex,
            and if they do, get the corresponding sample from this.samples and add
            it to the centroidSamples list
        */
        val centroidSamples: MutableList<DoubleArray> = mutableListOf()
        var counter = 0
        for (s in sampleParticleCentroidColumn.indices) {
            if (sampleParticleCentroidColumn[s] == centroidIndex) {
                centroidSamples.add(counter, this.samples[s])
                counter += 1
            }
        }
        return centroidSamples
    }

    private fun updateCentroid(centroidSamples: MutableList<DoubleArray>): DoubleArray {
        /*
            Calculate the average between the centroid's data samples,
            then move the centroid to that average.
        */
        val newCentroid = DoubleArray(this.nrDimensions)
        centroidSamples.forEach { sample ->
            for (i in sample.indices) {
                newCentroid[i] += sample[i]
            }
        }
        for (i in newCentroid.indices) {
            newCentroid[i] = newCentroid[i] / centroidSamples.size    
        }
        return newCentroid
    }

    private fun calcIntraClusterDistance(centroid: DoubleArray, centroidSamples: MutableList<DoubleArray>): Double {
        var distance = 0.0
        centroidSamples.forEach { sample ->
            distance += euclideanDistance(centroid, sample)
        }
        return distance
    }

    private fun calcInterClusterDistance(centroids: MutableList<DoubleArray>): Double {
        var distance = 0.0
        for (i in centroids.indices) {
            ((i + 1) until centroids.size).forEach { j ->
                distance += euclideanDistance(centroids[i], centroids[j])
            }
        }
        return distance
    }

    private fun uniformDist(min: Double, max: Double): Double {
        return Random().nextDouble() * (max - min) + min
    }

    private fun randomizeRs() {
        (0 until this.nrObjectives).forEach { i ->
            this.r1[i] = uniformDist(0.0, 1.0)
            this.r2[i] = uniformDist(0.0, 1.0)
        }
    }

    private fun euclideanDistance(u: DoubleArray, v: DoubleArray): Double {
        var distance = 0.0
        for (i in u.indices) {
            val diff = u[i] - v[i]
            distance += diff.pow(2.0)
        }
        return distance.pow((1.0 / 2.0))
    }

    private fun optimalDistance(u: DoubleArray, v: DoubleArray): Boolean {
        return u[0] < v[0] && u[1] > v[1]
    }

    private fun getRandomCentroid(extremes: MutableList<DoubleArray>): DoubleArray {
        val centroid = DoubleArray(this.nrDimensions)
        (0 until this.nrDimensions).forEach { i ->
            centroid[i] = uniformDist(extremes[i][0], extremes[i][1])
        }
        return centroid
    }

    private fun getExtremes(): MutableList<DoubleArray> {
        val extremes: MutableList<DoubleArray> = mutableListOf()
        (0 until this.nrDimensions).forEach { col ->
            val feature: DoubleArray = getFeatureFromSamples(col, this.samples)
            val bounds = DoubleArray(this.nrObjectives)
            bounds[0] = feature.min()!!
            bounds[1] = feature.max()!!
            extremes.add(col, bounds)
        }
        return extremes
    }

    private fun getFeatureFromSamples(col: Int, arr: MutableList<DoubleArray>): DoubleArray {
        val feature = DoubleArray(arr.size)
        (0 until arr.size).forEach { row ->
            feature[row] = arr[row][col]
        }
        return feature
    }

    private fun getColumnFromArray(col: Int, arr: MutableList<IntArray>): IntArray {
        val column = IntArray(arr.size)
        (0 until arr.size).forEach { row ->
            column[row] = arr[row][col]
        }
        return column
    }
}
