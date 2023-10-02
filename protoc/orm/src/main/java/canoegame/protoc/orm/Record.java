package canoegame.protoc.orm;

import com.google.protobuf.DescriptorProtos;

public class Record {
    public static final String SUFFIX = "Record";
    private String pkg;
    private String name;

    public String getPkg() {
        return pkg;
    }

    public String getName() {
        return name;
    }

    public String getFullName() {
        return pkg + "." + name;
    }

    public String getJavaFile() {
        return pkg.replace(".", "/") + "/" + name + ".java";
    }

    public static Record create(String pkg, DescriptorProtos.DescriptorProto msg) {
        var record = new Record();
        record.pkg = pkg;
        record.name = msg.getName() + SUFFIX;
        return record;
    }
}
