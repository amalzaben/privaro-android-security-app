# 🔐 Privaro – Android Privacy Protection App

## 📌 Overview

Privaro is an Android security application designed to protect visually impaired users from **shoulder-surfing attacks** when using TalkBack.

The app detects when sensitive information is about to be spoken and analyzes the surroundings to ensure privacy.

---

## ⚡ Key Features

* Detects sensitive TalkBack events (passwords, private data)
* Runs in the background using Android system services
* Uses camera + AI to detect nearby people
* Alerts user before sensitive information is spoken
* Allows user to **pause or continue safely**
* Provides simple audio feedback

---

## 🧠 How It Works

1. TalkBack triggers a sensitive event
2. App intercepts using Accessibility Service
3. Camera scans surroundings
4. Detects presence of people
5. Alerts user and pauses TalkBack if needed

---

## 📱 Screenshots

<p align="center">
  <img src="assets/sign_in.jpeg" width="200"/>
  <img src="assets/sign_up.jpeg" width="200"/>
  <img src="assets/home_page.jpeg" width="200"/>
</p>

<p align="center">
  <img src="assets/home_page_after_permissions.jpeg" width="200"/>
  <img src="assets/permissions.jpeg" width="200"/>
  <img src="assets/history.jpeg" width="200"/>
</p>

<p align="center">
  <img src="assets/sensitive_info_alert.jpeg" width="200"/>
  <img src="assets/people_detected.jpeg" width="200"/>
  <img src="assets/safe_no_one_around.jpeg" width="200"/>
</p>

---

## ⚙️ Tech Stack

* Kotlin (Android)
* Spring Boot (Backend)
* Gradle
* Android Accessibility Service
* Camera APIs
* Background services & system interrupts

---

## 🔒 Key Concepts

* Background execution & system-level hooks
* OS resource management (camera, accessibility, permissions)
* Privacy-aware design for accessibility users
* Real-time event-driven security

---

## 📁 Project Structure

* `app/` → Mobile application
* `backend/` → Spring Boot services
* `Project Proposal` → Proposal 



