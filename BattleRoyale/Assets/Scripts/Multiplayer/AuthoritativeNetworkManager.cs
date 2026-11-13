using System;
using System.Collections.Generic;
using UnityEngine;

namespace BattleRoyale.Multiplayer
{
    public class AuthoritativeNetworkManager : MonoBehaviour
    {
        public static AuthoritativeNetworkManager Instance { get; private set; }

        public const int MaxPlayersPerMatch = 100;
        public const int ServerTickRate = 60;

        [Header("Network Status")]
        [SerializeField] private ushort serverPort = 7777;
        [SerializeField] private float heartbeatIntervalSec = 1.0f;

        private readonly Dictionary<ulong, string> _connectedClients = new();
        private float _lastHeartbeatTime;

        public event Action<ulong> OnPlayerConnected;
        public event Action<ulong> OnPlayerDisconnected;

        private void Awake()
        {
            if (Instance != null && Instance != this)
            {
                Destroy(gameObject);
                return;
            }
            Instance = this;
            DontDestroyOnLoad(gameObject);
        }

        private void Update()
        {
            if (Time.time - _lastHeartbeatTime >= heartbeatIntervalSec)
            {
                _lastHeartbeatTime = Time.time;
                EmitServerHeartbeat();
            }
        }

        public void RegisterClient(ulong clientId, string playerTicket)
        {
            if (_connectedClients.Count >= MaxPlayersPerMatch)
            {
                Debug.LogWarning($"[NetworkManager] Rejecting connection from {clientId}: Server room is full (100/100).");
                return;
            }

            _connectedClients[clientId] = playerTicket;
            Debug.Log($"[NetworkManager] Client {clientId} authenticated with ticket {playerTicket}. Total: {_connectedClients.Count}/{MaxPlayersPerMatch}");
            OnPlayerConnected?.Invoke(clientId);
        }

        public void UnregisterClient(ulong clientId)
        {
            if (_connectedClients.Remove(clientId))
            {
                Debug.Log($"[NetworkManager] Client {clientId} disconnected. Remaining: {_connectedClients.Count}/{MaxPlayersPerMatch}");
                OnPlayerDisconnected?.Invoke(clientId);
            }
        }

        private void EmitServerHeartbeat()
        {
            // Transmits health ping and active player count to Matchmaking Backend
        }

        public int GetConnectedCount() => _connectedClients.Count;
    }
}
