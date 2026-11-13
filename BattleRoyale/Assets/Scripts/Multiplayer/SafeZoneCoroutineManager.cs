using System;
using System.Collections;
using UnityEngine;

namespace BattleRoyale.Multiplayer
{
    [Serializable]
    public struct SafeZonePhase
    {
        public int phaseIndex;
        public float targetRadius;
        public float waitDurationSec;
        public float shrinkDurationSec;
        public float damagePerSec;
    }

    [Serializable]
    public struct SafeZoneNetworkPacket
    {
        public int currentPhase;
        public bool isShrinking;
        public float remainingTime;
        public Vector2 currentBlueCenter;
        public float currentBlueRadius;
        public Vector2 targetSafeCenter;
        public float targetSafeRadius;
        public float currentDps;
    }

    /// <summary>
    /// Coroutine-based server-authoritative system that manages the shrinking safe zone circle during gameplay.
    /// Periodically calculates shrinkage coordinates, timers, and broadcasts state packets to all connected clients.
    /// </summary>
    public class SafeZoneCoroutineManager : MonoBehaviour
    {
        public static SafeZoneCoroutineManager Instance { get; private set; }

        [Header("World Boundaries")]
        [SerializeField] private float mapDimension = 5000f;
        [SerializeField] private float networkBroadcastRateSec = 0.05f; // 20 Hz network sync

        [Header("Zone Configuration Phases")]
        [SerializeField] private SafeZonePhase[] phases = new SafeZonePhase[]
        {
            new SafeZonePhase { phaseIndex = 1, targetRadius = 2200f, waitDurationSec = 90f, shrinkDurationSec = 60f, damagePerSec = 1.5f },
            new SafeZonePhase { phaseIndex = 2, targetRadius = 1500f, waitDurationSec = 75f, shrinkDurationSec = 50f, damagePerSec = 3.0f },
            new SafeZonePhase { phaseIndex = 3, targetRadius = 900f,  waitDurationSec = 60f, shrinkDurationSec = 40f, damagePerSec = 5.5f },
            new SafeZonePhase { phaseIndex = 4, targetRadius = 500f,  waitDurationSec = 45f, shrinkDurationSec = 35f, damagePerSec = 9.0f },
            new SafeZonePhase { phaseIndex = 5, targetRadius = 250f,  waitDurationSec = 35f, shrinkDurationSec = 25f, damagePerSec = 15.0f },
            new SafeZonePhase { phaseIndex = 6, targetRadius = 90f,   waitDurationSec = 25f, shrinkDurationSec = 20f, damagePerSec = 25.0f },
            new SafeZonePhase { phaseIndex = 7, targetRadius = 0f,    waitDurationSec = 15f, shrinkDurationSec = 20f, damagePerSec = 40.0f }
        };

        // Active State
        public Vector2 CurrentBlueCenter { get; private set; }
        public float CurrentBlueRadius { get; private set; }
        public Vector2 TargetSafeCenter { get; private set; }
        public float TargetSafeRadius { get; private set; }
        public float CurrentDamagePerSec { get; private set; }
        public bool IsShrinking { get; private set; }
        public float TimeRemaining { get; private set; }

        public event Action<SafeZoneNetworkPacket> OnSafeZoneSynchronized;

        private Coroutine _lifecycleCoroutine;

        private void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }
            Instance = this;

            CurrentBlueCenter = new Vector2(mapDimension / 2f, mapDimension / 2f);
            CurrentBlueRadius = mapDimension / 2f;
            TargetSafeCenter = CurrentBlueCenter;
            TargetSafeRadius = phases[0].targetRadius;
            CurrentDamagePerSec = phases[0].damagePerSec;
        }

        public void StartSafeZoneLifecycle()
        {
            if (_lifecycleCoroutine != null) StopCoroutine(_lifecycleCoroutine);
            _lifecycleCoroutine = StartCoroutine(SafeZoneShrinkCoroutine());
        }

        public void StopSafeZoneLifecycle()
        {
            if (_lifecycleCoroutine != null)
            {
                StopCoroutine(_lifecycleCoroutine);
                _lifecycleCoroutine = null;
            }
        }

        /// <summary>
        /// Coroutine sequence executing the complete multi-phase battleground shrink lifecycle.
        /// </summary>
        private IEnumerator SafeZoneShrinkCoroutine()
        {
            float centerCoord = mapDimension / 2f;
            CurrentBlueCenter = new Vector2(centerCoord, centerCoord);
            CurrentBlueRadius = centerCoord;
            TargetSafeCenter = CurrentBlueCenter;

            for (int i = 0; i < phases.Length; i++)
            {
                SafeZonePhase phase = phases[i];
                CurrentDamagePerSec = phase.damagePerSec;

                // 1. Calculate new safe circle coordinates strictly inscribed in the current circle
                if (i > 0)
                {
                    TargetSafeCenter = CalculateInscribedCircle(TargetSafeCenter, TargetSafeRadius, phase.targetRadius);
                    TargetSafeRadius = phase.targetRadius;
                }

                // 2. Wait Period Coroutine (Players see white circle and have time to rotate)
                IsShrinking = false;
                TimeRemaining = phase.waitDurationSec;

                while (TimeRemaining > 0f)
                {
                    yield return new WaitForSeconds(networkBroadcastRateSec);
                    TimeRemaining -= networkBroadcastRateSec;
                    BroadcastStateToClients(phase.phaseIndex);
                }

                // 3. Active Shrink Period Coroutine (Blue circle closes in on target white circle)
                IsShrinking = true;
                TimeRemaining = phase.shrinkDurationSec;

                Vector2 initialCenter = CurrentBlueCenter;
                float initialRadius = CurrentBlueRadius;

                while (TimeRemaining > 0f)
                {
                    yield return new WaitForSeconds(networkBroadcastRateSec);
                    TimeRemaining -= networkBroadcastRateSec;

                    float progress = Mathf.Clamp01(1f - (TimeRemaining / phase.shrinkDurationSec));
                    CurrentBlueCenter = Vector2.Lerp(initialCenter, TargetSafeCenter, progress);
                    CurrentBlueRadius = Mathf.Lerp(initialRadius, TargetSafeRadius, progress);

                    BroadcastStateToClients(phase.phaseIndex);
                }

                // Snap to target at completion of phase
                CurrentBlueCenter = TargetSafeCenter;
                CurrentBlueRadius = TargetSafeRadius;
                BroadcastStateToClients(phase.phaseIndex);
            }

            Debug.Log("[SafeZone] Final circle collapse complete. Match concluding.");
        }

        /// <summary>
        /// Mathematically guarantees child circle is entirely within parent circle:
        /// distance(parent, child) <= parentRadius - childRadius
        /// </summary>
        private Vector2 CalculateInscribedCircle(Vector2 parentCenter, float parentRadius, float childRadius)
        {
            float maxOffset = Mathf.Max(0f, parentRadius - childRadius);
            Vector2 randomDir = UnityEngine.Random.insideUnitCircle * maxOffset;
            Vector2 result = parentCenter + randomDir;

            result.x = Mathf.Clamp(result.x, childRadius, mapDimension - childRadius);
            result.y = Mathf.Clamp(result.y, childRadius, mapDimension - childRadius);
            return result;
        }

        /// <summary>
        /// Dispatches authoritative network packet to all 100 connected clients.
        /// </summary>
        private void BroadcastStateToClients(int phaseIndex)
        {
            var packet = new SafeZoneNetworkPacket
            {
                currentPhase = phaseIndex,
                isShrinking = IsShrinking,
                remainingTime = Mathf.Max(0f, TimeRemaining),
                currentBlueCenter = CurrentBlueCenter,
                currentBlueRadius = CurrentBlueRadius,
                targetSafeCenter = TargetSafeCenter,
                targetSafeRadius = TargetSafeRadius,
                currentDps = CurrentDamagePerSec
            };

            OnSafeZoneSynchronized?.Invoke(packet);
        }

        public bool IsPlayerOutsideBarrier(Vector3 worldPosition)
        {
            Vector2 player2D = new Vector2(worldPosition.x, worldPosition.z);
            return Vector2.Distance(player2D, CurrentBlueCenter) > CurrentBlueRadius;
        }
    }
}
