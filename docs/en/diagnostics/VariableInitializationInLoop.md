# Variable is initialized in a loop or branch

The diagnostic highlights a read after a loop or a branch when a path reaches
the read without assigning the variable. Initializing before the loop or in
every branch closes that path.

```bsl
For Each Row In Rows Do
    LastRow = Row;
EndDo;
Process(LastRow);
```

An empty collection skips the loop. Initialize `LastRow` before the loop,
for example with `Undefined`, and handle that value when reading it.

See the [strict typing recommendation](https://github.com/1C-Company/v8-code-style/blob/c3d043dbd72e82301c99ac29a2743dcc22507cc1/docs/checks/code_typification.md).

A numeric For loop initializes its counter before testing the bounds. A read
of the previous value in its initializer still precedes that assignment.
