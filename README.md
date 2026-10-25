# Type annotation placement

Does an annotation inside a type argument surprise you?

```java
List<Enclosing.@Valid Member> list;
```

Since Java 8 a type argument can carry an annotation, as in `List<@Valid Item>`, or between the two
names of a nested type, as in `List<Enclosing.@Valid Member>`.

## Why both positions exist

A type annotation applies to the **simple name immediately after it**, so `@Foo A.B` annotates `A` and
`A.@Foo B` annotates `B`. A type declared inside another therefore takes the annotation after the
last dot, and that position is the same for a static nested class and for an inner class.

The same rule shows in a package-qualified name: `java.util.@Foo List` is legal, because the annotation
stands before `List`, which is a class, while `@Foo java.util.List` is illegal, because the name it
stands before is `java`, a package.

```java
java.util.@Foo List<String> x;   // legal: @Foo stands before List, a class name
@Foo java.util.List<String> y;  // illegal: @Foo would stand before java, a package name
```

The leading position could not have been given the meaning the dot position has. Type annotations came
in with Java 8, from [JSR 308](https://jcp.org/en/jsr/detail?id=308), whose aim was to allow an
annotation on any use of a type and not only on a declaration: array levels, the package-qualified
name, the enclosing part of a qualified type. `@Foo A.B` is how a checker says something about `A` as
it appears there, so it cannot also mean "annotate `B`", and then the enclosing type would have no
spelling at all.

That leaves the leading position usable only where the name it annotates stands for an object at run
time, which is exactly where the name after the dot is an inner class. Against a static nested class it
is refused, as `@Foo java.util.List` is refused, because both annotate a path component.

Each name takes its own annotation, and both at once is legal:

```java
class Enclosing { class Member {} }

@Foo Enclosing.Member x;        // Foo on Enclosing, which stands for an object; the item carries nothing
@Foo Enclosing.@Bar Member y;   // Foo on Enclosing and Bar on the item
```

`Marker` is an annotation defined in this project, in `src/main/java`: written as
`List<Enclosing.@Marker Member>`, reflection finds it on the item; written as
`List<@Marker Enclosing.Member>`, reflection finds the item bare and the marker on `Enclosing`. The item
type is `Enclosing.Member` in both, and
[`MarkerPlacementTest`](src/test/java/com/fillumina/typeannotation/MarkerPlacementTest.java) asserts
both outcomes.

Two kinds of member class can be declared inside another class:

- **Static nested**: a static class, which needs no enclosing instance
- **Inner**: a non-static class, which exists only as part of an instance of the class that encloses it

Both kinds are declared in `Enclosing`:

```java
// abridged from src/main/java/com/fillumina/typeannotation/Enclosing.java
public class Enclosing {

    @NotBlank
    public String label;

    public Enclosing(String label) { this.label = label; }

    /** A nested class, reached from outside as {@code Enclosing.Nested}. */
    public static class Nested {

        @NotBlank
        public String name;

        public Nested(String name) { this.name = name; }
    }

    /** An inner class: it needs an instance of {@code Enclosing} to exist. */
    public class Member {

        @NotBlank
        public String name;

        public Member(String name) { this.name = name; }
    }
}
```

We can instantiate either of them with:

```java
// the enclosing class instance is not needed here, Enclosing is just a qualifier
Enclosing.Nested nested = new Enclosing.Nested("item");

// to instantiate a member class we need an instance of the enclosing class first
Enclosing enclosing = new Enclosing("label");
Enclosing.Member member = enclosing.new Member("item");
```

The two classes are in [`Enclosing.java`](src/main/java/com/fillumina/typeannotation/Enclosing.java);
the three lines above are compiled and run by
[`EnclosingInstantiationTest`](src/test/java/com/fillumina/typeannotation/EnclosingInstantiationTest.java).

Annotating them puts the annotation in front of the name of the class it belongs to, and for both
kinds that is the name after the dot:

```java
List<Enclosing.@Foo Nested> nestedItems;   // static nested: the only spelling that compiles
List<Enclosing.@Foo Member> memberItems;   // inner: the annotation is on Member
```

## Why the annotation moves

The Java Language Specification makes this an admissibility rule for qualified names,
[§9.7.4 *Where Annotations May Appear*](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.7.4):

> A type annotation is admissible if both of the following are true:
>
> - the simple name to which the annotation is closest is classified as a TypeName, not a PackageName;
> - if the simple name to which the annotation is closest is followed by "." and another TypeName —
> that is, the annotation appears as `@Foo T.U` — then U denotes an inner class of T.

The second clause turns on the enclosing instance: if `Outer.this` is legal in a class enclosed by
`Outer`, the enclosing name stands for an object at run time and may be annotated; if it is not legal,
as with a static nested class, the enclosing name "is logically just a name" and may not be annotated.
The specification's two examples, the second simplified:

```java
class A { static class B {} }
@Foo A.B x;   // illegal: B is static, so A is a name here and cannot be annotated

class C { class D {} }
@Foo C.D x;   // legal: D is inner, so C stands for an object at run time and @Foo lands on C
```

So the leading position is refused for a static nested item and admissible for an inner one, where it
lands on the enclosing class rather than on the item.

## The spellings, including the one that does not compile

`@Valid` is a type annotation like any other, so it lands in the same place. What differs is what
happens next:

- for a static nested item, the leading position does not compile: `List<@Valid Enclosing.Nested>`;
- for an inner item, it compiles and the annotation lands on `Enclosing`, which is not the item, so
  neither the elements nor the enclosing instance is validated.

The spellings themselves, as the fields of one class:

```java
// abridged from src/test/java/com/fillumina/typeannotation/Spellings.java

/** {@code TopLevelItems}: the annotation is on the type argument, which is the item. */
private List<@Valid PlainItem> topLevelItems;

/** {@code MemberItemsLastSegment}: the annotation is on the inner class, which is the item. */
private List<Enclosing.@Valid Member> memberItemsLastSegment;

/** {@code MemberItemsWholeName}: the annotation is on {@code Enclosing}, so nothing is validated. */
private List<@Valid Enclosing.Member> memberItemsWholeName;

/** {@code StaticNestedItems}: the annotation is between the enclosing class and the item. */
private List<Enclosing.@Valid Nested> staticNestedItems;
```

What a `@Valid` on the items of a collection means when the item is one of those two is answered by
running [Hibernate Validator](https://hibernate.org/validator/) instead of by reading the generated
source: one bean per spelling below, and a test asserting the outcome of each.

| written as                      | item type     | what Hibernate Validator does |
| ------------------------------- | ------------- | ----------------------------- |
| `List<@Valid PlainItem>`        | top level     | validates `PlainItem`         |
| `List<Enclosing.@Valid Member>` | inner         | validates `Enclosing.Member`  |
| `List<@Valid Enclosing.Member>` | inner         | **nothing is validated**      |
| `List<Enclosing.@Valid Nested>` | static nested | validates `Enclosing.Nested`  |
| `List<@Valid Enclosing.Nested>` | static nested | **does not compile**          |

The last row is the one javac refuses, and the reason is the second name, not the first: `Enclosing`
is an ordinary top-level class, but `Nested` is static, so no instance of `Enclosing` is part of an
instance of `Nested`. `Enclosing` in `Enclosing.Nested` therefore names a class and not an object,
and [§9.7.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.7.4) admits an
annotation in front of a qualified name only when the name after the dot denotes an inner class. That
is why a generator must put the annotation immediately before the simple name of the item: the
whole-name position is rejected, and the message differs by JDK.

Error up to JDK 22:

```
error: scoping construct cannot be annotated with type-use annotation: @jakarta.validation.Valid
```

Error on JDK 23 and later:

```
error: type annotation @jakarta.validation.Valid is not expected here
  (to annotate a qualified type, write Enclosing.@jakarta.validation.Valid Nested)
```

## Where the classes come from

[XJC](https://eclipse-ee4j.github.io/jaxb-ri/), the schema compiler of
[Jakarta XML Binding](https://jakarta.ee/specifications/xml-binding/), reads an XML schema and writes
the Java classes that map to it. A complex type written inside an element has no name of its own, so
there is nothing to call a top-level class: XJC puts the class inside the class that declares the
element, as a static nested class.

```xml
<!-- order.xsd, with xmlns:xs="http://www.w3.org/2001/XMLSchema" on the xs:schema element -->
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

```java
// generated by XJC 4.0.9 from the schema above, annotations and accessors left out
public class OrderType {

    protected List<OrderType.Item> item;

    // the anonymous complex type, which had no name in the schema
    public static class Item {

        protected String name;
    }
}
```

That `static` is the whole difficulty: an item type a schema writes inline is a static nested class, so
`List<@Valid OrderType.Item>` is rejected and the spelling that compiles is the one after the dot,
`List<OrderType.@Valid Item>`. `Enclosing.Nested` has that shape here.

`Member` is an inner class, which no schema produces: it is here because a static nested item cannot
take the whole-name spelling at all and an inner one can.

`Enclosing` carries a `@NotBlank` on its `label`, and the tests that expect a violation leave that
label blank, so that a cascade into the enclosing class would leave a violation behind instead of
passing silently. The constraint is live:

```java
// from src/test/java/com/fillumina/typeannotation/ItemValidationTest.java
assertEquals(List.of("label"), pathsOf(validator.validate(new Enclosing(""))));
```

## What a cascade follows

`@Valid` tells a validator one thing: when this object is validated, validate what the annotation
stands on as well. Validation starts at the object handed to `validate()` and follows only the places
marked that way — the properties of a bean, and the elements of a collection whose type argument is
marked. Nothing else is visited.

The bean is an ordinary one: a property holding one list, and a getter for it.

```java
// src/main/java/com/fillumina/typeannotation/MemberItemsLastSegment.java
public class MemberItemsLastSegment {

    // the copy the validator reads: the property is declared here
    private final List<Enclosing.@Valid Member> items;

    // no annotation here: only a validation of the constructor call would read it
    public MemberItemsLastSegment(List<Enclosing.Member> items) {
        this.items = items;
    }

    // the same type: the annotation is optional wherever the type is written
    public List<Enclosing.Member> getItems() {
        return items;
    }
}
```

`MemberItemsWholeName` is the same class with the annotation moved: its field is
`List<@Valid Enclosing.Member> items`, and nothing else changes.

An annotation is not part of the type. `@Foo String`, `@Bar String` and `String` are one and the same
type: the annotation decorates a use of it, and each use carries its own, or none at all. So an annotated
type goes wherever the plain one is expected:

```java
List<@Foo String> first = List.of("a");
List<@Bar String> second = first;   // the same type as the first, and its own annotation
List<String> third = second;        // the same type again, with no annotation
```

An assignment does not carry the annotation along. The three fields hold one and the same list, and
reflection returns `@Foo` on the element type of the first, `@Bar` on the element type of the second and
nothing on the third: an annotation belongs to the declaration it is written on, and the list itself
knows nothing about it.
[`MarkerPlacementTest`](src/test/java/com/fillumina/typeannotation/MarkerPlacementTest.java) asserts that
with two fields sharing one list.

Nor is an annotation part of a method's signature, so two methods that differ only in the annotations of
their parameter types are the same method:

```java
void take(List<@Foo String> items) { }
void take(List<@Bar String> items) { }   // the same method twice: does not compile
```

That is what `@Valid` here is: an annotation on a use of the type, not on the type itself. What counts is
the copy the validator reads, and in `MemberItemsLastSegment` that is the field's, because the field
declares the property. Had only the getter's return type carried the annotation, that copy would be read
instead, with the same result. That is why the constructor parameter carries no annotation: only a
validation of the constructor call would read it, as
`validator.forExecutables().validateConstructorParameters(constructor, arguments)` does, and nothing
here validates constructor calls.

Validating one goes like this, with both values blank so that a cascade into the enclosing instance
would show if it happened:

```java
Enclosing enclosing = new Enclosing("");          // blank label: violates @NotBlank
Enclosing.Member item = enclosing.new Member(""); // blank name: violates @NotBlank
MemberItemsLastSegment bean = new MemberItemsLastSegment(List.of(item));

Set<ConstraintViolation<MemberItemsLastSegment>> violations = validator.validate(bean);
violations.forEach(violation -> System.out.println(violation.getPropertyPath()));   // items[0].name
```

The walk behind that output is:

1. the bean itself, `MemberItemsLastSegment`, which is what `validate()` was handed;
2. its `items` property, read through `getItems()`;
3. the element `items[0]`, reached because the type argument is marked `@Valid`;
4. the `name` property of that element, which carries `@NotBlank` and is blank, so the violation is
   reported as `items[0].name`.

The walk stops there. `Member` never mentions the instance it was created from, so javac emits no field
for it: the object graph has no edge from the item to its `Enclosing`, and the `@NotBlank` on
`Enclosing.label` is not reported.

With `MemberItemsWholeName` the third step never happens: the list holds the same element, but its type
argument carries no annotation (`List<@Valid Enclosing.Member> items`), so the walk stops at the list
and no violation is reported at all.

So that bean comes out empty for two reasons:

1. the annotation that decides a cascade is the one on the element type, and there it stands on
   `Enclosing`, so the elements are never marked;
2. the item keeps no path to its enclosing instance. `Member` never mentions the instance it was
   created from, so javac emits no field for it. A class that does mention it, such as
   `Enclosing.UsesEnclosing`, gets a field, but that field is `this$0`: a reference, not a property,
   so the walk does not follow it.

`Enclosing.UsesEnclosing` is that second kind, and it only makes sense with the class it is declared
in, which is where `label` lives:

```java
// abridged from src/main/java/com/fillumina/typeannotation/Enclosing.java
public class Enclosing {

    @NotBlank
    public String label;

    public class UsesEnclosing {

        @NotBlank
        public String name;

        public UsesEnclosing(String name) {
            this.name = name;
        }

        public String enclosingLabel() {
            // reads the field of the class it is declared in, so javac keeps a reference to it
            return label;   // Enclosing.this.label
        }
    }
}
```

It is used by a bean of its own, `UsesEnclosingItems`, whose items are
`List<Enclosing.@Valid UsesEnclosing>`, and validating that bean reports only `items[0].name`: the
reference is there, but nothing marks it.

A *reference* there is the object's own link back to the instance it was created from, and javac keeps
it only for a class that uses it. *Synthetic* is the flag javac puts on a member it generated rather
than found in the source: `this$0` exists in the class file and in no source line, reflection shows
it through `Field.isSynthetic()`, and no annotation can be written on it. Validation walks properties,
so a kept reference is not a way onto the graph.

None of the above says that the constraints never fire; it says that the walk from these beans never
reaches them. The constraints are the `@NotBlank` on `Enclosing.label` and on the `name` of each item
class, and two checks show they fire as soon as an object that carries one is being validated:

- `validator.validate(new Enclosing(""))` reports `label`, so the constraint on `Enclosing.label` fires as
  soon as an `Enclosing` is the object being validated;
- `MemberItemsLastSegment` with one blank item reports `items[0].name`, so the constraint on `Member.name`
  fires as soon as the list element is marked, which in that bean it is.

The second reason can be undone, though not by merely keeping the reference: `UsesEnclosingItems` holds
items of `Enclosing.UsesEnclosing`, whose instances keep it, and validating that bean still reports only
`items[0].name`. What works is a public getter marked `@Valid`: `Enclosing.ReachableMember` returns the
instance from one:

```java
@Valid
public Enclosing getEnclosing() {
    return Enclosing.this;
}
```

`getEnclosing()` is a bean getter, so the validator sees a property named `enclosing`, and `@Valid` on it
puts the enclosing instance on the graph. Two beans carry such items: `ReachableMemberItems` marks the
element, `ReachableMemberItemsWholeName` marks only the enclosing name. Validating the first with a blank
item reports `items[0].name` and `items[0].enclosing.label`; validating the second reports nothing, because
its element is not marked and the walk stops at the list before that property is reached.
[`ReachableEnclosingTest`](src/test/java/com/fillumina/typeannotation/ReachableEnclosingTest.java) asserts
both outcomes.

## The type argument, and the container

`@Valid` on a collection has two places to stand, and the specification prefers one of them.
[Jakarta Validation 3.1](https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1.html),
[§5.7.1 *Object graph validation*](https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1.html#constraintdeclarationvalidationprocess-validationroutine-graphvalidation),
says `@Valid` "dictates the Jakarta Validation implementation to apply recursively the Jakarta
Validation routine on (each of) the associated object(s)".
[§5.1.3 *Graph validation*](https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1.html#constraintdeclarationvalidationprocess-requirements-graphvalidation)
sets out the same mechanism, and its examples
([§5.1.3.1](https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1.html#constraintdeclarationvalidationprocess-requirements-graphvalidation-examples)) mark the type
argument, `List<@Valid PhoneNumber>`, as the preferred spelling as of version 2.0 —
[JSR 380](https://jcp.org/en/jsr/detail?id=380), Bean Validation 2.0 — while `@Valid` on the container
is the "traditional style" that "continues to be supported"; writing it in both places is
"discouraged", because the elements would be validated twice.
[§5.5 *Container element constraints*](https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1.html#constraintdeclarationvalidationprocess-containerelementconstraints)
is where a constraint written on the elements of a list is defined.

Written out, the two are these:

```java
// the type argument: preferred since version 2.0
private List<@Valid PhoneNumber> phoneNumbers;

// the container: the traditional style, still supported
@Valid
private List<PhoneNumber> phoneNumbers;
```

Both validate the items, so what separates them is not what works but what is recommended and what a
library warns about. Hibernate Validator deprecates the container spelling, and reports it as
[`HV000271`](https://github.com/hibernate/hibernate-validator/blob/9.1.4.Final/engine/src/main/java/org/hibernate/validator/internal/util/logging/Log.java#L955).

## Why it exists

The XJC plugin [xjc-bean-validation-plugin](https://github.com/fillumina/xjc-bean-validation-plugin)
writes the constraints of the items of a collection onto the type argument, and for an item that is a
nested class the position of that annotation decides whether the items are validated at all. This
project is the runtime proof of the position, and of the fact that the alternative is not a different
style but an annotation on another type. The position itself is decided by the compiler and recorded
in the class file, not by the validator; the validator is where the consequence shows, which is why
the beans here are hand written and the tests run Hibernate Validator over them. It was written for
[issue #1](https://github.com/fillumina/xjc-bean-validation-plugin/issues/1) of that project.

## References

- How to write an annotation on a static nested or an inner class: Java Language Specification, Java SE
21, §9.7.4 *Where Annotations May Appear*, whose admissibility rule and examples are quoted above:
[https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.7.4](https://docs.oracle.com/javase/specs/jls/se21/html/jls-9.html#jls-9.7.4)
- The two kinds of class the rule turns on: JLS §8.1.3 *Inner Classes and Enclosing Instances*, "an
inner class is a nested class that is not explicitly or implicitly static", so *nested* covers both
and *inner* is the one that is not static:
[https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.3](https://docs.oracle.com/javase/specs/jls/se21/html/jls-8.html#jls-8.1.3)
- Jakarta Validation 3.1, §5.1.3 *Graph validation* and §5.1.3.1 *Examples*, §5.5 *Container element
constraints*, §5.7.1 *Object graph validation*:
[https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1.html](https://jakarta.ee/specifications/bean-validation/3.1/jakarta-validation-spec-3.1.html)
- JSR 380, Bean Validation 2.0, which introduced container element constraints and the type-argument
spelling: [https://jcp.org/en/jsr/detail?id=380](https://jcp.org/en/jsr/detail?id=380)
- Hibernate Validator, the implementation the tests run: the deprecation warning `HV000271` is declared
at [https://github.com/hibernate/hibernate-validator/blob/9.1.4.Final/engine/src/main/java/org/hibernate/validator/internal/util/logging/Log.java#L955](https://github.com/hibernate/hibernate-validator/blob/9.1.4.Final/engine/src/main/java/org/hibernate/validator/internal/util/logging/Log.java#L955)
of the release the tests use, and the library itself is at
[https://hibernate.org/validator/](https://hibernate.org/validator/)

## Running it

```sh
mvn -B verify
```

Hibernate Validator is a test dependency only, so nothing here is part of a library's surface.