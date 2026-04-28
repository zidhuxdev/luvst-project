import express from 'express';
import { getUserByUsername, getUserById, updateUser, getPartnerByUserId } from '../db/users.js';

const router = express.Router();

// Search user by username
router.get('/search', async (req, res) => {
  try {
    const { username } = req.query;
    
    if (!username) {
      return res.status(400).json({ error: 'Username required' });
    }
    
    const user = await getUserByUsername(username.toLowerCase().trim());
    
    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }
    
    // Don't return sensitive info
    res.json({
      userId: user.userId,
      username: user.username,
      name: user.name,
      photoUrl: user.photoUrl
    });
    
  } catch (error) {
    console.error('Search error:', error);
    res.status(500).json({ error: 'Search failed' });
  }
});

// Get user's partner info
router.get('/:userId/partner', async (req, res) => {
  try {
    const { userId } = req.params;
    
    const partner = await getPartnerByUserId(userId);
    
    if (!partner) {
      return res.status(404).json({ error: 'No partner found' });
    }
    
    res.json({
      userId: partner.userId,
      name: partner.name,
      photoUrl: partner.photoUrl,
      relationshipStartDate: partner.relationshipStartDate
    });
    
  } catch (error) {
    console.error('Get partner error:', error);
    res.status(500).json({ error: 'Failed to get partner info' });
  }
});

// Get user by ID
router.get('/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    
    const user = await getUserById(userId);
    
    if (!user) {
      return res.status(404).json({ error: 'User not found' });
    }
    
    res.json({
      userId: user.userId,
      email: user.email,
      name: user.name,
      photoUrl: user.photoUrl,
      username: user.username
    });
    
  } catch (error) {
    console.error('Get user error:', error);
    res.status(500).json({ error: 'Failed to get user' });
  }
});

export default router;
