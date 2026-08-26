import { readFile } from "node:fs/promises";
import { initializeApp, applicationDefault, cert } from "firebase-admin/app";
import { getFirestore, Timestamp } from "firebase-admin/firestore";

const credentialsJson = process.env.FIREBASE_SERVICE_ACCOUNT_JSON;
const projectId = process.env.FIREBASE_PROJECT_ID;

initializeApp({
  credential: credentialsJson ? cert(JSON.parse(credentialsJson)) : applicationDefault(),
  projectId: projectId || undefined,
});

const dataUrl = new URL("./data/credits.json", import.meta.url);
const raw = await readFile(dataUrl, "utf8");
const credits = JSON.parse(raw);
const db = getFirestore();
const now = Timestamp.now();

function normalize(value) {
  return String(value ?? "")
    .trim()
    .replace(/\s+/g, " ")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .toLowerCase();
}

const batch = db.batch();
for (const item of credits) {
  const ref = db.collection("credits").doc(item.id);
  batch.set(ref, {
    ...item,
    clientNameNormalized: normalize(item.clientName),
    clientDocumentNormalized: normalize(item.clientDocument),
    salespersonNameNormalized: normalize(item.salespersonName),
    amount: String(item.amount),
    interestRate: String(item.interestRate),
    isActive: true,
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
  });
}

await batch.commit();
console.log(`Seeded ${credits.length} credits.`);
