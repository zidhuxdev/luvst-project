import fs from 'fs/promises';
import path from 'path';
import { fileURLToPath } from 'url';

const __filename = fileURLToPath(import.meta.url);
const __dirname = path.dirname(__filename);

const DATA_DIR = path.join(__dirname, 'data');
const USERS_FILE = path.join(DATA_DIR, 'users.json');
const CONNECTIONS_FILE = path.join(DATA_DIR, 'connections.json');
const MEDIA_FILE = path.join(DATA_DIR, 'media.json');
const VOICE_FILE = path.join(DATA_DIR, 'voice.json');

// Ensure data directory exists
async function ensureDataDir() {
  try {
    await fs.mkdir(DATA_DIR, { recursive: true });
  } catch (error) {
    console.error('Error creating data directory:', error);
  }
}

// Initialize files if they don't exist
async function initFile(filePath, defaultData = []) {
  try {
    await fs.access(filePath);
  } catch {
    await fs.writeFile(filePath, JSON.stringify(defaultData, null, 2));
  }
}

async function init() {
  await ensureDataDir();
  await initFile(USERS_FILE, []);
  await initFile(CONNECTIONS_FILE, []);
  await initFile(MEDIA_FILE, []);
  await initFile(VOICE_FILE, []);
}

// Read data from file
async function readData(filePath) {
  try {
    const data = await fs.readFile(filePath, 'utf8');
    return JSON.parse(data);
  } catch {
    return [];
  }
}

// Write data to file
async function writeData(filePath, data) {
  await fs.writeFile(filePath, JSON.stringify(data, null, 2));
}

// User functions
export async function getUserById(userId) {
  const users = await readData(USERS_FILE);
  return users.find(u => u.userId === userId);
}

export async function getUserByEmail(email) {
  const users = await readData(USERS_FILE);
  return users.find(u => u.email.toLowerCase() === email.toLowerCase());
}

export async function getUserByUsername(username) {
  const users = await readData(USERS_FILE);
  return users.find(u => u.username.toLowerCase() === username.toLowerCase());
}

export async function createUser(userData) {
  const users = await readData(USERS_FILE);
  users.push(userData);
  await writeData(USERS_FILE, users);
  return userData;
}

export async function updateUser(userId, updates) {
  const users = await readData(USERS_FILE);
  const index = users.findIndex(u => u.userId === userId);
  if (index !== -1) {
    users[index] = { ...users[index], ...updates };
    await writeData(USERS_FILE, users);
    return users[index];
  }
  return null;
}

export async function getPartnerByUserId(userId) {
  const user = await getUserById(userId);
  if (!user || !user.partnerId) return null;
  return getUserById(user.partnerId);
}

// Connection functions
export async function createConnectionRequest(data) {
  const connections = await readData(CONNECTIONS_FILE);
  connections.push(data);
  await writeData(CONNECTIONS_FILE, connections);
  return data;
}

export async function getConnectionRequestById(requestId) {
  const connections = await readData(CONNECTIONS_FILE);
  return connections.find(c => c.requestId === requestId);
}

export async function getPendingRequestForUser(fromUserId, toUsername) {
  const connections = await readData(CONNECTIONS_FILE);
  return connections.find(c => 
    c.fromUserId === fromUserId && 
    c.toUsername.toLowerCase() === toUsername.toLowerCase() &&
    c.status === 'pending'
  );
}

export async function acceptConnectionRequest(requestId, acceptedByUserId) {
  const connections = await readData(CONNECTIONS_FILE);
  const index = connections.findIndex(c => c.requestId === requestId);
  if (index !== -1) {
    connections[index].status = 'accepted';
    connections[index].acceptedBy = acceptedByUserId;
    connections[index].acceptedAt = new Date().toISOString();
    await writeData(CONNECTIONS_FILE, connections);
    return connections[index];
  }
  return null;
}

// Media functions
export async function createMedia(mediaData) {
  const media = await readData(MEDIA_FILE);
  media.push(mediaData);
  await writeData(MEDIA_FILE, media);
  return mediaData;
}

export async function getMediaByUserId(userId) {
  const media = await readData(MEDIA_FILE);
  return media
    .filter(m => m.senderId === userId)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .map(m => ({
      id: m.mediaId,
      url: m.url,
      thumbnailUrl: m.thumbnailUrl,
      type: m.type,
      senderId: m.senderId,
      timestamp: new Date(m.createdAt).getTime(),
      partnerRating: m.partnerRating || 0,
      pointsEarned: m.points || 0
    }));
}

export async function getReceivedMedia(userId) {
  const media = await readData(MEDIA_FILE);
  return media
    .filter(m => m.receiverId === userId)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt))
    .map(m => ({
      id: m.mediaId,
      url: m.url,
      thumbnailUrl: m.thumbnailUrl,
      type: m.type,
      senderId: m.senderId,
      timestamp: new Date(m.createdAt).getTime(),
      partnerRating: m.partnerRating || 0,
      pointsEarned: m.points || 0
    }));
}

export async function updateMediaRating(mediaId, raterId, rating) {
  const media = await readData(MEDIA_FILE);
  const index = media.findIndex(m => m.mediaId === mediaId);
  if (index !== -1) {
    media[index].partnerRating = rating;
    media[index].ratedBy = raterId;
    media[index].ratedAt = new Date().toISOString();
    await writeData(MEDIA_FILE, media);
    return media[index];
  }
  return null;
}

// Voice message functions
export async function createVoiceMessage(data) {
  const messages = await readData(VOICE_FILE);
  messages.push(data);
  await writeData(VOICE_FILE, messages);
  return data;
}

export async function getVoiceMessagesByUserId(userId, messageId = null) {
  const messages = await readData(VOICE_FILE);
  
  if (messageId) {
    return messages.filter(m => m.messageId === messageId);
  }
  
  return messages
    .filter(m => m.receiverId === userId)
    .sort((a, b) => new Date(b.createdAt) - new Date(a.createdAt));
}

export async function markVoiceMessageAsListened(messageId) {
  const messages = await readData(VOICE_FILE);
  const index = messages.findIndex(m => m.messageId === messageId);
  if (index !== -1) {
    messages[index].isListened = true;
    messages[index].listenedAt = new Date().toISOString();
    await writeData(VOICE_FILE, messages);
    return messages[index];
  }
  return null;
}

// Initialize on module load
init().catch(console.error);
