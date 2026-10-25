# Type annotation placement

You may have seen Bean Validation written like this:

```java
@Valid
private List<PhoneNumber> phoneNumbers;
```

And then encountered this spelling:

```java
private List<@Valid PhoneNumber> phoneNumbers;
```

Or, less comfortably, this one:

```java
private List<Enclosing.@Valid Inner> items;
```

The annotation inside the angle brackets is intentional. It says that validation should continue
into each element of the list. The annotation after the dot is intentional too: when an element's
class is written with an enclosing class name, that is where the annotation belongs to mark the
element itself.

This project explains how to read and use those spellings. Its small Java classes and tests also
show what happens when an annotation is legal Java but is written on the wrong part of the type.
The examples use [Jakarta Validation][validation-spec], the current name of Bean Validation, and
[Hibernate Validator][hibernate-validator] to run the checks.

## Start with one object

Suppose a phone number is represented by a class with a constraint on its `number` field:

```java
import jakarta.validation.constraints.NotBlank;

class PhoneNumber {

    @NotBlank
    private final String number;

    PhoneNumber(String number) {
        this.number = number;
    }
}
```

[`@NotBlank`][notblank] requires a non-null character sequence with at least one non-whitespace
character. A `PhoneNumber` constructed with `""` therefore has an invalid field.

Creating that object does not itself run validation. Assigning it to a field or adding it to a
list does not run validation either. Something must ask a validator to check it. In a standalone
program that means calling [`Validator.validate()`][validator-api]; a framework may make that call
on your behalf.

When the phone number is the object handed directly to `validate()`, the validator checks its
`number` field. But what if the object handed to the validator is a contact holding a list of phone
numbers? The validator needs to be told to continue from the contact into the phone numbers.
That instruction is `@Valid`.

## Put the instruction on the list element

```java
private List<@Valid PhoneNumber> phoneNumbers;
```

Read this as: “A list of phone numbers; when this bean is validated, validate each phone number too.”

`List<PhoneNumber>` already says what kind of object the list holds. Putting `@Valid` before
`PhoneNumber`, inside the angle brackets, adds an instruction about those objects. The instruction
is called **cascaded validation**: validation continues into another object and checks the
constraints declared there.

`@Valid` is not itself a rule saying that a phone number is well formed. In this example that rule
is the `@NotBlank` on `PhoneNumber.number`. If `PhoneNumber` declared no constraints and no further
cascades, adding `@Valid` would not invent any checks for it.

There are three separate things you may want to require:

```java
@NotNull
@Size(min = 1)
private List<@NotNull @Valid PhoneNumber> phoneNumbers;
```

- The `@NotNull` before the field requires the **list itself** to be present.
- The `@Size(min = 1)` requires the list to contain at least one element.
- The `@NotNull` inside the angle brackets requires **each element** to be present.
- The `@Valid` inside the angle brackets asks the validator to check the constraints inside each
  non-null phone number.

The two `@NotNull` annotations are not duplicates. One applies to the list; the other applies to
its elements. An empty list passes `@NotNull` but fails `@Size(min = 1)`. A list containing a null
element has a size of one, but fails the element's `@NotNull`. A list containing a phone number with
a blank `number` fails that phone number's `@NotBlank`, reached through `@Valid`.

Without the corresponding `@NotNull`, null is allowed here: [`@Size`][size] accepts a null list,
and [cascaded validation ignores null references][object-graph]. `@Valid` alone therefore requires
neither a list nor an element to be non-null. [`@NotNull`][notnull] supplies that separate rule.

The specification calls rules written on type arguments **[container element constraints][container-constraints]**.
A constraint such as `@NotNull` checks the element directly; `@Valid` asks for validation to continue
inside it. They can be used together, but they do different jobs.

## A complete validation example

Here is the whole operation, including creating a validator and reading its result. The two small
classes are inside the example so that the code can be saved as `PhoneValidationExample.java`.

```java
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import jakarta.validation.ValidatorFactory;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class PhoneValidationExample {

    static class PhoneNumber {

        @NotBlank
        private final String number;

        PhoneNumber(String number) {
            this.number = number;
        }
    }

    static class Contact {

        @NotNull
        @Size(min = 1)
        private final List<@NotNull @Valid PhoneNumber> phoneNumbers;

        Contact(List<PhoneNumber> phoneNumbers) {
            this.phoneNumbers = phoneNumbers;
        }
    }

    public static void main(String[] args) {
        Contact contact = new Contact(List.of(new PhoneNumber("")));

        try (ValidatorFactory factory = Validation.buildDefaultValidatorFactory()) {
            Validator validator = factory.getValidator();

            validator.validate(contact).stream()
                    .map(violation -> violation.getPropertyPath().toString())
                    .sorted()
                    .forEach(System.out::println);
        }
    }
}
```

With Hibernate Validator, the output is:

```text
phoneNumbers[0].number
```

That is the path to the failed constraint, not the annotation's name or the error message.
The validator starts with the contact, reads its `phoneNumbers` field, and follows the `@Valid` on
the element type into the first phone number. It then finds that object's blank `number` field.

Replace `""` with `"12345"` and there is no output. Pass `List.of()` to `Contact` and the violation
is on `phoneNumbers`, because the list is too short. Pass `null` to `Contact` and the violation is
also on `phoneNumbers`, this time because the list is missing.

To try a null element, use `java.util.Arrays.asList((PhoneNumber) null)` instead of `List.of(...)`.
[`List.of()`][list-api] refuses null elements before validation has a chance to run. Hibernate
Validator reports the null element as `phoneNumbers[0].<list element>`.

The imports are from `jakarta.validation`. The validation API supplies the annotations and
interfaces; an implementation performs the checks. This project's [Maven dependencies](pom.xml)
provide Hibernate Validator and its expression-language dependency for tests. An application may
already receive those dependencies from its framework.

The example closes the [`ValidatorFactory`][factory-api] when it is finished. In an application,
a factory is normally reused rather than constructed for every object being validated.

## The traditional spelling still works

If you already use this:

```java
@Valid
private List<PhoneNumber> phoneNumbers;
```

you have not been asking the validator to check the `List` implementation's own fields. Bean
Validation gives `@Valid` on a collection a special meaning: cascade into its elements. For a
simple list of beans, it has the same purpose as:

```java
private List<@Valid PhoneNumber> phoneNumbers;
```

The specification's [graph-validation examples][graph-examples] call the second form the preferred
style as of Bean Validation 2.0. They explicitly say the traditional form continues to be supported.
Do not put `@Valid` in both places for the same container: the [graph-validation rules][graph-validation]
advise against that to avoid validating the elements twice.

This is also a practical migration issue. Hibernate Validator 9.1.4.Final, the version used here,
declares warning [`HV000271`][hv-container-warning] for `@Valid` on a container and asks for the
annotation on its type argument instead. That is a Hibernate Validator warning about the usage,
not a Java compiler deprecation of the `@Valid` annotation.

Two changes made the element-level spelling possible:

- Java 8 introduced annotations on uses of types through [JSR 308][jsr308]. Writing an annotation
  inside `List<...>` is Java syntax, not a new kind of generic type.
- [Bean Validation 2.0, JSR 380][jsr380], added support for container element constraints and
  cascading through type arguments. `@Valid` could then be used in that position.

Older Bean Validation examples use `javax.validation` imports. Jakarta Validation 3.0 changed the
namespace to `jakarta.validation`; the type-argument spelling predates that namespace change.
The specification's [version history][validation-history] describes both changes. Moving an
annotation inside the angle brackets does not, by itself, require changing between those namespaces.
Your imports must match the validation API and implementation your application uses.

## Why the element-level spelling is useful beyond lists

Putting an annotation on a type argument lets you say which part of a container you mean.
For example:

```java
private Map<@NotBlank String, @Valid PhoneNumber> phoneNumbersByLabel;
```

Here the map's keys must be non-blank strings, while its values are phone numbers whose own
constraints should be checked. Neither annotation requires the map itself to contain any entries.

The same idea works through nested containers:

```java
private List<List<@Valid PhoneNumber>> phoneNumberGroups;
```

The `@Valid` is on the phone number type in the inner list. It asks for each phone number to be
validated, not for a list object to be treated as a phone number.

Lists and maps work because the specification supplies [built-in value extractors][value-extractors]:
the validator knows how to obtain their elements, keys and values. A custom generic container needs
a suitable extractor too. An annotation inside arbitrary angle brackets is not enough on its own.
The [graph-validation rules][graph-validation] cover nested containers and this extractor requirement.

## When the element's name contains a dot

So far, the element has been written with a single name, `PhoneNumber`. Now suppose its class is
declared inside another class, and you refer to it as `Enclosing.Nested` or `Enclosing.Inner`.

Here is the relevant part of this project's [Enclosing.java](src/main/java/com/fillumina/typeannotation/Enclosing.java):

```java
public class Enclosing {

    @NotBlank
    public String label;

    public Enclosing(String label) {
        this.label = label;
    }

    public static class Nested {

        @NotBlank
        public String name;

        public Nested(String name) {
            this.name = name;
        }
    }

    public class Inner {

        @NotBlank
        public String name;

        public Inner(String name) {
            this.name = name;
        }
    }
}
```

These are two different kinds of member class:

- `Nested` is **static nested**. You can create it without creating an `Enclosing` object.
- `Inner` is **inner**, because it is not static. Creating it requires an enclosing instance.

The distinction is defined in [JLS §8.1.3, Inner Classes and Enclosing Instances][inner-classes].
In Java terminology, both classes are nested because they are declared inside another class.
Only the non-static one is inner; the names `Nested` and `Inner` make that difference visible here.

Creating them looks like this:

```java
Enclosing.Nested nested = new Enclosing.Nested("item");

Enclosing enclosing = new Enclosing("label");
Enclosing.Inner inner = enclosing.new Inner("item");
```

[EnclosingInstantiationTest](src/test/java/com/fillumina/typeannotation/EnclosingInstantiationTest.java)
compiles and runs those examples.

For either kind, put `@Valid` immediately before the element's own simple name:

```java
private List<Enclosing.@Valid Nested> nestedItems;
private List<Enclosing.@Valid Inner> innerItems;
```

The dot still separates the enclosing class name from the class declared inside it. The annotation
has not broken that name apart; Java allows an annotation at that point to say something about the
use of `Nested` or `Inner`.

**When you write a qualified element type, put the element's annotation after the last dot,
immediately before its simple class name.** If the class is in scope under a single name, including
through an import, the ordinary `List<@Valid Nested>` spelling works too. You do not have to add an
enclosing name just to use an annotation.

## Moving it before the enclosing name changes its meaning

It is tempting to treat the whole qualified name as one word and write:

```java
private List<@Valid Enclosing.Inner> items;
```

This compiles, but it does not put `@Valid` on `Inner`. It puts it on the enclosing part of the type,
`Enclosing`. The element itself is not marked for cascading. In the examples here, Hibernate
Validator therefore reports neither the invalid member nor its invalid enclosing instance.

This is not an alternative style for the same instruction. It is an annotation on a different
part of the type.

For a static nested class, the corresponding mistake does not compile at all:

```java
private List<@Valid Enclosing.Nested> items; // Does not compile.
```

The outcomes are collected below. “Validates” means that the element's declared constraints are
checked when the bean holding the list is validated.

| Field's element spelling | Element kind | Outcome |
| --- | --- | --- |
| `List<@Valid PlainItem>` | Top-level class | Validates each `PlainItem`. |
| `List<Enclosing.@Valid Nested>` | Static nested class | Validates each `Enclosing.Nested`. |
| `List<Enclosing.@Valid Inner>` | Inner class | Validates each `Enclosing.Inner`. |
| `List<@Valid Enclosing.Inner>` | Inner class | Compiles, but does not cascade into the elements. |
| `List<@Valid Enclosing.Nested>` | Static nested class | Does not compile. |

The four compilable field declarations are gathered in
[Spellings.java](src/test/java/com/fillumina/typeannotation/Spellings.java), alongside a comment
showing the rejected one. [ItemValidationTest](src/test/java/com/fillumina/typeannotation/ItemValidationTest.java)
checks the runtime outcomes and verifies where reflection finds `@Valid`.

The rejected spelling is not an active source line in the test suite: leaving it there would stop
the project from compiling. Compiler diagnostics vary with the JDK. The important point is the
rejection and its remedy, `Enclosing.@Valid Nested`, rather than a particular error message.

## Why Java distinguishes those positions

The rule is Java's, not Hibernate Validator's. A type annotation on a qualified name applies to
the simple name immediately following it:

```java
List<Enclosing.@Marker Inner> onTheInner;
List<@Marker Enclosing.Inner> onTheEnclosingType;
```

[`Marker`](src/main/java/com/fillumina/typeannotation/Marker.java) is this project's own annotation.
It has a runtime retention policy and a `TYPE_USE` target, but no validation meaning. That lets the
tests inspect placement without asking a validation library to interpret anything.

Reflection finds `@Marker` on the element type in the first declaration. In the second, the element
has no marker of its own; the marker is found on its annotated owner type, through
[`AnnotatedType.getAnnotatedOwnerType()`][annotated-type-api].
[MarkerPlacementTest](src/test/java/com/fillumina/typeannotation/MarkerPlacementTest.java) asserts
both results. Java records the position; Hibernate Validator then acts on the annotations it finds.

But why is the second position legal for `Inner` and illegal for `Nested`?

For the inner class, `Enclosing` describes the type of the enclosing instance. Java allows an
annotation on that enclosing part as well as on the inner class itself. Both positions can even
be annotated at once:

```java
List<@Marker Enclosing.@Marker Inner> bothParts;
```

For the static nested class, `Enclosing` is only a qualifier: no enclosing instance is required.
Java does not allow a type-use annotation on that qualifier. It must go on `Nested` instead.

[JLS §9.7.4, Where Annotations May Appear][annotation-placement], states the two relevant conditions:
the name being annotated must be a type name rather than a package name; and if that name is
followed by a dot and another type name, the latter must denote an inner class of the former.
The specification also explains the enclosing-instance distinction behind the rule.

Package names show the first condition:

```java
List<java.lang.@Marker String> legal;
List<@Marker java.lang.String> illegal; // Does not compile: java is a package name.
```

The examples put `@Marker` inside a type argument so that it can only be a type-use annotation.
Annotations written before a field declaration can have additional declaration meanings depending
on their targets; those are not needed to explain the positions here.

## Which occurrence of the type does the validator read?

The same type may appear on a field, a constructor parameter and a getter. You do not need to
repeat the annotation on every occurrence.

This project's [InnerItemsLastSegment](src/main/java/com/fillumina/typeannotation/InnerItemsLastSegment.java)
places it on the field:

```java
public class InnerItemsLastSegment {

    private final List<Enclosing.@Valid Inner> items;

    public InnerItemsLastSegment(List<Enclosing.Inner> items) {
        this.items = items;
    }

    public List<Enclosing.Inner> getItems() {
        return items;
    }
}
```

When `validator.validate(bean)` checks this bean, it reads the private field directly. It does not
need to call `getItems()`: the cascade is declared on the field's element type. This is field-based
validation. If the annotation were declared only on the getter's return type instead, the validator
would use that getter. The specification describes these [field and property access strategies][field-access]
and recommends choosing one rather than duplicating declarations on both.

The unannotated constructor parameter is deliberate. A call to `validate(bean)` does not validate
the constructor call that created the bean. Constructor parameter validation is a separate operation,
[`ExecutableValidator.validateConstructorParameters()`][executable-api]. A framework can arrange
that operation too, but these tests do not use it.

There is no assignment incompatibility between the annotated and unannotated lists. For Java's
ordinary type checking, the annotation does not create a new `List` or a new element type. Each
written occurrence has its own annotations:

```java
List<@Marker String> first = List.of("a");
List<String> second = first;
```

Both variables hold the same list. The assignment does not transfer `@Marker` to the second
variable's declared type. The [assignment test in MarkerPlacementTest](src/test/java/com/fillumina/typeannotation/MarkerPlacementTest.java)
checks this with fields and reflection. A type-checking tool may give a type annotation additional
meaning, but Bean Validation uses the declarations it inspects; it does not attach annotations to
the list object as values move around.

## What a cascade does and does not follow

To see the nested example fail, create both an invalid enclosing instance and an invalid member:

```java
Enclosing enclosing = new Enclosing("");
Enclosing.Inner item = enclosing.new Inner("");
InnerItemsLastSegment bean = new InnerItemsLastSegment(List.of(item));

validator.validate(bean).forEach(violation ->
        System.out.println(violation.getPropertyPath()));
```

Here `validator` is obtained from a factory as in the complete example above. The output is:

```text
items[0].name
```

The validator starts at `bean`, reads its `items` field, follows the element's `@Valid` into `item`,
and checks `item.name`. It does not also check every object that Java associates with that item.
In particular, creating the member through `enclosing.new Inner(...)` does not declare a cascade
back into `enclosing`.

The `@NotBlank` on `Enclosing.label` is still a working constraint. Validating `enclosing` directly
reports `label`. It is absent from the result above because there is no declared validation path
from the member to that object. [ItemValidationTest](src/test/java/com/fillumina/typeannotation/ItemValidationTest.java)
checks both facts.

Now use [InnerItemsWholeName](src/main/java/com/fillumina/typeannotation/InnerItemsWholeName.java),
whose field is `List<@Valid Enclosing.Inner>`, with the same invalid objects. There are no
violations: the annotation is on the enclosing part of the type, not on the element, so the
cascade never enters the member. The blank `name` and blank `label` do not get checked from that
starting point.

### Keeping an enclosing reference is not enough

An inner object may need to retain a reference to its enclosing instance. For example,
`Enclosing.UsesEnclosing` reads the outer `label`:

```java
public String enclosingLabel() {
    return Enclosing.this.label;
}
```

[UsesEnclosingItems](src/main/java/com/fillumina/typeannotation/UsesEnclosingItems.java) marks its
elements with `List<Enclosing.@Valid UsesEnclosing>`. Validation still reports only `items[0].name`.
Having an object reference available is not an instruction to cascade through it.

The tests also inspect javac's generated fields. With the compiler used for this project, an inner
class that uses its enclosing instance retains a synthetic reference, while `Inner`, which does
not use it, does not. A synthetic field is one generated by the compiler rather than declared in
the source; reflection identifies it with [`Field.isSynthetic()`][field-api].

That storage choice is a compiler detail, not the validation rule. Neither an absent reference nor
a retained but unmarked reference supplies a cascade. The explanation does not depend on the
compiler choosing a particular field name or omitting a field.

### An explicit cascade can reach the enclosing object

`Enclosing.ReachableInner` exposes its enclosing instance through an annotated getter:

```java
@Valid
public Enclosing getEnclosing() {
    return Enclosing.this;
}
```

This declares a bean property named `enclosing` and marks it for cascaded validation.
[ReachableInnerItems](src/main/java/com/fillumina/typeannotation/ReachableInnerItems.java) marks
the list elements too. With both values blank, the result contains:

```text
items[0].enclosing.label
items[0].name
```

There are now two declared steps: from the list to its member, and from that member through
`getEnclosing()` to the enclosing object.

[ReachableInnerItemsWholeName](src/main/java/com/fillumina/typeannotation/ReachableInnerItemsWholeName.java)
uses `List<@Valid Enclosing.ReachableInner>` instead. It still reports nothing. The getter's
cascade cannot help until validation has entered the member, and the element is not marked.
[ReachableEnclosingTest](src/test/java/com/fillumina/typeannotation/ReachableEnclosingTest.java)
checks the retained-reference case and both getter cases.

The general rule is the same as for the phone numbers: start with the object passed to
`validate()`, check its constraints, and follow the declared cascades. An enclosing instance is
not a shortcut around those declarations. The specification defines this in
[Object graph validation][object-graph].

## Why this matters for generated classes

This project grew out of [issue #1][placement-issue] in
[xjc-bean-validation-plugin][validation-plugin], a plugin that adds validation annotations to
classes generated from XML schemas.

[XJC][xjc] is the schema compiler of [Jakarta XML Binding][xml-binding]. With its default nested-class
mapping, an anonymous complex type declared inside a local element can become a static nested Java
class. For example, consider this fragment inside an XML schema whose `xs` prefix names
`http://www.w3.org/2001/XMLSchema`:

```xml
<xs:complexType name="OrderType">
    <xs:sequence>
        <xs:element name="item" minOccurs="1" maxOccurs="unbounded">
            <xs:complexType>
                <xs:sequence>
                    <xs:element name="name" type="xs:string"/>
                </xs:sequence>
            </xs:complexType>
        </xs:element>
    </xs:sequence>
</xs:complexType>
```

The relevant Java shape is:

```java
public class OrderType {

    protected List<OrderType.Item> item;

    public static class Item {

        protected String name;
    }
}
```

This is a simplified illustration: XML Binding annotations and accessors are left out. The
anonymous type does not need a schema type name of its own for XJC to derive a Java class name
from the element. Binding customizations can change where the class is generated.

If a validation plugin writes the qualified element name `OrderType.Item`, the correct cascade is:

```java
protected List<OrderType.@Valid Item> item;
```

Writing `List<@Valid OrderType.Item>` is rejected because `Item` is static nested. That is the
practical reason for the unusual dot position. The inner-class examples in this repository show
the other possible failure: a misplaced annotation can compile and silently fail to request an
element cascade.

These are hand-written beans, not an XJC integration test. They isolate Java's placement rule and
its effect on Hibernate Validator. They do not test how the plugin generates the source.

## Running the examples in this repository

Use JDK 21 or later and Maven:

```sh
mvn -B verify
```

The [build](pom.xml) targets Java 21 and uses Jakarta Validation 3.1.1 and Hibernate Validator
9.1.4.Final. Hibernate Validator is a test dependency; the beans themselves depend on the
validation API.

The executable checks are:

- [ItemValidationTest](src/test/java/com/fillumina/typeannotation/ItemValidationTest.java): validates
  the different list declarations, checks exact violation paths, and checks `@Valid` placement
  through reflection.
- [MarkerPlacementTest](src/test/java/com/fillumina/typeannotation/MarkerPlacementTest.java):
  checks annotation ownership independently of validation, and shows that assignment does not
  transfer a declaration's annotations.
- [EnclosingInstantiationTest](src/test/java/com/fillumina/typeannotation/EnclosingInstantiationTest.java):
  checks how static nested and inner objects are constructed.
- [ReachableEnclosingTest](src/test/java/com/fillumina/typeannotation/ReachableEnclosingTest.java):
  checks when validation reaches an enclosing instance, and separately inspects javac's retained
  references.

The phone-number tutorial above is a standalone example. The repository's tests use the smaller
`PlainItem` and `Enclosing` classes to isolate annotation placement. The rejected Java spellings
are explanations, not source files compiled by the normal build.

## References

The rules used above are linked where they are explained. These are the main starting points for
reading further:

- [Jakarta Validation 3.1 specification][validation-spec]:
  [field and property access][field-access], [cascaded validation][graph-validation],
  [preferred and traditional spellings][graph-examples],
  [container element constraints][container-constraints], and [object graph validation][object-graph].
- [Java Language Specification, Java SE 21, §9.7.4][annotation-placement]: where a type annotation
  may appear on a qualified name, including the static nested and inner-class distinction.
- [Java Language Specification, Java SE 21, §8.1.3][inner-classes]: inner classes and enclosing
  instances.
- [JSR 308][jsr308] and [JSR 380][jsr380]: the Java type-annotation feature and the Bean Validation
  2.0 feature that uses it for container elements.
- [Hibernate Validator][hibernate-validator] and its
  [container-cascade warning declaration][hv-container-warning]: the implementation used for the
  behavioural checks and its warning about the traditional spelling.
- [xjc-bean-validation-plugin][validation-plugin] and [the placement issue][placement-issue]: the
  source-generation problem that prompted these examples.

[validation-spec]: https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1.html
[validation-history]: https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1.html#whatsnew
[field-access]: https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1.html#constraintdeclarationvalidationprocess-requirements-propertyvalidation
[graph-validation]: https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1.html#constraintdeclarationvalidationprocess-requirements-graphvalidation
[graph-examples]: https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1.html#constraintdeclarationvalidationprocess-requirements-graphvalidation-examples
[container-constraints]: https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1.html#constraintdeclarationvalidationprocess-containerelementconstraints
[object-graph]: https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1.html#constraintdeclarationvalidationprocess-validationroutine-graphvalidation
[value-extractors]: https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1.html#valueextractordefinition-builtinvalueextractors
[validator-api]: https://jakarta.ee/specifications/bean-validation/3.1/apidocs/jakarta/validation/Validator.html
[factory-api]: https://jakarta.ee/specifications/bean-validation/3.1/apidocs/jakarta/validation/ValidatorFactory.html
[executable-api]: https://jakarta.ee/specifications/bean-validation/3.1/apidocs/jakarta/validation/executable/ExecutableValidator.html
[notblank]: https://jakarta.ee/specifications/bean-validation/3.1/apidocs/jakarta/validation/constraints/NotBlank.html
[notnull]: https://jakarta.ee/specifications/bean-validation/3.1/apidocs/jakarta/validation/constraints/NotNull.html
[size]: https://jakarta.ee/specifications/bean-validation/3.1/apidocs/jakarta/validation/constraints/Size.html
[annotation-placement]: https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.7.4
[inner-classes]: https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.3
[annotated-type-api]: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/AnnotatedType.html#getAnnotatedOwnerType()
[field-api]: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/lang/reflect/Field.html#isSynthetic()
[list-api]: https://docs.oracle.com/en/java/javase/21/docs/api/java.base/java/util/List.html#of(E)
[jsr308]: https://jcp.org/en/jsr/detail?id=308
[jsr380]: https://jcp.org/en/jsr/detail?id=380
[hibernate-validator]: https://hibernate.org/validator/
[hv-container-warning]: https://github.com/hibernate/hibernate-validator/blob/9.1.4.Final/engine/src/main/java/org/hibernate/validator/internal/util/logging/Log.java#L955-L956
[xjc]: https://eclipse-ee4j.github.io/jaxb-ri/
[xml-binding]: https://jakarta.ee/specifications/xml-binding/
[validation-plugin]: https://github.com/fillumina/xjc-bean-validation-plugin
[placement-issue]: https://github.com/fillumina/xjc-bean-validation-plugin/issues/1
