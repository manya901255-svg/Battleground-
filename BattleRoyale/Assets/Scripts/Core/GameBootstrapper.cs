using System.Collections;
using UnityEngine;
using UnityEngine.SceneManagement;

namespace BattleRoyale.Core
{
    public class GameBootstrapper : MonoBehaviour
    {
        [Header("Scene Configuration")]
        [SerializeField] private string mainMenuScene = "MainMenu";
        [SerializeField] private string serverScene = "BattleMap";

        [Header("Target Framerates")]
        [SerializeField] private int pcTargetFps = 144;
        [SerializeField] private int mobileTargetFps = 60;
        [SerializeField] private int serverTickRate = 60;

        private void Awake()
        {
            DontDestroyOnLoad(gameObject);
            InitializeRuntimeEnvironment();
        }

        private void InitializeRuntimeEnvironment()
        {
            if (Application.isBatchMode)
            {
                // Headless Dedicated Server
                Application.targetFrameRate = serverTickRate;
                QualitySettings.vSyncCount = 0;
                Debug.Log($"[Bootstrapper] Headless Dedicated Server Initialized at {serverTickRate} Hz.");
                SceneManager.LoadScene(serverScene);
            }
            else
            {
                // Client Runtime
                bool isMobile = Application.isMobilePlatform;
                Application.targetFrameRate = isMobile ? mobileTargetFps : pcTargetFps;
                QualitySettings.vSyncCount = isMobile ? 0 : 1;
                Screen.sleepTimeout = SleepTimeout.NeverSleep;

                Debug.Log($"[Bootstrapper] Client Initialized (Platform: {Application.platform}, Target FPS: {Application.targetFrameRate})");
                SceneManager.LoadScene(mainMenuScene);
            }
        }
    }
}
