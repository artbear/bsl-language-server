# Type change of a variable or structure key (TypeChange)

## Description

The diagnostic reports an assignment to a local variable or an existing structure key when the new value has a type disjoint from the known types immediately before the assignment. `Undefined` may be used to initialize or reset a value.

## Examples

```bsl
Amount = 10;
Amount = "Ten";
```

```bsl
Amount = 10;
AmountText = "Ten";
```
