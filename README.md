# 🛠️ AI Doomsday Toolbox

**Your offline AI survival companion for Android.** Run powerful AI models entirely on-device with no internet required. Perfect for when the world ends... or just when you want privacy.

[![Android](https://img.shields.io/badge/Platform-Android-green.svg)](https://developer.android.com)
[![Min SDK](https://img.shields.io/badge/Min%20SDK-26-blue.svg)](https://developer.android.com)
[![License](https://img.shields.io/badge/License-MIT-yellow.svg)](LICENSE)

## ✨ Features

### 🤖 AI Chat
- Chat with Large Language Models (LLMs) completely offline
- Support for GGUF model format
- OpenAI-compatible server mode on port **8080**
- Multiple model support with easy switching
- Customizable system prompts

### 🎨 Image Generation
- Generate images from text prompts
- **Supported models:**
  - Stable Diffusion (SD 1.5, SD 2.1, SDXL)
  - FLUX (schnell, dev)
- Adjustable parameters (steps, CFG scale, dimensions)

### 🎙️ Audio Transcription
- Transcribe audio files to text using Whisper
- Support for multiple languages
- Various model sizes (tiny, base, small, medium, large)

### 🎬 Video Summarization  
- Extract audio from videos
- Transcribe and summarize video content
- Uses FFmpeg for audio extraction

### 📄 PDF Tools
- Extract text from PDFs
- Summarize documents with AI
- OCR support for scanned documents

### 🖼️ Image Upscaling
- Upscale images with RealESRGAN
- Multiple scale factors (2x, 3x, 4x)
- High-quality AI enhancement

### 📚 Offline Wikipedia (Kiwix)
- Browse Wikipedia without internet
- ZIM file support
- Built-in Kiwix server on port **8888**
- Access from any device on your network

### 📤 Model & File Sharing
- Share AI models over LAN
- Web UI for downloading models
- QR codes for easy connection
- ZIM file sharing for offline content

### 📝 Notes
- Create and manage notes
- AI-powered note summarization
- Markdown support

## 🏗️ How It Was Built

This app integrates several native C++ AI inference engines into an Android app using JNI and native binaries:

### Core Technologies
- **[llama.cpp](https://github.com/ggerganov/llama.cpp)** - LLM inference
- **[whisper.cpp](https://github.com/ggerganov/whisper.cpp)** - Audio transcription
- **[stable-diffusion.cpp](https://github.com/leejet/stable-diffusion.cpp)** - Image generation
- **[FFmpeg](https://ffmpeg.org)** - Video/audio processing
- **[Kiwix-tools](https://github.com/kiwix/kiwix-tools)** - Offline Wikipedia
- **[RealESRGAN](https://github.com/xinntao/Real-ESRGAN)** - Image upscaling

### Android Stack
- **Kotlin** with Jetpack Compose for UI
- **Room Database** for local data persistence
- **NanoHTTPD** for embedded HTTP servers
- **ZXing** for QR code generation
- **ML Kit** for OCR
- **Apache PDFBox** for PDF handling

### Architecture
- Native binaries compiled for **arm64-v8a**
- Foreground services for long-running AI tasks
- Unified notification system for background processes
- Store Access Framework (SAF) for file management

## 🚀 Getting Started

### Requirements
- Android 8.0+ (API 26)
- arm64-v8a device (most modern Android phones)
- 4GB+ RAM recommended
- Storage space for AI models

### Building from Source

```bash
# Clone the repository
git clone https://github.com/ManuXD32/AI-Doomsday-Toolbox.git
cd AI-Doomsday-Toolbox

# Build with Gradle (requires Java 17+)
./gradlew assembleDebug

# Install on device
adb install app/build/outputs/apk/debug/app-debug.apk
```

### Download Models

Models are downloaded separately from within the app. The app includes a curated model catalog with popular options for each task.

## 📱 Screenshots

*Coming soon*

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

## 💖 Support the Developer

If you find this app useful, consider supporting development:

- ☕ [Ko-fi](https://ko-fi.com/L3L61QAJ1S)
- 💳 [PayPal](https://paypal.me/ManuelG815)

## 📄 License

This project is open source. See the [LICENSE](LICENSE) file for details.

## 👨‍💻 Author

**ManuXD32** - [GitHub](https://github.com/ManuXD32)

---

*Built with ❤️ for the AI apocalypse*
