# Array contains dissimilar values

The rule compares types across consecutive `Add` and `Insert` calls to one
variable of known type `Array` in a straight-line block. A new instance,
a different operation, or a branch boundary starts another sequence.

```bsl
Values = New Array;
Values.Add(10);
Values.Add("ten");
```

Use separate collections or a typed record for related values of different types.
See the [strict typing recommendation](https://github.com/1C-Company/v8-code-style/blob/c3d043dbd72e82301c99ac29a2743dcc22507cc1/docs/checks/code_typification.md).
