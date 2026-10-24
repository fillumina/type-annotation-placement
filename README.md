# Type annotation placement

Does an annotation inside a type argument surprise you?

```java
List<Enclosing.@Valid Member> list;
```

Since Java 8 a type argument can carry an annotation, as in `List<@Valid Item>`, or between the two names
of a nested type, as in `List<Enclosing.@Valid Member>`.

The annotation always stands immediately before the simple name of the type it annotates, so a type
declared inside another takes it after the last dot: `List<Enclosing.@Valid Member>`. That position is the
same for a static nested class and for an inner class.

What differs is the other position, in front of the whole name — `List<@Valid Enclosing.Member>`:

- for a static nested item, `List<@Valid Enclosing.Nested>` does not compile;
- for an inner item, it compiles and the annotation lands on `Enclosing`, which is not the item, so
  neither the elements nor the enclosing instance is validated.

Given this class:

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

Two kinds of member class can be declared inside another class:

- **Static nested**: a static class, which needs no enclosing instance
- **Inner**: a non-static class, which exists only as part of an instance of the class that encloses it

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

The same annotation, written for an item of each kind:

```java
// abridged from src/test/java/com/fillumina/typeannotation/Spellings.java

/** {@code TopLevelItems}: validate {@code PlainItem} */
private List<@Valid PlainItem> topLevelItems;

/** {@code MemberItemsLastSegment}: validate {@code Member} (inner class) */
private List<Enclosing.@Valid Member> memberItemsLastSegment;

/** {@code MemberItemsWholeName}: the annotation is on {@code Enclosing}, so nothing is validated. */
private List<@Valid Enclosing.Member> memberItemsWholeName;

/** {@code StaticNestedItems}: this is the correct way to validate a static nested class {@code Nested}. */
private List<Enclosing.@Valid Nested> staticNestedItems;
```

## The spellings, including the one that does not compile

What a `@Valid` on the items of a collection means when the item is one of those two is answered by
running Hibernate Validator instead of by reading the generated source: a bean per spelling, and a
test asserting the outcome of each. The last row has no bean, because javac refuses it.

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
and §9.7.4 admits an annotation in front of a qualified name only when the name after the dot
denotes an inner class. That is why a generator must put the annotation immediately before the
simple name of the item: the whole-name position is rejected, and the message differs by JDK.

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

[XJC](https://eclipse-ee4j.github.io/jaxb-ri/), the schema compiler of Jakarta XML Binding, reads an XML
schema and writes the Java classes that map to it. A complex type written inside an element has no name
of its own, so there is nothing to call a top-level class: XJC puts the class inside the class that
declares the element, as a static nested class.

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

`Member` is an inner class, which no schema produces: it is here because it is the only item type
that accepts the whole-name spelling at all.

`Enclosing` carries a `@NotBlank` on its `label`, and the tests that expect a violation leave that
label blank, so that a cascade into the enclosing class would leave a violation behind instead of
passing silently. The constraint is live:

```java
// from src/test/java/com/fillumina/typeannotation/ItemValidationTest.java
assertEquals(List.of("label"), pathsOf(validator.validate(new Enclosing(""))));
```

## Why the annotation moves

A type annotation applies to the simple name immediately after it, and in a qualified name that is the
first name: `@Foo T.U` annotates `T`. The Java Language Specification makes this an admissibility rule
for qualified names,
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

So for the static nested item the annotation cannot stand on the enclosing name at all, which is the row
that does not compile, while for the inner item it can — and then it marks `Enclosing` instead of the
item, which is the row that compiles and validates nothing.

## What a cascade follows

`@Valid` is the annotation that asks for cascading. Jakarta Validation 3.1, §5.7.1 *Object graph
validation*, says it "dictates the Jakarta Validation implementation to apply recursively the Jakarta
Validation routine on (each of) the associated object(s)". §5.1.3 *Graph validation* sets out the same
mechanism, and its examples (§5.1.3.1) mark the type argument, `List<@Valid PhoneNumber>`, as the
preferred spelling as of version 2.0 — JSR 380, Bean Validation 2.0 — while `@Valid` on the container is
the "traditional style" that "continues to be supported"; writing it in both places is "discouraged",
because the elements would be validated twice. Hibernate Validator nonetheless deprecates the container
spelling, and warns about it as HV000271. §5.5 *Container element constraints* is where a constraint
written on the elements of a list is defined.

Two consequences that look surprising and are not:

- a type annotation in front of a qualified name marks the enclosing type, not the item, and that type
is not on the graph being validated, so nothing is cascaded: neither the items nor the enclosing
instance.
- an enclosing instance is never reached through its inner objects: `MemberItemsLastSegment` reports
the blank item and says nothing about the blank `label` of the `Enclosing` it belongs to, because the
outer object is not part of the graph being validated.

None of that is a dead constraint: `Enclosing` on its own, handed to the validator, is reported on its
`label`, and the item is reported as soon as the annotation stands after the dot. What the whole-name
spelling changes is where `@Valid` lands. A cascade is decided by the annotations of the element type,
which is `Member`, so an annotation on `Enclosing` is never read; and the `Enclosing` an inner instance
belongs to is held in a synthetic field of `Member`, not in a property, so no cascade reaches it. That
is why `MemberItemsWholeName` validates nothing, whatever constraints
`Enclosing` carries.

## Why it exists

The XJC plugin [xjc-bean-validation-plugin](https://github.com/fillumina/xjc-bean-validation-plugin)
writes the constraints of the items of a collection onto the type argument, and for an item that is a
nested class the position of that annotation decides whether the items are validated at all. This
project is the runtime proof of the position, and of the fact that the alternative is not a different
style but an annotation on another type. It was written for
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

## Running it

```sh
mvn -B verify
```

Hibernate Validator is a test dependency only, so nothing here is part of a library's surface.