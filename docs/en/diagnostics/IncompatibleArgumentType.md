# Incompatible method argument type (IncompatibleArgumentType)

<!-- The block above is generated automatically, do not edit -->
## Description

The diagnostic flags incompatible arguments to a global or object method if every signature matching the call shape can be ruled out. It checks each signature as a whole. If different arguments rule out different overloads, the message identifies an incompatible combination.

Signatures come from BSL method comments, the installed platform's Syntax Assistant, and configuration types. To enable platform signatures, set `v8platform.binPath` to the platform's `bin` directory or use automatic platform detection. The `shlang_*.hbk` files from the same installation supply the primitive parameter types.

A separate diagnostic checks for missing arguments. This check skips cases where the argument, receiver, or parameter type cannot be established. An explicit `Undefined` value has a known type. The check uses the types declared in the signature, including explicit alternatives and the types of default values in BSL declarations.

## Example

```bsl
// Parameters:
//  Data - Array - report data.
Procedure FillReport(Data)
EndProcedure

Procedure GenerateReport()
    FillReport("text"); // Error on this argument
    Data = New Array;
    FillReport(Data);
EndProcedure
```
