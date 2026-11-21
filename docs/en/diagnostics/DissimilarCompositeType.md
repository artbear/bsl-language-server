# Dissimilar types in a composite type (DissimilarCompositeType)

## Description

The diagnostic reports declared parameter and return types that combine unrelated types, such as a string and a catalog object, a number and an array, or a value table and a value tree. References to different catalogs can be combined. Optional `Undefined` and `Null` alternatives are ignored. The check uses declarations only.

## Examples

The two alternatives require different operations. Pass a number when the method handles a number, and format it separately when you need text.

```bsl
// Parameters:
//  Amount - Number, String - amount.
Procedure SaveAmount(Amount)
    Save(Amount);
EndProcedure
```

```bsl
// Parameters:
//  Amount - Number - amount.
Procedure SaveAmount(Amount)
    Save(Amount);
EndProcedure
```

[Composite type limitations in the 1C coding guide](https://github.com/1C-Company/v8-code-style/blob/c3d043dbd72e82301c99ac29a2743dcc22507cc1/docs/checks/code_typification.md#ограничения-составных-типов).
