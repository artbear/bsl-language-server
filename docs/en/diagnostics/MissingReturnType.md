# Unknown function return type (MissingReturnType)

## Description

The diagnostic reports a return expression if its type is unknown and the function has no declared return type. MissingTypeDescription checks explicit documentation of exported functions separately. [UnknownMember](UnknownMember.md) checks missing members of a result with a known type.

## Examples

```bsl
Function GetText()
    Return ExternalLibrary.Get();
EndFunction
```

```bsl
// Returns:
//  String - value from the external library.
Function GetText()
    Return ExternalLibrary.Get();
EndFunction
```
