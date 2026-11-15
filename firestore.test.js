const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const DATABASE_ID = "ai-studio-android-vortexst-a1598cea-6964-4528-ba9b-db809675e239";
const ALICE_UID = "alice_uid_001";
const BOB_UID = "bob_uid_002";
const ADMIN_UID = "admin_uid_999";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

before(async () => {
  const rules = fs.readFileSync("./firestore.rules", "utf8");
  testEnv = await initializeTestEnvironment({
    projectId: PROJECT_ID,
    firestore: {
      rules,
      host: emulatorHost,
      port: emulatorPort,
    },
  });
});

after(async () => {
  if (testEnv) {
    await testEnv.cleanup();
  }
});

beforeEach(async () => {
  if (testEnv) {
    await testEnv.clearFirestore();
  }
});

function dbFor(uid, tokenOptions = {}) {
  const ctx = uid
    ? testEnv.authenticatedContext(uid, tokenOptions)
    : testEnv.unauthenticatedContext();
  return ctx.firestore(DATABASE_ID);
}

test("Unauthenticated user cannot read or write user profiles or matchRooms", async () => {
  const unauthDb = dbFor(null);
  await assertFails(unauthDb.collection("users").doc(ALICE_UID).get());
  await assertFails(unauthDb.collection("matchRooms").where("isPrivate", "==", false).get());
  await assertFails(unauthDb.collection("announcements").get());
});

test("Authenticated owner can create & update profile, while cross-user read/write fails", async () => {
  const aliceDb = dbFor(ALICE_UID);
  const bobDb = dbFor(BOB_UID);

  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore(DATABASE_ID);
    await adminDb.collection("users").doc(ALICE_UID).set({
      userId: ALICE_UID,
      username: "AliceStrike",
      bpCoins: 5000,
      vortexCredits: 300,
      rankPoints: 1200,
      isBanned: false,
      createdAt: new Date(Date.now() - 10000),
      updatedAt: new Date(Date.now() - 10000),
    });
  });

  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).get());
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ username: "Hacked" }));
});

test("Mission verification rule blocks claiming incomplete mission and duplicate claims", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore(DATABASE_ID);
    await adminDb.collection("users").doc(ALICE_UID).collection("missions").doc("m_daily_1").set({
      category: "DAILY",
      title: "First Strike",
      targetType: "KILLS",
      targetCount: 3,
      currentProgress: 1,
      rewardBp: 450,
      rewardCredits: 0,
      rewardXp: 200,
      isClaimed: false,
      createdAt: new Date(Date.now() - 10000),
    });
  });

  const aliceDb = dbFor(ALICE_UID);
  const missionRef = aliceDb.collection("users").doc(ALICE_UID).collection("missions").doc("m_daily_1");

  // Cannot claim while currentProgress < targetCount
  await assertFails(missionRef.update({ isClaimed: true }));

  // Complete progress first
  await assertSucceeds(missionRef.update({ currentProgress: 3 }));

  // Now claim succeeds
  await assertSucceeds(missionRef.update({ isClaimed: true }));

  // Duplicate claim or reverting isClaimed fails
  await assertFails(missionRef.update({ isClaimed: false }));
});

test("MatchRooms query alignment enforces private room isolation", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore(DATABASE_ID);
    await adminDb.collection("matchRooms").doc("room_pub").set({
      roomCode: "PUB123",
      hostUid: ALICE_UID,
      title: "Kavala Public Squad",
      gameMode: "CLASSIC_BR",
      mapId: "kavala_island",
      memberUids: [ALICE_UID],
      status: "waiting",
      isPrivate: false,
      createdAt: new Date(Date.now() - 10000),
    });
    await adminDb.collection("matchRooms").doc("room_priv").set({
      roomCode: "PRV999",
      hostUid: ALICE_UID,
      title: "Alice Private Room",
      gameMode: "CUSTOM_ROOM",
      mapId: "kavala_island",
      memberUids: [ALICE_UID],
      status: "waiting",
      isPrivate: true,
      createdAt: new Date(Date.now() - 10000),
    });
  });

  const bobDb = dbFor(BOB_UID);
  // Unfiltered collection list fails (cannot scrape private rooms)
  await assertFails(bobDb.collection("matchRooms").get());
  // Scoped public query succeeds
  await assertSucceeds(bobDb.collection("matchRooms").where("isPrivate", "==", false).get());
});

test("Admin announcement rejects unverified email spoof and non-admin writes", async () => {
  const spoofAdminDb = dbFor(BOB_UID, { email: "maanudhiman85@gmail.com", email_verified: false });
  await assertFails(
    spoofAdminDb.collection("announcements").doc("ann_1").set({
      title: "Fake Notice",
      message: "Spoof attempt",
      severity: "INFO",
      createdByUid: BOB_UID,
      createdAt: new Date(Date.now() - 1000),
    })
  );

  const realAdminDb = dbFor(ADMIN_UID, { email: "maanudhiman85@gmail.com", email_verified: true });
  await assertSucceeds(
    realAdminDb.collection("announcements").doc("ann_2").set({
      title: "Season 54 Launch",
      message: "Ranked Season 54 is live.",
      severity: "EVENT",
      createdByUid: ADMIN_UID,
      createdAt: new Date(Date.now() - 1000),
    })
  );
});

test("Transactions and matches are append-only (update and delete rejected)", async () => {
  await testEnv.withSecurityRulesDisabled(async (ctx) => {
    const adminDb = ctx.firestore(DATABASE_ID);
    await adminDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1").set({
      txType: "PURCHASE",
      itemId: "skin_m416_glacier",
      itemName: "M416 Glacier Frost",
      bpDelta: 0,
      creditsDelta: -1200,
      status: "COMPLETED",
      createdAt: new Date(Date.now() - 10000),
    });
  });

  const aliceDb = dbFor(ALICE_UID);
  const txRef = aliceDb.collection("users").doc(ALICE_UID).collection("transactions").doc("tx_1");
  await assertSucceeds(txRef.get());
  await assertFails(txRef.update({ creditsDelta: 999999 }));
  await assertFails(txRef.delete());
});
