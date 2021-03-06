# Диагностики

Используются для проверки кода на соответствие стандартам кодирования и для поиска возможных ошибок.

Некоторые диагностики выключены по умолчанию. Для их включения используйте [конфигурационный файл](../features/ConfigurationFile.md)</a>.

Для экранирования отдельных участков кода или файлов от срабатывания диагностик можно воспользоваться специальными комментариями вида `// BSLLS:КлючДиагностики-выкл`. Более подробно данная функциональность описана в [Экранирование участков кода](../features/DiagnosticIgnorance.md).

## Список реализованных диагностик

Общее количество: **86**

* Дефект кода: **54**
* Уязвимость: **2**
* Ошибка: **28**
* Потенциальная уязвимость: **2**

| Ключ | Название | Включена по умолчанию | Важность | Тип | Тэги |
| --- | --- | :-: | --- | --- | --- |
| [BeginTransactionBeforeTryCatch](BeginTransactionBeforeTryCatch.md) | Нарушение правил работы с транзакциями для метода 'НачатьТранзакцию' | Да | Важный | Ошибка | `standard` |
| [CanonicalSpellingKeywords](CanonicalSpellingKeywords.md) | Каноническое написание ключевых слов | Да | Информационный | Дефект кода | `standard` |
| [CodeBlockBeforeSub](CodeBlockBeforeSub.md) | Определения методов должны размещаться перед операторами тела модуля | Да | Блокирующий | Ошибка | `error` |
| [CodeOutOfRegion](CodeOutOfRegion.md) | Код расположен вне области | Да | Информационный | Дефект кода | `standard` |
| [CognitiveComplexity](CognitiveComplexity.md) | Когнитивная сложность | Да | Критичный | Дефект кода | `brainoverload` |
| [CommentedCode](CommentedCode.md) | Закомментированный фрагмент кода | Да | Незначительный | Дефект кода | `standard`<br/>`badpractice` |
| [CommitTransactionOutsideTryCatch](CommitTransactionOutsideTryCatch.md) | Нарушение правил работы с транзакциями для метода 'ЗафиксироватьТранзакцию' | Да | Важный | Ошибка | `standard` |
| [CompilationDirectiveLost](CompilationDirectiveLost.md) | Директивы компиляции методов | Да | Важный | Дефект кода | `standard`<br/>`unpredictable` |
| [CompilationDirectiveNeedLess](CompilationDirectiveNeedLess.md) | Лишняя директива компиляции | Да | Важный | Дефект кода | `clumsy`<br/>`standard`<br/>`unpredictable` |
| [CreateQueryInCycle](CreateQueryInCycle.md) | Выполнение запроса в цикле | Да | Критичный | Ошибка | `performance` |
| [CyclomaticComplexity](CyclomaticComplexity.md) | Цикломатическая сложность | Да | Критичный | Дефект кода | `brainoverload` |
| [DeletingCollectionItem](DeletingCollectionItem.md) | Удаление элемента при обходе коллекции посредством оператора "Для каждого ... Из ... Цикл" | Да | Важный | Ошибка | `standard`<br/>`error` |
| [DeprecatedCurrentDate](DeprecatedCurrentDate.md) | Использование устаревшего метода "ТекущаяДата" | Да | Важный | Ошибка | `standard`<br/>`deprecated`<br/>`unpredictable` |
| [DeprecatedFind](DeprecatedFind.md) | Использование устаревшего метода "Найти" | Да | Незначительный | Дефект кода | `deprecated` |
| [DeprecatedMessage](DeprecatedMessage.md) | Ограничение на использование устаревшего метода "Сообщить" | Да | Незначительный | Дефект кода | `standard`<br/>`deprecated` |
| [DeprecatedTypeManagedForm](DeprecatedTypeManagedForm.md) | Устаревшее использование типа "УправляемаяФорма" | Да | Информационный | Дефект кода | `standard`<br/>`deprecated` |
| [DuplicateRegion](DuplicateRegion.md) | Повторяющиеся разделы модуля | Да | Информационный | Дефект кода | `standard` |
| [EmptyCodeBlock](EmptyCodeBlock.md) | Пустой блок кода | Да | Важный | Дефект кода | `badpractice`<br/>`suspicious` |
| [EmptyRegion](EmptyRegion.md) | Область не должна быть пустой | Да | Информационный | Дефект кода | `standard` |
| [EmptyStatement](EmptyStatement.md) | Пустой оператор | Да | Информационный | Дефект кода | `badpractice` |
| [ExtraCommas](ExtraCommas.md) | Запятые без указания параметра в конце вызова метода | Да | Важный | Дефект кода | `standard`<br/>`badpractice` |
| [FormDataToValue](FormDataToValue.md) | Использование метода ДанныеФормыВЗначение | Да | Информационный | Дефект кода | `badpractice` |
| [FunctionNameStartsWithGet](FunctionNameStartsWithGet.md) | Имя функции не должно начинаться с "Получить" | Нет | Информационный | Дефект кода | `standard` |
| [FunctionShouldHaveReturn](FunctionShouldHaveReturn.md) | Функция должна содержать возврат | Да | Важный | Ошибка | `suspicious`<br/>`unpredictable` |
| [GetFormMethod](GetFormMethod.md) | Использование метода ПолучитьФорму | Да | Важный | Ошибка | `error` |
| [IdenticalExpressions](IdenticalExpressions.md) | Одинаковые выражения слева и справа от "foo" оператора | Да | Важный | Ошибка | `suspicious` |
| [IfConditionComplexity](IfConditionComplexity.md) | Использование сложных выражений в условии оператора "Если" | Да | Незначительный | Дефект кода | `brainoverload` |
| [IfElseDuplicatedCodeBlock](IfElseDuplicatedCodeBlock.md) | Повторяющиеся блоки кода в синтаксической конструкции Если...Тогда...ИначеЕсли... | Да | Незначительный | Дефект кода | `suspicious` |
| [IfElseDuplicatedCondition](IfElseDuplicatedCondition.md) | Повторяющиеся условия в синтаксической конструкции Если...Тогда...ИначеЕсли... | Да | Важный | Дефект кода | `suspicious` |
| [IfElseIfEndsWithElse](IfElseIfEndsWithElse.md) | Использование синтаксической конструкции Если...Тогда...ИначеЕсли... | Да | Важный | Дефект кода | `badpractice` |
| [InvalidCharacterInFile](InvalidCharacterInFile.md) | Недопустимый символ | Да | Важный | Ошибка | `error`<br/>`standard`<br/>`unpredictable` |
| [LineLength](LineLength.md) | Ограничение на длину строки | Да | Незначительный | Дефект кода | `standard`<br/>`badpractice` |
| [MagicNumber](MagicNumber.md) | Магические числа | Да | Незначительный | Дефект кода | `badpractice` |
| [MethodSize](MethodSize.md) | Ограничение на размер метода | Да | Важный | Дефект кода | `badpractice` |
| [MissingCodeTryCatchEx](MissingCodeTryCatchEx.md) | Конструкция "Попытка...Исключение...КонецПопытки" не содержит кода в исключении | Да | Важный | Ошибка | `standard`<br/>`badpractice` |
| [MissingSpace](MissingSpace.md) | Пропущены пробелы слева или справа от операторов `+ - * / = % < > <> <= >=`, а так же справа от `,` и `;` | Да | Информационный | Дефект кода | `badpractice` |
| [MissingTemporaryFileDeletion](MissingTemporaryFileDeletion.md) | Отсутствует удаление временного файла после использования | Да | Важный | Ошибка | `badpractice`<br/>`standard` |
| [MultilingualStringHasAllDeclaredLanguages](MultilingualStringHasAllDeclaredLanguages.md) | Есть локализованный текст для всех заявленных в конфигурации языков | Да | Незначительный | Ошибка | `error`<br/>`localize` |
| [MultilingualStringUsingWithTemplate](MultilingualStringUsingWithTemplate.md) | Частично локализованный текст используется в функции СтрШаблон | Да | Важный | Ошибка | `error`<br/>`localize` |
| [NestedConstructorsInStructureDeclaration](NestedConstructorsInStructureDeclaration.md) | Использование конструкторов с параметрами при объявлении структуры | Да | Незначительный | Дефект кода | `badpractice`<br/>`brainoverload` |
| [NestedFunctionInParameters](NestedFunctionInParameters.md) | Инициализация параметров методов и конструкторов вызовом вложенных методов | Да | Незначительный | Дефект кода | `standard`<br/>`brainoverload`<br/>`badpractice` |
| [NestedStatements](NestedStatements.md) | Управляющие конструкции не должны быть вложены слишком глубоко | Да | Критичный | Дефект кода | `badpractice`<br/>`brainoverload` |
| [NestedTernaryOperator](NestedTernaryOperator.md) | Вложенный тернарный оператор | Да | Важный | Дефект кода | `brainoverload` |
| [NonExportMethodsInApiRegion](NonExportMethodsInApiRegion.md) | Неэкспортные методы в областях ПрограммныйИнтерфейс и СлужебныйПрограммныйИнтерфейс | Да | Важный | Дефект кода | `standard` |
| [NonStandardRegion](NonStandardRegion.md) | Нестандартные разделы модуля | Да | Информационный | Дефект кода | `standard` |
| [NumberOfOptionalParams](NumberOfOptionalParams.md) | Ограничение на количество не обязательных параметров метода | Да | Незначительный | Дефект кода | `standard`<br/>`brainoverload` |
| [NumberOfParams](NumberOfParams.md) | Ограничение на количество параметров метода | Да | Незначительный | Дефект кода | `standard`<br/>`brainoverload` |
| [NumberOfValuesInStructureConstructor](NumberOfValuesInStructureConstructor.md) | Ограничение на количество значений свойств, передаваемых в конструктор структуры | Да | Незначительный | Дефект кода | `standard`<br/>`brainoverload` |
| [OSUsersMethod](OSUsersMethod.md) | Использование метода ПользователиОС | Да | Критичный | Потенциальная уязвимость | `suspicious` |
| [OneStatementPerLine](OneStatementPerLine.md) | Одно выражение в одной строке | Да | Незначительный | Дефект кода | `standard`<br/>`design` |
| [OrderOfParams](OrderOfParams.md) | Порядок параметров метода | Да | Важный | Дефект кода | `standard`<br/>`design` |
| [PairingBrokenTransaction](PairingBrokenTransaction.md) | Нарушение парности использования методов "НачатьТранзакцию()" и "ЗафиксироватьТранзакцию()" / "ОтменитьТранзакцию()" | Да | Важный | Ошибка | `standard` |
| [ParseError](ParseError.md) | Ошибка разбора исходного кода | Да | Критичный | Ошибка | `error` |
| [ProcedureReturnsValue](ProcedureReturnsValue.md) | Процедура не должна возвращать значение | Да | Блокирующий | Ошибка | `error` |
| [PublicMethodsDescription](PublicMethodsDescription.md) | Все методы программного интерфейса должны иметь описание | Да | Информационный | Дефект кода | `standard`<br/>`brainoverload`<br/>`badpractice` |
| [SelfAssign](SelfAssign.md) | Присвоение переменной самой себе | Да | Важный | Ошибка | `suspicious` |
| [SelfInsertion](SelfInsertion.md) | Вставка коллекции в саму себя | Да | Важный | Ошибка | `standard`<br/>`unpredictable`<br/>`performance` |
| [SemicolonPresence](SemicolonPresence.md) | Выражение должно заканчиваться символом ";" | Да | Незначительный | Дефект кода | `standard`<br/>`badpractice` |
| [SeveralCompilerDirectives](SeveralCompilerDirectives.md) | Ошибочное указание нескольких директив компиляции | Да | Критичный | Ошибка | `unpredictable`<br/>`error` |
| [SpaceAtStartComment](SpaceAtStartComment.md) | Пробел в начале комментария | Да | Информационный | Дефект кода | `standard` |
| [TempFilesDir](TempFilesDir.md) | Вызов функции КаталогВременныхФайлов() | Да | Важный | Дефект кода | `standard`<br/>`badpractice` |
| [TernaryOperatorUsage](TernaryOperatorUsage.md) | Использование тернарного оператора | Нет | Незначительный | Дефект кода | `brainoverload` |
| [TimeoutsInExternalResources](TimeoutsInExternalResources.md) | Таймауты при работе с внешними ресурсами | Да | Критичный | Ошибка | `unpredictable`<br/>`standard` |
| [TooManyReturns](TooManyReturns.md) | Метод не должен содержать много возвратов | Нет | Незначительный | Дефект кода | `brainoverload` |
| [TryNumber](TryNumber.md) | Приведение к числу в попытке | Да | Важный | Дефект кода | `standard` |
| [Typo](Typo.md) | Опечатка | Да | Информационный | Дефект кода | `badpractice` |
| [UnaryPlusInConcatenation](UnaryPlusInConcatenation.md) | Унарный плюс в конкатенации строк | Да | Блокирующий | Ошибка | `suspicious`<br/>`brainoverload` |
| [UnknownPreprocessorSymbol](UnknownPreprocessorSymbol.md) | Неизвестный символ препроцессора | Да | Критичный | Ошибка | `standard`<br/>`error` |
| [UnreachableCode](UnreachableCode.md) | Недостижимый код | Да | Незначительный | Ошибка | `design`<br/>`suspicious` |
| [UnusedLocalMethod](UnusedLocalMethod.md) | Неиспользуемый локальный метод | Да | Важный | Дефект кода | `standard`<br/>`suspicious` |
| [UnusedParameters](UnusedParameters.md) | Неиспользуемый параметр | Да | Важный | Дефект кода | `design` |
| [UseLessForEach](UseLessForEach.md) | Бесполезный перебор коллекции | Да | Критичный | Ошибка | `clumsy` |
| [UsingCancelParameter](UsingCancelParameter.md) | Работа с параметром "Отказ" | Да | Важный | Дефект кода | `standard`<br/>`badpractice` |
| [UsingExternalCodeTools](UsingExternalCodeTools.md) | Использование возможностей выполнения внешнего кода | Да | Критичный | Потенциальная уязвимость | `standard`<br/>`design` |
| [UsingFindElementByString](UsingFindElementByString.md) | Использование методов "НайтиПоНаименованию" и "НайтиПоКоду" | Да | Важный | Дефект кода | `standard`<br/>`badpractice`<br/>`performance` |
| [UsingGoto](UsingGoto.md) | Оператор "Перейти" не должен использоваться | Да | Критичный | Дефект кода | `standard`<br/>`badpractice` |
| [UsingHardcodeNetworkAddress](UsingHardcodeNetworkAddress.md) | Хранение ip-адресов в коде | Да | Критичный | Уязвимость | `standard` |
| [UsingHardcodePath](UsingHardcodePath.md) | Хранение путей к файлам в коде | Да | Критичный | Ошибка | `standard` |
| [UsingHardcodeSecretInformation](UsingHardcodeSecretInformation.md) | Хранение конфиденциальной информации в коде | Да | Критичный | Уязвимость | `standard` |
| [UsingModalWindows](UsingModalWindows.md) | Использование модальных окон | Нет | Важный | Дефект кода | `standard` |
| [UsingObjectNotAvailableUnix](UsingObjectNotAvailableUnix.md) | Использование объектов недоступных в Unix системах | Да | Критичный | Ошибка | `standard`<br/>`lockinos` |
| [UsingServiceTag](UsingServiceTag.md) | Использование служебных тегов | Да | Информационный | Дефект кода | `badpractice` |
| [UsingSynchronousCalls](UsingSynchronousCalls.md) | Использование синхронных вызовов | Нет | Важный | Дефект кода | `standard` |
| [UsingThisForm](UsingThisForm.md) | Использование устаревшего свойства "ЭтаФорма" | Да | Незначительный | Дефект кода | `standard`<br/>`deprecated` |
| [WrongUseOfRollbackTransactionMethod](WrongUseOfRollbackTransactionMethod.md) | Некорректное использование метода ОтменитьТранзакцию() | Да | Критичный | Ошибка | `standard` |
| [YoLetterUsage](YoLetterUsage.md) | Использование буквы "ё" в текстах модулей | Да | Информационный | Дефект кода | `standard` |