package com.example.data.cloud

import com.example.base.FirestoreEmulatorTestBase
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.LooperMode

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
@LooperMode(LooperMode.Mode.INSTRUMENTATION_TEST)
class CloudGameRepositoryRuleTest : FirestoreEmulatorTestBase() {

    private lateinit var repository: CloudGameRepository

    @Before
    fun initRepository() {
        repository = CloudGameRepository(db, auth)
    }

    @Test
    fun `ensureUserInitialized seeds profile, missions, mail, and purchaseDirectBundle deducts currency cleanly`() =
        runTest {
            val uid = signInTestUser("player_one@vortex.test")

            val initResult = repository.ensureUserInitialized("VortexTester")
            assertTrue("ensureUserInitialized failed: ${initResult.exceptionOrNull()}", initResult.isSuccess)

            val profile = repository.observeProfile(uid).first { it != null }
            assertNotNull(profile)
            assertEquals("VortexTester", profile!!.username)
            assertEquals(12000L, profile.bpCoins)
            assertEquals(650L, profile.vortexCredits)

            // Purchase fixed-price bundle succeeds and blocks duplicate purchase
            val buy1 = repository.purchaseDirectBundle(
                itemId = "bundle_m416_glacier",
                itemName = "M416 Glacier Frost",
                type = "WEAPON_SKIN",
                targetItemId = "m416",
                rarity = "MYTHIC",
                priceBp = 2500L,
                priceCredits = 150L
            )
            assertTrue("First purchase failed: ${buy1.exceptionOrNull()}", buy1.isSuccess)

            val duplicateBuy = repository.purchaseDirectBundle(
                itemId = "bundle_m416_glacier",
                itemName = "M416 Glacier Frost",
                type = "WEAPON_SKIN",
                targetItemId = "m416",
                rarity = "MYTHIC",
                priceBp = 2500L,
                priceCredits = 150L
            )
            assertTrue("Duplicate purchase should be blocked", duplicateBuy.isFailure)

            val updatedProfile = repository.observeProfile(uid).first { it != null && it.bpCoins == 9500L }
            assertEquals(500L, updatedProfile!!.vortexCredits)

            val txList = repository.observeTransactions(uid).first { it.isNotEmpty() }
            assertEquals("PURCHASE", txList.first().txType)
            assertEquals(-2500L, txList.first().bpDelta)
        }

    @Test
    fun `verified mission claim fails before target and succeeds after match completion`() =
        runTest {
            val uid = signInTestUser("mission_runner@vortex.test")
            assertTrue(repository.ensureUserInitialized("MissionAce").isSuccess)

            val missionsBefore = repository.observeMissions(uid).first { it.isNotEmpty() }
            val firstStrike = missionsBefore.first { it.id == "m_daily_1" }
            assertEquals(0L, firstStrike.currentProgress)

            // Premature claim must fail
            val prematureClaim = repository.claimVerifiedMission(firstStrike)
            assertTrue("Premature mission claim must fail", prematureClaim.isFailure)

            // Record a match with 4 kills to advance m_daily_1 (targetCount = 3)
            val matchRes = repository.recordCloudMatch(
                gameMode = "Classic BR",
                mapName = "Kavala Island",
                placement = 1,
                kills = 4,
                assists = 2,
                damage = 520f,
                durationSec = 310
            )
            assertTrue("recordCloudMatch failed: ${matchRes.exceptionOrNull()}", matchRes.isSuccess)

            val missionsAfter = repository.observeMissions(uid).first { list ->
                list.any { it.id == "m_daily_1" && it.currentProgress >= 3L }
            }
            val completedMission = missionsAfter.first { it.id == "m_daily_1" }
            assertEquals(3L, completedMission.currentProgress)

            val validClaim = repository.claimVerifiedMission(completedMission)
            assertTrue("Verified mission claim failed: ${validClaim.exceptionOrNull()}", validClaim.isSuccess)

            // Repeat claim must fail
            val repeatClaim = repository.claimVerifiedMission(completedMission.copy(isClaimed = true))
            assertTrue(repeatClaim.isFailure)
        }

    @Test
    fun `matchRooms private isolation and guild creation with chat rules succeed`() =
        runTest {
            val aliceUid = signInTestUser("alice_room@vortex.test")
            assertTrue(repository.ensureUserInitialized("AliceLead").isSuccess)

            val pubRoom = repository.createMatchRoom(
                title = "Open Squad",
                gameMode = "CLASSIC_BR",
                mapId = "kavala_island",
                teamSize = 4,
                maxPlayers = 100,
                isPrivate = false,
                weather = "CLEAR"
            )
            assertTrue(pubRoom.isSuccess)

            val privRoom = repository.createMatchRoom(
                title = "Secret Scrim",
                gameMode = "CUSTOM_ROOM",
                mapId = "kavala_island",
                teamSize = 4,
                maxPlayers = 100,
                isPrivate = true,
                weather = "SUNSET_STORM"
            )
            assertTrue(privRoom.isSuccess)

            // Create guild and post chat message
            val guildRes = repository.createGuild("Vortex Elite", "VTX", "Top squad")
            assertTrue("createGuild failed: ${guildRes.exceptionOrNull()}", guildRes.isSuccess)
            val guilds = repository.observeGuilds().first { it.isNotEmpty() }
            val myGuild = guilds.first { it.leaderUid == aliceUid }
            assertTrue(repository.sendGuildMessage(myGuild, "AliceLead", "Squad ready!").isSuccess)

            // Bob signs in: sees only public room, not Alice's private room
            signInTestUser("bob_guest@vortex.test")
            assertTrue(repository.ensureUserInitialized("BobGuest").isSuccess)
            val publicRooms = repository.observePublicMatchRooms().first { it.isNotEmpty() }
            assertFalse(publicRooms.any { it.isPrivate })
            assertTrue(publicRooms.any { it.title == "Open Squad" })

            // Signed out reads fail with PERMISSION_DENIED
            signOutTestUser()
            val unauthInit = repository.ensureUserInitialized("Nobody")
            assertTrue(unauthInit.isFailure)
        }
}
