# Call Feedback App

An Android-based system for collecting and analyzing user feedback on cellular call quality.

This project combines:

* An Android client application that captures post-call feedback and network metadata.
* A FastAPI backend for storing and serving collected data.
* A MongoDB database for scalable feedback storage.

The goal of the project is to understand real-world cellular call performance by combining subjective user feedback with objective network conditions.

---

# Features

## Android Client

* Detects call completion using Android telephony listeners.
* Displays an overlay feedback UI after calls.
* Collects:

  * Voice quality ratings
  * Audio issue reports
  * Call environment information
  * Optional user comments
* Automatically records:

  * Carrier name
  * Network type (WiFi / 2G / 3G / 4G / 5G)
  * Signal strength (dBm)
  * Approximate location
  * Timestamp
* Runs as a foreground service for reliable background execution.

## Backend

* REST API built with FastAPI.
* API-key based authentication.
* MongoDB integration.
* Feedback submission endpoint.
* Paginated feedback retrieval endpoint.
* Docker support for deployment.

---

# Tech Stack

## Mobile Application

* Kotlin
* Android SDK
* Android Studio
* TelephonyManager API
* ConnectivityManager API
* FusedLocationProviderClient

## Backend

* Python
* FastAPI
* MongoDB
* Docker

---

# System Workflow

1. The Android app runs as a foreground service.
2. The app listens for telephony state changes.
3. When a call ends, an overlay feedback UI appears.
4. The user optionally submits feedback.
5. Network and device metadata are collected automatically.
6. Data is sent securely to the backend API.
7. The backend validates and stores the data in MongoDB.

---

# Feedback Collected

## User Feedback

* Voice quality rating (1–5 stars)
* Audio issues:

  * Call dropped
  * Can't hear them
  * They can't hear me
  * Echo
  * Background noise
* Environment:

  * Indoor
  * Outdoor
  * In vehicle
  * Noisy area
* Optional comments

## Device Metadata

* Carrier name
* Network generation
* Signal strength (dBm)
* Approximate location
* Timestamp

---

# APIs Used

## TelephonyManager

Used for:

* Call state detection
* Network type detection
* Signal strength estimation

## ConnectivityManager

Used for:

* Detecting active network transport
* WiFi vs Cellular identification

## FusedLocationProviderClient

Used for:

* Obtaining approximate device location

---

# Foreground Service Design

Modern Android versions heavily restrict background execution. To ensure reliable call monitoring:

* The app runs as a foreground service.
* A persistent notification is displayed.
* The service remains active during call monitoring.
* Notification content updates dynamically based on call state.

This design improves reliability and prevents the service from being terminated by the system.

---


# Installation

## Android App

1. Open the Android project in Android Studio.
2. Build and install the APK on an Android device.
3. Grant required permissions:

   * Phone state
   * Notifications
   * Location
4. Start the foreground monitoring service.

---

## Backend

### Clone the Repository

```bash
git clone <repository-url>
cd <repository-name>
```

### Create Virtual Environment

```bash
python -m venv venv
source venv/bin/activate
```

### Install Dependencies

```bash
pip install -r requirements.txt
```

### Run the Backend

```bash
uvicorn main:app --reload
```

---


# Research Motivation

Traditional cellular performance measurements often rely solely on network-side metrics. This project focuses on combining:

* User perception of call quality
* Real-world device conditions
* Network characteristics

This enables large-scale analysis of call performance across carriers, locations, and environments.

---


# Acknowledgements

We would like to thank Prof. Bhaskaran Raman for taking out his precious time and guiding us throughout our project.

---

