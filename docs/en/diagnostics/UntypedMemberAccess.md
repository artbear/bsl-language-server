# Member access on an untyped value (UntypedMemberAccess)

## Description

The diagnostic reports property and method accesses whose receiver type is unknown in every considered context. It reports the first such access in each chain. An explicit `Arbitrary` type is allowed. [UnknownMember](UnknownMember.md) checks member names on known types.

## Examples

The parameter type is unknown at the call to `Count()`.

```bsl
Procedure PrintCount(Input) Export
    Message(Input.Count());
EndProcedure
```

The parameter description specifies an array.

```bsl
// Parameters:
//  Input - Array - items to count.
Procedure PrintCount(Input) Export
    Message(Input.Count());
EndProcedure
```
