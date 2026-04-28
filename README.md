# luvst - Couples App

A minimal, romantic Android app for couples to connect, share moments, and celebrate their love journey.

## Features

### Authentication
- Google Sign-In for quick and secure access

### Home
- Search for your partner by username
- Send connection requests
- View relationship duration counter
- Daily love duration display

### Luvst (Moments)
- Take photos or upload from gallery
- Share videos with your partner
- **Aggressive on-device image compression** before upload:
  - Resize: max 1280px on longest side
  - Format: JPEG/WebP
  - Quality: 60-85%
  - Target: <500KB per image
- Partner can rate shared moments
- Earn love points for sharing

### Inbox
- View all moments shared by your partner
- Rate received photos/videos (1-5 stars)
- Voice message support

### Voice Messages
- Record and send voice messages via HTTP Direct Protocol
- Stream audio with range request support
- Mark messages as listened

## Tech Stack

### Android App (Kotlin)
- Jetpack Compose for modern UI
- Hilt for dependency injection
- Retrofit + OkHttp for networking
- Coil for image loading
- Compressor library for image compression
- CameraX for photo capture
- DataStore for local preferences

### Backend (Node.js)
- Express.js HTTP server
- Sharp for server-side image optimization
- Multer for file uploads
- JWT authentication
- JSON file-based storage (easily upgradeable to database)
- Support for Cloudflare R2 CDN

## Project Structure

```
luvst-kotlin/
├── app/                          # Android app
│   ├── src/main/java/com/luvst/app/
│   │   ├── ui/screens/          # All screens (login, home, luvst, inbox)
│   │   ├── data/                # API service and local preferences
│   │   ├── di/                  # Dependency injection
│   │   └── theme/               # App theme (colors, typography)
│   └── build.gradle.kts         # App dependencies
├── server/                      # Node.js backend
│   ├── routes/                  # API routes (auth, users, media, voice)
│   ├── db/                      # Database functions
│   ├── index.js                 # Server entry point
│   └── package.json             # Server dependencies
└── build.gradle.kts            # Project-level config
```

## Getting Started

### Prerequisites
- Android Studio Hedgehog or newer
- Node.js 18+
- Google Cloud Console project with OAuth credentials

### Setup

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd luvst-kotlin
   ```

2. **Configure Android App**
   - Open the project in Android Studio
   - Add your Google Web Client ID in `app/build.gradle.kts`:
     ```kotlin
     buildConfigField("String", "GOOGLE_WEB_CLIENT_ID", "\"your-client-id.apps.googleusercontent.com\"")
     ```

3. **Start the Server**
   ```bash
   cd server
   npm install
   cp .env.example .env
   # Edit .env with your credentials
   npm start
   ```

4. **Run the App**
   - Connect an Android device or start an emulator
   - Click Run in Android Studio

## Environment Variables

Create `.env` file in the server directory:

```env
PORT=3000
NODE_ENV=development
GOOGLE_CLIENT_ID=your-google-client-id.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=your-secret
JWT_SECRET=your-jwt-secret
R2_ENDPOINT=https://your-account.r2.cloudflarestorage.com
R2_ACCESS_KEY_ID=your-key
R2_SECRET_ACCESS_KEY=your-secret
R2_BUCKET_NAME=luvst-media
CDN_BASE_URL=https://media.yourdomain.com
```

## Image Compression Strategy

The app implements a two-stage compression system for maximum speed:

### Stage 1: Device Compression (Primary)
- Resize large images to max 1280x1280
- JPEG/WebP format conversion
- Quality reduction to 60-85%
- Target file size: <500KB
- Reduces upload time by 60-80%

### Stage 2: Server Optimization (Secondary)
- Sharp library processing
- Progressive JPEG encoding
- Additional 10-20% size reduction

## API Endpoints

### Authentication
- `POST /api/auth/google` - Google sign in
- `POST /api/auth/verify` - Verify JWT token

### Users
- `GET /api/users/search?username={username}` - Search users
- `GET /api/users/{userId}/partner` - Get partner info

### Connections
- `POST /api/connections/request` - Send connection request
- `POST /api/connections/{id}/accept` - Accept request

### Media
- `POST /api/media/upload` - Upload photo/video
- `GET /api/users/{userId}/media` - Get shared media
- `GET /api/users/{userId}/received-media` - Get received media
- `POST /api/media/{id}/rate` - Rate media

### Voice
- `POST /api/voice-messages/upload` - Upload voice message
- `GET /api/voice-messages/user/{userId}` - Get voice messages
- `POST /api/voice-messages/{id}/listened` - Mark as listened
- `GET /api/voice-messages/stream/{id}` - Stream audio (HTTP direct protocol)

## Design Philosophy

- **Minimal & Romantic**: Soft pastels, elegant typography (Playfair Display + Quicksand)
- **Performance First**: Aggressive compression keeps uploads fast
- **Privacy Focused**: Direct device-to-server, no third-party image services
- **Couple-Centric**: Every feature designed for two people in love

## License

MIT License - Feel free to use for your own couples app!

---

Made with ❤️ for couples everywhere
