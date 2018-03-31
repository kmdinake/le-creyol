package pso

data class Swarm(private val nrObjectives: Int, private val dim: Int) {
    var particles: MutableList<Particle> = mutableListOf()
    var globalBest: DoubleArray = DoubleArray(this.nrObjectives)
    var globalBestCentroids: MutableList<DoubleArray> = mutableListOf()

    fun stringify() {
        println("-Printing Swarm Results-")
        println("Global Best Position => [")
        println("=> Inter Cluster Distance => ${this.globalBest[0]} ")
        println("=> Intra Cluster Distance => ${this.globalBest[1]} ")
        println("]")
        println("Global Best Centroids => [")
        this.globalBestCentroids.forEach { c ->
            print("=> [")
            (0 until this.dim).forEach { j ->
                print(" ${c[j]} ")
            }
            println("]")
        }
        println("]")
    }
}