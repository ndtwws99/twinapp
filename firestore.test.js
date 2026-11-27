const {
  initializeTestEnvironment,
  assertFails,
  assertSucceeds,
} = require("@firebase/rules-unit-testing");
const { test, before, after, beforeEach } = require("node:test");
const fs = require("node:fs");

let testEnv;
const PROJECT_ID = process.env.GCP_PROJECT || "demo-no-project";
const ALICE_UID = "alice_123";
const BOB_UID = "bob_456";
const CHARLIE_UID = "charlie_789";

const [emulatorHost, emulatorPortStr] = (process.env.FIRESTORE_EMULATOR_HOST || "127.0.0.1:8085").split(":");
const emulatorPort = parseInt(emulatorPortStr, 10);

function validUserPayload(uid, name, ts) {
  return {
    uid,
    name,
    role: "member",
    avatar: "https://example.com/avatar.png",
    cover: "",
    bio: "Xin chào, mình đang dùng Twin!",
    location: "Hà Nội",
    work: "Developer",
    website: "",
    discoverable: true,
    online: true,
    friends: [],
    friendRequests: [],
    savedPosts: [],
    lastActiveAt: Date.now(),
    createdAt: ts,
    updatedAt: ts,
  };
}

function validPostPayload(authorUid, ts, privacy = "public", allowedFriendUids = []) {
  return {
    authorUid,
    authorName: "Alice Twin",
    authorAvatar: "",
    content: "Chào mừng đến với mạng xã hội Twin!",
    feeling: "Vui vẻ",
    privacy,
    allowedFriendUids,
    imageUrl: "",
    pinned: false,
    reactions: {},
    comments: [],
    savedBy: [],
    sharesCount: 0,
    createdAt: ts,
  };
}

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

test("Unauthenticated users cannot read or write any Twin collection", async () => {
  const unauthDb = testEnv.unauthenticatedContext().firestore();
  const ts = new Date(Date.now() - 1000);
  await assertFails(unauthDb.collection("posts").where("privacy", "==", "public").get());
  await assertFails(unauthDb.collection("users").where("discoverable", "==", true).get());
  await assertFails(unauthDb.collection("stories").where("privacy", "==", "public").get());
  await assertFails(unauthDb.collection("posts").doc("p1").set(validPostPayload(ALICE_UID, ts)));
});

test("Users: self create/update succeeds; role escalation, shadow fields, and PII leak fail", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  // Create self profile
  await assertSucceeds(aliceDb.collection("users").doc(ALICE_UID).set(validUserPayload(ALICE_UID, "Alice", ts)));

  // Create owner-only private PII doc
  await assertSucceeds(
    aliceDb.collection("users").doc(ALICE_UID).collection("private").doc("info").set({
      uid: ALICE_UID,
      email: "alice@example.com",
      updatedAt: ts,
    })
  );

  // Bob cannot read Alice's private PII doc
  await assertFails(bobDb.collection("users").doc(ALICE_UID).collection("private").doc("info").get());

  // Bob can query discoverable public profiles
  await assertSucceeds(bobDb.collection("users").where("discoverable", "==", true).get());

  // Bob cannot escalate role or overwrite Alice's bio
  await assertFails(bobDb.collection("users").doc(ALICE_UID).update({ bio: "Hacked" }));
  await assertFails(aliceDb.collection("users").doc(ALICE_UID).update({ role: "admin" }));

  // Shadow field rejected on create
  await assertFails(
    bobDb.collection("users").doc(BOB_UID).set({
      ...validUserPayload(BOB_UID, "Bob", ts),
      isVerifiedGhost: true,
    })
  );

  // Bob can send friend request to Alice (only touches own UID in friendRequests)
  await assertSucceeds(
    bobDb.collection("users").doc(ALICE_UID).update({
      friendRequests: [BOB_UID],
    })
  );

  // Bob cannot add Charlie to Alice's friendRequests
  await assertFails(
    bobDb.collection("users").doc(ALICE_UID).update({
      friendRequests: [BOB_UID, CHARLIE_UID],
    })
  );
});

test("Posts: Kotlin create, reaction/comment/save updates, privacy isolation, and future timestamp rejection", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  // Public post create succeeds
  await assertSucceeds(aliceDb.collection("posts").doc("post_1").set(validPostPayload(ALICE_UID, ts, "public")));

  // Future timestamp fails
  await assertFails(
    aliceDb.collection("posts").doc("post_future").set(validPostPayload(ALICE_UID, new Date(Date.now() + 60000), "public"))
  );

  // Extra @DocumentId 'id' field fails
  await assertFails(
    aliceDb.collection("posts").doc("post_bad_id").set({
      id: "post_bad_id",
      ...validPostPayload(ALICE_UID, ts, "public"),
    })
  );

  // Bob can react, comment, save, share, and query public posts
  await assertSucceeds(bobDb.collection("posts").where("privacy", "==", "public").get());
  await assertSucceeds(bobDb.collection("posts").doc("post_1").update({ reactions: { [BOB_UID]: "love" } }));
  await assertSucceeds(bobDb.collection("posts").doc("post_1").update({ savedBy: [BOB_UID] }));
  await assertSucceeds(bobDb.collection("posts").doc("post_1").update({ sharesCount: 1 }));

  // Bob cannot edit Alice's post content or delete Alice's post
  await assertFails(bobDb.collection("posts").doc("post_1").update({ content: "Edited by Bob" }));
  await assertFails(bobDb.collection("posts").doc("post_1").delete());

  // Friends-only post visible to Bob (allowed) but NOT Charlie
  await assertSucceeds(
    aliceDb.collection("posts").doc("post_friends").set(validPostPayload(ALICE_UID, ts, "friends", [BOB_UID]))
  );
  await assertSucceeds(bobDb.collection("posts").doc("post_friends").get());
  await assertFails(charlieDb.collection("posts").doc("post_friends").get());

  // Alice can delete own post
  await assertSucceeds(aliceDb.collection("posts").doc("post_1").delete());
});

test("Stories, Groups, Pages, Notifications, and Twin Chat messages isolation", async () => {
  const aliceDb = testEnv.authenticatedContext(ALICE_UID).firestore();
  const bobDb = testEnv.authenticatedContext(BOB_UID).firestore();
  const charlieDb = testEnv.authenticatedContext(CHARLIE_UID).firestore();
  const ts = new Date(Date.now() - 5000);

  // Story
  await assertSucceeds(
    aliceDb.collection("stories").doc("st_1").set({
      type: "text",
      authorUid: ALICE_UID,
      authorName: "Alice",
      authorAvatar: "",
      text: "Hello Twin 24h!",
      bgId: "ig",
      imageUrl: "",
      caption: "",
      privacy: "public",
      likes: [],
      sharesCount: 0,
      createdAt: ts,
    })
  );
  await assertSucceeds(bobDb.collection("stories").where("privacy", "==", "public").get());
  await assertSucceeds(bobDb.collection("stories").doc("st_1").update({ likes: [BOB_UID] }));
  await assertFails(bobDb.collection("stories").doc("st_1").delete());

  // Group & Page
  await assertSucceeds(
    aliceDb.collection("groups").doc("grp_1").set({
      name: "Android Twin Dev",
      description: "Cộng đồng Twin",
      privacy: "public",
      coverUrl: "",
      ownerUid: ALICE_UID,
      members: [ALICE_UID],
      pendingMembers: [],
      createdAt: ts,
    })
  );
  await assertSucceeds(bobDb.collection("groups").where("privacy", "==", "public").get());
  await assertSucceeds(bobDb.collection("groups").doc("grp_1").update({ members: [ALICE_UID, BOB_UID] }));

  await assertSucceeds(
    aliceDb.collection("pages").doc("pg_1").set({
      name: "Twin Official",
      category: "Công nghệ",
      description: "Trang chính thức",
      coverUrl: "",
      visibility: "public",
      ownerUid: ALICE_UID,
      followers: [ALICE_UID],
      createdAt: ts,
    })
  );
  await assertSucceeds(bobDb.collection("pages").where("visibility", "==", "public").get());
  await assertSucceeds(bobDb.collection("pages").doc("pg_1").update({ followers: [ALICE_UID, BOB_UID] }));

  // Notifications: Bob sends to Alice; only Alice can read & mark read; Charlie denied
  await assertSucceeds(
    bobDb.collection("notifications").doc("n_1").set({
      toUid: ALICE_UID,
      fromUid: BOB_UID,
      fromName: "Bob",
      fromAvatar: "",
      type: "reaction",
      message: "Bob đã thích bài viết của bạn",
      read: false,
      createdAt: ts,
    })
  );
  await assertSucceeds(aliceDb.collection("notifications").where("toUid", "==", ALICE_UID).get());
  await assertSucceeds(aliceDb.collection("notifications").doc("n_1").update({ read: true }));
  await assertFails(charlieDb.collection("notifications").doc("n_1").get());
  await assertFails(charlieDb.collection("notifications").where("toUid", "==", ALICE_UID).get());

  // Twin Chat: Alice <-> Bob private chatId = alice_123_bob_456
  const chatId = `${ALICE_UID}_${BOB_UID}`;
  await assertSucceeds(
    aliceDb.collection("messages").doc(chatId).collection("items").doc("m_1").set({
      senderUid: ALICE_UID,
      senderName: "Alice",
      memberUids: [ALICE_UID, BOB_UID],
      text: "Chào Bob trên Twin Chat!",
      imageUrl: "",
      createdAt: ts,
    })
  );
  await assertSucceeds(
    bobDb.collection("messages").doc(chatId).collection("items").where("memberUids", "array-contains", BOB_UID).get()
  );
  // Charlie cannot read or write Alice-Bob chat
  await assertFails(
    charlieDb.collection("messages").doc(chatId).collection("items").doc("m_1").get()
  );
  await assertFails(
    charlieDb.collection("messages").doc(chatId).collection("items").doc("m_2").set({
      senderUid: CHARLIE_UID,
      senderName: "Charlie",
      memberUids: [ALICE_UID, BOB_UID],
      text: "Intrusion",
      imageUrl: "",
      createdAt: ts,
    })
  );
});
