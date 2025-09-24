# Barcode Scanner App

## Overview
The **Barcode Scanner App** is an Android application built in Java using Android Studio.  
It allows users to log in, register, scan barcodes, and manage product details efficiently.

---

## Features
- **User Authentication**: Login and Registration screens.
- **Barcode Scanning**: Uses the device camera to scan product barcodes.
- **Product Management**: Add, view, and edit product details.
- **Multiple Screens**: Smooth navigation between activities with data transfer.
- **Custom UI Components**: Includes custom product cards and bottom sheets for enhanced UX.
- **Data Storage**: Stores product information locally.
- **Sensors**:
    - **Camera** – Used for barcode scanning.
    - **Light Sensor** – Controls torch automatically in dark environments.
    - **Accelerometer** – Detects shake motion to restart scanning.

---

## Project Structure
- `MainActivity` – Entry point of the app.
- `LoginActivity` – Handles user login.
- `RegisterActivity` – Handles user registration.
- `CameraScannerActivity` – Launches camera, integrates sensors, and manages scanning.
- `BarcodeResultActivity` – Displays scanned barcode result.
- `AddProductActivity` – Adds new product details.
- `ProductDetailActivity` – Shows detailed product information.

---

## Requirements Fulfilled
- ✅ 3+ Activities (7 implemented).
- ✅ Data transfer between activities via intents.
- ✅ Custom View integration (scanner overlay, bottom sheets).
- ✅ Use of **3 sensors** (Camera, Light, Accelerometer).
- ✅ Data storage and persistence.
- ✅ Explicit and Implicit intents used.

---

## Installation
1. Open Android Studio (version 2024.3 or later).
2. Clone or extract the project into your `AndroidStudioProjects` folder.
3. Sync Gradle and build the project.
4. Run the app on an Android 12 emulator or device.

---

## Authors
- [Your Names & Matrikel Numbers Here]

---

## Notes
- Tested on Android 12 (API 31/32).
- Temporary build files (`/app/build`) can be deleted before packaging.
