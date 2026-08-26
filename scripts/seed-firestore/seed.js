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
const usersDataUrl = new URL("./data/users.json", import.meta.url);
const rawCredits = await readFile(dataUrl, "utf8");
const rawUsers = await readFile(usersDataUrl, "utf8");
const credits = JSON.parse(rawCredits);
const users = JSON.parse(rawUsers);
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

function fullName(item) {
  return [
    item.clientFirstName,
    item.clientSecondName,
    item.clientFirstSurname,
    item.clientSecondSurname,
  ].filter(Boolean).join(" ");
}

const batch = db.batch();
const usersByDocument = new Map();
for (const user of users) {
  const document = String(user.document);
  const documentNormalized = normalize(document);
  usersByDocument.set(document, { ...user, document, documentNormalized });
  const ref = db.collection("users").doc(documentNormalized);
  batch.set(ref, {
    id: documentNormalized,
    fullName: user.fullName,
    document,
    documentNormalized,
    passwordHash: user.passwordHash,
    role: user.role,
    isActive: true,
    createdAt: now,
    updatedAt: now,
  });
}

for (const item of credits) {
  const ref = db.collection("credits").doc(item.id);
  const clientName = fullName(item);
  const salesperson = usersByDocument.get(String(item.salespersonDocument));
  if (!salesperson) {
    throw new Error(`Missing seeded salesperson ${item.salespersonDocument} for credit ${item.id}`);
  }
  batch.set(ref, {
    ...item,
    clientName,
    clientNameNormalized: normalize(clientName),
    clientDocument: String(item.clientDocument),
    clientDocumentNormalized: normalize(item.clientDocument),
    registeredByUserId: salesperson.documentNormalized,
    salespersonDocument: salesperson.document,
    salespersonDocumentNormalized: salesperson.documentNormalized,
    salespersonName: salesperson.fullName,
    salespersonNameNormalized: normalize(salesperson.fullName),
    amount: String(item.amount),
    interestRate: String(item.interestRate),
    isActive: true,
    createdAt: now,
    updatedAt: now,
    deletedAt: null,
  });
}

await batch.commit();
console.log(`Seeded ${users.length} users and ${credits.length} credits.`);
