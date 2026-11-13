using System;
using System.Text;
using System.Threading.Tasks;
using UnityEngine;
using UnityEngine.Networking;

namespace BattleRoyale.Backend
{
    [Serializable]
    public class PlayerProfileData
    {
        public string userId;
        public string username;
        public int level;
        public int xp;
        public long bpCoins;
        public int vortexCredits;
        public string rankTier;
    }

    [Serializable]
    public class MatchSettlementRequest
    {
        public string userId;
        public int placement;
        public int kills;
        public float damage;
        public int survivalSeconds;
    }

    public class BackendClient : MonoBehaviour
    {
        public static BackendClient Instance { get; private set; }

        [SerializeField] private string backendBaseUrl = "https://api.vortexbattlegrounds.com";

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

        public async Task<PlayerProfileData> FetchProfileAsync(string authToken)
        {
            using var request = UnityWebRequest.Get($"{backendBaseUrl}/api/v1/player/profile");
            request.SetRequestHeader("Authorization", $"Bearer {authToken}");

            var op = request.SendWebRequest();
            while (!op.isDone) await Task.Yield();

            if (request.result != UnityWebRequest.Result.Success)
            {
                Debug.LogError($"[Backend] Failed to fetch profile: {request.error}");
                return null;
            }

            return JsonUtility.FromJson<PlayerProfileData>(request.downloadHandler.text);
        }

        public async Task<bool> SubmitMatchResultAsync(MatchSettlementRequest payload, string serverSecret)
        {
            string json = JsonUtility.ToJson(payload);
            using var request = new UnityWebRequest($"{backendBaseUrl}/api/v1/match/settle", "POST")
            {
                uploadHandler = new UploadHandlerRaw(Encoding.UTF8.GetBytes(json)),
                downloadHandler = new DownloadHandlerBuffer()
            };
            request.SetRequestHeader("Content-Type", "application/json");
            request.SetRequestHeader("X-Server-Secret", serverSecret);

            var op = request.SendWebRequest();
            while (!op.isDone) await Task.Yield();

            return request.result == UnityWebRequest.Result.Success;
        }
    }
}
