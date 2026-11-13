package com.example.game.engine

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class SafeZonePhaseConfig(
    val phaseNumber: Int,
    val targetRadius: Float,
    val waitDurationSec: Float,
    val shrinkDurationSec: Float,
    val dps: Float
)

data class SafeZoneSnapshot(
    val phase: Int,
    val totalPhases: Int,
    val isShrinking: Boolean,
    val timeRemainingSec: Float,
    // Active Electric Blue Zone (Current barrier that hurts players outside)
    val currentBlueCenterX: Float,
    val currentBlueCenterY: Float,
    val currentBlueRadius: Float,
    // Target Safe White Zone (Where players must navigate to)
    val targetSafeCenterX: Float,
    val targetSafeCenterY: Float,
    val targetSafeRadius: Float,
    val currentDps: Float
)

/**
 * Coroutine-based system that manages the shrinking safe zone circle during gameplay.
 * Calculates circle shrinkage timers and coordinates periodically to update all clients via StateFlow.
 */
class SafeZoneCoordinator(
    private val worldSize: Float = 5000f,
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.Default),
    private val onZoneUpdateBroadcast: ((SafeZoneSnapshot) -> Unit)? = null
) {
    private val phaseConfigs = listOf(
        SafeZonePhaseConfig(phaseNumber = 1, targetRadius = 2200f, waitDurationSec = 90f, shrinkDurationSec = 60f, dps = 1.5f),
        SafeZonePhaseConfig(phaseNumber = 2, targetRadius = 1500f, waitDurationSec = 75f, shrinkDurationSec = 50f, dps = 3.0f),
        SafeZonePhaseConfig(phaseNumber = 3, targetRadius = 900f, waitDurationSec = 60f, shrinkDurationSec = 40f, dps = 5.5f),
        SafeZonePhaseConfig(phaseNumber = 4, targetRadius = 500f, waitDurationSec = 45f, shrinkDurationSec = 35f, dps = 9.0f),
        SafeZonePhaseConfig(phaseNumber = 5, targetRadius = 250f, waitDurationSec = 35f, shrinkDurationSec = 25f, dps = 15.0f),
        SafeZonePhaseConfig(phaseNumber = 6, targetRadius = 90f, waitDurationSec = 25f, shrinkDurationSec = 20f, dps = 25.0f),
        SafeZonePhaseConfig(phaseNumber = 7, targetRadius = 0f, waitDurationSec = 15f, shrinkDurationSec = 20f, dps = 40.0f) // Final collapse
    )

    private var activeJob: Job? = null

    // Mutable snapshot state for high-frequency client observation
    private val _zoneState = MutableStateFlow(
        SafeZoneSnapshot(
            phase = 1,
            totalPhases = phaseConfigs.size,
            isShrinking = false,
            timeRemainingSec = phaseConfigs[0].waitDurationSec,
            currentBlueCenterX = worldSize / 2f,
            currentBlueCenterY = worldSize / 2f,
            currentBlueRadius = worldSize / 2f,
            targetSafeCenterX = worldSize / 2f,
            targetSafeCenterY = worldSize / 2f,
            targetSafeRadius = phaseConfigs[0].targetRadius,
            currentDps = phaseConfigs[0].dps
        )
    )
    val zoneState: StateFlow<SafeZoneSnapshot> = _zoneState.asStateFlow()

    fun start() {
        activeJob?.cancel()
        activeJob = scope.launch {
            runZoneCoroutineLifecycle()
        }
    }

    fun stop() {
        activeJob?.cancel()
    }

    /**
     * Main Coroutine Lifecycle: Iterates through each shrinking phase sequentially.
     */
    private suspend fun runZoneCoroutineLifecycle() {
        var currentBlueX = worldSize / 2f
        var currentBlueY = worldSize / 2f
        var currentBlueR = worldSize / 2f

        var targetSafeX = worldSize / 2f
        var targetSafeY = worldSize / 2f
        var targetSafeR = phaseConfigs[0].targetRadius

        for (config in phaseConfigs) {
            // Step A: Calculate new safe target circle strictly constrained inside current safe area
            if (config.phaseNumber > 1) {
                val (nextX, nextY) = calculateConstrainedCircleCenter(
                    parentCenterX = targetSafeX,
                    parentCenterY = targetSafeY,
                    parentRadius = targetSafeR,
                    childRadius = config.targetRadius
                )
                targetSafeX = nextX
                targetSafeY = nextY
                targetSafeR = config.targetRadius
            }

            // Step B: Wait Countdown Period (Players see white circle and have time to rotate)
            var waitTimer = config.waitDurationSec
            while (waitTimer > 0f && scope.isActive) {
                val tickStep = 0.05f // 50ms update tick
                delay((tickStep * 1000).toLong())
                waitTimer -= tickStep

                val snapshot = SafeZoneSnapshot(
                    phase = config.phaseNumber,
                    totalPhases = phaseConfigs.size,
                    isShrinking = false,
                    timeRemainingSec = waitTimer.coerceAtLeast(0f),
                    currentBlueCenterX = currentBlueX,
                    currentBlueCenterY = currentBlueY,
                    currentBlueRadius = currentBlueR,
                    targetSafeCenterX = targetSafeX,
                    targetSafeCenterY = targetSafeY,
                    targetSafeRadius = targetSafeR,
                    currentDps = config.dps
                )
                _zoneState.value = snapshot
                onZoneUpdateBroadcast?.invoke(snapshot)
            }

            // Step C: Active Shrink Period (Blue electric barrier closes in on white target)
            var shrinkTimer = config.shrinkDurationSec
            val initialBlueX = currentBlueX
            val initialBlueY = currentBlueY
            val initialBlueR = currentBlueR

            while (shrinkTimer > 0f && scope.isActive) {
                val tickStep = 0.05f
                delay((tickStep * 1000).toLong())
                shrinkTimer -= tickStep

                // Interpolate (Lerp) from initial blue circle toward target white circle
                val progress = (1f - (shrinkTimer / config.shrinkDurationSec)).coerceIn(0f, 1f)
                currentBlueX = initialBlueX + (targetSafeX - initialBlueX) * progress
                currentBlueY = initialBlueY + (targetSafeY - initialBlueY) * progress
                currentBlueR = initialBlueR + (targetSafeR - initialBlueR) * progress

                val snapshot = SafeZoneSnapshot(
                    phase = config.phaseNumber,
                    totalPhases = phaseConfigs.size,
                    isShrinking = true,
                    timeRemainingSec = shrinkTimer.coerceAtLeast(0f),
                    currentBlueCenterX = currentBlueX,
                    currentBlueCenterY = currentBlueY,
                    currentBlueRadius = currentBlueR,
                    targetSafeCenterX = targetSafeX,
                    targetSafeCenterY = targetSafeY,
                    targetSafeRadius = targetSafeR,
                    currentDps = config.dps
                )
                _zoneState.value = snapshot
                onZoneUpdateBroadcast?.invoke(snapshot)
            }

            // Snap at end of shrink
            currentBlueX = targetSafeX
            currentBlueY = targetSafeY
            currentBlueR = targetSafeR
        }
    }

    /**
     * Mathematically guarantees that a new circle of childRadius fits 100% inside parent circle.
     * Distance between centers <= parentRadius - childRadius
     */
    private fun calculateConstrainedCircleCenter(
        parentCenterX: Float,
        parentCenterY: Float,
        parentRadius: Float,
        childRadius: Float
    ): Pair<Float, Float> {
        val maxOffset = (parentRadius - childRadius).coerceAtLeast(0f)
        val angle = Random.nextFloat() * 2f * Math.PI.toFloat()
        val distance = sqrt(Random.nextFloat()) * maxOffset

        val newX = (parentCenterX + cos(angle) * distance).coerceIn(childRadius, worldSize - childRadius)
        val newY = (parentCenterY + sin(angle) * distance).coerceIn(childRadius, worldSize - childRadius)
        return Pair(newX, newY)
    }

    /**
     * Computes whether a player at (x, y) is outside the active electric barrier.
     */
    fun isPlayerOutside(playerX: Float, playerY: Float): Boolean {
        val state = _zoneState.value
        val dx = playerX - state.currentBlueCenterX
        val dy = playerY - state.currentBlueCenterY
        return sqrt(dx * dx + dy * dy) > state.currentBlueRadius
    }

    /**
     * Calculates damage tick to apply if outside the electric blue barrier.
     */
    fun calculateDamageForPlayer(playerX: Float, playerY: Float, deltaSec: Float): Float {
        if (!isPlayerOutside(playerX, playerY)) return 0f
        return _zoneState.value.currentDps * deltaSec
    }
}
