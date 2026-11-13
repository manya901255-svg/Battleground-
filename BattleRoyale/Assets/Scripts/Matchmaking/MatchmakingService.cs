using System;
using System.Threading.Tasks;
using UnityEngine;

namespace BattleRoyale.Matchmaking
{
    public enum MatchmakingQueueType { Solo, Duo, Squad, Ranked, CustomRoom }

    [Serializable]
    public class MatchmakingTicket
    {
        public string TicketId;
        public string PlayerId;
        public MatchmakingQueueType QueueType;
        public string Region;
        public string ServerAddress;
        public int ServerPort;
        public string MatchId;
    }

    public class MatchmakingService : MonoBehaviour
    {
        public static MatchmakingService Instance { get; private set; }

        [SerializeField] private string activeRegion = "ap-northeast-1";

        public bool IsSearching { get; private set; }

        public event Action<int, int> OnSearchProgress;
        public event Action<MatchmakingTicket> OnMatchFound;
        public event Action<string> OnMatchmakingFailed;

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

        public async Task StartMatchmaking(MatchmakingQueueType queueType)
        {
            if (IsSearching) return;
            IsSearching = true;

            int requiredPlayers = queueType == MatchmakingQueueType.Solo ? 100 : 100;
            int foundPlayers = 1;

            while (foundPlayers < requiredPlayers && IsSearching)
            {
                await Task.Delay(40);
                foundPlayers += UnityEngine.Random.Range(3, 8);
                foundPlayers = Mathf.Min(foundPlayers, requiredPlayers);
                OnSearchProgress?.Invoke(foundPlayers, requiredPlayers);
            }

            if (!IsSearching) return;

            var ticket = new MatchmakingTicket
            {
                TicketId = Guid.NewGuid().ToString(),
                PlayerId = "local_player",
                QueueType = queueType,
                Region = activeRegion,
                ServerAddress = "127.0.0.1",
                ServerPort = 7777,
                MatchId = Guid.NewGuid().ToString()
            };

            IsSearching = false;
            OnMatchFound?.Invoke(ticket);
        }

        public void CancelMatchmaking()
        {
            IsSearching = false;
        }
    }
}
