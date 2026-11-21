# Undeclared managed form parameter (UndeclaredFormParameter)

## Description

Declare each managed form parameter that the form module accesses through its `Parameters` property, including optional parameters. The diagnostic recognizes both standard parameters and parameters declared in form metadata.

[UnknownMember](UnknownMember.md) reports these accesses as part of its general member check. This key lets you enable the check for form parameters alone.

## Example

```bsl
// SelectionMode must be declared in the form.
Mode = Parameters.SelectionMode;
```
