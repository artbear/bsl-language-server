# Assigned value conflicts with the declared field type

The diagnostic compares the value's inferred type against the field type declared
in a documentation comment for a variable or parameter. It checks named field
assignments and calls to `Insert` on structures.

```bsl
// Parameters:
//  Data - Structure:
//  * Amount - Number
Procedure Update(Data)
    Data.Insert("Amount", "10");
EndProcedure
```

Pass the number `10` when the field stores an amount. See the
[strict typing recommendation](https://github.com/1C-Company/v8-code-style/blob/c3d043dbd72e82301c99ac29a2743dcc22507cc1/docs/checks/code_typification.md).
