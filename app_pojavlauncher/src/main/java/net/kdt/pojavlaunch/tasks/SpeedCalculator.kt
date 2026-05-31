package net.kdt.pojavlaunch.tasks

class SpeedCalculator {
    private var lastTime = System.currentTimeMillis()
    private var lastBytes: Long = 0
    private var speed = 0.0

    @Synchronized
    fun feed(currentBytes: Long): Double {
        val currentTime = System.currentTimeMillis()
        val deltaTime = currentTime - lastTime
        if (deltaTime >= 1000) {
            speed = (currentBytes - lastBytes) / (deltaTime / 1000.0)
            lastTime = currentTime
            lastBytes = currentBytes
        }
        return speed
    }
}
