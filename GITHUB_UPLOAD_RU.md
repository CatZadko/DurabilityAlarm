# Загрузка Durability Alarm на GitHub

Проект подготовлен для репозитория `DurabilityAlarm`. В корне уже есть `README.md`, `.gitignore` и MIT-лицензия с автором Zadko.

## Через сайт GitHub

1. Распакуй `DurabilityAlarm-Fabric-26.3-sources.zip` и открой папку `DurabilityAlarm-26.3-sources`.
2. Открой https://github.com/new и выбери свой аккаунт в поле Owner.
3. Введи имя репозитория `DurabilityAlarm`.
4. В Description можно указать: `Client-side durability warnings for Minecraft 26.3 Fabric.`
5. Для открытой публикации исходников выбери Public. Опции добавления README, `.gitignore` и лицензии при создании оставь выключенными: эти файлы уже подготовлены.
6. Нажми Create repository. На странице пустого репозитория выбери ссылку uploading an existing file. Если в репозитории уже есть файлы, используй Add file → Upload files.
7. В файловом менеджере нажми Ctrl+H для показа скрытых файлов, затем выдели всё содержимое папки `DurabilityAlarm-26.3-sources` и перетащи в область загрузки.
8. Загружай файлы и папки проекта в корень репозитория. Рядом должны находиться `README.md`, `build.gradle`, `src`, `gradle`, `gradlew` и `gradlew.bat`. Сам ZIP распаковывается локально; на GitHub отправляется его содержимое.
9. Укажи сообщение коммита `Initial release: Durability Alarm 1.0.3` и подтверди добавление файлов кнопкой Commit changes.
10. Открой главную страницу репозитория и проверь, что отображается README с иконкой и описанием мода.

При загрузке через сайт права выполнения `gradlew` могут не сохраниться. После скачивания исходников на Linux команда `chmod +x gradlew` из README включает запуск скрипта.

## Что входит в архив

- Исходники мода версии 1.0.3 для Minecraft 26.3 / Fabric.
- Gradle Wrapper и настройки сборки.
- Иконка, английский и русский интерфейс.
- README на английском и руководство на русском.
- Тексты для Modrinth, история изменений и лицензии.

Подготовка этого архива не запускала Gradle, Minecraft или сборку JAR. Публикация исходников сама по себе не создаёт готовый мод; его можно собрать отдельно по README.

Документация GitHub:

- https://docs.github.com/en/repositories/creating-and-managing-repositories/creating-a-new-repository
- https://docs.github.com/en/repositories/working-with-files/managing-files/adding-a-file-to-a-repository
