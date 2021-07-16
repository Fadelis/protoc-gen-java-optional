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
import java.util.Locale;
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
  private static final String DIR_SEPARATOR = java.io.File.separator;
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
  private static final Map<JavaType, String> PRIMITIVE_CLASSES = ImmutableMap.<JavaType, String>builder()
      .put(JavaType.INT, Integer.class.getSimpleName())
      .put(JavaType.LONG, Long.class.getSimpleName())
      .put(JavaType.FLOAT, Float.class.getSimpleName())
      .put(JavaType.DOUBLE, Double.class.getSimpleName())
      .put(JavaType.BOOLEAN, Boolean.class.getSimpleName())
      .put(JavaType.STRING, String.class.getSimpleName())
      .put(JavaType.BYTE_STRING, ByteString.class.getName())
      .build();
  private static final Map<String, String> PRIMITIVE_OPTIONALS = ImmutableMap.<String, String>builder()
      .put(Integer.class.getSimpleName(), OptionalInt.class.getName())
      .put(Long.class.getSimpleName(), OptionalLong.class.getName())
      .put(Double.class.getSimpleName(), OptionalDouble.class.getName())
      .build();
  private static final Map<String, String> PRIMITIVE_OPTIONAL_GETTER_METHODS = ImmutableMap.<String, String>builder()
      .put(Integer.class.getSimpleName(), "getAsInt")
      .put(Long.class.getSimpleName(), "getAsLong")
      .put(Double.class.getSimpleName(), "getAsDouble")
      .build();

  public static void main(String[] args) {
    ProtocPlugin.generate(new OptionalGenerator());
  }

  private Parameters parameters;
  private ProtoTypeMap protoTypeMap;

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
    String javaPackage = fileDescriptor.getOptions().hasJavaPackage()
        ? fileDescriptor.getOptions().getJavaPackage()
        : protoPackage;

    return fileDescriptor.getMessageTypeList().stream()
        .flatMap(messageDescriptor -> handleMessage(messageDescriptor, protoPackage, javaPackage));
  }

  private Stream<File> handleMessage(DescriptorProto messageDescriptor, String protoPackage, String javaPackage) {
    String fileName = javaPackage.replace(".", DIR_SEPARATOR) + DIR_SEPARATOR + messageDescriptor.getName() + JAVA_EXTENSION;
    String fullMethodName = protoPackage + "." + messageDescriptor.getName();

    return Stream.of(
        createFile(messageDescriptor, fileName, fullMethodName, BUILDER_SCOPE, this::createBuilderMethods),
        createFile(messageDescriptor, fileName, fullMethodName, CLASS_SCOPE, this::createClassMethods))
        .filter(Optional::isPresent)
        .map(Optional::get);
  }

  private Optional<File> createFile(
      DescriptorProto messageDescriptor,
      String fileName,
      String fullMethodName,
      String scopeType,
      Function<FieldDescriptorProto, Optional<String>> createMethods
  ) {
    return messageDescriptor.getFieldList().stream()
        .map(createMethods)
        .filter(Optional::isPresent)
        .map(Optional::get)
        .collect(Collectors.collectingAndThen(Collectors.joining(DELIMITER), Optional::of))
        .filter(value -> !value.isEmpty())
        .map(methodsContent -> File.newBuilder()
            .setName(fileName)
            .setContent(methodsContent + DELIMITER)
            .setInsertionPoint(scopeType + fullMethodName)
            .build());
  }

  private Optional<String> createBuilderMethods(FieldDescriptorProto fieldDescriptor) {
    if (hasFieldPresence(fieldDescriptor)) {
      return Stream.of(setOrClearMethod(fieldDescriptor), optionalSetOrClearMethod(fieldDescriptor))
          .filter(Optional::isPresent)
          .map(Optional::get)
          .collect(Collectors.collectingAndThen(Collectors.joining(DELIMITER), Optional::of));
    }
    return Optional.empty();
  }

  private Optional<String> createClassMethods(FieldDescriptorProto fieldDescriptor) {
    if (hasFieldPresence(fieldDescriptor)) {
      return optionalGet(fieldDescriptor);
    }
    return Optional.empty();
  }

  private Optional<String> setOrClearMethod(FieldDescriptorProto fieldDescriptor) {
    if (!parameters.isSetterObject()) {
      return Optional.empty();
    }
    Map<?, ?> context = ImmutableMap.builder()
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
    Map<?, ?> context = ImmutableMap.builder()
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
    Map<?, ?> context = ImmutableMap.builder()
        .put(METHOD_NAME, getJavaMethodName(fieldDescriptor))
        .put(FIELD_TYPE, javaTypeName)
        .put(OPTIONAL_CLASS, getOptionalClassName(javaTypeName))
        .put(PRIMITIVE_OPTIONAL, isPrimitiveOptional(javaTypeName))
        .build();
    return Optional.of(applyTemplate(templatePath("optionalGet.mustache"), context));
  }

  private String getJavaMethodName(FieldDescriptorProto fieldDescriptor) {
    return fieldDescriptor.getJsonName().substring(0, 1).toUpperCase(Locale.ROOT) + fieldDescriptor.getJsonName().substring(1);
  }

  private String getJavaTypeName(FieldDescriptorProto fieldDescriptor) {
    String protoTypeName = fieldDescriptor.getTypeName();
    if (protoTypeName.isEmpty()) {
      return Optional.of(fieldDescriptor.getType())
          .map(FieldDescriptor.Type::valueOf)
          .map(FieldDescriptor.Type::getJavaType)
          .map(PRIMITIVE_CLASSES::get)
          .orElseThrow(() -> new IllegalArgumentException("Failed to find java type for field:\n" + fieldDescriptor));
    }
    return Optional.ofNullable(protoTypeMap.toJavaTypeName(protoTypeName))
        .orElseThrow(() -> new IllegalArgumentException("Failed to find java type for prototype '" + protoTypeName + "'"));
  }

  private String getOptionalClassName(String javaTypeName) {
    return parameters.isUsePrimitiveOptionals()
        ? PRIMITIVE_OPTIONALS.getOrDefault(javaTypeName, DEFAULT_OPTIONAL_CLASS)
        : DEFAULT_OPTIONAL_CLASS;
  }

  private String getOptionalGetterMethod(String javaTypeName) {
    return parameters.isUsePrimitiveOptionals()
        ? PRIMITIVE_OPTIONAL_GETTER_METHODS.getOrDefault(javaTypeName, DEFAULT_OPTIONAL_GETTER_METHOD)
        : DEFAULT_OPTIONAL_GETTER_METHOD;
  }

  private boolean isPrimitiveOptional(String javaTypeName) {
    return parameters.isUsePrimitiveOptionals() && PRIMITIVE_OPTIONALS.containsKey(javaTypeName);
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
}
