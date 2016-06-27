package net.yefremov.pegasusxml;

import com.linkedin.data.schema.ArrayDataSchema;
import com.linkedin.data.schema.DataSchema;
import com.linkedin.data.schema.EnumDataSchema;
import com.linkedin.data.schema.RecordDataSchema;
import com.predic8.schema.ComplexType;
import com.predic8.schema.Element;
import com.predic8.schema.Schema;
import com.predic8.schema.SchemaList;
import com.predic8.schema.Sequence;
import com.predic8.schema.SimpleType;
import com.predic8.schema.restriction.BaseRestriction;
import com.predic8.schema.restriction.TokenRestriction;
import com.predic8.schema.restriction.facet.EnumerationFacet;
import com.predic8.schema.restriction.facet.Facet;

import javax.xml.namespace.QName;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static com.predic8.soamodel.Consts.SCHEMA_NS;

public class XsdGenerator {

  private static final QName BOOLEAN = new QName(SCHEMA_NS, "boolean");
  private static final QName STRING = new QName(SCHEMA_NS, "string");
  private static final QName INT = new QName(SCHEMA_NS, "int");
  private static final QName LONG = new QName(SCHEMA_NS, "long");
  private static final QName BINARY = new QName(SCHEMA_NS, "base64Binary");

  private final String targetNamespace;
  private final Map<DataSchema, QName> registeredTypes = new HashMap<>();

  public XsdGenerator(String targetNamespace) {
    this.targetNamespace = targetNamespace;
  }

  public Schema generateXmlSchema(Collection<DataSchema> dataSchemas) {
    Schema schema = new Schema(targetNamespace);
    dataSchemas.forEach(dataSchema -> registerDataType(schema, dataSchema));
    return schema;
  }

  private QName registerDataType(Schema schema, DataSchema dataSchema) {
    return registeredTypes.computeIfAbsent(dataSchema, ds -> registerNewDataType(schema, ds));
  }

  private QName registerNewDataType(Schema schema, DataSchema dataSchema) {
    switch (dataSchema.getDereferencedType()) {
      case BOOLEAN: return BOOLEAN;
      case BYTES: return BINARY;
      case INT: return INT;
      case LONG: return LONG;
      case STRING: return STRING;
      case ENUM: return registerEnum(schema, (EnumDataSchema) dataSchema);
      case RECORD: return registerRecord(schema, (RecordDataSchema) dataSchema);
      case ARRAY: return registerArray(schema, (ArrayDataSchema) dataSchema);
      default: throw new IllegalArgumentException("Unexpected type: " + dataSchema.getType());
    }
  }

  private QName registerEnum(Schema schema, EnumDataSchema enumSchema) {
    List<Facet> facets = enumSchema.getSymbols().stream().map(symbol -> {
      EnumerationFacet facet = new EnumerationFacet();
      facet.setValue(symbol);
      return facet;
    }).collect(Collectors.toList());

    BaseRestriction restriction = new TokenRestriction();
    restriction.setBase(STRING);
    restriction.setFacets(facets);

    SimpleType simpleType = schema.newSimpleType(enumSchema.getFullName());
    simpleType.setRestriction(restriction);
    return new QName(targetNamespace, simpleType.getName());
  }

  private QName registerRecord(Schema schema, RecordDataSchema recordSchema) {
    ComplexType complexType = schema.newComplexType(recordSchema.getFullName());
    Sequence sequence = complexType.newSequence();
    for (RecordDataSchema.Field field : recordSchema.getFields()) {
      QName type = registerDataType(schema, field.getType());
      Element element = sequence.newElement(field.getName(), type);
      if (field.getOptional()) {
        element.setMinOccurs("0");
      }
    }
    return new QName(targetNamespace, complexType.getName());
  }

  private QName registerArray(Schema schema, ArrayDataSchema arraySchema) {
    QName itemType = registerDataType(schema, arraySchema.getItems());
    SchemaList schemaList = new SchemaList();
    schemaList.setItemType(itemType.getLocalPart());
    SimpleType simpleType = schema.newSimpleType(itemType.getLocalPart() + "Array");
    simpleType.setList(schemaList);
    return new QName(targetNamespace, simpleType.getName());
  }


}
