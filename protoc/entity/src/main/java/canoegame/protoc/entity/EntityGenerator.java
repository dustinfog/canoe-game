package canoegame.protoc.entity;

import com.google.common.base.Strings;
import com.google.protobuf.compiler.PluginProtos;
import com.salesforce.jprotoc.Generator;
import com.salesforce.jprotoc.GeneratorException;
import com.salesforce.jprotoc.ProtoTypeMap;
import com.salesforce.jprotoc.ProtocPlugin;
import org.canoegame.entity.Extension;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.Stream;

public class EntityGenerator extends Generator {
    public static void main(String[] args) {
        if (args.length == 0) {
            // Generate from protoc via stdin
            ProtocPlugin.generate(List.of(new EntityGenerator()), List.of(Extension.database));
        } else {
            // Process from a descriptor_dump file via command line arg
            ProtocPlugin.debug(List.of(new EntityGenerator()), List.of(Extension.database), args[0]);
        }
    }

    @Override
    protected List<PluginProtos.CodeGeneratorResponse.Feature> supportedFeatures() {
        return Collections.singletonList(PluginProtos.CodeGeneratorResponse.Feature.FEATURE_PROTO3_OPTIONAL);
    }

    @Override
    public Stream<PluginProtos.CodeGeneratorResponse.File> generate(PluginProtos.CodeGeneratorRequest request)
            throws GeneratorException {

        final ProtoTypeMap protoTypeMap = ProtoTypeMap.of(request.getProtoFileList());

        List<PluginProtos.CodeGeneratorResponse.File> protoFiles = new ArrayList<>();
        for(var protoFile : request.getProtoFileList()) {
            var fileOptions = protoFile.getOptions();
            final String javaPackage = Strings.emptyToNull(
                    fileOptions.hasJavaPackage() ?
                            fileOptions.getJavaPackage() :
                            protoFile.getPackage());

            System.out.println("javaPackage: " + javaPackage);


            var msgs = protoFile.getMessageTypeList();

            for (var msg : msgs) {
                var database = msg.getOptions().getExtension(Extension.database);

                if (database.isEmpty()) {
                    continue;
                }

                var ctx = new EntityContext(javaPackage, msg, protoTypeMap);

                String content = applyTemplate("entity.mustache", ctx);
                protoFiles.add(PluginProtos.CodeGeneratorResponse.File
                        .newBuilder()
                        .setName(ctx.getJavaFile())
                        .setContent(content)
                        .build());
            }
        }

        return protoFiles.stream();
    }
}
