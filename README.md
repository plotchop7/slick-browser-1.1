# Slick — liquid monochrome browser

Полностью переписанный нативный Android-браузер по концепту `concept1reworked.html`: Kotlin, Jetpack Compose, Single Activity, нативный WebView и аппаратные Bitmap-снапшоты вкладок.

## Требования

- JDK 17
- Android SDK 34, Build Tools 34.0.0
- Gradle 8.5 через входящий в репозиторий Wrapper
- Android 8.0+ (API 26)

## Сборка

```bash
./gradlew assembleDebug
./gradlew lintDebug assembleRelease
```

Debug APK: `app/build/outputs/apk/debug/app-debug.apk`.

Release APK без внешней подписи: `app/build/outputs/apk/release/app-release-unsigned.apk`. Для публикации задайте собственный keystore в CI или локальной конфигурации подписи; секреты в репозиторий не входят.

## GitHub Actions

Workflow `.github/workflows/build.yml` запускается вручную, по push в `main`/`master` и по pull request. Он устанавливает JDK 17 и SDK 34, выполняет lint, собирает Debug и Release и прикрепляет оба APK к артефактам workflow.

## Структура репозитория

```text
Slick/
├── .github/workflows/build.yml
├── app/
│   ├── build.gradle.kts
│   ├── proguard-rules.pro
│   └── src/main/
│       ├── AndroidManifest.xml
│       ├── java/com/slick/browser/
│       │   ├── MainActivity.kt
│       │   ├── BrowserViewModel.kt
│       │   ├── data/
│       │   │   ├── BrowserModels.kt
│       │   │   ├── BrowserRepository.kt
│       │   │   └── DownloadController.kt
│       │   ├── web/
│       │   │   ├── SlickWebViewFactory.kt
│       │   │   ├── SnapshotManager.kt
│       │   │   └── WebInjector.kt
│       │   └── ui/
│       │       ├── BrowserScreen.kt
│       │       ├── ZeroPoint.kt
│       │       ├── SearchField.kt
│       │       ├── TabManager.kt
│       │       ├── BookmarksPanel.kt
│       │       ├── OverflowMenu.kt
│       │       ├── SettingsPanel.kt
│       │       ├── DataPanels.kt
│       │       ├── components/
│       │       │   ├── Liquid.kt
│       │       │   ├── SlickGestures.kt
│       │       │   └── Controls.kt
│       │       └── theme/SlickTheme.kt
│       └── res/
│           ├── drawable/ic_slick.xml
│           └── values/{strings.xml,themes.xml}
├── gradle/wrapper/{gradle-wrapper.jar,gradle-wrapper.properties}
├── gradlew
├── gradlew.bat
├── build.gradle.kts
├── settings.gradle.kts
├── gradle.properties
└── README.md
```

## Архитектура

- `BrowserViewModel` — единое состояние режимов, два изолированных пула вкладок, закладки, история, настройки, политика энергосбережения.
- `SlickWebViewFactory` — жизненный цикл WebView, навигация, User-Agent, textZoom, ошибки, загрузки и инжектор.
- `SnapshotManager` — PixelCopy с Canvas fallback; снапшоты используются при переключении вкладок и в менеджере.
- `SlickGestures` — единый двухфазный контроллер натяжения, удержания 200 мс, обратимых и отменяемых жестов.
- `Liquid` — Canvas, нативный `BlurMaskFilter`, контурный прогресс с квадратичным догоняющим ускорением, волны и ч/б ColorMatrix.
- `BrowserRepository` — SharedPreferences + JSON для настроек, закладок и обычной истории; инкогнито туда не записывается.

## Управление

- Тап по `slick`, затем горизонтальный свайп по логотипу в течение 3 секунд — вход/выход инкогнито.
- Долгое нажатие `slick` — глобальные настройки.
- На нижней строке и Zero Point: вертикальные свайпы с удержанием открывают сохранённые и вкладки; повторный свайп вверх раскрывает сетку.
- Горизонтальный свайп по нижней строке сайта с удержанием 200 мс переключает вкладку; ранний отпуск возвращает исходную.
- Тап по нижней строке — новая вкладка; зажатие — редактирование текущего URL.
- В карусели: вниз — растяжение и открытие, вверх — закрытие. В сетке: влево/вправо — закрытие.
- Зажатие карточек и закладок открывает компактные действия; закладки переставляются перетаскиванием.

## Приватность

Инкогнито включает `FLAG_SECURE`, запрещает персонализированное обучение IME, отключает DOM-хранилище и дисковый режим кэша, не записывает историю и загрузки. Выход или завершение инкогнито уничтожает его WebView, снапшоты, cookies и WebStorage. Обычные и инкогнито-вкладки имеют раздельные пулы и никогда не отображаются вместе.

Авторская метка сборки сохранена в заголовке `MainActivity.kt`, `Log.i("SlickCore", …)` и metadata `AndroidManifest.xml`.
