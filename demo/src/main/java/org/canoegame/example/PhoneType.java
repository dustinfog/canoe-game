// Generated by the protocol buffer compiler.  DO NOT EDIT!
// source: test.proto

package org.canoegame.example;

/**
 * Protobuf enum {@code protocol.PhoneType}
 */
public enum PhoneType
    implements com.google.protobuf.ProtocolMessageEnum {
  /**
   * <code>MOBILE = 0;</code>
   */
  MOBILE(0),
  /**
   * <code>HOME = 1;</code>
   */
  HOME(1),
  /**
   * <code>WORK = 2;</code>
   */
  WORK(2),
  UNRECOGNIZED(-1),
  ;

  /**
   * <code>MOBILE = 0;</code>
   */
  public static final int MOBILE_VALUE = 0;
  /**
   * <code>HOME = 1;</code>
   */
  public static final int HOME_VALUE = 1;
  /**
   * <code>WORK = 2;</code>
   */
  public static final int WORK_VALUE = 2;


  public final int getNumber() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalArgumentException(
          "Can't get the number of an unknown enum value.");
    }
    return value;
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   * @deprecated Use {@link #forNumber(int)} instead.
   */
  @java.lang.Deprecated
  public static PhoneType valueOf(int value) {
    return forNumber(value);
  }

  /**
   * @param value The numeric wire value of the corresponding enum entry.
   * @return The enum associated with the given numeric wire value.
   */
  public static PhoneType forNumber(int value) {
    switch (value) {
      case 0: return MOBILE;
      case 1: return HOME;
      case 2: return WORK;
      default: return null;
    }
  }

  public static com.google.protobuf.Internal.EnumLiteMap<PhoneType>
      internalGetValueMap() {
    return internalValueMap;
  }
  private static final com.google.protobuf.Internal.EnumLiteMap<
      PhoneType> internalValueMap =
        new com.google.protobuf.Internal.EnumLiteMap<PhoneType>() {
          public PhoneType findValueByNumber(int number) {
            return PhoneType.forNumber(number);
          }
        };

  public final com.google.protobuf.Descriptors.EnumValueDescriptor
      getValueDescriptor() {
    if (this == UNRECOGNIZED) {
      throw new java.lang.IllegalStateException(
          "Can't get the descriptor of an unrecognized enum value.");
    }
    return getDescriptor().getValues().get(ordinal());
  }
  public final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptorForType() {
    return getDescriptor();
  }
  public static final com.google.protobuf.Descriptors.EnumDescriptor
      getDescriptor() {
    return org.canoegame.example.Test.getDescriptor().getEnumTypes().get(0);
  }

  private static final PhoneType[] VALUES = values();

  public static PhoneType valueOf(
      com.google.protobuf.Descriptors.EnumValueDescriptor desc) {
    if (desc.getType() != getDescriptor()) {
      throw new java.lang.IllegalArgumentException(
        "EnumValueDescriptor is not for this type.");
    }
    if (desc.getIndex() == -1) {
      return UNRECOGNIZED;
    }
    return VALUES[desc.getIndex()];
  }

  private final int value;

  private PhoneType(int value) {
    this.value = value;
  }

  // @@protoc_insertion_point(enum_scope:protocol.PhoneType)
}

