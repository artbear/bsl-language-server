# Assignment to a form attribute in a no-context method (AssignToFormAttributeInNoContext)

## Description

Managed form methods annotated with `&AtServerNoContext` or `&AtClientAtServerNoContext` cannot access form context.
Assigning to a form attribute name creates a local variable instead of updating the form. Writing an object property through such an attribute is also incorrect.
The diagnostic uses the current form's metadata. Explicit local variables and parameters shadowing an attribute are excluded. Metadata is required.

## Examples

```bsl
// PictureAddress is a form attribute.
&AtServerNoContext
Procedure StorePicture(Picture)
    PictureAddress = PutToTempStorage(Picture); // Incorrect
EndProcedure
```

Use `&AtServer` to update form attributes, or return the computed value and assign it in a method with form context.
