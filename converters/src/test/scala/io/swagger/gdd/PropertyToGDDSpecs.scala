package io.swagger.gdd

import scala.collection.JavaConverters._

import io.swagger.gdd.models.Schema
import io.swagger.models.properties
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalacheck.Prop.{apply => _, _}
import org.specs2.specification.core.SpecStructure
import org.specs2.{ScalaCheck, Specification}

import SwaggerGenerators._

/**
 * Tests [[SwaggerToGDD.propertyToGDD]].
 */
class PropertyToGDDSpecs extends Specification with ScalaCheck with TestHelpers {
  override def is: SpecStructure = s2"""
  SwaggerToGDD.propertyToGDD converts a Swagger Property to a Schema. It should populate the particular fields of the
  Schema based on the type of Swagger Property passed in.

  For all Property types:
    id should be set by the Property's name                                     ${AllProperties.id}
    description should be set by the Property's description                     ${AllProperties.description}
    required should be set by the Property's required                           ${AllProperties.required}
  For BooleanProperties:
    type should be "boolean"                                                    ${BooleanProperties.`type`}
  For StringProperties:
    type should be "string"                                                     ${StringProperties.`type`}
    format should be "byte" only if the Property's format was "byte"            ${StringProperties.format}
    pattern should be set by the Property's pattern                             ${StringProperties.pattern}
    enum should be set by the Property's enum                                   ${StringProperties.enum}
    default should be set by the Property's default                             ${StringProperties.default}
  For EmailProperties:
    type should be "string"                                                     ${EmailProperties.`type`}
    pattern should be set by the Property's pattern                             ${EmailProperties.pattern}
    default should be set by the Property's default                             ${EmailProperties.default}
  For UUIDProperties:
    type should be "string"                                                     ${UUIDProperties.`type`}
    pattern should be set by the Property's pattern                             ${UUIDProperties.pattern}
  For DateProperties:
    type should be "string"                                                     ${DateProperties.`type`}
    format should be "date"                                                     ${DateProperties.format}
  For DateTimeProperties:
    type should be "string"                                                     ${DateTimeProperties.`type`}
    format should be "date-time"                                                ${DateTimeProperties.format}
  For ByteArrayProperties:
    type should be "string"                                                     ${ByteArrayProperties.`type`}
    format should be "byte"                                                     ${ByteArrayProperties.format}
  For IntegerProperties:
    type should be "integer"                                                    ${IntegerProperties.`type`}
    format should be "int32"                                                    ${IntegerProperties.format}
    minimum should be set by the Property's minimum                             ${IntegerProperties.minimum}
    maximum should be set by the Property's maximum                             ${IntegerProperties.maximum}
    default should be set by the Property's default                             ${IntegerProperties.default}
  For LongProperties:
    type should be "string"                                                     ${LongProperties.`type`}
    format should be "int64"                                                    ${LongProperties.format}
    minimum should be set by the Property's minimum                             ${LongProperties.minimum}
    maximum should be set by the Property's maximum                             ${LongProperties.maximum}
    default should be set by the Property's default                             ${LongProperties.default}
  For BaseIntegerProperties:
    type should be "integer"                                                    ${BaseIntegerProperties.`type`}
    minimum should be set by the Property's minimum                             ${BaseIntegerProperties.minimum}
    maximum should be set by the Property's maximum                             ${BaseIntegerProperties.maximum}
  For DoubleProperties:
    type should be "number"                                                     ${DoubleProperties.`type`}
    format should be "double"                                                   ${DoubleProperties.format}
    minimum should be set by the Property's minimum                             ${DoubleProperties.minimum}
    maximum should be set by the Property's maximum                             ${DoubleProperties.maximum}
    default should be set by the Property's default                             ${DoubleProperties.default}
  For FloatProperties:
    type should be "number"                                                     ${FloatProperties.`type`}
    format should be "float"                                                    ${FloatProperties.format}
    minimum should be set by the Property's minimum                             ${FloatProperties.minimum}
    maximum should be set by the Property's maximum                             ${FloatProperties.maximum}
    default should be set by the Property's default                             ${FloatProperties.default}
  For DecimalProperties:
    type should be "number"                                                     ${DecimalProperties.`type`}
    minimum should be set by the Property's minimum                             ${DecimalProperties.minimum}
    maximum should be set by the Property's maximum                             ${DecimalProperties.maximum}
  For ArrayProperties:
    type should be "array"                                                      ${ArrayProperties.`type`}
    items should be set by the Property's items                                 ${ArrayProperties.items}
  For MapProperties:
    type should be "object"                                                     ${MapProperties.`type`}
    additionalProperties should be set by the Property's additionalProperties   ${MapProperties.additionalProperties}
  For ObjectProperties:
    type should be "object"                                                     ${ObjectProperties.`type`}
    properties should be set by the Property's properties                       ${ObjectProperties.properties}
  For RefProperties:
    $$ref should be set by the Property's $$ref (#/definitions/{id})            ${RefProperties.$ref}
  """

  def testSetBy[T, P <: properties.Property](g: Gen[P])(t: Schema => T)(p: P => T) = {
    forAll(g) { property =>
      val schema = SwaggerToGDD.propertyToGDD(property)
      t(schema) must beEqualTo(p(property))
    }
  }

  def testSetTo[T, P <: properties.Property](g: Gen[P])(t: Schema => T)(expected: T) = {
    forAll(g) { property =>
      val schema = SwaggerToGDD.propertyToGDD(property)
      t(schema) must beEqualTo(expected)
    }
  }

  object AllProperties {
    def id = testSetBy(genProperty())(_.getId)(_.getName)
    def description = testSetBy(genProperty())(_.getDescription)(_.getDescription)
    def required = testSetBy(genProperty())(_.getRequired)(_.getRequired)
  }

  object BooleanProperties {
    def `type` = testSetTo(genBooleanProperty)(_.getType)("boolean")
  }

  object StringProperties {
    def `type` = testSetTo(genStringProperty)(_.getType)("string")
    def format = {
      forAll(genStringProperty, arbitrary[String].suchThat(_ != "byte")) { (property, fakeFormat) =>
        property.setFormat("byte")
        val byteFormatSchema = SwaggerToGDD.propertyToGDD(property)
        property.setFormat(fakeFormat)
        val nonByteFormatSchema = SwaggerToGDD.propertyToGDD(property)
        (byteFormatSchema.getFormat must beEqualTo("byte")) and (nonByteFormatSchema.getFormat must beNull)
      }
    }
    def pattern = testSetBy(genStringProperty)(_.getPattern)(_.getPattern)
    def enum = testSetBy(genStringProperty)(_.getEnum)(_.getEnum)
    def default = testSetBy(genStringProperty)(_.getDefault)(_.getDefault)
  }

  object EmailProperties {
    def `type` = testSetTo(genEmailProperty)(_.getType)("string")
    def pattern = testSetBy(genEmailProperty)(_.getPattern)(_.getPattern)
    def default = testSetBy(genEmailProperty)(_.getDefault)(_.getDefault)
  }

  object UUIDProperties {
    def `type` = testSetTo(genUUIDProperty)(_.getType)("string")
    def pattern = testSetBy(genUUIDProperty)(_.getPattern)(_.getPattern)
  }

  object DateProperties {
    def `type` = testSetTo(genDateProperty)(_.getType)("string")
    def format = testSetTo(genDateProperty)(_.getFormat)("date")
  }

  object DateTimeProperties {
    def `type` = testSetTo(genDateTimeProperty)(_.getType)("string")
    def format = testSetTo(genDateTimeProperty)(_.getFormat)("date-time")
  }

  object ByteArrayProperties {
    def `type` = testSetTo(genByteArrayProperty)(_.getType)("string")
    def format = testSetTo(genByteArrayProperty)(_.getFormat)("byte")
  }

  object IntegerProperties {
    def `type` = testSetTo(genIntegerProperty)(_.getType)("integer")
    def format = testSetTo(genIntegerProperty)(_.getFormat)("int32")
    def minimum = testSetBy(genIntegerProperty)(_.getMinimum)(p => Option(p.getMinimum).map(_.toString).orNull)
    def maximum = testSetBy(genIntegerProperty)(_.getMaximum)(p => Option(p.getMaximum).map(_.toString).orNull)
    def default = testSetBy(genIntegerProperty)(_.getDefault)(p => Option(p.getDefault).map(_.toString).orNull)
  }

  object LongProperties {
    def `type` = testSetTo(genLongProperty)(_.getType)("string")
    def format = testSetTo(genLongProperty)(_.getFormat)("int64")
    def minimum = testSetBy(genLongProperty)(_.getMinimum)(p => Option(p.getMinimum).map(_.toString).orNull)
    def maximum = testSetBy(genLongProperty)(_.getMaximum)(p => Option(p.getMaximum).map(_.toString).orNull)
    def default = testSetBy(genLongProperty)(_.getDefault)(p => Option(p.getDefault).map(_.toString).orNull)
  }

  object BaseIntegerProperties {
    def `type` = testSetTo(genBaseIntegerProperty)(_.getType)("integer")
    def minimum = testSetBy(genBaseIntegerProperty)(_.getMinimum)(p => Option(p.getMinimum).map(_.toString).orNull)
    def maximum = testSetBy(genBaseIntegerProperty)(_.getMaximum)(p => Option(p.getMaximum).map(_.toString).orNull)
  }

  object DoubleProperties {
    def `type` = testSetTo(genDoubleProperty)(_.getType)("number")
    def format = testSetTo(genDoubleProperty)(_.getFormat)("double")
    def minimum = testSetBy(genDoubleProperty)(_.getMinimum)(p => Option(p.getMinimum).map(_.toString).orNull)
    def maximum = testSetBy(genDoubleProperty)(_.getMaximum)(p => Option(p.getMaximum).map(_.toString).orNull)
    def default = testSetBy(genDoubleProperty)(_.getDefault)(p => Option(p.getDefault).map(_.toString).orNull)
  }

  object FloatProperties {
    def `type` = testSetTo(genFloatProperty)(_.getType)("number")
    def format = testSetTo(genFloatProperty)(_.getFormat)("float")
    def minimum = testSetBy(genFloatProperty)(_.getMinimum)(p => Option(p.getMinimum).map(_.toString).orNull)
    def maximum = testSetBy(genFloatProperty)(_.getMaximum)(p => Option(p.getMaximum).map(_.toString).orNull)
    def default = testSetBy(genFloatProperty)(_.getDefault)(p => Option(p.getDefault).map(_.toString).orNull)
  }

  object DecimalProperties {
    def `type` = testSetTo(genDecimalProperty)(_.getType)("number")
    def minimum = testSetBy(genDecimalProperty)(_.getMinimum)(p => Option(p.getMinimum).map(_.toString).orNull)
    def maximum = testSetBy(genDecimalProperty)(_.getMaximum)(p => Option(p.getMaximum).map(_.toString).orNull)
  }

  object ArrayProperties {
    def `type` = testSetTo(genArrayProperty())(_.getType)("array")
    def items = testSetBy(genArrayProperty())(_.getItems)(p => SwaggerToGDD.propertyToGDD(p.getItems))
  }

  object MapProperties {
    def `type` = testSetTo(genMapProperty())(_.getType)("object")
    def additionalProperties = testSetBy(genMapProperty().guarantee(_.getAdditionalProperties))(_.getAdditionalProperties)(p => SwaggerToGDD.propertyToGDD(p.getAdditionalProperties))
  }

  object ObjectProperties {
    def `type` = testSetTo(genObjectProperty())(_.getType)("object")
    def properties = testSetBy(genObjectProperty().guarantee(_.getProperties))(_.getProperties)(p => p.getProperties.asScala.mapValues(SwaggerToGDD.propertyToGDD).asJava)
  }

  object RefProperties {
    def $ref = testSetBy(genSchema().map(Map(_)).flatMap(genRefProperty))(_.get$ref())(_.getSimpleRef)
  }

  // todo FileProperties

}
