# Missing query parameter (MissingQueryParameter)

<!-- Блоки выше заполняются автоматически, не трогать -->
## Description

The rule checks the parameters of a `Query` before calls to `Execute`, `ExecuteBatch` and `ExecuteBatchWithIntermediateData`. It highlights the first occurrence of each missing name in the query language. Related locations point to the executions. Parameter names are case-insensitive.

If only some reachable paths set the parameter, the message describes a possible omission. For an unexecuted local query, the rule also checks normal method exits while the method still owns the object. Setting a parameter after execution affects subsequent calls.

The analysis follows text assignments, aliases, constant concatenation and replacement, and unambiguous helpers in the same module. A dynamic text or an opaque mutation requires independent evidence from a path with a known query state.

## Examples

The following call can execute without a value for `Warehouse`.

```bsl
Query = New Query("SELECT &Warehouse AS Warehouse");
If ApplyFilter Then
    Query.SetParameter("Warehouse", SelectedWarehouse);
EndIf;
Return Query.Execute();
```

If the query should run only when a filter is present, move the execution into the same branch. If it always needs the parameter, set it before the condition.

## Sources

* [Issue describing the parameter check](https://github.com/1c-syntax/bsl-language-server/issues/536)
