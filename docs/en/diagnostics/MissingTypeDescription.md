# Missing parameter or return type description (MissingTypeDescription)

## Description

Document the type of each exported method parameter and the return type of each exported function. Document local method parameters when the module contains no direct call from another method or the module body. This includes handlers and methods called only dynamically. A link to another method description is accepted in the same way as in [MissingParameterDescription](MissingParameterDescription.md).

The description gives callers a type contract. Static analysis can use it for type checks and editor suggestions when the call is outside the module or is made dynamically.

## Examples

```bsl
Function AmountIncludingTax(Amount) Export
    Return Amount * 1.2;
EndFunction
```

```bsl
// Parameters:
//  Amount - Number - amount before tax.
// Returns:
//  Number - amount including tax.
Function AmountIncludingTax(Amount) Export
    Return Amount * 1.2;
EndFunction
```

## Sources

- [Exported procedures and functions in the 1C coding guide](https://github.com/1C-Company/v8-code-style/blob/c3d043dbd72e82301c99ac29a2743dcc22507cc1/docs/checks/code_typification.md#экспортные-процедуры-и-функции) ([test scenario § 4.51](https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L405-L420)).
- [Indirect calls in the 1C coding guide](https://github.com/1C-Company/v8-code-style/blob/c3d043dbd72e82301c99ac29a2743dcc22507cc1/docs/checks/code_typification.md#разрыв-прямого-контекста-выполнения-кода) ([test scenario § 4.52](https://github.com/1c-syntax/bsl-language-server/blob/447dde1e005ed72aacc998622b2c3579fe9204cd/src/test/java/com/github/_1c_syntax/bsl/languageserver/types/SpecStrictTypingControlTest.java#L425-L441)).
