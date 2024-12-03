# 🌤️ WeatherNow  

WeatherNow is a feature-rich weather app built with Android Studio and Kotlin, offering hyperlocal weather updates, hourly and weekly forecasts, user authentication, theme customization, and more. The app integrates Firebase for secure user login, OpenWeatherMap API for accurate weather data, and supports offline functionality for seamless access.

---

## 🌟 **Features**  

- **Current Location Weather:** Real-time weather updates for the user's current location.  
- **City Search:** Search weather data for various cities with a custom REST API displaying images of major South African cities.  
- **Edit Profile:** Users can edit their profile and add a profile picture.  
- **Dark Mode:** Toggle between light and dark themes, with preferences saved for future logins.  
- **Firebase Authentication:** Supports secure login and registration, including Google sign-in and biometric login.  
- **Temperature Units:** Choose between Celsius and Fahrenheit for temperature display.  
- **Push Notifications:** Receive weather updates and alerts via Firebase Cloud Messaging.  
- **Offline Mode:** Access cached weather data for the last searched city when offline using Room Database and WorkManager.  
- **Language Translation:** View the app in English, Afrikaans, or Zulu, powered by Google Cloud Translation API.  

---

## 🛠 **Technologies & Tools**

- **Language:** Kotlin  
- **IDE:** Android Studio  
- **Authentication:** Firebase Authentication (email/password, Google sign-in, biometric login)  
- **Notifications:** Firebase Cloud Messaging (FCM)  
- **Networking:** Retrofit for API requests  
- **Weather Data:** OpenWeatherMap API  
- **Settings:** SharedPreferences for local data storage  
- **Offline Access:** Room Database and WorkManager for caching and syncing  
- **Translation:** Google Cloud Translation API  
- **Architecture:** MVVM (Model-View-ViewModel)  

---

## 📖 **Usage**

1. **Login or Register:**  
   - Sign in with credentials, Google, or biometric login.  

2. **Edit Profile:**  
   - Update profile details and add a profile picture.  

3. **Weather Overview:**  
   - Displays current weather details such as temperature, humidity, and wind speed.  

4. **City Search:**  
   - Use the search feature to find weather for any city.  
   - Custom API displays images of major South African cities.  

5. **Settings:**  
   - **Dark Mode:** Toggle dark mode, with preferences saved for future sessions.  
   - **Temperature Units:** Switch between Celsius and Fahrenheit.  
   - **Language Selection:** Change the app's language to English, Afrikaans, or Zulu.  
   - **Push Notifications:** Enable/disable weather alerts.  

6. **Offline Mode:**  
   - Access cached weather data from the last searched city even when offline.  

---

## 🧩 **Key Features in Detail**  

### 🔄 **Dark Mode Persistence**  
- Saves user preferences via SharedPreferences.  
- Automatically applies dark mode on login if previously enabled.  
- Users can toggle the theme anytime in the settings menu.  

### 🗄️ **Offline Mode**  
- Caches last searched city’s weather data using Room Database.  
- Automatically syncs offline data when reconnected using WorkManager.  

---

## 📡 **API Reference**  

1. **OpenWeatherMap API:**  
   - Provides real-time weather data (temperature, humidity, wind speed).  
   - [API Documentation](https://openweathermap.org/api)  

2. **Google Cloud Translation API:**  
   - Powers multilingual support in the app for English, Afrikaans, and Zulu.  
   - [API Documentation](https://cloud.google.com/translate/docs)  

3. **Json Bin API:**  
   - Custom-designed REST API for displaying images of South African cities.  

---

## 🔑 **Firebase Integration**  

- **Authentication:**  
   - Secure user registration and login with Firebase Authentication.  
   - Supports email/password, Google sign-in, and biometric login.  

- **Notifications:**  
   - Real-time weather updates and alerts via Firebase Cloud Messaging (FCM).  

- **Profile Management:**  
   - Allows users to view and update profile information.  

---

## 📝 **Code Attribution**  

This project incorporates custom and open-source code. Notable attributions include:  

1. **OpenWeatherMap API Integration:**  
   - Guided by SpaceTechnologies' tutorial.  
   - **Reference:** Spacetechnologies.com, "How to Implement OpenWeatherMap API into Android Studio"  
     - [Read the Tutorial](https://www.spaceotechnologies.com/blog/implement-openweathermap-api-android-app-tutorial/)  

2. **Firebase Integration:**  
   - Referenced Firebase’s official documentation for Authentication, Firestore, and Cloud Messaging.  

3. **Json Bin API:**  
   - Custom API for retrieving and displaying city images, tailored for WeatherNow.  

---

## 🎯 **Future Enhancements**  

- Hourly precipitation radar maps.  
- Wear OS support for smartwatches.  
- Weather-based daily reminders and insights.  

---

## 📎 **References**  

- SpaceTechnologies. (2018). *How to Implement OpenWeatherMap API into Android Studio*.  
  Available at: [https://www.spaceotechnologies.com/blog/implement-openweathermap-api-android-app-tutorial/](https://www.spaceotechnologies.com/blog/implement-openweathermap-api-android-app-tutorial/)  

---

## 📥 **Contributing**  

Contributions are welcome! Please submit a pull request or open an issue to get started.  

---

## 📜 **License**  

This project is licensed under the [MIT License](LICENSE).  
