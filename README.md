# Protoc Generator Java Optional ![Build pipeline](https://github.com/Fadelis/protoc-gen-java-optional/workflows/Build%20pipeline/badge.svg)

A Java Protoc plugin extending generated java classes with null safe `setOrClear` and `getOptional` methods. Also works for
protobuf primitive variables with the [optional](https://github.com/protocolbuffers/protobuf/blob/v3.12.0/docs/field_presence.md)
keyword.

## Quick usage

### Using `protoc` binary

You must have `protoc` binary installed in your system and have to download `protoc-gen-java-optional` executable based on
platform from GitHub [releases](https://github.com/Fadelis/protoc-gen-java-optional/releases)
or at `Files` section in [maven-central](https://mvnrepository.com/artifact/org.grpcmock/protoc-gen-java-optional/1.3.0).

With defaults:

```
protoc --proto-path=./protos/ --java_out=./out/directory --plugin=protoc-gen-java-optional=./path/to/plugin/protoc-gen-java-optional.exe --java-optional_out=./out/directory ./protos/sample.proto
```

With some parameters:

```
protoc --proto-path=./protos/ --java_out=./out/directory --plugin=protoc-gen-java-optional=./path/to/plugin/protoc-gen-java-optional.exe --java-optional_out=setter_optional=true,use_primitive_optionals=true:./out/directory ./protos/sample.proto
```

### Using [protobuf-maven-plugin](https://github.com/xolstice/protobuf-maven-plugin)

```xml

<build>
  <extensions>
    <extension>
      <groupId>kr.motd.maven</groupId>
      <artifactId>os-maven-plugin</artifactId>
      <version>${os-maven-plugin.version}</version>
    </extension>
  </extensions>
  <plugins>
    <plugin>
      <groupId>org.xolstice.maven.plugins</groupId>
      <artifactId>protobuf-maven-plugin</artifactId>
      <version>${protobuf-maven-plugin.version}</version>
      <executions>
        <execution>
          <id>protobuf</id>
          <goals>
            <goal>compile</goal>
          </goals>
          <phase>generate-sources</phase>
          <configuration>
            <protocArtifact>com.google.protobuf:protoc:${protoc.version}:exe:${os.detected.classifier}</protocArtifact>
            <protoSourceRoot>${project.basedir}/src/test/resources/</protoSourceRoot>
            <useArgumentFile>true</useArgumentFile>
            <protocPlugins>
              <protocPlugin>
                <id>java-optional</id>
                <groupId>org.grpcmock</groupId>
                <artifactId>protoc-gen-java-optional</artifactId>
                <version>1.3.0</version>
                <mainClass>org.grpcmock.protoc.plugin.OptionalGenerator</mainClass>
              </protocPlugin>
            </protocPlugins>
          </configuration>
        </execution>
      </executions>
    </plugin>
  </plugins>
</build>
```

This plugin does not support passing java protoc plugin parameters
until https://github.com/xolstice/protobuf-maven-plugin/issues/56 is solved, so default options will be used.

### Using [protoc-jar-maven-plugin](https://github.com/os72/protoc-jar-maven-plugin)

This maven plugin is not supported as it does not support running multiple output types as a single protoc command. This protoc
plugin needs to be run together with the `java_out` target in order to extend generated classes.

## Configuration parameters

- `setter_object` - boolean flag indicating whether to add setter methods with the nullable object itself as argument.
  Default `true`.
- `setter_optional` - boolean flag indicating whether to add setter methods with `Optional` as an argument. Default `false`.
- `getter_optional` - boolean flag indicating whether to add getter methods returning `Optional`. Default `true`.
- `use_primitive_optionals` - boolean flag indicating whether to use primitive
  optionals (`OptionalInt/OptionalLong/OptionalDouble`) for `optional` protobuf primitive's setters and getters. Default `false`.
