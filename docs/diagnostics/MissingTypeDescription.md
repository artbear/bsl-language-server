# Отсутствует описание типа параметра или результата (MissingTypeDescription)

## Описание диагностики

Описывайте тип каждого параметра экспортного метода и тип результата экспортной функции. Для локального метода типы параметров нужны, если в модуле нет прямого вызова из другого метода или из тела модуля. Это включает обработчики и методы, которые вызываются только динамически. Ссылка на описание другого метода учитывается так же, как в [MissingParameterDescription](MissingParameterDescription.md).

Описание задаёт контракт для вызывающего кода. Когда вызов находится вне модуля или выполняется динамически, статический анализатор может использовать этот контракт для проверки типов и подсказок.

## Примеры

```bsl
Функция СуммаСНалогом(Сумма) Экспорт
    Возврат Сумма * 1.2;
КонецФункции
```

```bsl
// Параметры:
//  Сумма - Число - сумма без налога.
// Возвращаемое значение:
//  Число - сумма с налогом.
Функция СуммаСНалогом(Сумма) Экспорт
    Возврат Сумма * 1.2;
КонецФункции
```

## Источники

- [Экспортные процедуры и функции](https://github.com/1C-Company/v8-code-style/blob/c3d043dbd72e82301c99ac29a2743dcc22507cc1/docs/checks/code_typification.md#экспортные-процедуры-и-функции) ([§ 4.51 полигона](https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L405-L420)).
- [Разрыв прямого контекста выполнения кода](https://github.com/1C-Company/v8-code-style/blob/c3d043dbd72e82301c99ac29a2743dcc22507cc1/docs/checks/code_typification.md#разрыв-прямого-контекста-выполнения-кода) ([§ 4.52 полигона](https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L425-L441)).
