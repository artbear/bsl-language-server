# IncompatibleArgumentType в сценариях строгой типизации

Из 18 сценариев `SpecStrictTypingControlTest` два прямо проверяют назначение диагностики — § 2.22 и § 2.30. В § 2.31 два вызова иллюстрируют пропуск при неизвестном типе параметра. В § 2.23 и § 4.18 есть допустимые по контракту платформы вызовы, а тесты ожидают нарушения других правил.

Сопоставлены [спецификация из коммита `447dde1e005ed72aacc998622b2c3579fe9204cd`][spec] и [текущая реализация][impl]. Исходное сравнение зафиксировано ссылками на [реализацию][impl-before] и [тест][tests-before] из коммита `358b1fc59b51394c80d10256d92103fc4ead2579`. Номера пунктов взяты из `@DisplayName`. Сценарии ниже описывают BSL из исполняемых текстовых блоков Java. Например, пример в Javadoc § 2.30 упоминает `РасходТовара`, а тест передаёт объект `Справочник1`. [Пункт 2.30][s230]

## Прямые проверки и близкие сценарии

| Пункт и Java-тест | Что происходит на каждой позиции | Ожидание спецификации и отношение к правилу |
| --- | --- | --- |
| 2.22 [`controlOfArgumentTypeIntersection`][s222] | В вызове процедуры `МакетПечати(Справочники.Справочник1.СоздатьЭлемент())` параметр `Док` описан как `ДокументОбъект.Документ1`. Вложенный вызов без аргументов даёт `СправочникОбъект.Справочник1` из метаданных. Проверяемое выражение — весь аргумент внешнего вызова. | Прямая проверка несовместимости. JUnit ожидает ключ `IncompatibleArgumentType`, который зарегистрирован у правила и получен в CLI. |
| 2.30 [`controlOfArgumentTypeMismatch`][s230] | То же выражение передаётся функции `МакетПечати` с таким же контрактом. Результат присваивается `Ответ`. В теле есть `Возврат Док`, а ожидаемая ошибка относится к аргументу в месте вызова. | Прямая проверка передачи в функцию. Ожидаемый ключ JUnit и фактический ключ SARIF совпадают — `IncompatibleArgumentType`. |
| 2.31 [`noErrorsInsideBodyWithTwoComputedParameterTypes`][s231] | В одной строке `ОбработкаОбъекта` получает `СправочникОбъект.Справочник1`, в другой — `ДокументОбъект.Документ1`. У `Док` отсутствует объявленный контракт. Запись `Док.Реквизит1 = ""` в теле — отдельная операция. Этот реквизит есть в XML [справочника][catalog] и [документа][document]. | На обоих вызовах правило пропускает проверку из-за неизвестного ожидаемого типа. `IncompatibleArgumentType` входит в набор ключей, запрещённых тестом. Проверка свойства для двух расчётных типов относится к анализу тела. |
| 2.23 [`controlOfDeclaredVersusComputedFieldTypes`][s223] | В вызове `Данные.Вставить("Ссылка", 10)` первый аргумент `Строка` совпадает с типом ключа в HBK. Второй аргумент `Число` допустим для параметра `Произвольный`, хотя в комментарии конкретному полю `Ссылка` назначен тип `СправочникСсылка.Справочник1`. | Вызов допустим по сигнатуре метода. `DeclaredTypeMismatch` требует сверки значения с объявлением конкретного поля. |
| 4.18 [`controlOfMixedArrayValues`][s418] | Три вызова `Массив.Добавить` передают значения типов `Число`, `Строка` и `СправочникСсылка.Справочник1` из вызова `ПустаяСсылка()`. Контракт HBK у `Добавить` допускает `Произвольный` во всех трёх вызовах. | Все три передачи допустимы по сигнатуре. `DissimilarCollectionValues` требует сравнить значения, попавшие в один массив через разные вызовы. |

Диагностика посещает обычные и квалифицированные вызовы и ставит `Error` на диапазон выражения аргумента. Для BSL-метода она берёт тип из описания формального параметра. При неизвестном типе на любой стороне доказанного конфликта нет. С несколькими сигнатурами достаточно одной подходящей целиком. В § 2.31 комментарий с типом отсутствует, поэтому множество ожидаемых типов пусто даже при известных типах обоих аргументов. [Посещение вызовов и диапазон][impl], [контракты BSL-методов и совместимость][contracts], [тип формального параметра][parameter], [проверка целой сигнатуры][overloads]

В Java-спецификации `codesFor` собирает уникальные коды из `getDiagnostics()`. В § 2.22 и § 2.30 утверждения проверяют присутствие `IncompatibleArgumentType`, а в § 2.31 — отсутствие всех ключей из набора, включая `IncompatibleArgumentType`. Отдельный `IncompatibleArgumentTypeDiagnosticTest` проверяет диапазоны и уровень ошибки, вызовы общего модуля и модуля объекта, а также целые перегрузки. [Ключ и набор ключей спецификации][keys], [извлечение кодов][assertions], [JUnit диагностики][tests]

## Остальные сценарии

Здесь предмет проверки — объявления, свойства, присваивания или другие операции. Ожидаемый в Java ключ приведён для каждого сценария.

| Пункт и Java-тест | Исполняемый сценарий |
| --- | --- |
| 2.18 [`controlOfMissingTypeAtCreation`][s218] | `Значение = НеизвестнаяФункция()`. Требуется `MissingType` при создании переменной. Вызов без аргументов. |
| 2.19 [`controlOfMissingTypeAtUsage`][s219] | После `НеизвестнаяФункция()` выполняется `Значение.НесуществующееПоле = 1`. `UntypedMemberAccess` относится к доступу к свойству. |
| 2.20 [`controlOfReturnTypes`][s220] | Функция возвращает `1` без объявления типа возврата. После вызова без аргументов идёт запись в свойство результата. Ожидается `MissingReturnType`. |
| 2.21 [`controlOfTypeChange`][s221] | `Значение` последовательно принимает число и строку. `TypeChange` относится к последовательности присваиваний. |
| 2.24 [`strictTypesAnnotationEnablesControl`][s224] | В модуле с `//@strict-types` переменной присваивают результат вызова без аргументов. Тест проверяет включение `MissingType`. В прямых примерах § 2.22 и § 2.30 аннотации нет. |
| 4.3 [`controlOfInitializationInsideLoop`][s403] | `Накопитель` получает значение внутри цикла, затем читается после него. Ожидается `VariableInitializationInLoop`. |
| 4.12 [`controlOfStructureKeyTypeChange`][s412] | После конструктора `Новый Структура("Ключ", 0)` свойству `Ключ` присваивают строку. Ожидается `TypeChange`. Конструктор находится за пределами двух обработчиков вызовов правила. |
| 4.51 [`controlOfExportedMethodTypes`][s451] | У экспортной процедуры `ЭкспортнаяБезТипов` параметр без описания. Пример не вызывает процедуру и не содержит функции. Ожидается `MissingTypeDescription`. |
| 4.52 [`controlOfDetachedMethodTypes`][s452] | `БезПрямогоВызова` записывает свойство параметра без описания, вызовов процедуры в модуле нет. Ожидается `MissingTypeDescription`. |
| 4.55 [`controlOfStringIndexAccess`][s455] | В `Элементы["Номер"].Видимость = Ложь` строка служит индексом коллекции. Ожидается `StringIndexAccess`. |
| 4.61 [`controlOfArbitraryFormParameter`][s461] | У параметра обработчика указан `Произвольный`, тело читает `Параметры.Ссылка`. `ArbitraryFormParameter` относится к декларации. Фикстура не вызывает обработчик. |
| 4.62 [`controlOfUndeclaredFormParameters`][s462] | `Параметры.НеописанныйПараметр` — чтение свойства формы. Ожидается `UndeclaredFormParameter`. |
| 4.68 [`controlOfDissimilarCompositeTypes`][s468] | Контракт `Строка, СправочникОбъект.Справочник1` объявлен у процедуры без вызовов. `DissimilarCompositeType` оценивает сочетание типов в декларации. Для проверки аргумента достаточно совместимости с одним из объявленных типов. |

## Проверка исходных JUnit-методов

На Java 25 с тестовой базой ветки прошли все три выбранных метода из исходного класса `SpecStrictTypingControlTest` коммита `447dde1e005ed72aacc998622b2c3579fe9204cd`. [§ 2.22][s222] и [§ 2.30][s230] подтвердили наличие `IncompatibleArgumentType`, [§ 2.31][s231] — отсутствие ключей из `TYPE_CONTROL_KEYS`, включая этот ключ. Оригинал Java-файла сверили с Git blob, временно добавили в тестовые исходники и удалили после прогона. В этом запуске Gradle выбирал ровно три метода.

## Проверка через CLI

На Java 25 прогнаны все 18 BSL-блоков из спецификации, каждый в отдельном модуле, и три контрольных модуля. Фикстура метаданных взята из того же коммита, что и Java-класс. CLI зарегистрировал 126 объектов метаданных и 2 495 платформенных контекстов, обработал 44 BSL-файла вместе с модулями фикстуры и завершился с кодом 0. Выполнялась только `IncompatibleArgumentType`. Использованы реальная HBK 8.3.27.1786 и JAR ветки с SHA-256 `a359e73854d2c020be31475b0c16a2e5493d570a68e555b28d9f62b2512c7f36`.

| Пример | Результат SARIF |
| --- | --- |
| § 2.22 | Один `error` на `Справочники.Справочник1.СоздатьЭлемент()` в вызове процедуры. Это строка [216 во входном Java-файле][s222], строка 8, столбцы 15–55 в извлечённом BSL. |
| § 2.30 | Один `error` на том же выражении в вызове функции. Это строка [300 во входном Java-файле][s230], строка 8, столбцы 23–63 в извлечённом BSL. |
| § 2.31, § 2.23, § 4.18 и остальные 13 блоков | Ноль результатов этой диагностики. Сигнатуры `Структура.Вставить` и `Массив.Добавить` дополнительно сверены через загрузчик HBK. |
| § 2.31 с добавленным типом `ДокументОбъект.Документ1` у `Док` | Один `error` на аргументе-справочнике. Вызов с документом проходит. |
| Тот же контроль с `НеизвестнаяФункция()` вместо аргумента-справочника | Ноль результатов. Тип первого аргумента неизвестен и правило пропускает его. |
| `Коллекция.Вставить("строка", 0)` для нового `Массив` | Один `error` на первом аргументе `"строка"`. По HBK индекс должен быть `Число`. |

Столбцы SARIF отсчитываются с единицы, конец диапазона исключён. Всего четыре результата с идентификатором `IncompatibleArgumentType`. Все четыре диапазона, уровни и тексты совпали с прогоном до переименования. Новый ключ зарегистрирован в SARIF и указан в ссылке на справку. Вывод типов внутри метода и остальные ожидаемые диагностики остаются предметом отдельных проверок.

Для воспроизведения положите извлечённые BSL-блоки в `input/`, экспортируйте туда [метаданные Designer][metadata] в `input/designer/` и задайте в `config.json` абсолютный путь к ним в `configurationRoot`, путь к HBK 8.3.27.1786 в `v8platform.binPath`, `diagnostics.mode` равным `ONLY` и `diagnostics.parameters` равным `{"IncompatibleArgumentType": true}`.

```sh
java -jar bsl-language-server.jar analyze -w input -s input -c config.json -o out -r sarif
```

[spec]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java
[s218]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L119-L130
[s219]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L139-L151
[s220]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L160-L176
[s221]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L185-L197
[s222]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L206-L223
[s223]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L233-L247
[s224]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L262-L276
[s230]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L282-L307
[s231]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L318-L334
[s403]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L345-L359
[s412]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L367-L379
[s418]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L388-L402
[s451]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L411-L422
[s452]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L432-L443
[s455]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L460-L472
[s461]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L481-L494
[s462]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L503-L515
[s468]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L526-L539
[keys]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L64-L106
[assertions]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L548-L554
[metadata]: https://github.com/1c-syntax/bsl-language-server/tree/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/resources/metadata/designer
[catalog]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/resources/metadata/designer/Catalogs/%D0%A1%D0%BF%D1%80%D0%B0%D0%B2%D0%BE%D1%87%D0%BD%D0%B8%D0%BA1.xml#L84-L92
[document]: https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/resources/metadata/designer/Documents/%D0%94%D0%BE%D0%BA%D1%83%D0%BC%D0%B5%D0%BD%D1%821.xml#L79-L87
[impl]: ../../src/main/java/com/github/_1c_syntax/bsl/languageserver/diagnostics/IncompatibleArgumentTypeDiagnostic.java
[impl-before]: https://github.com/artbear/bsl-language-server/blob/358b1fc59b51394c80d10256d92103fc4ead2579/src/main/java/com/github/_1c_syntax/bsl/languageserver/diagnostics/IncompatibleMethodArgumentTypeDiagnostic.java#L39-L79
[contracts]: https://github.com/artbear/bsl-language-server/blob/358b1fc59b51394c80d10256d92103fc4ead2579/src/main/java/com/github/_1c_syntax/bsl/languageserver/types/MethodArgumentTypes.java#L176-L229
[parameter]: https://github.com/artbear/bsl-language-server/blob/358b1fc59b51394c80d10256d92103fc4ead2579/src/main/java/com/github/_1c_syntax/bsl/languageserver/types/index/SymbolTypeIndex.java#L387-L400
[overloads]: https://github.com/artbear/bsl-language-server/blob/358b1fc59b51394c80d10256d92103fc4ead2579/src/main/java/com/github/_1c_syntax/bsl/languageserver/types/MethodArgumentTypes.java#L80-L135
[tests]: ../../src/test/java/com/github/_1c_syntax/bsl/languageserver/diagnostics/IncompatibleArgumentTypeDiagnosticTest.java
[tests-before]: https://github.com/artbear/bsl-language-server/blob/358b1fc59b51394c80d10256d92103fc4ead2579/src/test/java/com/github/_1c_syntax/bsl/languageserver/diagnostics/IncompatibleMethodArgumentTypeDiagnosticTest.java#L53-L124
