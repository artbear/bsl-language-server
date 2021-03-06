# Опечатка (Typo)

| Тип | Поддерживаются<br/>языки | Важность | Включена<br/>по умолчанию | Время на<br/>исправление (мин) | Тэги |
| :-: | :-: | :-: | :-: | :-: | :-: |
| `Дефект кода` | `BSL`<br/>`OS` | `Информационный` | `Да` | `1` | `badpractice` |

## Параметры 

| Имя | Тип | Описание | Значение по умолчанию |
| :-: | :-: | :-- | :-: |
| `userWordsToIgnore` | `Строка` | ```Пользовательский словарь исключений``` | `````` |
| `minWordLength` | `Целое` | ```Минимальная длина проверяемых слов``` | ```3``` |

<!-- Блоки выше заполняются автоматически, не трогать -->
## Описание диагностики
<!-- Описание диагностики заполняется вручную. Необходимо понятным языком описать смысл и схему работу -->
Проверка орфографических ошибок осуществляется с помощью LanguageTool. Проверяемые строки разбиваются по camelCase 
и проверяются на соответствие во встроенном словаре.

## Источники
<!-- Необходимо указывать ссылки на все источники, из которых почерпнута информация для создания диагностики -->
<!-- Примеры источников

* Источник: [Стандарт: Тексты модулей](https://its.1c.ru/db/v8std#content:456:hdoc)
* Полезная информаця: [Отказ от использования модальных окон](https://its.1c.ru/db/metod8dev#content:5272:hdoc)
* Источник: [Cognitive complexity, ver. 1.4](https://www.sonarsource.com/docs/CognitiveComplexity.pdf) -->
* Полезная информация: [Русский язык для всех](http://gramota.ru/)
* [Источник](https://languagetool.org/ru/)

<!-- Блоки ниже заполняются автоматически, не трогать -->
### Экранирование кода

```bsl
// BSLLS:Typo-off
// BSLLS:Typo-on
```

### Параметр конфигурационного файла

```json
"Typo": {
    "userWordsToIgnore": "",
    "minWordLength": 3
}
```

## Сниппеты

<!-- Блоки ниже заполняются автоматически, не трогать -->
### Экранирование кода

```bsl
// BSLLS:Typo-off
// BSLLS:Typo-on
```

### Параметр конфигурационного файла

```json
"Typo": {
    "userWordsToIgnore": "",
    "minWordLength": 3
}
```
