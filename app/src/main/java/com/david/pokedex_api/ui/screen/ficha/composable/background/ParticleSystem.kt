package com.david.pokedex_api.ui.screen.ficha.composable.background

import kotlin.random.Random

data class Particle(
    var x: Float,
    var y: Float,
    var vx: Float,
    var vy: Float,
    var alpha: Float,
    var size: Float,
    var life: Float,
    var rotation: Float = 0f,
    var seed: Float = Random.nextFloat() // variación individual
)

/**
 * Genera una lista de partículas con posiciones y velocidades aleatorias.
 * Los valores están normalizados (0..1), se escalan al tamaño del canvas al dibujar.
 */
fun spawnParticles(
    count: Int,
    xRange: ClosedFloatingPointRange<Float> = 0f..1f,
    yRange: ClosedFloatingPointRange<Float> = 0f..1f,
    vxRange: ClosedFloatingPointRange<Float> = 0f..0f,
    vyRange: ClosedFloatingPointRange<Float> = -0.003f..-0.001f,
    sizeRange: ClosedFloatingPointRange<Float> = 0.02f..0.06f,
): List<Particle> = List(count) {
    Particle(
        x = Random.nextFloat() * (xRange.endInclusive - xRange.start) + xRange.start,
        y = Random.nextFloat() * (yRange.endInclusive - yRange.start) + yRange.start,
        vx = Random.nextFloat() * (vxRange.endInclusive - vxRange.start) + vxRange.start,
        vy = Random.nextFloat() * (vyRange.endInclusive - vyRange.start) + vyRange.start,
        alpha = Random.nextFloat() * 0.5f + 0.3f,
        size = Random.nextFloat() * (sizeRange.endInclusive - sizeRange.start) + sizeRange.start,
        life = Random.nextFloat()
    )
}
