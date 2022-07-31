# Purpose

This project shows what happens when an entity has an `hstore` column type (PostgreSQL),
and [Hibernate Envers](https://hibernate.org/orm/envers/) auditing is enabled.

```java
import com.vladmihalcea.hibernate.type.basic.PostgreSQLHStoreType;

// ...
@Entity
@TypeDef(name = "PostgreSQLHStoreType", typeClass = PostgreSQLHStoreType.class)
@Audited
public class Post {
    @Id
    private Long id;

    @Type(type = "PostgreSQLHStoreType")
    @Column(columnDefinition = "hstore")
    private Map<String, String> translations;

    // ...
}
```

## Test

Open a shell in project root and run the following command (You need JDK, Maven and Docker):

```bash
mvn test
```

The test should fail with the following message:

```text
...
INFO: Envers integration enabled? : true
[ERROR] Tests run: 1, Failures: 0, Errors: 1, Skipped: 0, Time elapsed: 5.754 s <<< FAILURE! - in io.sadeq.TestInitSessionFactory
[ERROR] io.sadeq.TestInitSessionFactory  Time elapsed: 5.754 s  <<< ERROR!
org.hibernate.MappingException: Type not supported for auditing: com.vladmihalcea.hibernate.type.basic.PostgreSQLHStoreType, on entity io.sadeq.Post, property 'translations'.
...
```

## Debug

`PostgreSQLHStoreType` is provided by [Hibernate Types project](https://github.com/vladmihalcea/hibernate-types).
Here's
a [permanent link](https://github.com/vladmihalcea/hibernate-types/blob/697c7a6b379972bbd6514463717ee9d10b840619/hibernate-types-55/src/main/java/com/vladmihalcea/hibernate/type/basic/PostgreSQLHStoreType.java)
to the version this project uses. In particular:

```java
public class PostgreSQLHStoreType extends ImmutableType<Map> {
    // ...
}
```

where [ImmutableType](https://github.com/vladmihalcea/hibernate-types/blob/697c7a6b379972bbd6514463717ee9d10b840619/hibernate-types-55/src/main/java/com/vladmihalcea/hibernate/type/ImmutableType.java)
is defined as:

```java
public abstract class ImmutableType<T> implements UserType, Type {
    // ...
}
```

Apparently, Envers doesn't like `Type`. If the class only implemented `UserType`, it would be OK (however,
read [this article](https://vladmihalcea.com/hibernate-no-dialect-mapping-for-jdbc-type/) for a possible issue with
native queries). However, when it implements `Type`, Envers fails because it expects the types to implement a child
of `Type`, called `BasicType` (see [here](https://github.com/hibernate/hibernate-orm/blob/d0e5692026a631dfa925be933a14e0e9b5cc6829/hibernate-envers/src/main/java/org/hibernate/envers/configuration/internal/metadata/BasicMetadataGenerator.java#L46)):

```java
public final class BasicMetadataGenerator {
    // ...
    boolean addBasic(/* */) {
        if (value.getType() instanceof BasicType) {
            // ...
            return true;
        }
        return false;
    }
}
```

You can
see [the logic](https://github.com/hibernate/hibernate-orm/blob/d0e5692026a631dfa925be933a14e0e9b5cc6829/hibernate-envers/src/main/java/org/hibernate/envers/configuration/internal/metadata/AuditMetadataGenerator.java#L199-L215)
where it checks for a type and throws an exception if the type is neither Basic nor Component (= Composite).

## Workaround

The problem can be resolved if `ImmutableType` implements `BasicType` rather than `Type`. It requires the implementation
of an additional method:

```java
public interface BasicType extends Type {
    /**
     * Get the names under which this type should be registered in the type registry.
     *
     * @return The keys under which to register this type.
     */
    String[] getRegistrationKeys();
}
```

This won't be too hard, for example:

```java
public abstract class ImmutableType<T> implements UserType, BasicType {
    // ...

    String[] getRegistrationKeys() {
        return new String[]{getName()};
    }
}
```