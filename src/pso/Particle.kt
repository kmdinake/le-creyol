package pso

data class Particle(val id: Int, val dim: Int, val nrObjectives: Int) { // shorthand syntax for declaring properties and initializing the properties from primary constructor
    var x: DoubleArray = DoubleArray(this.nrObjectives)
    var v: DoubleArray = DoubleArray(this.nrObjectives)
    var y: DoubleArray = DoubleArray(this.nrObjectives)
    var centroids: MutableList<DoubleArray> = mutableListOf()

    fun stringifyPosBest() {
        print("Personal Best for Particle ${this.id} (y) => [")
        (0 until this.nrObjectives).forEach { j ->
            print(" ${this.y[j]} ")
        }
        println("]")
    }

    fun stringifyPos() {
        print("Position for Particle ${this.id} (x) => [")
        (0 until this.nrObjectives).forEach { j ->
            print(" ${this.x[j]} ")
        }
        println("]")
    }

    fun stringifyVel() {
        print("Velocity for Particle ${this.id} (v) => [")
        (0 until this.nrObjectives).forEach { j ->
            print(" ${this.v[j]} ")
        }
        println("]")
    }

    fun stringifyCentroids() {
        print("Centroids for Particle ${this.id} (centroids) => [")
        this.centroids.forEach { c ->
            print("=> [")
            (0 until this.dim).forEach { j ->
                print(" ${c[j]} ")
            }
            println("]")
        }
        println("]")
    }
}