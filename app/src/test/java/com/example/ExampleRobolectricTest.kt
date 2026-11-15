package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.game.engine.BattleRoyaleEngine
import com.example.game.map.MapCatalog
import com.example.game.model.AmmoType
import com.example.game.model.ConsumableType
import com.example.game.model.GameModeType
import com.example.game.model.WeaponCatalog
import kotlinx.coroutines.flow.first
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Vortex Strike", appName)
    }

    @Test
    fun `test battle royale engine player count and weapons`() {
        var matchFinished = false
        val engine = BattleRoyaleEngine(
            gameMode = GameModeType.CLASSIC_BR,
            mapConfig = MapCatalog.getKavalaIsland(),
            soundSynthesizer = null,
            onMatchFinished = { _, _, _, _ -> matchFinished = true }
        )

        // 100 Players initialized
        assertEquals(100, engine.players.size)
        assertTrue(engine.userPlayer.isUser)
        assertTrue(engine.userPlayer.isAlive)

        // Check weapon catalog creation
        val m416 = WeaponCatalog.createM416()
        assertEquals("M416", m416.name)
        assertEquals(30, m416.currentAmmo)
        assertEquals(30, m416.totalCapacity)

        val awm = WeaponCatalog.createAWM()
        assertEquals("AWM", awm.name)
        assertEquals(105f, awm.baseDamage, 0.1f)
    }

    @Test
    fun `test vehicle entry and exit`() {
        val engine = BattleRoyaleEngine(
            gameMode = GameModeType.CLASSIC_BR,
            mapConfig = MapCatalog.getKavalaIsland(),
            soundSynthesizer = null,
            onMatchFinished = { _, _, _, _ -> }
        )

        val vehicle = engine.vehicles.first()
        engine.enterVehicle(vehicle)

        assertEquals(vehicle.id, engine.userPlayer.vehicleId)
        assertTrue(engine.userPlayer.isDriver)

        engine.exitVehicle()
        assertEquals(null, engine.userPlayer.vehicleId)
        assertFalse(engine.userPlayer.isDriver)
    }

    @Test
    fun `test consumable healing and boost`() {
        val engine = BattleRoyaleEngine(
            gameMode = GameModeType.CLASSIC_BR,
            mapConfig = MapCatalog.getKavalaIsland(),
            soundSynthesizer = null,
            onMatchFinished = { _, _, _, _ -> }
        )

        engine.userPlayer.health = 50f
        engine.userPlayer.consumableInventory[ConsumableType.FIRST_AID] = 2

        engine.useConsumable(ConsumableType.FIRST_AID)

        // Health should increase by 75 (capped at 100)
        assertEquals(100f, engine.userPlayer.health, 0.1f)
        assertEquals(1, engine.userPlayer.consumableInventory[ConsumableType.FIRST_AID])
    }

    @Test
    fun `test safe zone coordinator calculations and boundaries`() {
        val coordinator = com.example.game.engine.SafeZoneCoordinator(worldSize = 5000f)
        val initialSnapshot = coordinator.zoneState.value

        assertEquals(1, initialSnapshot.phase)
        assertEquals(2500f, initialSnapshot.currentBlueCenterX, 0.1f)
        assertEquals(2500f, initialSnapshot.currentBlueCenterY, 0.1f)
        assertEquals(2500f, initialSnapshot.currentBlueRadius, 0.1f)

        // Center position is safely inside
        assertFalse(coordinator.isPlayerOutside(2500f, 2500f))
        assertEquals(0f, coordinator.calculateDamageForPlayer(2500f, 2500f, 1.0f), 0.01f)

        // Out-of-bounds coordinates (outside blue radius)
        assertTrue(coordinator.isPlayerOutside(5500f, 5500f))
        val damage = coordinator.calculateDamageForPlayer(5500f, 5500f, 1.0f)
        assertTrue(damage > 0f)
    }

    @Test
    fun `test mission authoritative claim verification`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = com.example.data.GameRepository.create(context)
        repository.initializeDefaultDataIfEmpty()

        // Attempting to claim incomplete mission d_1 (0 / 3 kills) must return false
        val claimResultIncomplete = repository.claimMissionReward("d_1")
        assertFalse(claimResultIncomplete)

        // After completing match with 4 kills, d_1 should advance and allow claiming
        repository.recordMatchFinished("Classic BR", "Kavala", 1, 4, 600f, 400)
        val claimResultCompleted = repository.claimMissionReward("d_1")
        assertTrue(claimResultCompleted)

        // Attempting to claim already claimed mission must return false
        val claimResultDuplicate = repository.claimMissionReward("d_1")
        assertFalse(claimResultDuplicate)
    }

    @Test
    fun `test spectator mode and vehicle nitro trigger`() {
        val engine = BattleRoyaleEngine(
            gameMode = GameModeType.CLASSIC_BR,
            mapConfig = MapCatalog.getKavalaIsland(),
            soundSynthesizer = null,
            onMatchFinished = { _, _, _, _ -> }
        )

        // Enter vehicle and test nitro boost
        val vehicle = engine.vehicles.first()
        engine.enterVehicle(vehicle)
        engine.triggerNitroBoost()
        engine.honkVehicleHorn()

        // Test spectator cycling
        engine.spectateNext()
        engine.spectatePrev()
    }

    @Test
    fun `test daily reward cooldown, direct bundle purchase with duplicate guard, and username update`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = com.example.data.GameRepository.create(context)
        repository.initializeDefaultDataIfEmpty()

        // Daily reward succeeds once (+1500 BP) and blocks repeat claim within 24h
        val firstClaim = repository.claimDailyReward()
        assertTrue(firstClaim)
        val secondClaim = repository.claimDailyReward()
        assertFalse(secondClaim)

        // Update username persists
        repository.updateUsername("ShadowReaper")
        val updatedName = repository.userProfile.first()?.username
        assertEquals("ShadowReaper", updatedName)

        // Direct cosmetic bundle purchase succeeds and prevents duplicate purchase
        val bundle = com.example.data.InventoryItemEntity(
            id = "para_matrix_test",
            name = "Matrix Neon Parachute",
            type = "PARACHUTE",
            targetItemId = "parachute",
            rarity = "EPIC",
            priceBp = 6500
        )
        val bpBefore = repository.userProfile.first()?.bpCoins ?: 0
        val firstBuy = repository.purchaseShopItem(bundle, useCredits = false)
        assertTrue(firstBuy)
        val bpAfter = repository.userProfile.first()?.bpCoins ?: 0
        assertEquals(bpBefore - 6500, bpAfter)
        val duplicateBuy = repository.purchaseShopItem(bundle, useCredits = false)
        assertFalse(duplicateBuy)

        // Character preset and graphics quality persist
        repository.updateSelectedCharacter("cyber_viper")
        assertEquals("cyber_viper", repository.userProfile.first()?.selectedCharacterId)
        repository.updateGraphicsQuality("Ultra")
        assertEquals("Ultra", repository.settings.first()?.graphicsQuality)
    }

    @Test
    fun `test 3d tps controls, camera rotation, jump, sprint, and deployable shield`() {
        val engine = BattleRoyaleEngine(
            gameMode = GameModeType.OFFLINE_PRACTICE,
            mapConfig = MapCatalog.getKavalaIsland(),
            soundSynthesizer = null,
            teamSize = 1,
            characterPresetId = "blaze_striker",
            onMatchFinished = { _, _, _, _ -> }
        )

        assertEquals(24, engine.players.size)
        assertEquals("blaze_striker", engine.userPlayer.characterPresetId)
        assertEquals(100f, engine.userPlayer.shield, 0.1f)

        // Camera rotation updates yaw and clamps pitch
        val initialYaw = engine.cameraYawDegrees
        engine.rotateCamera(100f, 50f)
        assertTrue(engine.cameraYawDegrees != initialYaw)
        assertTrue(engine.cameraPitchDegrees in -18f..38f)

        // Jump gives upward vertical velocity
        engine.jump()
        assertTrue(engine.userPlayer.vz > 0f)

        // Sprint lock toggles
        assertFalse(engine.isSprintLocked)
        engine.toggleSprintLock()
        assertTrue(engine.isSprintLocked)

        // Deploy tactical shield spawns shield in front of user
        assertTrue(engine.deployableShields.isEmpty())
        engine.deployTacticalShield()
        assertEquals(1, engine.deployableShields.size)

        // Energy drink restores shield and boost
        engine.userPlayer.shield = 60f
        engine.userPlayer.consumableInventory[ConsumableType.ENERGY_DRINK] = 1
        engine.useConsumable(ConsumableType.ENERGY_DRINK)
        assertTrue(engine.userPlayer.shield > 60f)
        assertTrue(engine.userPlayer.boost > 0f)
        assertTrue(engine.userPlayer.ep > 100f)
    }

    @Test
    fun `test squad members, tactical skills, coins resupply, and throwable consumption`() {
        val engine = BattleRoyaleEngine(
            gameMode = GameModeType.OFFLINE_PRACTICE,
            mapConfig = MapCatalog.getKavalaIsland(),
            soundSynthesizer = null,
            teamSize = 4,
            onMatchFinished = { _, _, _, _ -> }
        )

        val initialState = engine.engineState.value
        assertEquals(4, initialState.squadMembers.size)
        assertEquals(600, initialState.matchCoins)
        assertEquals(4, initialState.throwableCount)

        // Spending coins for supply reduces coins by 300 and adds First Aid
        val firstAidBefore = engine.userPlayer.consumableInventory[ConsumableType.FIRST_AID] ?: 0
        engine.spendCoinsForSupply()
        assertEquals((engine.userPlayer.consumableInventory[ConsumableType.FIRST_AID] ?: 0), firstAidBefore + 1)

        // Throwing grenade decrements throwable inventory
        engine.throwItem()
        assertEquals(1, engine.throwables.size)
        assertEquals(2, engine.userPlayer.throwableInventory[com.example.game.model.ThrowableType.FRAG_GRENADE])

        // Tactical skills activate without errors
        engine.activateDashSkill()
        engine.activateMedicSkill()
        engine.activateRadarScan()

        // Immutable snapshots are populated safely
        assertEquals(24, initialState.playersSnapshot.size)
        assertTrue(initialState.groundLootSnapshot.isNotEmpty())
        assertTrue(initialState.vehiclesSnapshot.isNotEmpty())
    }

    @Test
    fun `test optimized webp drawables, mipmaps, themes, and maps remain intact`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val drawableIds = listOf(
            R.drawable.vortex_lobby_banner_1794457279200,
            R.drawable.vortex_theme_desert_1794458568561,
            R.drawable.vortex_theme_cyberpunk_1794458584831,
            R.drawable.vortex_theme_snow_1794458598409,
            R.drawable.vortex_hero_soldier_1794458613075,
            R.drawable.vortex_game_icon_1794457264483,
            R.mipmap.ic_launcher,
            R.mipmap.ic_launcher_round
        )
        drawableIds.forEach { resId ->
            val drawable = androidx.core.content.ContextCompat.getDrawable(context, resId)
            assertNotNull("Drawable $resId must load cleanly", drawable)
        }
        assertEquals(8, com.example.ui.theme.LobbyWallpaperTheme.entries.size)
        assertEquals(5, MapCatalog.getAllMaps().size)
    }

    @Test
    fun `test companion pet persistence, profile likes, and signature bio`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = com.example.data.GameRepository.create(context)
        repository.initializeDefaultDataIfEmpty()

        repository.updateSelectedPet(com.example.game.model.PetPreset.FROST_OWL.id)
        repository.updateSignatureBio("Booyah Champion S54!")
        val likesBefore = repository.userProfile.first()?.likesCount ?: 0
        repository.incrementProfileLikes()

        val updated = repository.userProfile.first()
        assertNotNull(updated)
        assertEquals(com.example.game.model.PetPreset.FROST_OWL.id, updated!!.selectedPetId)
        assertEquals("Booyah Champion S54!", updated.signatureBio)
        assertEquals(likesBefore + 1, updated.likesCount)
    }

    @Test
    fun `test pet passive skills and explosive barrels snapshot`() {
        val kavala = MapCatalog.getKavalaIsland()
        val generatedBarrels = MapCatalog.generateBarrels(kavala)
        assertTrue(generatedBarrels.isNotEmpty())

        val frostEngine = BattleRoyaleEngine(
            gameMode = GameModeType.OFFLINE_PRACTICE,
            mapConfig = kavala,
            soundSynthesizer = null,
            teamSize = 4,
            activePetId = com.example.game.model.PetPreset.FROST_OWL.id,
            onMatchFinished = { _, _, _, _ -> }
        )
        assertEquals(115f, frostEngine.userPlayer.maxShield, 0.1f)
        assertEquals(115f, frostEngine.userPlayer.shield, 0.1f)
        assertTrue(frostEngine.explosiveBarrels.isNotEmpty())
        assertTrue(frostEngine.engineState.value.explosiveBarrelsSnapshot.isNotEmpty())
        assertEquals(com.example.game.model.PetPreset.FROST_OWL.id, frostEngine.engineState.value.activePetId)

        val pupEngine = BattleRoyaleEngine(
            gameMode = GameModeType.OFFLINE_PRACTICE,
            mapConfig = kavala,
            soundSynthesizer = null,
            teamSize = 4,
            activePetId = com.example.game.model.PetPreset.CYBER_PUP.id,
            onMatchFinished = { _, _, _, _ -> }
        )
        pupEngine.activateRadarScan()
        assertEquals(8.5f, pupEngine.engineState.value.radarScanActiveSec, 0.1f)
    }

    @Test
    fun `test weather preset, emote trigger, drop weapon slot, and squad revive`() {
        val trainingEngine = BattleRoyaleEngine(
            gameMode = GameModeType.TRAINING_GROUND,
            mapConfig = MapCatalog.getKavalaIsland(),
            soundSynthesizer = null,
            teamSize = 1,
            weatherType = com.example.game.model.WeatherType.DENSE_FOG,
            onMatchFinished = { _, _, _, _ -> }
        )
        assertEquals(1, trainingEngine.players.size)
        assertEquals(com.example.game.model.WeatherType.DENSE_FOG, trainingEngine.engineState.value.weatherType)

        val engine = BattleRoyaleEngine(
            gameMode = GameModeType.CLASSIC_BR,
            mapConfig = MapCatalog.getKavalaIsland(),
            soundSynthesizer = null,
            teamSize = 4,
            weatherType = com.example.game.model.WeatherType.NIGHT_OPS,
            petVisible = false,
            onMatchFinished = { _, _, _, _ -> }
        )

        assertEquals(com.example.game.model.WeatherType.NIGHT_OPS, engine.engineState.value.weatherType)
        assertFalse(engine.engineState.value.petVisible)

        // Emote trigger updates state banner
        engine.triggerEmote(com.example.game.model.EmotePreset.VICTORY_DANCE)
        assertTrue(engine.engineState.value.activeEmoteText?.contains("Booyah") == true)

        // Drop weapon slot spawns ground loot
        engine.userPlayer.primaryWeapon1 = WeaponCatalog.createM416()
        val lootBefore = engine.groundLoot.size
        engine.dropWeaponSlot(1)
        assertEquals(null, engine.userPlayer.primaryWeapon1)
        assertEquals(lootBefore + 1, engine.groundLoot.size)

        // Knocked nearby teammate can be targeted by startRevive & cancelled cleanly
        val ally = engine.players.first { !it.isUser && it.teamId == engine.userPlayer.teamId }
        ally.x = engine.userPlayer.x + 20f
        ally.y = engine.userPlayer.y + 10f
        ally.isKnocked = true
        ally.health = 25f
        engine.startRevive()
        engine.cancelRevive()
    }

    @Test
    fun `test hud layout codec, pet upgrade, and claim all eligible missions`() = kotlinx.coroutines.runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val repository = com.example.data.GameRepository.create(context)
        repository.initializeDefaultDataIfEmpty()

        // HUD Layout Codec round-trip and persistence
        val defaultMap = com.example.ui.screens.HudLayoutCodec.defaultMap().toMutableMap()
        defaultMap["FIRE"] = defaultMap["FIRE"]!!.copy(offsetX = 0.12f, offsetY = -0.08f, scale = 1.25f, opacity = 0.80f)
        val json = com.example.ui.screens.HudLayoutCodec.serialize(defaultMap)
        repository.saveHudLayout(2, json)
        val savedSettings = repository.settings.first()
        assertEquals(2, savedSettings?.hudLayoutPreset)
        val parsed = com.example.ui.screens.HudLayoutCodec.parse(savedSettings?.hudLayoutJson ?: "")
        assertEquals(1.25f, parsed["FIRE"]?.scale ?: 0f, 0.01f)
        assertEquals(0.12f, parsed["FIRE"]?.offsetX ?: 0f, 0.01f)

        // Weather preference and pet upgrade
        repository.updateSelectedWeather(com.example.game.model.WeatherType.SUNSET_STORM.name)
        assertEquals("SUNSET_STORM", repository.settings.first()?.selectedWeather)
        val petLvlBefore = repository.userProfile.first()?.petLevel ?: 1
        val nextLvl = repository.upgradePetLevelLocal()
        assertEquals(petLvlBefore + 1, nextLvl)

        // Completing a match advances d_1 and claimAllEligibleMissions claims it atomically
        repository.recordMatchFinished("Classic BR", "Kavala", 1, 5, 650f, 400)
        val claimedCount = repository.claimAllEligibleMissions()
        assertTrue(claimedCount >= 1)
    }
}
