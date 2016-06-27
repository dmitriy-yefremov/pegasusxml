package net.yefremov.pegasusxml;

import com.google.common.base.Throwables;
import com.google.common.collect.ImmutableList;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.DataSchemaLocation;
import com.linkedin.data.schema.DataSchemaResolver;
import com.linkedin.data.schema.NamedDataSchema;
import com.linkedin.data.schema.SchemaParser;
import com.linkedin.data.schema.SchemaParserFactory;
import com.linkedin.data.schema.resolver.FileDataSchemaLocation;
import com.linkedin.data.schema.resolver.FileDataSchemaResolver;
import com.linkedin.util.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class PdscParser {

  private static final Logger LOG = LoggerFactory.getLogger(PdscParser.class);

  public Map<DataSchema, File> parseSources(File path, String resolverPath) {
    List<File> sources = expandSources(path);
    return parseSources(sources, resolverPath);
  }

  public Map<DataSchema, File> parseSources(List<File> sources, String resolverPath) {
    Map<DataSchema, File> result = new HashMap<>();
    FileDataSchemaResolver resolver = new FileDataSchemaResolver(SchemaParserFactory.instance(), resolverPath);
    sources.forEach( source -> {
      FileDataSchemaLocation schemaLocation = new FileDataSchemaLocation(source);
      List<DataSchema> schemas;
      if (resolver.locationResolved(schemaLocation)) {
        LOG.info("Skipping {}, already resolved", source);
        schemas = findResolvedSchemas(schemaLocation, resolver);

      } else {
        LOG.info("Parsing {}", source);
        schemas = parseSchema(schemaLocation, resolver);
      }
      schemas.forEach(schema -> result.put(schema, source));
    });
    return result;
  }


  private List<DataSchema> findResolvedSchemas(DataSchemaLocation schemaLocation, DataSchemaResolver resolver) {
    List<DataSchema> result = new ArrayList<>();
    Map<String, DataSchemaLocation> resolvedLocations = resolver.nameToDataSchemaLocations();
    resolvedLocations.forEach( (name, location) -> {
      if (location.equals(schemaLocation)) {
        NamedDataSchema existingSchema = resolver.existingDataSchema(name);
        if (existingSchema != null) {
          result.add(existingSchema);
        }
      }
    });
    return result;
  }

  private List<DataSchema> parseSchema(DataSchemaLocation schemaLocation, DataSchemaResolver resolver) {
    SchemaParser parser = new SchemaParser(resolver);
    try (FileInputStream schemaStream = new FileInputStream(schemaLocation.getSourceFile())) {
      parser.setLocation(schemaLocation);
      parser.parse(schemaStream);
      if (parser.hasError()) {
        throw new IllegalArgumentException(parser.errorMessage());
      } else {
        return parser.topLevelDataSchemas();
      }
    } catch (IOException e) {
      throw Throwables.propagate(e);
    }
  }

  private List<File> expandSources(File path) {
    if (path.isFile()) {
      return ImmutableList.of(path);
    } else if (path.isDirectory()) {
      FileUtil.FileExtensionFilter filter = new FileUtil.FileExtensionFilter(FileDataSchemaResolver.DEFAULT_EXTENSION);
      return FileUtil.listFiles(path, filter);
    } else {
      throw new IllegalArgumentException("Path not found: " + path.getAbsolutePath());
    }
  }

}
