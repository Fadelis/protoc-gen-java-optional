package org.grpcmock.protoc.plugin;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Label;
import com.google.protobuf.DescriptorProtos.FieldDescriptorProto.Type;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.Descriptors.FieldDescriptor;
import com.google.protobuf.Descriptors.FieldDescriptor.JavaType;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.Feature;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File;
import com.salesforce.jprotoc.Generator;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtoTypeMap;
import com.salesforce.jprotoc.ProtocPlugin;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class OptionalGenerator extends Generator {

  private static final String DELIMITER = "\n";
  private static final String JAVA_EXTENSION = ".java";
  private static final String DIR_SEPARATOR = "/";
  private static final String TEMPLATES_DIRECTORY = "templates" + DIR_SEPARATOR;
  private static final String METHOD_NAME = "javaMethodName";
  private static final String FIELD_TYPE = "javaFieldType";
  private static final String OPTIONAL_CLASS = "optionalClass";
  private static final String PRIMITIVE_OPTIONAL = "primitiveOptional";
  private static final String OPTIONAL_GETTER_METHOD = "optionalGetterMethod";
  private static final String BUILDER_SCOPE = "builder_scope:";
  private static final String CLASS_SCOPE = "class_scope:";
  private static final String DEFAULT_OPTIONAL_CLASS = Optional.class.getName();
  private static final String DEFAULT_OPTIONAL_GETTER_METHOD = "get";
  private static final Map<JavaType, String> PRIMITIVE_CLASSES =
      ImmutableMap.<JavaType, String>builder()
          .put(JavaType.INT, Integer.class.getSimpleName())
          .put(JavaType.LONG, Long.class.getSimpleName())
          .put(JavaType.FLOAT, Float.class.getSimpleName())
          .put(JavaType.DOUBLE, Double.class.getSimpleName())
          .put(JavaType.BOOLEAN, Boolean.class.getSimpleName())
          .put(JavaType.STRING, String.class.getSimpleName())
          .put(JavaType.BYTE_STRING, ByteString.class.getName())
          .build();
  private static final Map<String, String> PRIMITIVE_OPTIONALS =
      ImmutableMap.<String, String>builder()
          .put(Integer.class.getSimpleName(), OptionalInt.class.getName())
          .put(Long.class.getSimpleName(), OptionalLong.class.getName())
          .put(Double.class.getSimpleName(), OptionalDouble.class.getName())
          .build();
  private static final Map<String, String> PRIMITIVE_OPTIONAL_GETTER_METHODS =
      ImmutableMap.<String, String>builder()
          .put(Integer.class.getSimpleName(), "getAsInt")
          .put(Long.class.getSimpleName(), "getAsLong")
          .put(Double.class.getSimpleName(), "getAsDouble")
          .build();
  private Parameters parameters;
  private ProtoTypeMap protoTypeMap;

  public static void main(String[] args) {
    ProtocPlugin.generate(new OptionalGenerator());
  }

  private static boolean hasFieldPresence(FieldDescriptorProto fieldDescriptor) {
    return fieldDescriptor.getLabel() != Label.LABEL_REPEATED
        && (fieldDescriptor.getProto3Optional()
            || fieldDescriptor.getType() == Type.TYPE_MESSAGE
            || fieldDescriptor.hasOneofIndex());
  }

  private static String templatePath(String path) {
    return TEMPLATES_DIRECTORY + path;
  }

  @Override
  public List<File> generateFiles(CodeGeneratorRequest request) throws GeneratorException {
    // create a map from proto types to java types
    this.protoTypeMap = ProtoTypeMap.of(request.getProtoFileList());
    this.parameters = Parameters.from(request.getParameter());

    return request.getProtoFileList().stream()
        .filter(file -> request.getFileToGenerateList().contains(file.getName()))
        .flatMap(this::handleProtoFile)
        .collect(Collectors.toList());
  }

  @Override
  protected List<Feature> supportedFeatures() {
    return Collections.singletonList(Feature.FEATURE_PROTO3_OPTIONAL);
  }

  private Stream<File> handleProtoFile(FileDescriptorProto fileDescriptor) {
    String protoPackage = fileDescriptor.getPackage();
    String javaPackage =
        fileDescriptor.getOptions().hasJavaPackage()
            ? fileDescriptor.getOptions().getJavaPackage()
            : protoPackage;

    return fileDescriptor.getMessageTypeList().stream()
        .flatMap(
            descriptor ->
                handleMessage(
                    descriptor,
                    getFileName(fileDescriptor, descriptor),
                    protoPackage,
                    javaPackage));
  }

  private Stream<File> handleMessage(
      DescriptorProto messageDescriptor, String fileName, String protoPackage, String javaPackage) {
    String javaPackagePath =
        javaPackage.isEmpty() ? "" : javaPackage.replace(".", DIR_SEPARATOR) + DIR_SEPARATOR;
    String protoPackagePath = protoPackage.isEmpty() ? "" : protoPackage + ".";
    String filePath = javaPackagePath + fileName + JAVA_EXTENSION;
    String fullMethodName = protoPackagePath + messageDescriptor.getName();

    return Stream.concat(
        handleSingleMessage(messageDescriptor, filePath, fullMethodName),
        messageDescriptor.getNestedTypeList().stream()
            .filter(nestedDescriptor -> !nestedDescriptor.getOptions().getMapEntry())
            .flatMap(
                nestedDescriptor ->
                    handleMessage(nestedDescriptor, fileName, fullMethodName, javaPackage)));
  }

  private Stream<File> handleSingleMessage(
      DescriptorProto messageDescriptor, String filePath, String fullMethodName) {
    return Stream.of(
            createFile(
                messageDescriptor,
                filePath,
                fullMethodName,
                BUILDER_SCOPE,
                this::createBuilderMethods),
            createFile(
                messageDescriptor, filePath, fullMethodName, CLASS_SCOPE, this::createClassMethods))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private Optional<File> createFile(
      DescriptorProto messageDescriptor,
      String fileName,
      String fullMethodName,
      String scopeType,
      Function<FieldDescriptorProto, Optional<String>> createMethods) {
    return messageDescriptor.getFieldList().stream()
        .map(createMethods)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.collectingAndThen(Collectors.joining(DELIMITER), Optional::of))
        .filter(value -> !value.isEmpty())
        .map(
            methodsContent ->
                File.newBuilder()
                    .setName(fileName)
                    .setContent(methodsContent + DELIMITER)
                    .setInsertionPoint(scopeType + fullMethodName)
                    .build());
  }

  private Optional<String> createBuilderMethods(FieldDescriptorProto fieldDescriptor) {
    if (hasFieldPresence(fieldDescriptor)) {
      return Stream.of(
              setOrClearMethod(fieldDescriptor),
              optionalSetOrClearMethod(fieldDescriptor),
              optionalGet(fieldDescriptor),
              optionalGetNullable(fieldDescriptor))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.collectingAndThen(Collectors.joining(DELIMITER), Optional::of));
    }
    return Optional.empty();
  }

  private Optional<String> createClassMethods(FieldDescriptorProto fieldDescriptor) {
    if (hasFieldPresence(fieldDescriptor)) {
      return Stream.of(optionalGet(fieldDescriptor), optionalGetNullable(fieldDescriptor))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.collectingAndThen(Collectors.joining(DELIMITER), Optional::of));
    }
    return Optional.empty();
  }

  private Optional<String> setOrClearMethod(FieldDescriptorProto fieldDescriptor) {
    if (!parameters.isSetterObject()) {
      return Optional.empty();
    }
    Map<?, ?> context =
        ImmutableMap.builder()
            .put(METHOD_NAME, getJavaMethodName(fieldDescriptor))
            .put(FIELD_TYPE, getJavaTypeName(fieldDescriptor))
            .build();
    return Optional.of(applyTemplate(templatePath("setOrClear.mustache"), context));
  }

  private Optional<String> optionalSetOrClearMethod(FieldDescriptorProto fieldDescriptor) {
    if (!parameters.isSetterOptional()) {
      return Optional.empty();
    }

    String javaTypeName = getJavaTypeName(fieldDescriptor);
    Map<?, ?> context =
        ImmutableMap.builder()
            .put(METHOD_NAME, getJavaMethodName(fieldDescriptor))
            .put(FIELD_TYPE, javaTypeName)
            .put(OPTIONAL_CLASS, getOptionalClassName(javaTypeName))
            .put(PRIMITIVE_OPTIONAL, isPrimitiveOptional(javaTypeName))
            .put(OPTIONAL_GETTER_METHOD, getOptionalGetterMethod(javaTypeName))
            .build();
    return Optional.of(applyTemplate(templatePath("optionalSetOrClear.mustache"), context));
  }

  private Optional<String> optionalGet(FieldDescriptorProto fieldDescriptor) {
    if (!parameters.isGetterOptional()) {
      return Optional.empty();
    }

    String javaTypeName = getJavaTypeName(fieldDescriptor);
    Map<?, ?> context =
        ImmutableMap.builder()
            .put(METHOD_NAME, getJavaMethodName(fieldDescriptor))
            .put(FIELD_TYPE, javaTypeName)
            .put(OPTIONAL_CLASS, getOptionalClassName(javaTypeName))
            .put(PRIMITIVE_OPTIONAL, isPrimitiveOptional(javaTypeName))
            .build();
    return Optional.of(applyTemplate(templatePath("optionalGet.mustache"), context));
  }

  private Optional<String> optionalGetNullable(FieldDescriptorProto fieldDescriptor) {
    if (!parameters.isGetterOptional()) {
      return Optional.empty();
    }

    String javaTypeName = getJavaTypeName(fieldDescriptor);
    Map<?, ?> context =
        ImmutableMap.builder()
            .put(METHOD_NAME, getJavaMethodName(fieldDescriptor))
            .put(FIELD_TYPE, javaTypeName)
            .put(OPTIONAL_CLASS, getOptionalClassName(javaTypeName))
            .put(PRIMITIVE_OPTIONAL, isPrimitiveOptional(javaTypeName))
            .build();
    return Optional.of(applyTemplate(templatePath("optionalGetNullable.mustache"), context));
  }

  private String getFileName(
      FileDescriptorProto fileDescriptor, DescriptorProto messageDescriptor) {
    if (fileDescriptor.getOptions().getJavaMultipleFiles()) {
      return messageDescriptor.getName();
    }
    if (fileDescriptor.getOptions().hasJavaOuterClassname()) {
      return fileDescriptor.getOptions().getJavaOuterClassname();
    }
    String protoPackage = fileDescriptor.hasPackage() ? "." + fileDescriptor.getPackage() : "";
    String protoTypeName = protoPackage + "." + messageDescriptor.getName();
    return Optional.ofNullable(protoTypeMap.toJavaTypeName(protoTypeName))
        .map(javaType -> javaType.substring(0, javaType.lastIndexOf('.')))
        .map(javaType -> javaType.substring(javaType.lastIndexOf('.') + 1))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Failed to find filename for proto '" + fileDescriptor.getName() + "'"));
  }

  private String getJavaMethodName(FieldDescriptorProto fieldDescriptor) {
    return underscoresToCamelCase(fieldDescriptor.getName(), true, false);
  }

  private String getJavaTypeName(FieldDescriptorProto fieldDescriptor) {
    String protoTypeName = fieldDescriptor.getTypeName();
    if (protoTypeName.isEmpty()) {
      return Optional.of(fieldDescriptor.getType())
          .map(FieldDescriptor.Type::valueOf)
          .map(FieldDescriptor.Type::getJavaType)
          .map(PRIMITIVE_CLASSES::get)
          .orElseThrow(
              () ->
                  new IllegalArgumentException(
                      "Failed to find java type for field:\n" + fieldDescriptor));
    }
    return Optional.ofNullable(protoTypeMap.toJavaTypeName(protoTypeName))
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "Failed to find java type for prototype '" + protoTypeName + "'"));
  }

  private String getOptionalClassName(String javaTypeName) {
    return parameters.isUsePrimitiveOptionals()
        ? PRIMITIVE_OPTIONALS.getOrDefault(javaTypeName, DEFAULT_OPTIONAL_CLASS)
        : DEFAULT_OPTIONAL_CLASS;
  }

  private String getOptionalGetterMethod(String javaTypeName) {
    return parameters.isUsePrimitiveOptionals()
        ? PRIMITIVE_OPTIONAL_GETTER_METHODS.getOrDefault(
            javaTypeName, DEFAULT_OPTIONAL_GETTER_METHOD)
        : DEFAULT_OPTIONAL_GETTER_METHOD;
  }

  private boolean isPrimitiveOptional(String javaTypeName) {
    return parameters.isUsePrimitiveOptionals() && PRIMITIVE_OPTIONALS.containsKey(javaTypeName);
  }

  private String underscoresToCamelCase(
      String input, boolean capNextLetter, boolean preservePeriod) {
    StringBuilder result = new StringBuilder();

    // Note:  I distrust ctype.h due to locales.
    for (int i = 0; i < input.length(); i++) {
      char c = input.charAt(i);
      if ('a' <= c && c <= 'z') {
        if (capNextLetter) {
          result.append((char) (c + ('A' - 'a')));
        } else {
          result.append(c);
        }
        capNextLetter = false;
      } else if ('A' <= c && c <= 'Z') {
        if (i == 0 && !capNextLetter) {
          // Force first letter to lower-case unless explicitly told to
          // capitalize it.
          result.append((char) (c + ('a' - 'A')));
        } else {
          // Capital letters after the first are left as-is.
          result.append(c);
        }
        capNextLetter = false;
      } else if ('0' <= c && c <= '9') {
        result.append(c);
        capNextLetter = true;
      } else {
        capNextLetter = true;
        if (c == '.' && preservePeriod) {
          result.append(c);
        }
      }
    }
    // Add a trailing "_" if the name should be altered.
    if (input.length() > 0 && input.charAt(input.length() - 1) == '#') {
      result.append('_');
    }

    // https://github.com/protocolbuffers/protobuf/issues/8101
    // To avoid generating invalid identifiers - if the input string
    // starts with _<digit> (or multiple underscores then digit) then
    // we need to preserve the underscore as an identifier cannot start
    // with a digit.
    // This check is being done after the loop rather than before
    // to handle the case where there are multiple underscores before the
    // first digit. We let them all be consumed so we can see if we would
    // start with a digit.
    // Note: not preserving leading underscores for all otherwise valid identifiers
    // so as to not break anything that relies on the existing behaviour
    if (result.length() > 0
        && '0' <= result.charAt(0)
        && result.charAt(0) <= '9'
        && input.length() > 0
        && input.charAt(0) == '_') {
      result.insert(0, '_');
    }
    return result.toString();
  }
}
