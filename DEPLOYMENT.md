# Deploy luvst to Railway + Connect Android App

## Step 1: Deploy Server to Railway

### 1.1 Install Railway CLI (optional but recommended)
```bash
npm install -g @railway/cli
```

### 1.2 Deploy via Railway Dashboard (Easiest)

1. Go to https://railway.app and sign in with GitHub
2. Click "New Project" → "Deploy from GitHub repo"
3. Select your `luvst-kotlin` repository
4. Railway will auto-detect the `railway.toml` and `Procfile`

### 1.3 Set Environment Variables in Railway Dashboard

Go to your project → Variables → Add the following:

```
PORT=3000
NODE_ENV=production
GOOGLE_CLIENT_ID=161811534652-vqfks1ooidjcp5eq24uqdbuebp5q1bjg.apps.googleusercontent.com
GOOGLE_CLIENT_SECRET=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
JWT_SECRET=i45ngxir4Z3JRADjENHhEhM7Tmz2Nw7OoXCydrmOAXk
R2_ENDPOINT=https://pub-3ab59b8f25ba43769d968211da80e759.r2.dev
R2_ACCESS_KEY_ID=e1e7ffa863461bc904214b3c3aaf392e
R2_SECRET_ACCESS_KEY=yd3ec42a08f011807da7030944c0634215637369f3560decc2d8b8a1359255241
R2_BUCKET_NAME=zidhuxd-server
CDN_BASE_URL=https://cdn.zidhuxd.com
```

### 1.4 Get Your Railway URL

After deployment, Railway will give you a URL like:
```
https://luvst-server-production.up.railway.app
```

Copy this URL - you'll need it for the Android app.

---

## Step 2: Connect Android App to Railway Server

### 2.1 Update Base URL in `app/build.gradle.kts`

Open `app/build.gradle.kts` and change line 26:

```kotlin
buildConfigField("String", "BASE_URL", "\"https://your-railway-app.up.railway.app/\"")
```

Replace `your-railway-app.up.railway.app` with your actual Railway domain.

### 2.2 Rebuild the App

In Android Studio:
1. Click **Build** → **Rebuild Project**
2. This generates `BuildConfig.BASE_URL` with your Railway URL

### 2.3 Update Google OAuth (Important!)

Your Google OAuth is configured for the Android app. The server needs to verify the Google ID token. Make sure:

1. In Google Cloud Console: https://console.cloud.google.com
2. Go to APIs & Services → Credentials
3. Your OAuth 2.0 Client ID `161811534652-...` should have:
   - Application type: Android
   - Package name: `com.luvst.app`
   - SHA-1 certificate fingerprint (add your debug and release keys)

---

## Step 3: Configure CORS (Production)

If you want to restrict CORS to only your app, update `server/index.js`:

```javascript
// Replace line 25:
app.use(cors());

// With:
const allowedOrigins = [
  'https://your-railway-app.up.railway.app',
  // Add your app domain if you have one
];

app.use(cors({
  origin: (origin, callback) => {
    if (!origin || allowedOrigins.includes(origin)) {
      callback(null, true);
    } else {
      callback(new Error('Not allowed by CORS'));
    }
  },
  credentials: true
}));
```

---

## Step 4: Test the Connection

### 4.1 Test Server Health
Open browser:
```
https://your-railway-app.up.railway.app/health
```
Should return: `{"status":"ok"}`

### 4.2 Test from Android App
1. Run the app on your device
2. Check Android Studio Logcat for API calls
3. If you see connection errors, check:
   - Internet permission in `AndroidManifest.xml` ✓ (already added)
   - `android:usesCleartextTraffic="true"` ✓ (already added for http)
   - For https (Railway), this should work automatically

### 4.3 Common Issues

**SSL/TLS Errors:**
Railway uses HTTPS by default - this should work fine. If you get certificate errors:
- Make sure you're using `https://` not `http://`
- Check system time on device is correct

**CORS Errors:**
If app can't connect:
```
E/ApiService: CORS error
```
Keep CORS open (`app.use(cors())`) for testing, restrict later.

**Google Sign-In Fails:**
- Check `GOOGLE_CLIENT_ID` in Railway matches your Google Console
- Add your debug SHA-1 to Google Console:
  ```bash
  cd ~/.android
  keytool -list -v -keystore debug.keystore -alias androiddebugkey -storepass android -keypass android
  ```

---

## Step 5: Production Checklist

- [ ] Server deployed to Railway with all env vars set
- [ ] `BASE_URL` in `build.gradle.kts` updated to Railway URL
- [ ] App rebuilt after URL change
- [ ] Google OAuth SHA-1 fingerprints added to Google Console
- [ ] CORS configured (optional for production)
- [ ] Cloudflare R2 credentials working for media storage
- [ ] Test user registration/login flow
- [ ] Test media upload with compression
- [ ] Test partner connection flow

---

## Quick Reference: Files to Modify

| File | What to Change |
|------|----------------|
| `app/build.gradle.kts` line 26 | Update `BASE_URL` to your Railway URL |
| `server/.env` (local) | Copy same values to Railway dashboard |
| Google Cloud Console | Add Android app SHA-1 fingerprints |
| `server/index.js` line 25 | Configure CORS for production (optional) |

---

## Railway Deployment via CLI (Alternative)

```bash
cd luvst-kotlin/server

# Login to Railway
railway login

# Link project
railway link

# Deploy
railway up

# Get URL
railway status
```

---

Your luvst app should now be connected! 💕
