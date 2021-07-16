package org.grpcmock.protoc.plugin;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;

final class Parameters {

  private static final String DELIMITER = ",";
  private static final String ASSIGMENT = "=";
  private static final String SETTER_OBJECT_KEY = "setter_object";
  private static final String SETTER_OPTIONAL_KEY = "setter_optional";
  private static final String GETTER_OPTIONAL_KEY = "getter_optional";
  private static final String USE_PRIMITIVE_OPTIONALS_KEY = "use_primitive_optionals";
  private static final boolean DEFAULT_SETTER_OBJECT = true;
  private static final boolean DEFAULT_SETTER_OPTIONAL = false;
  private static final boolean DEFAULT_GETTER_OPTIONAL = true;
  private static final boolean DEFAULT_USE_PRIMITIVE_OPTIONALS = false;

  /**
   * Flag indicating whether to add setter methods with the nullable object itself as an argument. Default {@code true}.
   */
  private final boolean setterObject;
  /**
   * Flag indicating whether to add setter methods with {@link java.util.Optional} as an argument. Default {@code false}.
   */
  private final boolean setterOptional;
  /**
   * Flag indicating whether to add getter methods returning {@link java.util.Optional}. Default {@code true}.
   */
  private final boolean getterOptional;
  /**
   * Flag indicating whether to use primitive optionals ({@link java.util.OptionalInt} and similar) for {@code optional} protobuf
   * primitive's setters and getters. Default {@code false}.
   */
  private final boolean usePrimitiveOptionals;

  Parameters(boolean setterObject, boolean setterOptional, boolean getterOptional, boolean usePrimitiveOptionals) {
    this.setterObject = setterObject;
    this.setterOptional = setterOptional;
    this.getterOptional = getterOptional;
    this.usePrimitiveOptionals = usePrimitiveOptionals;
  }

  public static Parameters from(String parametersRaw) {
    Map<String, Boolean> values = Optional.ofNullable(parametersRaw)
        .map(parameters -> Stream.of(parameters.split(DELIMITER))
            .map(parameterRaw -> parameterRaw.split(ASSIGMENT))
            .collect(Collectors.toMap(split -> split[0], Parameters::safeParseBoolean)))
        .orElseGet(Collections::emptyMap);

    return new Parameters(
        values.getOrDefault(SETTER_OBJECT_KEY, DEFAULT_SETTER_OBJECT),
        values.getOrDefault(SETTER_OPTIONAL_KEY, DEFAULT_SETTER_OPTIONAL),
        values.getOrDefault(GETTER_OPTIONAL_KEY, DEFAULT_GETTER_OPTIONAL),
        values.getOrDefault(USE_PRIMITIVE_OPTIONALS_KEY, DEFAULT_USE_PRIMITIVE_OPTIONALS)
    );
  }

  public boolean isSetterObject() {
    return setterObject;
  }

  public boolean isSetterOptional() {
    return setterOptional;
  }

  public boolean isGetterOptional() {
    return getterOptional;
  }

  public boolean isUsePrimitiveOptionals() {
    return usePrimitiveOptionals;
  }

  private static boolean safeParseBoolean(String[] input) {
    return input.length == 2 && Boolean.parseBoolean(input[1]);
  }
}
