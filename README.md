# Androed Tools  
[![Current release](https://img.shields.io/github/v/release/flar1o/AndroedTools)](https://github.com/flar1o/AndroedTools/releases/latest)

**Инструмент для диагностики и управления Android-устройствами через ADB**  

Androed Tools предоставляет визуальный интерфейс для мониторинга состояния устройства (CPU, RAM, батарея), установки/удаления приложений, выполнения ADB/Fastboot-команд и других задач.

---

## 🔧 Требования  
- **Java 21** (установите с [Oracle JDK](https://www.oracle.com/java/technologies/downloads/)  или [OpenJDK](https://adoptium.net/))   
- Поддержка Windows/Linux/macOS  
- Android Debug Bridge (ADB) встроен в приложение  

---

## 🚀 Функции  
- **Мониторинг устройства**:  
  - Графики нагрузки CPU, использование RAM, уровень заряда батареи  
  - Данные о подключении Wi-Fi/USB  
- **Управление приложениями**:  
  - Установка/удаление APK  
  - Список установленных приложений с фильтрацией  
- **Инструменты**:  
  - Скриншоты экрана  
  - Изменение разрешения экрана  
  - Интеграция с [scrcpy](https://github.com/Genymobile/scrcpy)  для управления через Wi-Fi  
  - Настройка статус-бара (скрытие часов, Wi-Fi и т.д.)  
- **Консоль ADB/Fastboot**:  
  - Выполнение команд напрямую
- **Поддержка подключения по Wi-Fi**  

---

## 📖 Первое подключение  
Подробная инструкция по настройке ADB и подключению устройства доступна внутри приложения:  
1. Откройте вкладку **"Помощь"** в интерфейсе.  
2. Следуйте шагам для активации режима отладки и подключения через USB/Wi-Fi.  

---

## 📦 Установка  
1. Скачайте последнюю версию с [релизов GitHub](https://github.com/flar1o/AndroedTools/releases).   
2. Распакуйте архив и запустите `AndroedTools.exe` (Windows) или `./AndroedTools` (Linux/macOS).  

---

## 🌐 Поддержка  
- **GitHub Issues**: [Создать запрос](https://github.com/flar1o/AndroedTools/issues)   
- **Email**: skyzmin05@ya.ru  

---

### Лицензия  
MIT License - см. файл [LICENSE](LICENSE) для деталей.  

---

### Примеры скриншотов  
![Интерфейс Androed Tools](screenshots/main_window.png)  
*Главный экран с мониторингом устройства*

---

### Благодарности  
- Использует [DDMLib](https://github.com/android/ddmlib)  для работы с ADB  
- Огромное спасибо Qwen за основную работу
- Спасибо ChatGPT за реализацию выгрузки в exe файл и главный логотип
- Спасибо DeepSeek за дизайн

---

**Совет:** Для корректной работы убедитесь, что на устройстве включена отладка по USB и установлены драйверы ADB.  

--- 
