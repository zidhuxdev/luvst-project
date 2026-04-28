import express from 'express';
import multer from 'multer';
import path from 'path';
import { v4 as uuidv4 } from 'uuid';
import fs from 'fs/promises';
import { createVoiceMessage, getVoiceMessagesByUserId, markVoiceMessageAsListened, getUserById } from '../db/index.js';

const router = express.Router();

// Configure multer for voice uploads
const storage = multer.diskStorage({
  destination: async (req, file, cb) => {
    const uploadDir = path.join(process.cwd(), 'uploads', 'voice');
    try {
      await fs.mkdir(uploadDir, { recursive: true });
      cb(null, uploadDir);
    } catch (error) {
      cb(error);
    }
  },
  filename: (req, file, cb) => {
    const uniqueName = `${uuidv4()}.webm`;
    cb(null, uniqueName);
  }
});

const upload = multer({
  storage,
  limits: { fileSize: 10 * 1024 * 1024 }, // 10MB limit for voice
  fileFilter: (req, file, cb) => {
    // Accept audio files
    const allowedTypes = /audio|webm|ogg|mp3|wav|m4a/;
    const mimetype = allowedTypes.test(file.mimetype);
    
    if (mimetype) {
      return cb(null, true);
    }
    cb(new Error('Only audio files are allowed'));
  }
});

// Upload voice message - HTTP Direct Protocol
router.post('/upload', upload.single('voice'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No voice file uploaded' });
    }
    
    const { senderId, receiverId, duration } = req.body;
    
    if (!senderId || !receiverId) {
      return res.status(400).json({ error: 'Sender and receiver IDs required' });
    }
    
    // Verify users exist
    const sender = await getUserById(senderId);
    const receiver = await getUserById(receiverId);
    
    if (!sender || !receiver) {
      return res.status(404).json({ error: 'User not found' });
    }
    
    const messageId = uuidv4();
    const fileUrl = `/uploads/voice/${req.file.filename}`;
    const durationSeconds = parseInt(duration) || 0;
    
    // Save voice message to database
    const voiceMessage = await createVoiceMessage({
      messageId,
      senderId,
      receiverId,
      url: fileUrl,
      duration: durationSeconds,
      isListened: false,
      createdAt: new Date().toISOString()
    });
    
    res.json({
      messageId,
      url: fileUrl,
      duration: durationSeconds,
      timestamp: voiceMessage.createdAt
    });
    
  } catch (error) {
    console.error('Voice upload error:', error);
    res.status(500).json({ error: 'Voice upload failed', message: error.message });
  }
});

// Get voice messages for a user
router.get('/user/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const messages = await getVoiceMessagesByUserId(userId);
    
    // Enrich with sender names
    const enrichedMessages = await Promise.all(
      messages.map(async (msg) => {
        const sender = await getUserById(msg.senderId);
        return {
          messageId: msg.messageId,
          senderId: msg.senderId,
          senderName: sender?.name || 'Unknown',
          url: msg.url,
          duration: msg.duration,
          timestamp: msg.createdAt,
          isListened: msg.isListened
        };
      })
    );
    
    res.json(enrichedMessages);
    
  } catch (error) {
    console.error('Get voice messages error:', error);
    res.status(500).json({ error: 'Failed to get voice messages' });
  }
});

// Mark voice message as listened
router.post('/:messageId/listened', async (req, res) => {
  try {
    const { messageId } = req.params;
    
    const updated = await markVoiceMessageAsListened(messageId);
    
    if (!updated) {
      return res.status(404).json({ error: 'Message not found' });
    }
    
    res.json({ success: true, messageId });
    
  } catch (error) {
    console.error('Mark listened error:', error);
    res.status(500).json({ error: 'Failed to update message' });
  }
});

// Stream voice message - HTTP Direct Protocol for playback
router.get('/stream/:messageId', async (req, res) => {
  try {
    const { messageId } = req.params;
    
    // Get message details
    const messages = await getVoiceMessagesByUserId(null, messageId);
    if (!messages || messages.length === 0) {
      return res.status(404).json({ error: 'Message not found' });
    }
    
    const message = messages[0];
    const filePath = path.join(process.cwd(), message.url.replace('/uploads/', 'uploads/'));
    
    // Check if file exists
    try {
      await fs.access(filePath);
    } catch {
      return res.status(404).json({ error: 'Audio file not found' });
    }
    
    // Stream the file with proper headers for HTTP direct protocol
    const stat = await fs.stat(filePath);
    const fileSize = stat.size;
    const range = req.headers.range;
    
    if (range) {
      // Handle range requests for seeking
      const parts = range.replace(/bytes=/, '').split('-');
      const start = parseInt(parts[0], 10);
      const end = parts[1] ? parseInt(parts[1], 10) : fileSize - 1;
      const chunksize = (end - start) + 1;
      
      res.writeHead(206, {
        'Content-Range': `bytes ${start}-${end}/${fileSize}`,
        'Accept-Ranges': 'bytes',
        'Content-Length': chunksize,
        'Content-Type': 'audio/webm',
        'Cache-Control': 'no-cache'
      });
      
      // Stream the chunk
      const file = await fs.open(filePath, 'r');
      const buffer = Buffer.alloc(chunksize);
      await file.read(buffer, 0, chunksize, start);
      await file.close();
      res.end(buffer);
      
    } else {
      // Full file stream
      res.writeHead(200, {
        'Content-Length': fileSize,
        'Content-Type': 'audio/webm',
        'Accept-Ranges': 'bytes',
        'Cache-Control': 'no-cache'
      });
      
      const file = await fs.readFile(filePath);
      res.end(file);
    }
    
  } catch (error) {
    console.error('Stream error:', error);
    res.status(500).json({ error: 'Failed to stream audio' });
  }
});

export default router;
