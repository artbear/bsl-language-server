# Missed a required method parameter (MissedRequiredParameter)

<!-- Блоки выше заполняются автоматически, не трогать -->
## Description

Required parameters must not be omitted when calling methods, otherwise the value `Undefined` will be passed to the parameter, which the method often cannot process.
If the value `Undefined` is valid, then you need to
- explicitly pass a value
- or make the parameter optional with a default value of `Undefined`.
The check uses resolved signatures of global platform functions, object and common module methods, and constructors. Unknown receivers are skipped. One overload without missing required arguments is sufficient. Default values and variadic tails do not require an argument. Local methods take precedence over global platform functions with the same name.

The `requiredParameters` option can require explicit optional arguments: `Format,1-2 | NumberInWords,2`. Positions start at 1; hyphens separate individual positions, not ranges. Names are case insensitive; both platform name translations are accepted. Only known parameters of resolved signatures are affected. An empty setting retains signature requirements only.

## Examples

For example

```bsl
Procedure ChangeFormFieldColor(Form, FiledName, Color)
```

Incorrect:

```bsl
ChangeFormFieldColor(,"Result", StyleColors.ArthursShirtColor); // missing first parameter Form
ChangeFormFieldColor(,,); // missing all required parameters
```

Correct:

```bsl
ChangeFormFieldColor(ThisObject, "Result", Color); // all required parameters are specified
```

## Sources

[Parameters of procedures and functions (RU)](https://its.1c.ru/db/v8std#content:640:hdoc)
