using UnityEngine;
using UnityEngine.UI;

namespace BattleRoyale.UI
{
    public class TacticalHUDController : MonoBehaviour
    {
        [Header("Match Indicators")]
        [SerializeField] private Text aliveText;
        [SerializeField] private Text killsText;
        [SerializeField] private Text zoneTimerText;
        [SerializeField] private Slider healthBar;
        [SerializeField] private Slider shieldBar;

        [Header("Weapon Status")]
        [SerializeField] private Text weaponNameText;
        [SerializeField] private Text ammoCounterText;
        [SerializeField] private Text fireModeText;

        [Header("Cross-Platform Touch Elements")]
        [SerializeField] private GameObject mobileControlCluster;

        private void Start()
        {
            // Auto-detect mobile platform and toggle virtual controls
            if (mobileControlCluster != null)
            {
                mobileControlCluster.SetActive(Application.isMobilePlatform);
            }
        }

        public void UpdateMatchStatus(int alive, int kills, float zoneSeconds)
        {
            if (aliveText != null) aliveText.text = alive.ToString();
            if (killsText != null) killsText.text = kills.ToString();
            if (zoneTimerText != null)
            {
                int m = (int)(zoneSeconds / 60);
                int s = (int)(zoneSeconds % 60);
                zoneTimerText.text = $"{m:00}:{s:00}";
            }
        }

        public void UpdateVitals(float currentHealth, float maxHealth, float currentShield, float maxShield)
        {
            if (healthBar != null) healthBar.value = currentHealth / maxHealth;
            if (shieldBar != null) shieldBar.value = currentShield / maxShield;
        }

        public void UpdateWeaponInfo(string weaponName, int currentAmmo, int maxAmmo, string fireMode)
        {
            if (weaponNameText != null) weaponNameText.text = weaponName;
            if (ammoCounterText != null) ammoCounterText.text = $"{currentAmmo} / {maxAmmo}";
            if (fireModeText != null) fireModeText.text = fireMode;
        }
    }
}
