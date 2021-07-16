import static org.assertj.core.api.Assertions.assertThat;

import com.google.protobuf.ByteString;
import org.junit.jupiter.api.Test;
import org.test.datatypes.TestEnum;
import org.test.datatypes.TestMessage;
import org.test.datatypes.TestSubMessage;

class DataTypesTest {

  @Test
  void should_clear_field_data_on_null() {
    TestMessage.Builder builder = populatedTestMessage();

    builder.setOrClearOneofString(null);
    assertThat(builder.hasOneofString()).isFalse();

    builder.setOrClearOptionalBool(null);
    assertThat(builder.hasOptionalBool()).isFalse();

    builder.setOrClearOptionalString(null);
    assertThat(builder.hasOptionalString()).isFalse();

    builder.setOrClearOptionalBytes(null);
    assertThat(builder.hasOptionalBytes()).isFalse();

    builder.setOrClearOptionalEnum(null);
    assertThat(builder.hasOptionalEnum()).isFalse();

    builder.setOrClearOptionalFloat(null);
    assertThat(builder.hasOptionalFloat()).isFalse();

    builder.setOrClearOptionalDouble(null);
    assertThat(builder.hasOptionalDouble()).isFalse();

    builder.setOrClearOptionalFixed32(null);
    assertThat(builder.hasOptionalFixed32()).isFalse();

    builder.setOrClearOptionalFixed64(null);
    assertThat(builder.hasOptionalFixed64()).isFalse();

    builder.setOrClearOptionalInt32(null);
    assertThat(builder.hasOptionalInt32()).isFalse();

    builder.setOrClearOptionalInt64(null);
    assertThat(builder.hasOptionalInt64()).isFalse();

    builder.setOrClearOptionalUint32(null);
    assertThat(builder.hasOptionalUint32()).isFalse();

    builder.setOrClearOptionalUint64(null);
    assertThat(builder.hasOptionalUint64()).isFalse();

    builder.setOrClearSubMessage(null);
    assertThat(builder.hasSubMessage()).isFalse();
  }

  @Test
  void should_set_field_data_on_non_null() {
    TestMessage.Builder builder = populatedTestMessage();

    builder.setOrClearOneofString("one-of");
    assertThat(builder.hasOneofString()).isTrue();

    builder.setOrClearOptionalBool(true);
    assertThat(builder.hasOptionalBool()).isTrue();

    builder.setOrClearOptionalString("opt-string");
    assertThat(builder.hasOptionalString()).isTrue();

    builder.setOrClearOptionalBytes(ByteString.copyFromUtf8("opt-bytes"));
    assertThat(builder.hasOptionalBytes()).isTrue();

    builder.setOrClearOptionalEnum(TestEnum.value1);
    assertThat(builder.hasOptionalEnum()).isTrue();

    builder.setOrClearOptionalFloat(3.5F);
    assertThat(builder.hasOptionalFloat()).isTrue();

    builder.setOrClearOptionalDouble(3.5D);
    assertThat(builder.hasOptionalDouble()).isTrue();

    builder.setOrClearOptionalFixed32(10);
    assertThat(builder.hasOptionalFixed32()).isTrue();

    builder.setOrClearOptionalFixed64(10L);
    assertThat(builder.hasOptionalFixed64()).isTrue();

    builder.setOrClearOptionalInt32(10);
    assertThat(builder.hasOptionalInt32()).isTrue();

    builder.setOrClearOptionalInt64(10L);
    assertThat(builder.hasOptionalInt64()).isTrue();

    builder.setOrClearOptionalUint32(10);
    assertThat(builder.hasOptionalUint32()).isTrue();

    builder.setOrClearOptionalUint64(10L);
    assertThat(builder.hasOptionalUint64()).isTrue();

    builder.setOrClearSubMessage(TestSubMessage.newBuilder().setString("value").build());
    assertThat(builder.hasSubMessage()).isTrue();
  }

  @Test
  void should_have_optional_populated_with_field_data() {
    TestMessage message = populatedTestMessage().build();

    assertThat(message.getOptionalOneofString()).contains("one-of");
    assertThat(message.getOptionalOptionalBool()).contains(true);
    assertThat(message.getOptionalOptionalString()).contains("opt-string");
    assertThat(message.getOptionalOptionalBytes()).contains(ByteString.copyFromUtf8("opt-bytes"));
    assertThat(message.getOptionalOptionalEnum()).contains(TestEnum.value1);
    assertThat(message.getOptionalOptionalFloat()).contains(3.5F);
    assertThat(message.getOptionalOptionalDouble()).contains(3.5D);
    assertThat(message.getOptionalOptionalFixed32()).contains(10);
    assertThat(message.getOptionalOptionalFixed64()).contains(10L);
    assertThat(message.getOptionalOptionalInt32()).contains(10);
    assertThat(message.getOptionalOptionalInt64()).contains(10L);
    assertThat(message.getOptionalOptionalUint32()).contains(10);
    assertThat(message.getOptionalOptionalUint64()).contains(10L);
    assertThat(message.getOptionalSubMessage()).contains(TestSubMessage.newBuilder().setString("value").build());
  }

  @Test
  void should_have_empty_optional_on_no_data() {
    TestMessage message = TestMessage.getDefaultInstance();

    assertThat(message.getOptionalOneofString()).isEmpty();
    assertThat(message.getOptionalOptionalBool()).isEmpty();
    assertThat(message.getOptionalOptionalString()).isEmpty();
    assertThat(message.getOptionalOptionalBytes()).isEmpty();
    assertThat(message.getOptionalOptionalEnum()).isEmpty();
    assertThat(message.getOptionalOptionalFloat()).isEmpty();
    assertThat(message.getOptionalOptionalDouble()).isEmpty();
    assertThat(message.getOptionalOptionalFixed32()).isEmpty();
    assertThat(message.getOptionalOptionalFixed64()).isEmpty();
    assertThat(message.getOptionalOptionalInt32()).isEmpty();
    assertThat(message.getOptionalOptionalInt64()).isEmpty();
    assertThat(message.getOptionalOptionalUint32()).isEmpty();
    assertThat(message.getOptionalOptionalUint64()).isEmpty();
    assertThat(message.getOptionalSubMessage()).isEmpty();
  }

  private static TestMessage.Builder populatedTestMessage() {
    return TestMessage.newBuilder()
        .setOneofString("one-of")
        .setOptionalBool(true)
        .setOptionalString("opt-string")
        .setOptionalBytes(ByteString.copyFromUtf8("opt-bytes"))
        .setOptionalEnum(TestEnum.value1)
        .setOptionalFloat(3.5F)
        .setOptionalDouble(3.5D)
        .setOptionalFixed32(10)
        .setOptionalFixed64(10L)
        .setOptionalInt32(10)
        .setOptionalInt64(10L)
        .setOptionalUint32(10)
        .setOptionalUint64(10L)
        .setSubMessage(TestSubMessage.newBuilder().setString("value").build());
  }
}
