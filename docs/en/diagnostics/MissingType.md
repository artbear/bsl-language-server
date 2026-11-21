# Variable type is missing at initialization

At the first assignment to a local variable, the expression or the variable
description should establish the value's type. The rule checks simple
assignments in exported and non-exported methods.

In normal mode the rule applies to modules with `//@strict-types` before the
first semantic element. Explicitly enable `MissingType` in the configuration
to check modules without the annotation.

```bsl
Data = LoadData();
```

If the loader guarantees a structure, declare it:

```bsl
Data = LoadData(); // Structure
```

See the [strict typing recommendation](https://github.com/1C-Company/v8-code-style/blob/c3d043dbd72e82301c99ac29a2743dcc22507cc1/docs/checks/code_typification.md).
