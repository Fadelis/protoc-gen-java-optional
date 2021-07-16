package org.grpcmock.protoc.plugin;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.stream.Stream;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

class ParametersTest {

  @ParameterizedTest
  @MethodSource("parametersParseProvider")
  void should_correctly_parse_parameters_string(String parametersInput, Parameters expected) {
    Parameters parsed = Parameters.from(parametersInput);

    assertThat(parsed.isSetterObject()).isEqualTo(expected.isSetterObject());
    assertThat(parsed.isSetterOptional()).isEqualTo(expected.isSetterOptional());
    assertThat(parsed.isGetterOptional()).isEqualTo(expected.isGetterOptional());
    assertThat(parsed.isUsePrimitiveOptionals()).isEqualTo(expected.isUsePrimitiveOptionals());
  }

  static Stream<Arguments> parametersParseProvider() {
    return Stream.of(
        Arguments.of("", new Parameters(true, false, true, false)),
        Arguments.of("setter_object=false", new Parameters(false, false, true, false)),
        Arguments.of("setter_object=false,setter_optional=true", new Parameters(false, true, true, false)),
        Arguments.of("setter_object=false,getter_optional=false", new Parameters(false, false, false, false)),
        Arguments.of("setter_object=true,setter_optional=true,getter_optional=true,use_primitive_optionals=true",
            new Parameters(true, true, true, true)),
        Arguments.of("use_primitive_optionals=true", new Parameters(true, false, true, true))
    );
  }
}
