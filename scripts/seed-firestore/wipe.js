import { initializeApp, applicationDefault, cert } from "firebase-admin/app";
import { getFirestore } from "firebase-admin/firestore";

const credentialsJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
const projectId = process.env.FIREBASE_PROJECT_ID;

initializeApp({
  credential: credentialsJson ? cert(JSON.parse(credentialsJson)) : applicationDefault(),
  projectId: projectId || undefined,
});

const db = getFirestore();

// Deletes every document in a collection, in batches (Firestore batch
// writes cap at 500 ops). Only "credits" and "email_jobs" — "users" is
// left alone, and re-running `npm run seed` afterwards recreates the
// original 10 credits + derives "clients" from them.
const COLLECTIONS_TO_WIPE = ["credits", "email_jobs"];
const BATCH_SIZE = 400;

async function wipeCollection(name) {
  const collectionRef = db.collection(name);
  let deleted = 0;
  // eslint-disable-next-line no-constant-condition
  while (true) {
    const snapshot = await collectionRef.limit(BATCH_SIZE).get();
    if (snapshot.empty) break;
    const batch = db.batch();
    for (const doc of snapshot.docs) {
      batch.delete(doc.ref);
    }
    await batch.commit();
    deleted += snapshot.size;
  }
  return deleted;
}

for (const collection of COLLECTIONS_TO_WIPE) {
  const deleted = await wipeCollection(collection);
  console.log(`Deleted ${deleted} documents from "${collection}".`);
}
