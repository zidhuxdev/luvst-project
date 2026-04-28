import express from 'express';
import { v4 as uuidv4 } from 'uuid';
import { createConnectionRequest, getConnectionRequestById, acceptConnectionRequest, getPendingRequestForUser, updateUser } from '../db/index.js';

const router = express.Router();

// Send connection request
router.post('/request', async (req, res) => {
  try {
    const { fromUserId, toUsername, relationshipStartDate } = req.body;
    
    if (!fromUserId || !toUsername) {
      return res.status(400).json({ error: 'Missing required fields' });
    }
    
    // Check if there's already a pending request
    const existingRequest = await getPendingRequestForUser(fromUserId, toUsername);
    if (existingRequest) {
      return res.status(409).json({ error: 'Connection request already sent' });
    }
    
    const requestId = uuidv4();
    const request = await createConnectionRequest({
      requestId,
      fromUserId,
      toUsername,
      relationshipStartDate: relationshipStartDate || Date.now(),
      status: 'pending',
      createdAt: new Date().toISOString()
    });
    
    res.json({
      requestId: request.requestId,
      status: request.status,
      message: 'Connection request sent successfully'
    });
    
  } catch (error) {
    console.error('Connection request error:', error);
    res.status(500).json({ error: 'Failed to send connection request' });
  }
});

// Accept connection request
router.post('/:requestId/accept', async (req, res) => {
  try {
    const { requestId } = req.params;
    const { userId } = req.body; // User accepting the request
    
    const request = await getConnectionRequestById(requestId);
    
    if (!request) {
      return res.status(404).json({ error: 'Request not found' });
    }
    
    if (request.status !== 'pending') {
      return res.status(400).json({ error: 'Request already processed' });
    }
    
    // Update request status
    await acceptConnectionRequest(requestId, userId);
    
    // Update both users to set each other as partners
    const startDate = request.relationshipStartDate || Date.now();
    
    await updateUser(request.fromUserId, { 
      partnerId: userId,
      relationshipStartDate: startDate
    });
    
    await updateUser(userId, { 
      partnerId: request.fromUserId,
      relationshipStartDate: startDate
    });
    
    res.json({
      requestId,
      status: 'accepted',
      message: 'Connection accepted! You are now connected with your partner.'
    });
    
  } catch (error) {
    console.error('Accept connection error:', error);
    res.status(500).json({ error: 'Failed to accept connection' });
  }
});

// Get pending requests for a user
router.get('/pending/:userId', async (req, res) => {
  try {
    const { userId } = req.params;
    
    // This would need implementation in db layer
    res.json([]);
    
  } catch (error) {
    console.error('Get pending requests error:', error);
    res.status(500).json({ error: 'Failed to get pending requests' });
  }
});

export default router;
