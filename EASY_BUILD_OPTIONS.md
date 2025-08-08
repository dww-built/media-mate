# Easy APK Build Options (No Android Studio Required)

## Option 1: GitHub Actions (Recommended)

1. **Create GitHub repository:**
   ```bash
   cd /home/davew/claude/mobile_mate_app
   git init
   git add .
   git commit -m "Initial Media Mate app"
   ```

2. **Push to GitHub** (create new repo at github.com first)
   ```bash
   git remote add origin https://github.com/YourUsername/media-mate.git
   git push -u origin main
   ```

3. **Automatic APK building:**
   - GitHub Actions will automatically build APK on every push
   - Download from Actions tab → Latest workflow → Artifacts
   - Takes ~5 minutes, completely free

## Option 2: Online Build Services

### AppBuild.dev (Free)
1. Go to https://appbuild.dev
2. Upload the entire `/home/davew/claude/mobile_mate_app` folder as ZIP
3. Click "Build APK"
4. Download APK in ~10 minutes

### GitHub Codespaces (Free with GitHub account)
1. Push code to GitHub repository
2. Open repository in GitHub Codespaces
3. Install Android SDK in codespace:
   ```bash
   wget https://dl.google.com/android/repository/commandlinetools-linux-8512546_latest.zip
   mkdir android-sdk && cd android-sdk
   unzip ../commandlinetools-linux-8512546_latest.zip
   yes | cmdline-tools/bin/sdkmanager --sdk_root=. "platform-tools" "platforms;android-34" "build-tools;34.0.0"
   export ANDROID_HOME=$PWD
   ```
4. Build APK: `./gradlew assembleDebug`

## Option 3: Docker Build (If you have Docker)

```bash
cd /home/davew/claude/mobile_mate_app

# Create Dockerfile for Android build
cat > Dockerfile << 'EOF'
FROM openjdk:11-jdk-slim
RUN apt-get update && apt-get install -y wget unzip
WORKDIR /app
COPY . .
RUN wget https://dl.google.com/android/repository/commandlinetools-linux-8512546_latest.zip
RUN mkdir android-sdk && cd android-sdk && unzip ../commandlinetools-linux-8512546_latest.zip
ENV ANDROID_HOME=/app/android-sdk
RUN yes | android-sdk/cmdline-tools/bin/sdkmanager --sdk_root=$ANDROID_HOME "platform-tools" "platforms;android-34" "build-tools;34.0.0"
RUN chmod +x gradlew
RUN ./gradlew assembleDebug
EOF

# Build APK in Docker
docker build -t media-mate-builder .
docker create --name temp media-mate-builder
docker cp temp:/app/app/build/outputs/apk/debug/app-debug.apk ./media-mate.apk
docker rm temp
```

## Option 4: Install Android Command Line Tools (Lightweight)

```bash
# Download command line tools only (smaller than Android Studio)
cd /tmp
wget https://dl.google.com/android/repository/commandlinetools-linux-8512546_latest.zip
mkdir -p ~/android-sdk/cmdline-tools
cd ~/android-sdk/cmdline-tools
unzip /tmp/commandlinetools-linux-8512546_latest.zip
mv cmdline-tools latest

# Set environment
echo 'export ANDROID_HOME=~/android-sdk' >> ~/.bashrc
echo 'export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin:$ANDROID_HOME/platform-tools' >> ~/.bashrc
source ~/.bashrc

# Install required components
yes | sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# Build APK
cd /home/davew/claude/mobile_mate_app
./gradlew assembleDebug
```

## Recommendation

**Use Option 1 (GitHub Actions)** - it's the easiest and most reliable:
1. Takes 2 minutes to set up
2. Builds automatically 
3. No local installation needed
4. Professional CI/CD workflow
5. Free with GitHub account

The APK will be ready for download from GitHub Actions artifacts within 5 minutes of pushing the code.