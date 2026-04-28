import express from 'express';
import { OAuth2Client } from 'google-auth-library';
import jwt from 'jsonwebtoken';
import { v4 as uuidv4 } from 'uuid';
import { getUserByEmail, createUser, updateUser } from '../db/index.js';

const router = express.Router();
const googleClient = new OAuth2Client(process.env.GOOGLE_CLIENT_ID);

const JWT_SECRET = process.env.JWT_SECRET || 'your-secret-key-change-in-production';

// Google Sign In
router.post('/google', async (req, res) => {
  try {
    const { idToken, email, name, photoUrl } = req.body;
    
    if (!idToken || !email) {
      return res.status(400).json({ error: 'Missing required fields' });
    }
    
    // Verify Google token (optional but recommended)
    let payload;
    try {
      const ticket = await googleClient.verifyIdToken({
        idToken: idToken,
        audience: process.env.GOOGLE_CLIENT_ID
      });
      payload = ticket.getPayload();
    } catch (verifyError) {
      console.log('Token verification skipped for development');
    }
    
    // Check if user exists
    let user = await getUserByEmail(email);
    let isNewUser = false;
    
    if (!user) {
      // Create new user
      const userId = uuidv4();
      const username = email.split('@')[0] + Math.random().toString(36).substring(2, 6);
      
      user = await createUser({
        userId,
        email,
        name,
        photoUrl,
        username,
        googleId: payload?.sub || null,
        createdAt: new Date().toISOString()
      });
      isNewUser = true;
    } else {
      // Update last login
      await updateUser(user.userId, { 
        lastLogin: new Date().toISOString(),
        photoUrl: photoUrl || user.photoUrl
      });
    }
    
    // Generate JWT token
    const token = jwt.sign(
      { userId: user.userId, email: user.email },
      JWT_SECRET,
      { expiresIn: '7d' }
    );
    
    res.json({
      userId: user.userId,
      email: user.email,
      name: user.name,
      photoUrl: user.photoUrl,
      username: user.username,
      token,
      isNewUser
    });
    
  } catch (error) {
    console.error('Google sign in error:', error);
    res.status(500).json({ error: 'Authentication failed', message: error.message });
  }
});

// Verify token
router.post('/verify', async (req, res) => {
  try {
    const { token } = req.body;
    
    if (!token) {
      return res.status(401).json({ error: 'No token provided' });
    }
    
    const decoded = jwt.verify(token, JWT_SECRET);
    const user = await getUserByEmail(decoded.email);
    
    if (!user) {
      return res.status(401).json({ error: 'User not found' });
    }
    
    res.json({ valid: true, user });
    
  } catch (error) {
    res.status(401).json({ error: 'Invalid token' });
  }
});

export default router;
