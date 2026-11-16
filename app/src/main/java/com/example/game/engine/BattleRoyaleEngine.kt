package com.example.game.engine

import com.example.game.audio.SoundSynthesizer
import com.example.game.graphics.TerrainHeightfield
import com.example.game.map.MapCatalog
import com.example.game.map.MapConfig
import com.example.game.model.AirdropCrate
import com.example.game.model.AmmoType
import com.example.game.model.BotBehavior
import com.example.game.model.BotDifficulty
import com.example.game.model.BulletProjectile
import com.example.game.model.ConsumableType
import com.example.game.model.DamagePopup
import com.example.game.model.DeployableShield
import com.example.game.model.EmotePreset
import com.example.game.model.ExplosiveBarrel
import com.example.game.model.GameModeType
import com.example.game.model.GroundLoot
import com.example.game.model.KillFeedEntry
import com.example.game.model.MatchPhase
import com.example.game.model.PetPreset
import com.example.game.model.Player
import com.example.game.model.PlayerStance
import com.example.game.model.RedZoneState
import com.example.game.model.ScopeType
import com.example.game.model.SmokeCloud
import com.example.game.model.ThrowableProjectile
import com.example.game.model.ThrowableType
import com.example.game.model.VehicleInstance
import com.example.game.model.WeaponCatalog
import com.example.game.model.WeatherType
import com.example.game.model.ZoneState
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.sin
import kotlin.math.sqrt
import kotlin.random.Random

data class ExplosionEffect(
    val x: Float,
    val y: Float,
    val maxRadius: Float = 220f,
    var elapsedSec: Float = 0f,
    val durationSec: Float = 0.65f
)

data class MatchEngineState(
    val phase: MatchPhase = MatchPhase.WAITING_IN_LOBBY,
    val lobbyCountdownSec: Float = 4f,
    val planeProgress: Float = 0f,
    val planeX: Float = 500f,
    val planeY: Float = 500f,
    val isUserInPlane: Boolean = true,
    val altitudeMeters: Float = 1500f,
    val isParachuteDeployed: Boolean = false,
    val aliveCount: Int = 100,
    val totalPlayers: Int = 100,
    val userKills: Int = 0,
    val userDamage: Float = 0f,
    val userPlacement: Int = 0,
    val userTeamAliveCount: Int = 4,
    val userHealth: Float = 100f,
    val userMaxHealth: Float = 100f,
    val userShield: Float = 100f,
    val userEp: Float = 100f,
    val userMaxEp: Float = 200f,
    val matchCoins: Int = 600,
    val squadMembers: List<Player> = emptyList(),
    val safeZone: ZoneState = ZoneState(),
    val redZone: RedZoneState? = null,
    val airdrops: List<AirdropCrate> = emptyList(),
    val smokeClouds: List<SmokeCloud> = emptyList(),
    val explosions: List<ExplosionEffect> = emptyList(),
    val deployableShields: List<DeployableShield> = emptyList(),
    val damagePopups: List<DamagePopup> = emptyList(),
    val killFeed: List<KillFeedEntry> = emptyList(),
    val nearbyLoot: List<GroundLoot> = emptyList(),
    val nearbyVehicle: VehicleInstance? = null,
    val isRevivingTeammate: Boolean = false,
    val reviveProgress: Float = 0f,
    val matchDurationSec: Int = 0,
    val isSpectating: Boolean = false,
    val spectatedPlayer: Player? = null,
    val vehicleNitroActive: Boolean = false,
    val vehicleSpeedKmh: Int = 0,
    val radioCommsBanner: String? = null,
    val takedownBanner: String? = null,
    val streakCount: Int = 0,
    val cameraYawDegrees: Float = -45f,
    val cameraPitchDegrees: Float = 11f,
    val isSprintLocked: Boolean = false,
    val lastHitTimestampMs: Long = 0L,
    val lastHitWasHeadshot: Boolean = false,
    val dashCooldownSec: Float = 0f,
    val dashActiveSec: Float = 0f,
    val shieldCooldownSec: Float = 0f,
    val medicCooldownSec: Float = 0f,
    val scanCooldownSec: Float = 0f,
    val radarScanActiveSec: Float = 0f,
    val isReloading: Boolean = false,
    val reloadProgress: Float = 0f,
    val medkitCount: Int = 5,
    val throwableCount: Int = 3,
    val activePetId: String = "draco_flame",
    val petVisible: Boolean = true,
    val weatherType: WeatherType = WeatherType.CLEAR,
    val userAssists: Int = 0,
    val activeEmoteText: String? = null,
    val nearbyKnockedAllyId: String? = null,
    val nearbyKnockedAllyName: String? = null,
    val trainingDummyHits: Int = 0,
    val trainingDummyDamage: Int = 0,
    val latencyMs: Int = 24,
    val backpackUsedUnits: Int = 0,
    val backpackMaxUnits: Int = 180,
    val playersSnapshot: List<Player> = emptyList(),
    val groundLootSnapshot: List<GroundLoot> = emptyList(),
    val vehiclesSnapshot: List<VehicleInstance> = emptyList(),
    val bulletsSnapshot: List<BulletProjectile> = emptyList(),
    val explosiveBarrelsSnapshot: List<ExplosiveBarrel> = emptyList()
)

class BattleRoyaleEngine(
    val gameMode: GameModeType,
    val mapConfig: MapConfig = MapCatalog.getKavalaIsland(),
    private val soundSynthesizer: SoundSynthesizer? = null,
    private val autoPickupEnabled: Boolean = true,
    private val aimAssistEnabled: Boolean = true,
    val teamSize: Int = 4,
    val characterPresetId: String = "vortex_ghost",
    val activePetId: String = "draco_flame",
    val weatherType: WeatherType = WeatherType.CLEAR,
    val petVisible: Boolean = true,
    @Volatile var cameraSensitivity: Float = 1.0f,
    @Volatile var adsSensitivity: Float = 0.85f,
    val equippedPrimaryWeaponId: String = "m416",
    val weaponSkinMap: Map<String, String> = emptyMap(),
    val weaponUpgradeMap: Map<String, Int> = emptyMap(),
    private val onMatchFinished: (placement: Int, kills: Int, damage: Float, durationSec: Int) -> Unit
) {
    private val scope = CoroutineScope(Dispatchers.Default)
    private var loopJob: Job? = null
    private val worldLock = Any()
    val safeZoneCoordinator = SafeZoneCoordinator(worldSize = mapConfig.worldSize, scope = scope)

    private val _engineState = MutableStateFlow(
        MatchEngineState(totalPlayers = gameMode.maxPlayers, aliveCount = gameMode.maxPlayers)
    )
    val engineState: StateFlow<MatchEngineState> = _engineState.asStateFlow()

    // Game world actors (guarded by worldLock for cross-thread safety)
    val players = mutableListOf<Player>()
    lateinit var userPlayer: Player
    val groundLoot = mutableListOf<GroundLoot>()
    val vehicles = mutableListOf<VehicleInstance>()
    val bullets = mutableListOf<BulletProjectile>()
    val throwables = mutableListOf<ThrowableProjectile>()
    val smokeClouds = mutableListOf<SmokeCloud>()
    val explosions = mutableListOf<ExplosionEffect>()
    val deployableShields = mutableListOf<DeployableShield>()
    val damagePopups = mutableListOf<DamagePopup>()
    val airdrops = mutableListOf<AirdropCrate>()
    val killFeed = mutableListOf<KillFeedEntry>()
    val explosiveBarrels = mutableListOf<ExplosiveBarrel>()

    // Flight path
    private val planeStartX = 400f
    private val planeStartY = 400f
    private val planeEndX = 4600f
    private val planeEndY = 4600f

    // Input & Camera State
    @Volatile var moveJoystickX: Float = 0f
    @Volatile var moveJoystickY: Float = 0f
    @Volatile var isFiringInput: Boolean = false
    @Volatile var isAimingDownSights: Boolean = false
    @Volatile var cameraYawDegrees: Float = -45f
    @Volatile var cameraPitchDegrees: Float = 11f
    var aimAngleDegrees: Float
        get() = cameraYawDegrees
        set(value) { cameraYawDegrees = value }
    @Volatile var isSprintLocked: Boolean = false

    private var lastUserFireTimeMs: Long = 0L
    private var spectatedPlayerId: String? = null
    private var nitroDurationSec: Float = 0f
    private var currentSpeedKmh: Int = 0
    private var elapsedCombatSec = 0f
    private var radioBannerTimerSec: Float = 0f
    private var takedownBannerTimerSec: Float = 0f
    private var activeTakedownText: String? = null
    private var shieldCooldownSec: Float = 0f
    private var dashCooldownSec: Float = 0f
    private var dashActiveSec: Float = 0f
    private var medicCooldownSec: Float = 0f
    private var scanCooldownSec: Float = 0f
    private var radarScanActiveSec: Float = 0f
    private var matchCoins: Int = 600
    private var reloadTotalSec: Float = 2.0f
    private var lastHitTimestampMs: Long = 0L
    private var lastHitWasHeadshot: Boolean = false
    private var emoteBannerTimerSec: Float = 0f
    private var activeEmoteLabel: String? = null
    private var revivingTargetId: String? = null
    private var reviveElapsedSec: Float = 0f
    private val reviveDurationSec: Float = 3.0f
    private var trainingDummyHits: Int = 0
    private var trainingDummyDamage: Int = 0
    private val damageContributors = mutableMapOf<String, MutableSet<String>>()

    init {
        initializeMatch()
    }

    fun start() {
        loopJob?.cancel()
        loopJob = scope.launch {
            var lastTimeMs = System.currentTimeMillis()
            while (isActive) {
                val now = System.currentTimeMillis()
                val deltaSec = ((now - lastTimeMs) / 1000f).coerceIn(0.005f, 0.1f)
                lastTimeMs = now

                updateGameTick(deltaSec)
                delay(16) // ~60 FPS simulation tick
            }
        }
    }

    fun stop() {
        loopJob?.cancel()
        safeZoneCoordinator.stop()
    }

    private fun initializeMatch() {
        players.clear()
        groundLoot.clear()
        vehicles.clear()
        bullets.clear()
        throwables.clear()
        smokeClouds.clear()
        explosions.clear()
        deployableShields.clear()
        damagePopups.clear()
        airdrops.clear()
        killFeed.clear()
        explosiveBarrels.clear()
        damageContributors.clear()
        trainingDummyHits = 0
        trainingDummyDamage = 0
        revivingTargetId = null
        reviveElapsedSec = 0f
        elapsedCombatSec = 0f

        groundLoot.addAll(MapCatalog.generateInitialLoot(mapConfig))
        vehicles.addAll(MapCatalog.generateVehicles(mapConfig))
        explosiveBarrels.addAll(MapCatalog.generateBarrels(mapConfig))

        val isInstantCombat = gameMode == GameModeType.TRAINING_GROUND ||
            gameMode == GameModeType.TEAM_DEATHMATCH ||
            gameMode == GameModeType.OFFLINE_PRACTICE

        fun buildConfiguredWeapon(id: String): com.example.game.model.Weapon {
            val cleanId = id.lowercase()
            return WeaponCatalog.createById(
                id = cleanId,
                skin = weaponSkinMap[cleanId],
                upgradeLevel = weaponUpgradeMap[cleanId] ?: 1
            )
        }

        // Apply player's equipped weapon + unlocked skins/upgrades in combat (preserving sniper training rule)
        val chosenId = equippedPrimaryWeaponId.lowercase()
        val startPrimary1 = when (gameMode) {
            GameModeType.SNIPER_TRAINING -> buildConfiguredWeapon(if (chosenId == "awm") "awm" else "kar98k")
            else -> if (chosenId == "deagle") buildConfiguredWeapon("m416") else buildConfiguredWeapon(chosenId)
        }
        val startPrimary2 = when (gameMode) {
            GameModeType.TRAINING_GROUND, GameModeType.SNIPER_TRAINING -> buildConfiguredWeapon("awm")
            GameModeType.OFFLINE_PRACTICE, GameModeType.TEAM_DEATHMATCH -> buildConfiguredWeapon("ump45")
            else -> null
        }

        val petMaxShieldBonus = if (activePetId == PetPreset.FROST_OWL.id) 15f else 0f
        val charShieldBonus = if (characterPresetId == "blaze_striker") 10f else 0f
        userPlayer = Player(
            id = "user_player_0",
            name = "VortexGhost",
            isUser = true,
            teamId = 1,
            x = 2500f,
            y = 2500f,
            angle = cameraYawDegrees,
            backpackLevel = 2,
            armorLevel = if (isInstantCombat) 2 else 1,
            helmetLevel = if (isInstantCombat) 2 else 1,
            shield = 100f + petMaxShieldBonus + charShieldBonus,
            maxShield = 100f + petMaxShieldBonus + charShieldBonus,
            characterPresetId = characterPresetId,
            primaryWeapon1 = startPrimary1,
            primaryWeapon2 = startPrimary2,
            pistol = buildConfiguredWeapon("deagle"),
            melee = buildConfiguredWeapon("pan"),
            activeSlot = if (chosenId == "deagle" && gameMode != GameModeType.SNIPER_TRAINING) 3 else 1
        )
        userPlayer.ammoInventory[AmmoType.AMMO_556] = 150
        userPlayer.ammoInventory[AmmoType.AMMO_45ACP] = 120
        userPlayer.ammoInventory[AmmoType.AMMO_762] = 90
        userPlayer.ammoInventory[AmmoType.AMMO_300MAG] = 40
        userPlayer.ammoInventory[AmmoType.AMMO_12GAUGE] = 32
        userPlayer.consumableInventory[ConsumableType.FIRST_AID] = 3
        userPlayer.consumableInventory[ConsumableType.ENERGY_DRINK] = 2
        userPlayer.throwableInventory[ThrowableType.FRAG_GRENADE] = 3
        userPlayer.throwableInventory[ThrowableType.SMOKE_GRENADE] = 1
        userPlayer.ep = 100f
        players.add(userPlayer)

        // Spawn AI Opponents & Squadmates with varied difficulties (Recruit, Tactical, Veteran)
        val botCount = gameMode.maxPlayers - 1
        val squadNames = listOf("Viper_42", "Shadow_X", "Titan_K9")
        val botNames = listOf(
            "ShadowBlade", "ViperStrike", "ApexSniper", "FrostBite", "GhostRider",
            "NeonNinja", "TacticalTitan", "CyberWolf", "DeltaForce", "IronClad",
            "RavenKing", "ThunderBolt", "ZeroCool", "RogueOne", "BulletStorm",
            "HavocKnight", "SpecterX", "AlphaPredator", "StormBreaker", "NightHawk"
        )
        val presets = listOf("vortex_ghost", "blaze_striker", "cyber_viper", "frost_recon", "apex_titan")

        for (i in 1..botCount) {
            val effectiveTeamSize = teamSize.coerceIn(1, 4)
            val team = when (gameMode) {
                GameModeType.TEAM_DEATHMATCH -> if (i <= 3) 1 else 2
                else -> (i / effectiveTeamSize) + 1
            }
            val isSquadmate = team == 1
            val name = if (isSquadmate && i <= squadNames.size) {
                squadNames[i - 1]
            } else if (i <= botNames.size) {
                botNames[i - 1]
            } else {
                "Operative_$i"
            }
            val difficulty = when (i % 3) {
                0 -> BotDifficulty.VETERAN
                1 -> BotDifficulty.RECRUIT
                else -> BotDifficulty.TACTICAL
            }
            // Spawn squadmates close by and initial opponents near Pochinki / center for immediate TPS combat
            val nearCenter = i <= 14
            val bx = when {
                isSquadmate -> 2500f + (Random.nextFloat() - 0.5f) * 160f
                nearCenter -> 2500f + (Random.nextFloat() - 0.5f) * 860f
                else -> 900f + Random.nextFloat() * 3200f
            }
            val by = when {
                isSquadmate -> 2500f + (Random.nextFloat() - 0.5f) * 160f
                nearCenter -> 2500f + (Random.nextFloat() - 0.5f) * 860f
                else -> 900f + Random.nextFloat() * 3200f
            }

            val bot = Player(
                id = "bot_$i",
                name = name,
                isUser = false,
                teamId = team,
                x = bx,
                y = by,
                angle = Random.nextFloat() * 360f,
                shield = if (difficulty == BotDifficulty.VETERAN) 100f else 60f,
                maxShield = 100f,
                armorLevel = if (gameMode == GameModeType.TEAM_DEATHMATCH || difficulty == BotDifficulty.VETERAN) 2 else 1,
                helmetLevel = if (gameMode == GameModeType.TEAM_DEATHMATCH) 2 else 1,
                characterPresetId = presets[i % presets.size],
                botDifficulty = difficulty,
                primaryWeapon1 = if (gameMode == GameModeType.SNIPER_TRAINING) WeaponCatalog.createKar98k() else WeaponCatalog.createM416()
            )
            bot.ammoInventory[AmmoType.AMMO_556] = 180
            bot.ammoInventory[AmmoType.AMMO_762] = 90
            players.add(bot)
        }

        val startPhase = if (isInstantCombat) MatchPhase.ACTIVE_COMBAT else MatchPhase.WAITING_IN_LOBBY
        if (gameMode == GameModeType.OFFLINE_PRACTICE) {
            safeZoneCoordinator.start()
        }

        _engineState.value = MatchEngineState(
            phase = startPhase,
            totalPlayers = players.size,
            aliveCount = players.size,
            userHealth = userPlayer.health,
            userMaxHealth = userPlayer.maxHealth,
            userShield = userPlayer.shield,
            userEp = userPlayer.ep,
            userMaxEp = userPlayer.maxEp,
            matchCoins = matchCoins,
            squadMembers = players.filter { it.teamId == userPlayer.teamId }.take(4),
            cameraYawDegrees = cameraYawDegrees,
            cameraPitchDegrees = cameraPitchDegrees,
            medkitCount = totalMedkitsCount(),
            throwableCount = totalThrowableCount(),
            activePetId = activePetId,
            petVisible = petVisible,
            weatherType = weatherType,
            userAssists = userPlayer.assists,
            backpackUsedUnits = computeBackpackWeight(),
            backpackMaxUnits = userPlayer.backpackLevel * 90 + 60 + (if (characterPresetId == "apex_titan") 45 else 0),
            playersSnapshot = players.toList(),
            groundLootSnapshot = groundLoot.toList(),
            vehiclesSnapshot = vehicles.toList(),
            bulletsSnapshot = bullets.toList(),
            explosiveBarrelsSnapshot = explosiveBarrels.toList()
        )
    }

    private fun computeBackpackWeight(): Int {
        val ammoWeight = userPlayer.ammoInventory.values.sum() / 5
        val medWeight = totalMedkitsCount() * 10
        val throwWeight = totalThrowableCount() * 12
        return ammoWeight + medWeight + throwWeight
    }

    private fun totalMedkitsCount(): Int =
        (userPlayer.consumableInventory[ConsumableType.FIRST_AID] ?: 0) +
            (userPlayer.consumableInventory[ConsumableType.MEDKIT] ?: 0) +
            (userPlayer.consumableInventory[ConsumableType.BANDAGE] ?: 0) +
            (userPlayer.consumableInventory[ConsumableType.ENERGY_DRINK] ?: 0) +
            (userPlayer.consumableInventory[ConsumableType.PAINKILLER] ?: 0)

    private fun totalThrowableCount(): Int =
        userPlayer.throwableInventory.values.sum()

    private fun updateGameTick(deltaSec: Float) {
        synchronized(worldLock) {
            val state = _engineState.value
            when (state.phase) {
                MatchPhase.WAITING_IN_LOBBY -> updateLobbyPhase(deltaSec, state)
                MatchPhase.PLANE_FLIGHT -> updatePlanePhase(deltaSec, state)
                MatchPhase.SKYDIVING -> updateSkydivingPhase(deltaSec, state)
                MatchPhase.ACTIVE_COMBAT, MatchPhase.SPECTATING -> updateCombatPhase(deltaSec, state)
                MatchPhase.MATCH_ENDED -> { /* Standby */ }
            }
        }
    }

    private fun updateLobbyPhase(deltaSec: Float, state: MatchEngineState) {
        val remaining = state.lobbyCountdownSec - deltaSec
        if (remaining <= 0f) {
            _engineState.value = state.copy(
                phase = MatchPhase.PLANE_FLIGHT,
                lobbyCountdownSec = 0f,
                planeProgress = 0f,
                isUserInPlane = true
            )
        } else {
            _engineState.value = state.copy(lobbyCountdownSec = remaining)
        }
    }

    private fun updatePlanePhase(deltaSec: Float, state: MatchEngineState) {
        val newProgress = (state.planeProgress + deltaSec * 0.055f).coerceAtMost(1f)
        val curX = planeStartX + (planeEndX - planeStartX) * newProgress
        val curY = planeStartY + (planeEndY - planeStartY) * newProgress

        players.filter { !it.isUser && it.isAlive }.forEach { bot ->
            if (Random.nextFloat() < 0.035f && newProgress > 0.15f) {
                bot.x = curX + (Random.nextFloat() - 0.5f) * 450f
                bot.y = curY + (Random.nextFloat() - 0.5f) * 450f
            }
        }

        if (newProgress >= 1f && state.isUserInPlane) {
            jumpFromPlane()
        } else {
            _engineState.value = state.copy(
                planeProgress = newProgress,
                planeX = curX,
                planeY = curY
            )
        }
    }

    fun jumpFromPlane() {
        synchronized(worldLock) {
            val state = _engineState.value
            if (!state.isUserInPlane) return

            userPlayer.x = state.planeX
            userPlayer.y = state.planeY
            userPlayer.z = 320f
            soundSynthesizer?.playJump()

            _engineState.value = state.copy(
                phase = MatchPhase.SKYDIVING,
                isUserInPlane = false,
                altitudeMeters = 1500f,
                isParachuteDeployed = false
            )
        }
    }

    fun deployParachute() {
        synchronized(worldLock) {
            val state = _engineState.value
            if (state.phase == MatchPhase.SKYDIVING && !state.isParachuteDeployed) {
                _engineState.value = state.copy(isParachuteDeployed = true)
                soundSynthesizer?.playJump()
            }
        }
    }

    private fun updateSkydivingPhase(deltaSec: Float, state: MatchEngineState) {
        val descentSpeed = if (state.isParachuteDeployed) 60f else 185f
        val newAlt = (state.altitudeMeters - descentSpeed * deltaSec).coerceAtLeast(0f)

        // Steer relative to camera yaw while skydiving
        val rad = Math.toRadians(cameraYawDegrees.toDouble())
        val fx = cos(rad).toFloat()
        val fy = sin(rad).toFloat()
        val rx = -fy
        val ry = fx
        val forwardIn = -moveJoystickY
        val rightIn = moveJoystickX
        val steerSpeed = if (state.isParachuteDeployed) 95f else 145f
        userPlayer.x = (userPlayer.x + (fx * forwardIn + rx * rightIn) * steerSpeed * deltaSec).coerceIn(200f, 4800f)
        userPlayer.y = (userPlayer.y + (fy * forwardIn + ry * rightIn) * steerSpeed * deltaSec).coerceIn(200f, 4800f)
        userPlayer.z = newAlt * 0.2f

        var parachuteDeployed = state.isParachuteDeployed
        if (newAlt <= 320f && !parachuteDeployed) {
            parachuteDeployed = true
        }

        if (newAlt <= 0f) {
            userPlayer.z = 0f
            userPlayer.landingRecoverySec = 0.35f
            soundSynthesizer?.playFootstep()
            safeZoneCoordinator.start()
            _engineState.value = state.copy(
                phase = MatchPhase.ACTIVE_COMBAT,
                altitudeMeters = 0f,
                isParachuteDeployed = false
            )
        } else {
            _engineState.value = state.copy(
                altitudeMeters = newAlt,
                isParachuteDeployed = parachuteDeployed
            )
        }
    }

    fun rotateCamera(deltaX: Float, deltaY: Float) {
        val sens = if (isAimingDownSights) adsSensitivity * 0.18f else cameraSensitivity * 0.28f
        cameraYawDegrees = (cameraYawDegrees + deltaX * sens) % 360f
        cameraPitchDegrees = (cameraPitchDegrees + deltaY * sens * 0.7f).coerceIn(-18f, 38f)
    }

    fun jump() {
        synchronized(worldLock) {
            if (!userPlayer.isAlive || userPlayer.vehicleId != null) return
            if (userPlayer.z <= 0.5f) {
                userPlayer.vz = 230f
                userPlayer.stance = PlayerStance.JUMPING
                // Detect low obstacle / crate nearby for vault animation
                val rad = Math.toRadians(userPlayer.angle.toDouble())
                val aheadX = userPlayer.x + cos(rad).toFloat() * 48f
                val aheadY = userPlayer.y + sin(rad).toFloat() * 48f
                val nearVaultCover = mapConfig.obstacles.any { o ->
                    val dx = aheadX - o.x; val dy = aheadY - o.y
                    o.type != com.example.game.map.ObstacleType.TREE && (dx * dx + dy * dy) < (o.radius + 26f) * (o.radius + 26f)
                }
                if (nearVaultCover) {
                    userPlayer.climbVaultTimerSec = 0.45f
                }
                soundSynthesizer?.playJump()
            }
        }
    }

    fun toggleSprintLock() {
        isSprintLocked = !isSprintLocked
        soundSynthesizer?.playUiClick()
    }

    fun deployTacticalShield() {
        synchronized(worldLock) {
            if (!userPlayer.isAlive || userPlayer.vehicleId != null || shieldCooldownSec > 0f) return
            val rad = Math.toRadians(cameraYawDegrees.toDouble())
            val sx = (userPlayer.x + cos(rad).toFloat() * 58f).coerceIn(80f, mapConfig.worldSize - 80f)
            val sy = (userPlayer.y + sin(rad).toFloat() * 58f).coerceIn(80f, mapConfig.worldSize - 80f)
            if (deployableShields.size >= 3) {
                deployableShields.removeAt(0)
            }
            deployableShields.add(
                DeployableShield(
                    ownerId = userPlayer.id,
                    x = sx,
                    y = sy,
                    angleDegrees = cameraYawDegrees
                )
            )
            shieldCooldownSec = 3.5f
            soundSynthesizer?.playShieldDeploy()
        }
    }

    fun activateDashSkill() {
        synchronized(worldLock) {
            if (!userPlayer.isAlive || dashCooldownSec > 0f) return
            dashActiveSec = 5.0f
            dashCooldownSec = 18.0f
            soundSynthesizer?.playNitroBoost()
            _engineState.value = _engineState.value.copy(
                dashActiveSec = dashActiveSec,
                dashCooldownSec = dashCooldownSec
            )
            sendQuickComms("Overdrive Dash Active!")
        }
    }

    fun activateMedicSkill() {
        synchronized(worldLock) {
            if (!userPlayer.isAlive || medicCooldownSec > 0f) return
            userPlayer.ep = (userPlayer.ep + 45f).coerceAtMost(userPlayer.maxEp)
            userPlayer.health = (userPlayer.health + 20f).coerceAtMost(userPlayer.maxHealth)
            medicCooldownSec = 15.0f
            soundSynthesizer?.playShieldDeploy()
            _engineState.value = _engineState.value.copy(
                userHealth = userPlayer.health,
                userEp = userPlayer.ep,
                medicCooldownSec = medicCooldownSec
            )
        }
    }

    fun activateRadarScan() {
        synchronized(worldLock) {
            if (!userPlayer.isAlive || scanCooldownSec > 0f) return
            radarScanActiveSec = if (activePetId == PetPreset.CYBER_PUP.id) 8.5f else 6.0f
            scanCooldownSec = 14.0f
            soundSynthesizer?.playRadioChirp()
            _engineState.value = _engineState.value.copy(
                radarScanActiveSec = radarScanActiveSec,
                scanCooldownSec = scanCooldownSec
            )
            sendQuickComms("Tactical Pulse Scanning Area!")
        }
    }

    fun spendCoinsForSupply() {
        synchronized(worldLock) {
            if (!userPlayer.isAlive || matchCoins < 300) return
            matchCoins -= 300
            userPlayer.shield = userPlayer.maxShield
            userPlayer.ep = (userPlayer.ep + 50f).coerceAtMost(userPlayer.maxEp)
            userPlayer.consumableInventory[ConsumableType.FIRST_AID] =
                (userPlayer.consumableInventory[ConsumableType.FIRST_AID] ?: 0) + 1
            userPlayer.activeWeapon?.let { w ->
                userPlayer.ammoInventory[w.ammoType] = (userPlayer.ammoInventory[w.ammoType] ?: 0) + 60
            }
            soundSynthesizer?.playReload()
            sendQuickComms("Field Supply Resupplied (-300 Coins)!")
        }
    }

    fun setAudioMuted(muted: Boolean) {
        soundSynthesizer?.setMuted(muted)
    }

    fun useQuickHeal() {
        synchronized(worldLock) {
            val priority = listOf(
                ConsumableType.MEDKIT,
                ConsumableType.FIRST_AID,
                ConsumableType.BANDAGE,
                ConsumableType.ENERGY_DRINK,
                ConsumableType.PAINKILLER
            )
            val chosen = priority.firstOrNull { (userPlayer.consumableInventory[it] ?: 0) > 0 } ?: return
            useConsumable(chosen)
        }
    }

    private fun updateCombatPhase(deltaSec: Float, state: MatchEngineState) {
        elapsedCombatSec += deltaSec
        val duration = elapsedCombatSec.toInt()
        if (radioBannerTimerSec > 0f) radioBannerTimerSec = (radioBannerTimerSec - deltaSec).coerceAtLeast(0f)
        if (takedownBannerTimerSec > 0f) takedownBannerTimerSec = (takedownBannerTimerSec - deltaSec).coerceAtLeast(0f)
        if (emoteBannerTimerSec > 0f) emoteBannerTimerSec = (emoteBannerTimerSec - deltaSec).coerceAtLeast(0f)
        if (shieldCooldownSec > 0f) shieldCooldownSec = (shieldCooldownSec - deltaSec).coerceAtLeast(0f)
        if (dashCooldownSec > 0f) dashCooldownSec = (dashCooldownSec - deltaSec).coerceAtLeast(0f)
        if (dashActiveSec > 0f) dashActiveSec = (dashActiveSec - deltaSec).coerceAtLeast(0f)
        if (medicCooldownSec > 0f) medicCooldownSec = (medicCooldownSec - deltaSec).coerceAtLeast(0f)
        if (scanCooldownSec > 0f) scanCooldownSec = (scanCooldownSec - deltaSec).coerceAtLeast(0f)
        if (radarScanActiveSec > 0f) radarScanActiveSec = (radarScanActiveSec - deltaSec).coerceAtLeast(0f)

        // Advance active ally revive if in range
        val curRevTargetId = revivingTargetId
        if (curRevTargetId != null) {
            val ally = players.find { it.id == curRevTargetId && it.isAlive && it.isKnocked }
            val inRange = ally != null && sqrt((ally.x - userPlayer.x) * (ally.x - userPlayer.x) + (ally.y - userPlayer.y) * (ally.y - userPlayer.y)) < 110f
            if (ally != null && inRange && userPlayer.isAlive) {
                reviveElapsedSec += deltaSec
                if (reviveElapsedSec >= reviveDurationSec) {
                    ally.isKnocked = false
                    ally.health = 55f
                    ally.bleedOutTimerSec = 25f
                    revivingTargetId = null
                    reviveElapsedSec = 0f
                    soundSynthesizer?.playShieldDeploy()
                    sendQuickComms("Revived ${ally.name}!")
                }
            } else {
                revivingTargetId = null
                reviveElapsedSec = 0f
            }
        }

        // 1. Update Safe Zone
        val coordSnap = safeZoneCoordinator.zoneState.value
        val updatedZone = ZoneState(
            safeCenterX = coordSnap.targetSafeCenterX,
            safeCenterY = coordSnap.targetSafeCenterY,
            safeRadius = coordSnap.targetSafeRadius,
            currentBlueCenterX = coordSnap.currentBlueCenterX,
            currentBlueCenterY = coordSnap.currentBlueCenterY,
            currentBlueRadius = coordSnap.currentBlueRadius,
            targetBlueCenterX = coordSnap.targetSafeCenterX,
            targetBlueCenterY = coordSnap.targetSafeCenterY,
            targetBlueRadius = coordSnap.targetSafeRadius,
            phase = coordSnap.phase,
            timeUntilShrinkSec = coordSnap.timeRemainingSec,
            isShrinking = coordSnap.isShrinking,
            blueDps = coordSnap.currentDps
        )

        // 2. Update User Input & Jump Physics
        updateUserPlayer(deltaSec)

        // 3. Update AI Bots
        updateBots(deltaSec, updatedZone)

        // 4. Update Vehicles
        updateVehicles(deltaSec)

        // 5. Update Projectiles, Shields & Popups
        updateProjectiles(deltaSec)
        updateShieldsAndPopups(deltaSec)

        // 6. Update Airdrops
        updateAirdrops(deltaSec)

        // 7. Zone Damage
        applyZoneDamage(deltaSec, updatedZone)

        // 8. Auto-Pickup & Nearby UI
        if (autoPickupEnabled && userPlayer.isAlive) {
            val autoGrab = groundLoot.filter { loot ->
                val dx = loot.x - userPlayer.x
                val dy = loot.y - userPlayer.y
                sqrt(dx * dx + dy * dy) < 42f && (
                    loot.ammoType != AmmoType.NONE ||
                        loot.consumable != null ||
                        loot.armorLevel > userPlayer.armorLevel ||
                        loot.helmetLevel > userPlayer.helmetLevel ||
                        (loot.weapon != null && (userPlayer.primaryWeapon1 == null || userPlayer.primaryWeapon2 == null))
                    )
            }
            autoGrab.forEach { pickUpLoot(it) }
        }

        val nearbyL = groundLoot.filter { loot ->
            val dx = loot.x - userPlayer.x
            val dy = loot.y - userPlayer.y
            sqrt(dx * dx + dy * dy) < 85f
        }
        val nearbyV = vehicles.firstOrNull { v ->
            val dx = v.x - userPlayer.x
            val dy = v.y - userPlayer.y
            sqrt(dx * dx + dy * dy) < 110f && !v.isDestroyed
        }

        val alive = players.count { it.isAlive }
        val userPlacement = if (userPlayer.isAlive) alive else (state.userPlacement.takeIf { it > 0 } ?: alive + 1)

        // Victory Check
        if (userPlayer.isAlive && alive == 1 && (gameMode == GameModeType.CLASSIC_BR || gameMode == GameModeType.OFFLINE_PRACTICE || gameMode == GameModeType.SNIPER_TRAINING)) {
            safeZoneCoordinator.stop()
            soundSynthesizer?.playVictoryFanfare()
            _engineState.value = state.copy(
                phase = MatchPhase.MATCH_ENDED,
                aliveCount = 1,
                userPlacement = 1,
                userKills = userPlayer.kills,
                userDamage = userPlayer.damageDealt,
                userHealth = userPlayer.health,
                userShield = userPlayer.shield,
                matchDurationSec = duration
            )
            onMatchFinished(1, userPlayer.kills, userPlayer.damageDealt, duration)
            return
        }

        // Defeat -> Spectating
        if (!userPlayer.isAlive && state.phase == MatchPhase.ACTIVE_COMBAT) {
            val aliveBots = players.filter { it.isAlive && it.id != userPlayer.id }
            spectatedPlayerId = aliveBots.firstOrNull()?.id
            _engineState.value = state.copy(
                phase = MatchPhase.SPECTATING,
                isSpectating = true,
                spectatedPlayer = players.find { it.id == spectatedPlayerId },
                userPlacement = userPlacement,
                userHealth = 0f,
                userShield = 0f,
                matchDurationSec = duration
            )
        }

        var currentSpectated = players.find { it.id == spectatedPlayerId }
        if (state.phase == MatchPhase.SPECTATING) {
            if (alive <= 1) {
                safeZoneCoordinator.stop()
                _engineState.value = state.copy(
                    phase = MatchPhase.MATCH_ENDED,
                    isSpectating = false,
                    aliveCount = 1,
                    userPlacement = userPlacement,
                    matchDurationSec = duration
                )
                onMatchFinished(userPlacement, userPlayer.kills, userPlayer.damageDealt, duration)
                return
            } else if (currentSpectated == null || !currentSpectated.isAlive) {
                val nextAlive = players.firstOrNull { it.isAlive && it.id != userPlayer.id }
                spectatedPlayerId = nextAlive?.id
                currentSpectated = nextAlive
            }
        }

        val reloadProgressVal = if (userPlayer.isReloading && reloadTotalSec > 0f) {
            (1f - (userPlayer.reloadTimerSec / reloadTotalSec)).coerceIn(0f, 1f)
        } else 0f

        val nearbyKnocked = if (userPlayer.isAlive) {
            players.firstOrNull { p ->
                !p.isUser && p.teamId == userPlayer.teamId && p.isAlive && p.isKnocked &&
                    sqrt((p.x - userPlayer.x) * (p.x - userPlayer.x) + (p.y - userPlayer.y) * (p.y - userPlayer.y)) < 105f
            }
        } else null

        _engineState.value = state.copy(
            aliveCount = alive,
            userKills = userPlayer.kills,
            userAssists = userPlayer.assists,
            userDamage = userPlayer.damageDealt,
            userPlacement = userPlacement,
            userHealth = userPlayer.health,
            userMaxHealth = userPlayer.maxHealth,
            userShield = userPlayer.shield,
            userEp = userPlayer.ep,
            userMaxEp = userPlayer.maxEp,
            matchCoins = matchCoins,
            squadMembers = players.filter { it.teamId == userPlayer.teamId }.take(4),
            safeZone = updatedZone,
            airdrops = airdrops.toList(),
            smokeClouds = smokeClouds.toList(),
            explosions = explosions.toList(),
            deployableShields = deployableShields.toList(),
            damagePopups = damagePopups.toList(),
            killFeed = killFeed.takeLast(6),
            nearbyLoot = nearbyL,
            nearbyVehicle = nearbyV,
            isRevivingTeammate = revivingTargetId != null,
            reviveProgress = if (revivingTargetId != null) (reviveElapsedSec / reviveDurationSec).coerceIn(0f, 1f) else 0f,
            nearbyKnockedAllyId = nearbyKnocked?.id,
            nearbyKnockedAllyName = nearbyKnocked?.name,
            matchDurationSec = duration,
            spectatedPlayer = currentSpectated,
            vehicleNitroActive = nitroDurationSec > 0f,
            vehicleSpeedKmh = currentSpeedKmh,
            radioCommsBanner = if (radioBannerTimerSec > 0f) state.radioCommsBanner else null,
            takedownBanner = if (takedownBannerTimerSec > 0f) activeTakedownText else null,
            activeEmoteText = if (emoteBannerTimerSec > 0f) activeEmoteLabel else null,
            streakCount = userPlayer.kills,
            cameraYawDegrees = cameraYawDegrees,
            cameraPitchDegrees = cameraPitchDegrees,
            isSprintLocked = isSprintLocked,
            lastHitTimestampMs = lastHitTimestampMs,
            lastHitWasHeadshot = lastHitWasHeadshot,
            dashCooldownSec = dashCooldownSec,
            dashActiveSec = dashActiveSec,
            shieldCooldownSec = shieldCooldownSec,
            medicCooldownSec = medicCooldownSec,
            scanCooldownSec = scanCooldownSec,
            radarScanActiveSec = radarScanActiveSec,
            isReloading = userPlayer.isReloading,
            reloadProgress = reloadProgressVal,
            medkitCount = totalMedkitsCount(),
            throwableCount = totalThrowableCount(),
            activePetId = activePetId,
            petVisible = petVisible,
            weatherType = weatherType,
            trainingDummyHits = trainingDummyHits,
            trainingDummyDamage = trainingDummyDamage,
            latencyMs = 22 + ((duration % 5)),
            backpackUsedUnits = computeBackpackWeight(),
            backpackMaxUnits = userPlayer.backpackLevel * 90 + 60 + (if (characterPresetId == "apex_titan") 45 else 0),
            playersSnapshot = players.toList(),
            groundLootSnapshot = groundLoot.toList(),
            vehiclesSnapshot = vehicles.toList(),
            bulletsSnapshot = bullets.toList(),
            explosiveBarrelsSnapshot = explosiveBarrels.toList()
        )
    }

    private fun updateUserPlayer(deltaSec: Float) {
        if (!userPlayer.isAlive) return

        if (userPlayer.landingRecoverySec > 0f) {
            userPlayer.landingRecoverySec = (userPlayer.landingRecoverySec - deltaSec).coerceAtLeast(0f)
        }
        if (userPlayer.climbVaultTimerSec > 0f) {
            userPlayer.climbVaultTimerSec = (userPlayer.climbVaultTimerSec - deltaSec).coerceAtLeast(0f)
        }
        if (userPlayer.emoteAnimSec > 0f) {
            userPlayer.emoteAnimSec = (userPlayer.emoteAnimSec - deltaSec).coerceAtLeast(0f)
        }

        // Vertical Jump & Gravity Physics
        if (userPlayer.z > 0f || userPlayer.vz > 0f) {
            userPlayer.z += userPlayer.vz * deltaSec
            userPlayer.vz -= 620f * deltaSec
            if (userPlayer.z <= 0f) {
                userPlayer.z = 0f
                if (userPlayer.vz < -110f) {
                    userPlayer.landingRecoverySec = 0.35f
                    soundSynthesizer?.playFootstep()
                }
                userPlayer.vz = 0f
                if (userPlayer.stance == PlayerStance.JUMPING) {
                    userPlayer.stance = PlayerStance.STAND
                }
            }
        }

        // Water Swimming Detection (bridges safely bypass river swimming)
        val inWater = userPlayer.vehicleId == null &&
            userPlayer.z <= 1.0f &&
            TerrainHeightfield.isInWaterNotOnBridge(mapConfig, userPlayer.x, userPlayer.y)
        userPlayer.isInWater = inWater
        if (inWater && userPlayer.stance != PlayerStance.SWIMMING) {
            userPlayer.stance = PlayerStance.SWIMMING
        } else if (!inWater && userPlayer.stance == PlayerStance.SWIMMING) {
            userPlayer.stance = PlayerStance.STAND
        }

        // Driving Vehicle
        if (userPlayer.vehicleId != null) {
            val v = vehicles.find { it.id == userPlayer.vehicleId }
            if (v != null && !v.isDestroyed) {
                userPlayer.x = v.x
                userPlayer.y = v.y
                userPlayer.angle = v.rotationAngle
                return
            } else {
                exitVehicle()
            }
        }

        // Camera-Relative Movement Vectors
        val rad = Math.toRadians(cameraYawDegrees.toDouble())
        val fx = cos(rad).toFloat()
        val fy = sin(rad).toFloat()
        val rx = -fy
        val ry = fx

        val forwardIn = -moveJoystickY
        val rightIn = moveJoystickX
        val moveMag = sqrt(forwardIn * forwardIn + rightIn * rightIn).coerceAtMost(1f)
        val shouldSprint = (moveMag > 0.72f || (isSprintLocked && moveMag > 0.08f)) &&
            userPlayer.stance == PlayerStance.STAND && !isAimingDownSights

        userPlayer.isSprinting = shouldSprint

        val baseSpeed = when (userPlayer.stance) {
            PlayerStance.STAND -> if (shouldSprint) 275f else 175f
            PlayerStance.CROUCH -> 135f
            PlayerStance.PRONE -> 72f
            PlayerStance.JUMPING -> 230f
            PlayerStance.SWIMMING -> 120f
            PlayerStance.DRIVING -> 0f
        }
        val dashMul = if (dashActiveSec > 0f) 1.32f else 1.0f
        val boostMultiplier = if (userPlayer.boost > 60f) 1.15f else if (userPlayer.boost > 20f) 1.08f else 1.0f
        val speed = baseSpeed * boostMultiplier * dashMul * (if (moveMag > 0.05f) moveMag.coerceAtLeast(0.45f) else 0f)
        userPlayer.currentSpeed = speed

        if (moveMag > 0.05f) {
            val worldDirX = fx * forwardIn + rx * rightIn
            val worldDirY = fy * forwardIn + ry * rightIn
            val normLen = sqrt(worldDirX * worldDirX + worldDirY * worldDirY).coerceAtLeast(0.001f)
            val vx = (worldDirX / normLen) * speed
            val vy = (worldDirY / normLen) * speed

            val targetX = userPlayer.x + vx * deltaSec
            val targetY = userPlayer.y + vy * deltaSec
            if (!isCollidingWithBuildings(targetX, userPlayer.y, 18f)) {
                userPlayer.x = targetX.coerceIn(50f, mapConfig.worldSize - 50f)
            }
            if (!isCollidingWithBuildings(userPlayer.x, targetY, 18f)) {
                userPlayer.y = targetY.coerceIn(50f, mapConfig.worldSize - 50f)
            }

            userPlayer.animWalkPhase += deltaSec * (if (shouldSprint) 11f else 7.5f)
            if (!isFiringInput && !isAimingDownSights) {
                userPlayer.angle = Math.toDegrees(atan2(vy.toDouble(), vx.toDouble())).toFloat()
            }
        } else {
            userPlayer.currentSpeed = 0f
        }

        // Determine Effective Aim Yaw (with subtle magnetic aim assist when ADS/firing)
        var targetAimYaw = cameraYawDegrees
        if ((isAimingDownSights || isFiringInput) && aimAssistEnabled) {
            val closestEnemy = players
                .filter { !it.isUser && it.isAlive }
                .mapNotNull { enemy ->
                    val dx = enemy.x - userPlayer.x
                    val dy = enemy.y - userPlayer.y
                    val dist = sqrt(dx * dx + dy * dy)
                    if (dist < 750f) {
                        val angleToEnemy = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                        var diff = (angleToEnemy - cameraYawDegrees) % 360f
                        if (diff > 180f) diff -= 360f
                        if (diff < -180f) diff += 360f
                        if (abs(diff) < 12f) angleToEnemy to dist else null
                    } else null
                }
                .minByOrNull { it.second }
            if (closestEnemy != null) {
                targetAimYaw = closestEnemy.first
            }
        }

        if (isAimingDownSights || isFiringInput || moveMag <= 0.05f) {
            userPlayer.angle = targetAimYaw
        }

        // EP & Boost Passive Health + Shield Recharge
        if (userPlayer.ep > 0f && userPlayer.health < userPlayer.maxHealth) {
            val conv = (3.5f * deltaSec).coerceAtMost(userPlayer.ep)
            userPlayer.ep = (userPlayer.ep - conv).coerceAtLeast(0f)
            userPlayer.health = (userPlayer.health + conv).coerceAtMost(userPlayer.maxHealth)
        }
        if (userPlayer.boost > 0f) {
            userPlayer.boost = (userPlayer.boost - deltaSec * 1.5f).coerceAtLeast(0f)
            if (userPlayer.health < userPlayer.maxHealth) {
                userPlayer.health = (userPlayer.health + deltaSec * 2.5f).coerceAtMost(userPlayer.maxHealth)
            }
            if (userPlayer.shield < userPlayer.maxShield) {
                userPlayer.shield = (userPlayer.shield + deltaSec * 3.5f).coerceAtMost(userPlayer.maxShield)
            }
        }

        // Reload Progress
        if (userPlayer.isReloading) {
            userPlayer.reloadTimerSec -= deltaSec
            if (userPlayer.reloadTimerSec <= 0f) {
                finishReload(userPlayer)
            }
        }

        // Weapon Fire
        if (isFiringInput && !userPlayer.isReloading) {
            val weapon = userPlayer.activeWeapon
            val now = System.currentTimeMillis()
            if (weapon != null && (now - lastUserFireTimeMs) >= weapon.effectiveFireRateMs) {
                lastUserFireTimeMs = now
                userPlayer.angle = targetAimYaw
                fireWeapon(userPlayer, targetAimYaw)
            }
        }
    }

    private fun updateBots(deltaSec: Float, safeZone: ZoneState) {
        val now = System.currentTimeMillis()
        players.filter { !it.isUser && it.isAlive }.forEach { bot ->
            if (bot.isKnocked) {
                bot.bleedOutTimerSec -= deltaSec
                if (bot.bleedOutTimerSec <= 0f) {
                    bot.isAlive = false
                    killFeed.add(KillFeedEntry("Bleed Out", bot.name, "Blue Zone", false, false))
                }
                return@forEach
            }

            // Bot jump gravity
            if (bot.z > 0f || bot.vz > 0f) {
                bot.z += bot.vz * deltaSec
                bot.vz -= 620f * deltaSec
                if (bot.z <= 0f) {
                    bot.z = 0f
                    bot.vz = 0f
                }
            }

            val distToUser = sqrt((bot.x - userPlayer.x) * (bot.x - userPlayer.x) + (bot.y - userPlayer.y) * (bot.y - userPlayer.y))
            val distToSafeCenter = sqrt((bot.x - safeZone.safeCenterX) * (bot.x - safeZone.safeCenterX) + (bot.y - safeZone.safeCenterY) * (bot.y - safeZone.safeCenterY))
            val isOutsideBlue = distToSafeCenter > safeZone.currentBlueRadius

            val isEnemyToUser = bot.teamId != userPlayer.teamId
            if (isOutsideBlue && gameMode != GameModeType.TRAINING_GROUND) {
                bot.botBehavior = BotBehavior.FLEEING_ZONE
                bot.botTargetX = safeZone.safeCenterX + (Random.nextFloat() - 0.5f) * safeZone.safeRadius * 0.4f
                bot.botTargetY = safeZone.safeCenterY + (Random.nextFloat() - 0.5f) * safeZone.safeRadius * 0.4f
            } else if (isEnemyToUser && distToUser < 600f && userPlayer.isAlive && gameMode != GameModeType.TRAINING_GROUND) {
                bot.botBehavior = BotBehavior.COMBAT_ENGAGE
                bot.botTargetPlayerId = userPlayer.id
            } else {
                if (bot.primaryWeapon1 == null) {
                    bot.botBehavior = BotBehavior.LOOTING
                    if (Random.nextFloat() < 0.08f) {
                        bot.primaryWeapon1 = WeaponCatalog.createM416()
                        bot.ammoInventory[AmmoType.AMMO_556] = 120
                    }
                } else {
                    bot.botBehavior = BotBehavior.HUNTING
                }
            }

            val diff = bot.botDifficulty
            when (bot.botBehavior) {
                BotBehavior.COMBAT_ENGAGE -> {
                    val angleToUser = Math.toDegrees(atan2((userPlayer.y - bot.y).toDouble(), (userPlayer.x - bot.x).toDouble())).toFloat()
                    bot.angle = angleToUser + (Random.nextFloat() - 0.5f) * diff.accuracyJitterDeg

                    // Tactical strafing + occasional jump on Veteran AI
                    if (distToUser > 210f) {
                        val rad = Math.toRadians(bot.angle.toDouble())
                        bot.x += (cos(rad) * 115f * diff.moveSpeedMul * deltaSec).toFloat()
                        bot.y += (sin(rad) * 115f * diff.moveSpeedMul * deltaSec).toFloat()
                    } else {
                        val strafeRad = Math.toRadians((bot.angle + 90f).toDouble())
                        bot.x += (cos(strafeRad) * 70f * diff.moveSpeedMul * deltaSec).toFloat()
                        bot.y += (sin(strafeRad) * 70f * diff.moveSpeedMul * deltaSec).toFloat()
                    }
                    bot.animWalkPhase += deltaSec * 8f

                    if (diff == BotDifficulty.VETERAN && bot.z <= 0f && Random.nextFloat() < 0.015f) {
                        bot.vz = 190f
                    }

                    if (now - bot.botLastFireTimeMs > diff.fireCooldownMs) {
                        bot.botLastFireTimeMs = now
                        if (bot.primaryWeapon1 == null) bot.primaryWeapon1 = WeaponCatalog.createAKM()
                        fireWeapon(bot, bot.angle)
                    }
                }
                BotBehavior.FLEEING_ZONE -> {
                    val angleToSafe = Math.toDegrees(atan2((bot.botTargetY - bot.y).toDouble(), (bot.botTargetX - bot.x).toDouble())).toFloat()
                    bot.angle = angleToSafe
                    val rad = Math.toRadians(bot.angle.toDouble())
                    bot.x += (cos(rad) * 200f * diff.moveSpeedMul * deltaSec).toFloat()
                    bot.y += (sin(rad) * 200f * diff.moveSpeedMul * deltaSec).toFloat()
                    bot.animWalkPhase += deltaSec * 10f
                }
                else -> {
                    if (Random.nextFloat() < 0.025f) {
                        bot.angle += (Random.nextFloat() - 0.5f) * 90f
                    }
                    val rad = Math.toRadians(bot.angle.toDouble())
                    bot.x = (bot.x + cos(rad) * 95f * deltaSec).toFloat().coerceIn(100f, 4900f)
                    bot.y = (bot.y + sin(rad) * 95f * deltaSec).toFloat().coerceIn(100f, 4900f)
                    bot.animWalkPhase += deltaSec * 6f
                }
            }
        }
    }

    private fun fireWeapon(shooter: Player, aimAngle: Float) {
        val weapon = shooter.activeWeapon ?: return

        if (weapon.maxCapacity > 0 && weapon.currentAmmo <= 0) {
            reloadWeapon(shooter)
            return
        }

        if (weapon.maxCapacity > 0) {
            weapon.currentAmmo--
        }
        shooter.lastFireTimestampMs = System.currentTimeMillis()
        val isSilenced = weapon.equippedMuzzle.isSilenced

        if (shooter.isUser) {
            soundSynthesizer?.playGunfire(weapon.name, isSilenced)
        }

        val stanceSpreadMul = when (shooter.stance) {
            PlayerStance.PRONE -> 0.55f
            PlayerStance.CROUCH -> 0.72f
            else -> 1.0f
        }
        val charRecoilMul = if (shooter.isUser && shooter.characterPresetId == "vortex_ghost") 0.90f else 1.0f
        val spread = (Random.nextFloat() - 0.5f) * weapon.effectiveRecoil * stanceSpreadMul * charRecoilMul *
            (if (isAimingDownSights && shooter.isUser) 4.0f else 9.5f)
        val finalAngle = aimAngle + spread
        val rad = Math.toRadians(finalAngle.toDouble())
        val vx = (cos(rad) * weapon.bulletSpeed).toFloat()
        val vy = (sin(rad) * weapon.bulletSpeed).toFloat()

        bullets.add(
            BulletProjectile(
                shooterId = shooter.id,
                x = shooter.x + (cos(rad) * 22f).toFloat(),
                y = shooter.y + (sin(rad) * 22f).toFloat(),
                vx = vx,
                vy = vy,
                damage = weapon.effectiveDamage,
                maxDistance = 1350f,
                isSniper = weapon.category == com.example.game.model.WeaponCategory.SNIPER_RIFLE
            )
        )
    }

    private fun updateProjectiles(deltaSec: Float) {
        val bulletIterator = bullets.iterator()
        while (bulletIterator.hasNext()) {
            val b = bulletIterator.next()
            val dx = b.vx * deltaSec
            val dy = b.vy * deltaSec
            b.x += dx
            b.y += dy
            b.distanceTraveled += sqrt(dx * dx + dy * dy)

            // Check Deployable Shield Collision
            val hitShield = deployableShields.firstOrNull { s ->
                s.ownerId != b.shooterId && sqrt((b.x - s.x) * (b.x - s.x) + (b.y - s.y) * (b.y - s.y)) < 36f
            }
            if (hitShield != null) {
                hitShield.health -= b.damage
                soundSynthesizer?.playShieldHit()
                if (hitShield.health <= 0f) {
                    deployableShields.remove(hitShield)
                }
                bulletIterator.remove()
                continue
            }

            // Check Explosive Barrel Collision
            val hitBarrel = explosiveBarrels.firstOrNull { barrel ->
                !barrel.isExploded && sqrt((b.x - barrel.x) * (b.x - barrel.x) + (b.y - barrel.y) * (b.y - barrel.y)) < 22f
            }
            if (hitBarrel != null) {
                hitBarrel.health -= b.damage
                val barrelShooter = players.find { it.id == b.shooterId }
                if (barrelShooter?.isUser == true) {
                    lastHitTimestampMs = System.currentTimeMillis()
                    lastHitWasHeadshot = hitBarrel.health <= 0f
                    damagePopups.add(
                        DamagePopup(
                            worldX = hitBarrel.x,
                            worldY = hitBarrel.y,
                            worldZ = 18f,
                            amount = b.damage.toInt(),
                            isHeadshot = hitBarrel.health <= 0f,
                            isShieldHit = false
                        )
                    )
                }
                if (hitBarrel.health <= 0f) {
                    detonateExplosiveBarrel(hitBarrel, barrelShooter)
                }
                bulletIterator.remove()
                continue
            }

            // Check Training Ground Silhouette Range Targets
            if (gameMode == GameModeType.TRAINING_GROUND && b.shooterId == userPlayer.id) {
                val rangeTargets = listOf(
                    2440f to 2360f,
                    2500f to 2360f,
                    2560f to 2360f,
                    2620f to 2360f
                )
                val hitDummy = rangeTargets.firstOrNull { (tx, ty) ->
                    sqrt((b.x - tx) * (b.x - tx) + (b.y - ty) * (b.y - ty)) < 26f
                }
                if (hitDummy != null) {
                    val dmgAmt = b.damage.toInt()
                    trainingDummyHits++
                    trainingDummyDamage += dmgAmt
                    userPlayer.damageDealt += b.damage
                    lastHitTimestampMs = System.currentTimeMillis()
                    lastHitWasHeadshot = b.isSniper
                    damagePopups.add(
                        DamagePopup(
                            worldX = hitDummy.first,
                            worldY = hitDummy.second,
                            worldZ = 22f,
                            amount = dmgAmt,
                            isHeadshot = b.isSniper,
                            isShieldHit = false
                        )
                    )
                    soundSynthesizer?.playShieldHit()
                    bulletIterator.remove()
                    continue
                }
            }

            if (isCollidingWithBuildings(b.x, b.y, 4f)) {
                bulletIterator.remove()
                continue
            }

            var hit = false
            val shooterTeam = players.find { it.id == b.shooterId }?.teamId
            for (target in players) {
                if (target.id == b.shooterId || !target.isAlive) continue
                if (shooterTeam != null && target.teamId == shooterTeam && gameMode != GameModeType.TRAINING_GROUND) continue

                val dist = sqrt((b.x - target.x) * (b.x - target.x) + (b.y - target.y) * (b.y - target.y))
                if (dist < 28f) {
                    hit = true
                    applyBulletDamage(b, target)
                    break
                }
            }

            if (hit || b.distanceTraveled >= b.maxDistance) {
                bulletIterator.remove()
            }
        }

        val throwableIterator = throwables.iterator()
        while (throwableIterator.hasNext()) {
            val t = throwableIterator.next()
            t.x += t.vx * deltaSec
            t.y += t.vy * deltaSec
            t.vx *= 0.96f
            t.vy *= 0.96f
            t.remainingFuseSec -= deltaSec

            if (t.remainingFuseSec <= 0f && !t.hasDetonated) {
                t.hasDetonated = true
                detonateThrowable(t)
                throwableIterator.remove()
            }
        }

        val smokeIterator = smokeClouds.iterator()
        while (smokeIterator.hasNext()) {
            val s = smokeIterator.next()
            s.remainingDurationSec -= deltaSec
            if (s.remainingDurationSec <= 0f) smokeIterator.remove()
        }

        val expIterator = explosions.iterator()
        while (expIterator.hasNext()) {
            val e = expIterator.next()
            e.elapsedSec += deltaSec
            if (e.elapsedSec >= e.durationSec) expIterator.remove()
        }
    }

    private fun updateShieldsAndPopups(deltaSec: Float) {
        val shieldIt = deployableShields.iterator()
        while (shieldIt.hasNext()) {
            val s = shieldIt.next()
            s.remainingSec -= deltaSec
            if (s.remainingSec <= 0f || s.health <= 0f) shieldIt.remove()
        }

        val popIt = damagePopups.iterator()
        while (popIt.hasNext()) {
            val p = popIt.next()
            p.ageSec += deltaSec
            if (p.ageSec >= p.durationSec) popIt.remove()
        }
    }

    private fun applyBulletDamage(bullet: BulletProjectile, victim: Player) {
        val isHeadshot = Random.nextFloat() < 0.28f
        val shooterObj = players.find { it.id == bullet.shooterId }
        val headMultiplier = when {
            !isHeadshot -> 1.0f
            shooterObj?.characterPresetId == "frost_recon" -> 2.52f
            else -> 2.25f
        }

        val armorReduction = when (victim.armorLevel) {
            1 -> 0.25f
            2 -> 0.35f
            3 -> 0.50f
            else -> 0f
        }
        val helmetReduction = if (isHeadshot) {
            when (victim.helmetLevel) {
                1 -> 0.25f
                2 -> 0.35f
                3 -> 0.50f
                else -> 0f
            }
        } else 0f

        val finalDamage = (bullet.damage * headMultiplier * (1f - (if (isHeadshot) helmetReduction else armorReduction))).coerceAtLeast(10f)

        var remaining = finalDamage
        var shieldHit = false
        if (victim.shield > 0f) {
            shieldHit = true
            val absorbed = remaining.coerceAtMost(victim.shield)
            victim.shield -= absorbed
            remaining -= absorbed
        }
        if (remaining > 0f) {
            victim.health = (victim.health - remaining).coerceAtLeast(0f)
        }

        val shooter = players.find { it.id == bullet.shooterId }
        if (shooter != null) {
            shooter.damageDealt += finalDamage
            damageContributors.getOrPut(victim.id) { mutableSetOf() }.add(shooter.id)
            if (shooter.isUser) {
                lastHitTimestampMs = System.currentTimeMillis()
                lastHitWasHeadshot = isHeadshot
                damagePopups.add(
                    DamagePopup(
                        worldX = victim.x + (Random.nextFloat() - 0.5f) * 14f,
                        worldY = victim.y + (Random.nextFloat() - 0.5f) * 14f,
                        worldZ = victim.z + 24f,
                        amount = finalDamage.toInt(),
                        isHeadshot = isHeadshot,
                        isShieldHit = shieldHit && victim.shield > 0f
                    )
                )
                if (isHeadshot) {
                    soundSynthesizer?.playHeadshotDing()
                } else if (shieldHit) {
                    soundSynthesizer?.playShieldHit()
                }
            }
        }

        if (victim.health <= 0f) {
            // Squadmate knockdown before full elimination when in squad play
            if (!victim.isUser && victim.teamId == userPlayer.teamId && teamSize > 1 && !victim.isKnocked) {
                victim.isKnocked = true
                victim.health = 45f
                victim.bleedOutTimerSec = 25f
                killFeed.add(KillFeedEntry(shooter?.name ?: "Enemy", victim.name, "Knockdown", isHeadshot, isKnockdown = true))
            } else {
                handlePlayerElimination(victim, shooter, isHeadshot)
            }
        }
    }

    private fun handlePlayerElimination(victim: Player, killer: Player?, isHeadshot: Boolean) {
        if (!victim.isAlive) return
        victim.health = 0f
        victim.isAlive = false
        victim.isKnocked = false

        killer?.kills = (killer?.kills ?: 0) + 1
        // Credit assist if user damaged victim earlier and another teammate/bot finished them
        val contributors = damageContributors[victim.id].orEmpty()
        if (killer != null && killer.id != userPlayer.id && userPlayer.id in contributors) {
            userPlayer.assists++
        }
        if (killer?.isUser == true) {
            matchCoins += 150
            val petEpBonus = if (activePetId == PetPreset.DRACO_FLAME.id) 15f else 0f
            killer.ep = (killer.ep + 25f + petEpBonus).coerceAtMost(killer.maxEp)
            if (killer.characterPresetId == "nova_valkyrie") {
                killer.health = (killer.health + 15f).coerceAtMost(killer.maxHealth)
            }
            val k = killer.kills
            activeTakedownText = when {
                k >= 5 -> "UNSTOPPABLE! ($k KILLS)"
                k >= 3 -> "TAKEDOWN SPREE! ($k KILLS)"
                k == 2 -> "DOUBLE KILL!"
                else -> "FIRST BLOOD!"
            }
            takedownBannerTimerSec = 2.6f
            soundSynthesizer?.playHeadshotDing()
        }

        val killerName = killer?.name ?: "Unknown"
        val weaponName = killer?.activeWeapon?.name ?: "Weapon"
        killFeed.add(KillFeedEntry(killerName, victim.name, weaponName, isHeadshot, isKnockdown = false))

        groundLoot.add(
            GroundLoot(
                x = victim.x,
                y = victim.y,
                name = "${victim.name}'s Death Crate",
                ammoType = AmmoType.AMMO_556,
                ammoCount = 90,
                armorLevel = victim.armorLevel,
                helmetLevel = victim.helmetLevel,
                consumable = ConsumableType.FIRST_AID
            )
        )
    }

    private fun detonateExplosiveBarrel(barrel: ExplosiveBarrel, shooter: Player?) {
        if (barrel.isExploded) return
        barrel.isExploded = true
        val blastRadius = 210f
        soundSynthesizer?.playExplosion()
        explosions.add(ExplosionEffect(x = barrel.x, y = barrel.y, maxRadius = blastRadius))
        players.filter { it.isAlive }.forEach { p ->
            val dist = sqrt((p.x - barrel.x) * (p.x - barrel.x) + (p.y - barrel.y) * (p.y - barrel.y))
            if (dist < blastRadius) {
                val falloff = (1f - (dist / blastRadius)).coerceAtLeast(0.25f)
                val dmg = 115f * falloff
                p.health = (p.health - dmg).coerceAtLeast(0f)
                if (shooter != null && p.id != shooter.id) {
                    shooter.damageDealt += dmg
                    if (shooter.isUser) {
                        damagePopups.add(
                            DamagePopup(
                                worldX = p.x,
                                worldY = p.y,
                                worldZ = p.z + 26f,
                                amount = dmg.toInt(),
                                isHeadshot = true,
                                isShieldHit = false
                            )
                        )
                    }
                }
                if (p.health <= 0f) {
                    handlePlayerElimination(p, shooter, true)
                }
            }
        }
    }

    private fun detonateThrowable(t: ThrowableProjectile) {
        when (t.type) {
            ThrowableType.FRAG_GRENADE -> {
                soundSynthesizer?.playExplosion()
                explosions.add(ExplosionEffect(x = t.x, y = t.y, maxRadius = t.type.blastRadius))
                players.filter { it.isAlive }.forEach { p ->
                    val dist = sqrt((p.x - t.x) * (p.x - t.x) + (p.y - t.y) * (p.y - t.y))
                    if (dist < t.type.blastRadius) {
                        val falloff = 1f - (dist / t.type.blastRadius)
                        val dmg = t.type.damage * falloff
                        p.health -= dmg
                        if (p.health <= 0f) {
                            handlePlayerElimination(p, players.find { it.id == t.throwerId }, false)
                        }
                    }
                }
            }
            ThrowableType.SMOKE_GRENADE -> {
                smokeClouds.add(SmokeCloud(x = t.x, y = t.y))
            }
            else -> {}
        }
    }

    fun throwItem() {
        synchronized(worldLock) {
            val currentType = userPlayer.activeThrowable
            val count = userPlayer.throwableInventory[currentType] ?: 0
            val chosenType = if (count > 0) {
                currentType
            } else {
                userPlayer.throwableInventory.entries.firstOrNull { it.value > 0 }?.key ?: return
            }
            userPlayer.throwableInventory[chosenType] = ((userPlayer.throwableInventory[chosenType] ?: 1) - 1).coerceAtLeast(0)

            val rad = Math.toRadians(cameraYawDegrees.toDouble())
            val vx = (cos(rad) * 450f).toFloat()
            val vy = (sin(rad) * 450f).toFloat()
            throwables.add(
                ThrowableProjectile(
                    throwerId = userPlayer.id,
                    type = chosenType,
                    x = userPlayer.x,
                    y = userPlayer.y,
                    vx = vx,
                    vy = vy,
                    remainingFuseSec = chosenType.fuseSec
                )
            )
        }
    }

    fun reloadWeapon(player: Player) {
        synchronized(worldLock) {
            val weapon = player.activeWeapon ?: return
            if (player.isReloading || weapon.maxCapacity <= 0 || weapon.currentAmmo >= weapon.totalCapacity) return

            val availableAmmo = player.ammoInventory[weapon.ammoType] ?: 0
            if (availableAmmo <= 0) return

            player.isReloading = true
            val petReloadMul = if (player.isUser && activePetId == PetPreset.VORTEX_ROBO.id) 1.15f else 1.0f
            val charReloadMul = if (player.isUser && player.characterPresetId == "seraph_echo") 1.12f else 1.0f
            val reloadDur = 1.65f / (weapon.equippedMag.reloadSpeedMultiplier * petReloadMul * charReloadMul)
            player.reloadTimerSec = reloadDur
            if (player.isUser) {
                reloadTotalSec = reloadDur
                soundSynthesizer?.playReload()
            }
        }
    }

    private fun finishReload(player: Player) {
        val weapon = player.activeWeapon ?: return
        player.isReloading = false

        val needed = weapon.totalCapacity - weapon.currentAmmo
        val available = player.ammoInventory[weapon.ammoType] ?: 0
        val toLoad = needed.coerceAtMost(available)

        weapon.currentAmmo += toLoad
        player.ammoInventory[weapon.ammoType] = available - toLoad
    }

    fun enterVehicle(vehicle: VehicleInstance) {
        synchronized(worldLock) {
            if (vehicle.driverPlayerId == null) {
                vehicle.driverPlayerId = userPlayer.id
                userPlayer.vehicleId = vehicle.id
                userPlayer.isDriver = true
                userPlayer.stance = PlayerStance.DRIVING
                soundSynthesizer?.playVehicleEngineRev()
            } else if (vehicle.passengerPlayerIds.size < vehicle.type.seats - 1) {
                vehicle.passengerPlayerIds.add(userPlayer.id)
                userPlayer.vehicleId = vehicle.id
                userPlayer.isDriver = false
                userPlayer.stance = PlayerStance.DRIVING
            }
        }
    }

    fun exitVehicle() {
        synchronized(worldLock) {
            val vid = userPlayer.vehicleId ?: return
            val v = vehicles.find { it.id == vid }
            if (v != null) {
                if (v.driverPlayerId == userPlayer.id) {
                    v.driverPlayerId = null
                } else {
                    v.passengerPlayerIds.remove(userPlayer.id)
                }
                userPlayer.x = v.x + 35f
                userPlayer.y = v.y + 35f
            }
            userPlayer.vehicleId = null
            userPlayer.isDriver = false
            userPlayer.stance = PlayerStance.STAND
            currentSpeedKmh = 0
        }
    }

    private fun updateVehicles(deltaSec: Float) {
        val isNitro = nitroDurationSec > 0f
        if (isNitro) {
            nitroDurationSec = (nitroDurationSec - deltaSec).coerceAtLeast(0f)
        }

        vehicles.forEach { v ->
            if (v.driverPlayerId == userPlayer.id && !v.isDestroyed) {
                val radCam = Math.toRadians(cameraYawDegrees.toDouble())
                val fx = cos(radCam).toFloat()
                val fy = sin(radCam).toFloat()
                val rx = -fy
                val ry = fx
                val forwardIn = -moveJoystickY
                val rightIn = moveJoystickX
                val moveMagnitude = sqrt(forwardIn * forwardIn + rightIn * rightIn)
                val accel = if (isNitro) 190f else 95f
                val maxSpd = if (isNitro) v.type.maxSpeed * 1.45f else v.type.maxSpeed

                if (moveMagnitude > 0.1f) {
                    val dirX = fx * forwardIn + rx * rightIn
                    val dirY = fy * forwardIn + ry * rightIn
                    v.rotationAngle = Math.toDegrees(atan2(dirY.toDouble(), dirX.toDouble())).toFloat()
                    v.currentSpeed = (v.currentSpeed + accel * deltaSec).coerceAtMost(maxSpd)
                } else {
                    v.currentSpeed = (v.currentSpeed - 90f * deltaSec).coerceAtLeast(0f)
                }
                currentSpeedKmh = (v.currentSpeed * 0.42f).toInt()

                val rad = Math.toRadians(v.rotationAngle.toDouble())
                v.x = (v.x + cos(rad) * v.currentSpeed * deltaSec).toFloat().coerceIn(50f, mapConfig.worldSize - 50f)
                v.y = (v.y + sin(rad) * v.currentSpeed * deltaSec).toFloat().coerceIn(50f, mapConfig.worldSize - 50f)

                players.filter { it.isAlive && it.id != userPlayer.id }.forEach { bot ->
                    val dist = sqrt((bot.x - v.x) * (bot.x - v.x) + (bot.y - v.y) * (bot.y - v.y))
                    if (dist < 45f && v.currentSpeed > 35f) {
                        bot.health = 0f
                        handlePlayerElimination(bot, userPlayer, false)
                    }
                }
            }
        }
    }

    private fun applyZoneDamage(deltaSec: Float, zone: ZoneState) {
        if (gameMode == GameModeType.TRAINING_GROUND) return
        players.filter { it.isAlive }.forEach { p ->
            val dmg = safeZoneCoordinator.calculateDamageForPlayer(p.x, p.y, deltaSec)
            if (dmg > 0f) {
                p.health -= dmg
                if (p.health <= 0f) {
                    p.health = 0f
                    p.isAlive = false
                    killFeed.add(KillFeedEntry("Electric Zone", p.name, "Blue Barrier", false, false))
                }
            }
        }
    }

    private fun updateAirdrops(deltaSec: Float) {
        if (airdrops.isEmpty() && _engineState.value.matchDurationSec > 25) {
            airdrops.add(
                AirdropCrate(
                    x = _engineState.value.safeZone.safeCenterX + (Random.nextFloat() - 0.5f) * 600f,
                    y = _engineState.value.safeZone.safeCenterY + (Random.nextFloat() - 0.5f) * 600f
                )
            )
        }

        airdrops.forEach { drop ->
            if (!drop.isLanded) {
                drop.altitude -= 45f * deltaSec
                if (drop.altitude <= 0f) {
                    drop.altitude = 0f
                    drop.isLanded = true
                    val awm = WeaponCatalog.createAWM()
                    drop.lootItems.add(GroundLoot(x = drop.x, y = drop.y, name = awm.name, weapon = awm))
                    drop.lootItems.add(GroundLoot(x = drop.x, y = drop.y, name = ".300 Magnum (20)", ammoType = AmmoType.AMMO_300MAG, ammoCount = 20))
                    drop.lootItems.add(GroundLoot(x = drop.x, y = drop.y, name = "Level 3 Military Armor", armorLevel = 3))
                    drop.lootItems.add(GroundLoot(x = drop.x, y = drop.y, name = "Level 3 Spetsnaz Helmet", helmetLevel = 3))
                }
            }
        }
    }

    fun pickUpLoot(loot: GroundLoot) {
        synchronized(worldLock) {
            if (loot.weapon != null) {
                if (userPlayer.primaryWeapon1 == null) {
                    userPlayer.primaryWeapon1 = loot.weapon
                    userPlayer.activeSlot = 1
                } else if (userPlayer.primaryWeapon2 == null) {
                    userPlayer.primaryWeapon2 = loot.weapon
                    userPlayer.activeSlot = 2
                } else {
                    if (userPlayer.activeSlot == 1) userPlayer.primaryWeapon1 = loot.weapon
                    else userPlayer.primaryWeapon2 = loot.weapon
                }
                soundSynthesizer?.playReload()
            }

            if (loot.ammoType != AmmoType.NONE && loot.ammoCount > 0) {
                val cur = userPlayer.ammoInventory[loot.ammoType] ?: 0
                userPlayer.ammoInventory[loot.ammoType] = cur + loot.ammoCount
            }

            if (loot.armorLevel > userPlayer.armorLevel) {
                userPlayer.armorLevel = loot.armorLevel
                userPlayer.shield = userPlayer.maxShield
            }

            if (loot.helmetLevel > userPlayer.helmetLevel) {
                userPlayer.helmetLevel = loot.helmetLevel
            }

            if (loot.backpackLevel > userPlayer.backpackLevel) {
                userPlayer.backpackLevel = loot.backpackLevel
            }

            if (loot.consumable != null) {
                val cur = userPlayer.consumableInventory[loot.consumable] ?: 0
                userPlayer.consumableInventory[loot.consumable] = cur + 1
            }

            if (loot.scope != null && userPlayer.activeWeapon != null) {
                userPlayer.activeWeapon?.equippedScope = loot.scope
            }

            matchCoins += 50
            groundLoot.remove(loot)
        }
    }

    fun useConsumable(type: ConsumableType) {
        synchronized(worldLock) {
            val count = userPlayer.consumableInventory[type] ?: 0
            if (count <= 0) return

            userPlayer.consumableInventory[type] = count - 1
            if (type.healAmount > 0f) {
                userPlayer.health = (userPlayer.health + type.healAmount).coerceAtMost(userPlayer.maxHealth)
            }
            if (type.boostAmount > 0f) {
                userPlayer.boost = (userPlayer.boost + type.boostAmount).coerceAtMost(100f)
                userPlayer.ep = (userPlayer.ep + type.boostAmount * 1.15f).coerceAtMost(userPlayer.maxEp)
                userPlayer.shield = (userPlayer.shield + type.boostAmount * 0.6f).coerceAtMost(userPlayer.maxShield)
            }
            soundSynthesizer?.playShieldDeploy()
        }
    }

    fun isCollidingWithBuildings(x: Float, y: Float, radius: Float): Boolean {
        for (b in mapConfig.buildings) {
            if (x + radius >= b.x && x - radius <= b.x + b.width &&
                y + radius >= b.y && y - radius <= b.y + b.height
            ) {
                return true
            }
        }
        return false
    }

    fun honkVehicleHorn() {
        soundSynthesizer?.playVehicleHorn()
    }

    fun triggerNitroBoost() {
        if (userPlayer.isDriver) {
            nitroDurationSec = 3.5f
            soundSynthesizer?.playNitroBoost()
        }
    }

    fun spectateNext() {
        synchronized(worldLock) {
            val aliveBots = players.filter { it.isAlive && it.id != userPlayer.id }
            if (aliveBots.isEmpty()) return
            val currentIndex = aliveBots.indexOfFirst { it.id == spectatedPlayerId }
            val nextIndex = if (currentIndex < 0) 0 else (currentIndex + 1) % aliveBots.size
            spectatedPlayerId = aliveBots[nextIndex].id
        }
    }

    fun spectatePrev() {
        synchronized(worldLock) {
            val aliveBots = players.filter { it.isAlive && it.id != userPlayer.id }
            if (aliveBots.isEmpty()) return
            val currentIndex = aliveBots.indexOfFirst { it.id == spectatedPlayerId }
            val prevIndex = if (currentIndex <= 0) aliveBots.size - 1 else currentIndex - 1
            spectatedPlayerId = aliveBots[prevIndex].id
        }
    }

    fun sendQuickComms(callout: String) {
        soundSynthesizer?.playRadioChirp()
        radioBannerTimerSec = 3.5f
        _engineState.value = _engineState.value.copy(
            radioCommsBanner = "${userPlayer.name}: \"$callout\""
        )
    }

    fun triggerEmote(emote: EmotePreset) {
        soundSynthesizer?.playUiClick()
        userPlayer.emoteAnimSec = 2.8f
        activeEmoteLabel = "${emote.badge} ${emote.label}"
        emoteBannerTimerSec = 2.8f
        radioBannerTimerSec = 2.8f
        _engineState.value = _engineState.value.copy(
            activeEmoteText = activeEmoteLabel,
            radioCommsBanner = "${userPlayer.name}: [${emote.badge} ${emote.label}]"
        )
    }

    fun startRevive() {
        synchronized(worldLock) {
            if (!userPlayer.isAlive) return
            val knocked = players.firstOrNull { p ->
                !p.isUser && p.teamId == userPlayer.teamId && p.isAlive && p.isKnocked &&
                    sqrt((p.x - userPlayer.x) * (p.x - userPlayer.x) + (p.y - userPlayer.y) * (p.y - userPlayer.y)) < 110f
            } ?: return
            revivingTargetId = knocked.id
            reviveElapsedSec = 0f
            soundSynthesizer?.playShieldDeploy()
        }
    }

    fun cancelRevive() {
        synchronized(worldLock) {
            revivingTargetId = null
            reviveElapsedSec = 0f
        }
    }

    fun dropWeaponSlot(slot: Int) {
        synchronized(worldLock) {
            val dropped = if (slot == 1) {
                userPlayer.primaryWeapon1.also { userPlayer.primaryWeapon1 = null }
            } else {
                userPlayer.primaryWeapon2.also { userPlayer.primaryWeapon2 = null }
            } ?: return
            groundLoot.add(
                GroundLoot(
                    x = userPlayer.x + 28f,
                    y = userPlayer.y + 18f,
                    name = dropped.name,
                    weapon = dropped
                )
            )
            if (userPlayer.activeSlot == slot) {
                userPlayer.activeSlot = if (slot == 1 && userPlayer.primaryWeapon2 != null) 2 else 1
            }
            soundSynthesizer?.playUiClick()
        }
    }

    fun dropConsumable(type: ConsumableType) {
        synchronized(worldLock) {
            val cur = userPlayer.consumableInventory[type] ?: 0
            if (cur <= 0) return
            userPlayer.consumableInventory[type] = cur - 1
            groundLoot.add(
                GroundLoot(
                    x = userPlayer.x - 24f,
                    y = userPlayer.y + 20f,
                    name = type.displayName,
                    consumable = type
                )
            )
            soundSynthesizer?.playUiClick()
        }
    }

    fun exitSpectatingToSettlement() {
        safeZoneCoordinator.stop()
        val state = _engineState.value
        _engineState.value = state.copy(
            phase = MatchPhase.MATCH_ENDED,
            isSpectating = false
        )
        onMatchFinished(state.userPlacement, userPlayer.kills, userPlayer.damageDealt, state.matchDurationSec)
    }
}
