package net.yefremov.pegasusxml;

import com.linkedin.data.schema.DataSchema;
import com.predic8.schema.Schema;

import java.io.File;
import java.util.Map;


public class Main {

  public static void main(String... args) {
    File path = new File(args[0]);
    String resolverPath = args[1];
    String targetNamespace = args[2];
    Map<DataSchema, File> dataSchemas = new PdscParser().parseSources(path, resolverPath);
    Schema schema = new XsdGenerator(targetNamespace).generateXmlSchema(dataSchemas.keySet());
    System.out.println(schema.getAsString());
  }
}
