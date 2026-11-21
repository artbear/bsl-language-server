# String index for a known form item (StringIndexAccess)

## Description

The diagnostic reports a literal string index when the form item metadata makes the same item available through dot notation. Numeric and computed indices are allowed.

## Examples

```bsl
Items["Number"].Visible = False;
```

```bsl
Items.Number.Visible = False;
```
