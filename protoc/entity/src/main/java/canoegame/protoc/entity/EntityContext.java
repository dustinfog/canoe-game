package canoegame.protoc.entity;

import com.google.protobuf.DescriptorProtos;
import com.salesforce.jprotoc.ProtoTypeMap;

import java.util.ArrayList;
import java.util.List;

public class EntityContext {
    public static final String SUFFIX = "EntityBase";

    private final String pkg;
    private final DescriptorProtos.DescriptorProto message;
    private final List<Field> fields;

    private final ProtoTypeMap protoTypeMap;

    public EntityContext(String pkg, DescriptorProtos.DescriptorProto message, ProtoTypeMap protoTypeMap) {
        this.pkg = pkg;
        this.message = message;
        this.protoTypeMap = protoTypeMap;
        fields = new ArrayList<>();
        for (var field : message.getFieldList()) {
            fields.add(new Field(field.getName(), field.getNumber()));
        }
    }

    public DescriptorProtos.DescriptorProto getMessage() {
        return message;
    }

    public List<Field> getFields() {
        return fields;
    }

    public String getPkg() {
        return pkg;
    }

    public String getFullName() {
        return pkg + "." + getClassName();
    }

    public String getClassName() {
        return message.getName() + SUFFIX;
    }

    public String getJavaFile() {
        return pkg.replace(".", "/") + "/" + getClassName() + ".java";
    }

    public record Field(String name, int number) {
        public String getUpperName() {
            return name.toUpperCase();
        }
    }
}
