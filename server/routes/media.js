import express from 'express';
import multer from 'multer';
import path from 'path';
import { v4 as uuidv4 } from 'uuid';
import sharp from 'sharp';
import fs from 'fs/promises';
import { createMedia, getMediaByUserId, getReceivedMedia, updateMediaRating, getUserById } from '../db/index.js';

const router = express.Router();

// Configure multer for file uploads
const storage = multer.diskStorage({
  destination: async (req, file, cb) => {
    const uploadDir = path.join(process.cwd(), 'uploads', 'media');
    try {
      await fs.mkdir(uploadDir, { recursive: true });
      cb(null, uploadDir);
    } catch (error) {
      cb(error);
    }
  },
  filename: (req, file, cb) => {
    const uniqueName = `${uuidv4()}${path.extname(file.originalname)}`;
    cb(null, uniqueName);
  }
});

const upload = multer({
  storage,
  limits: { fileSize: 50 * 1024 * 1024 }, // 50MB limit
  fileFilter: (req, file, cb) => {
    const allowedTypes = /jpeg|jpg|png|gif|webp|mp4|mov|heic/;
    const extname = allowedTypes.test(path.extname(file.originalname).toLowerCase());
    const mimetype = allowedTypes.test(file.mimetype);
    
    if (extname && mimetype) {
      return cb(null, true);
    }
    cb(new Error('Only images and videos are allowed'));
  }
});

// Upload media
router.post('/upload', upload.single('media'), async (req, res) => {
  try {
    if (!req.file) {
      return res.status(400).json({ error: 'No file uploaded' });
    }
    
    const { senderId, type } = req.body;
    if (!senderId) {
      return res.status(400).json({ error: 'Sender ID required' });
    }
    
    const sender = await getUserById(senderId);
    if (!sender) {
      return res.status(404).json({ error: 'Sender not found' });
    }
    
    if (!sender.partnerId) {
      return res.status(400).json({ error: 'No partner connected' });
    }
    
    const mediaId = uuidv4();
    const filePath = req.file.path;
    const originalName = req.file.originalname;
    const mimeType = req.file.mimetype;
    const isVideo = mimeType.startsWith('video');
    
    let thumbnailUrl = null;
    let finalUrl = `/uploads/media/${req.file.filename}`;
    let compressionInfo = null;
    
    // Server-side optimization for images
    if (!isVideo && mimeType.startsWith('image')) {
      try {
        const originalStats = await fs.stat(filePath);
        const originalSize = originalStats.size;
        
        // Process with sharp
        const processedPath = filePath.replace(path.extname(filePath), '_processed.jpg');
        const image = sharp(filePath);
        const metadata = await image.metadata();
        
        // Resize if too large
        let pipeline = image
          .resize(1920, 1920, { fit: 'inside', withoutEnlargement: true })
          .jpeg({ quality: 85, progressive: true });
        
        await pipeline.toFile(processedPath);
        
        // Replace original with processed
        await fs.unlink(filePath);
        await fs.rename(processedPath, filePath);
        
        const processedStats = await fs.stat(filePath);
        const savedPercentage = Math.round(((originalSize - processedStats.size) / originalSize) * 100);
        
        compressionInfo = {
          originalSize,
          compressedSize: processedStats.size,
          originalDimensions: { width: metadata.width, height: metadata.height },
          compressedDimensions: { width: metadata.width, height: metadata.height },
          format: 'JPEG',
          quality: 85,
          savedPercentage: Math.max(0, savedPercentage)
        };
        
        // Create thumbnail
        const thumbnailName = `${uuidv4()}_thumb.jpg`;
        const thumbnailPath = path.join(path.dirname(filePath), thumbnailName);
        await sharp(filePath)
          .resize(400, 400, { fit: 'cover' })
          .jpeg({ quality: 70 })
          .toFile(thumbnailPath);
        
        thumbnailUrl = `/uploads/media/${thumbnailName}`;
        
      } catch (processError) {
        console.error('Image processing error:', processError);
        // Continue with original file if processing fails
      }
    }
    
    // Calculate points (10 points per upload)
    const points = 10;
    
    // Save to database
    const media = await createMedia({
      mediaId,
      url: finalUrl,
      thumbnailUrl,
      type: isVideo ? 'VIDEO' : 'IMAGE',
      senderId,
      receiverId: sender.partnerId,
      originalName,
      mimeType,
      points,
      compressionInfo,
      createdAt: new Date().toISOString()
    });
    
    res.json({
      mediaId,
      url: finalUrl,
      thumbnailUrl,
      points,
      compressionInfo
    });
    
  } catch (error) {
    console.error('Upload error:', error);
    res.status(500).json({ error: 'Upload failed', message: error.message });
  }
});

// Get user's shared media
router.get('/user/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const media = await getMediaByUserId(userId);
    res.json(media);
  } catch (error) {
    console.error('Get media error:', error);
    res.status(500).json({ error: 'Failed to get media' });
  }
});

// Get received media
router.get('/received/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    const media = await getReceivedMedia(userId);
    res.json(media);
  } catch (error) {
    console.error('Get received media error:', error);
    res.status(500).json({ error: 'Failed to get received media' });
  }
});

// Rate media
router.post('/:mediaId/rate', async (req, res) => {
  try {
    const { mediaId } = req.params;
    const { raterId, rating } = req.body;
    
    if (!raterId || !rating || rating < 1 || rating > 5) {
      return res.status(400).json({ error: 'Invalid rating' });
    }
    
    const media = await updateMediaRating(mediaId, raterId, rating);
    
    if (!media) {
      return res.status(404).json({ error: 'Media not found' });
    }
    
    // Calculate points: 5 points per star
    const pointsAwarded = rating * 5;
    
    res.json({
      mediaId,
      rating,
      pointsAwarded,
      senderNewTotal: pointsAwarded // Would need to calculate actual total from DB
    });
    
  } catch (error) {
    console.error('Rate media error:', error);
    res.status(500).json({ error: 'Failed to rate media' });
  }
});

export default router;
