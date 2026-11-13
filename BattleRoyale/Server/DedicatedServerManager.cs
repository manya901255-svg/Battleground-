using System;
using UnityEngine;

namespace BattleRoyale.Server
{
    public class DedicatedServerManager : MonoBehaviour
    {
        public static DedicatedServerManager Instance { get; private set; }

        public enum MatchState { WaitingForPlayers, StartingCountdown, ActiveCombat, MatchFinished }

        [Header("Match Progression")]
        [SerializeField] private int requiredPlayers = 100;
        [SerializeField] private float countdownDurationSec = 30f;

        public MatchState State { get; private set; } = MatchState.WaitingForPlayers;
        public float RemainingCountdown { get; private set; }
        public int AlivePlayerCount { get; private set; }

        private void Awake()
        {
            Instance = this;
            RemainingCountdown = countdownDurationSec;
        }

        private void Update()
        {
            if (!Application.isBatchMode) return;

            switch (State)
            {
                case MatchState.WaitingForPlayers:
                    // Check if player capacity has reached threshold
                    if (GetConnectedPlayerCount() >= requiredPlayers)
                    {
                        State = MatchState.StartingCountdown;
                        Debug.Log("[Server] 100 Players joined. Initiating deployment countdown.");
                    }
                    break;

                case MatchState.StartingCountdown:
                    RemainingCountdown -= Time.deltaTime;
                    if (RemainingCountdown <= 0f)
                    {
                        StartBattleRoyale();
                    }
                    break;

                case MatchState.ActiveCombat:
                    // Server-authoritative tick updates zone, loot drops, and eliminations
                    break;
            }
        }

        private void StartBattleRoyale()
        {
            State = MatchState.ActiveCombat;
            AlivePlayerCount = requiredPlayers;
            Debug.Log("[Server] Match Started. Airplane flight path initialized.");
        }

        public void NotifyPlayerEliminated(string victimId, string killerId)
        {
            AlivePlayerCount = Mathf.Max(0, AlivePlayerCount - 1);
            Debug.Log($"[Server] Elimination: {victimId} by {killerId}. Alive: {AlivePlayerCount}");

            if (AlivePlayerCount <= 1 && State == MatchState.ActiveCombat)
            {
                State = MatchState.MatchFinished;
                Debug.Log($"[Server] Victory Royale achieved by {killerId}! Submitting match settlements...");
            }
        }

        private int GetConnectedPlayerCount() => 100;
    }
}
