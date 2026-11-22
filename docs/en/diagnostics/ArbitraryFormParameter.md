# Arbitrary parameter carries a form initialization structure

The rule checks `OpenForm` calls when the analyzer resolves a specific managed
form and knows the fields of the outer parameter structure. It highlights a
nested structure passed to a declared arbitrary form parameter.

```bsl
Data = New Structure("SelectionMode", True);
OpenForm("Document.Order.Form.DocumentForm", New Structure("Data", Data));
```

Declare `SelectionMode` as a separate form parameter and pass it in the outer
structure. See the form parameters section of the
[strict typing recommendation](https://github.com/1C-Company/v8-code-style/blob/c3d043dbd72e82301c99ac29a2743dcc22507cc1/docs/checks/code_typification.md).
