# Missed a required method parameter (MissedRequiredParameter)

<!-- Блоки выше заполняются автоматически, не трогать -->
## Description

Omitting a required argument can cause a runtime error in a platform method. A user-defined method may receive `Undefined` for a parameter that cannot handle it. If `Undefined` is valid, pass it explicitly or declare it as the parameter's default in the user-defined method.

The check uses resolved signatures of global platform functions, object and common module methods, and constructors. Unknown receivers are skipped. One overload without missing required arguments is sufficient. Default values and variadic tails do not require an argument. Local methods take precedence over global platform functions with the same name.

For platform methods, including metadata object methods, parameter requirements come from signatures loaded from the 1C Syntax Assistant (HBK). To locate these files, you can set `v8platform.binPath` to the platform's `bin` directory. The Russian and English method names use the same signature.

## Examples

`Query.SetParameter("Code")` omits the required `Value` argument. The correct call is `Query.SetParameter("Code", CodeValue)`.

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
