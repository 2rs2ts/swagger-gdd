package io.swagger.gdd

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

  object AllProperties {
    def id = {
      forAll(genProperty()) { property =>
        val schema = SwaggerToGDD.propertyToGDD(property)
        schema.getId must beEqualTo(property.getName)
      }
    }

    def description = {
      forAll(genProperty().guarantee(_.getDescription)) { property =>
        val schema = SwaggerToGDD.propertyToGDD(property)
        schema.getDescription must beEqualTo(property.getDescription)
      }
    }

    def required = {
      forAll(genProperty().guarantee(_.getRequired)) { property =>
        val schema = SwaggerToGDD.propertyToGDD(property)
        schema.getRequired must beEqualTo(property.getRequired)
      }
    }
  }

  object BooleanProperties {
    def `type` = pending
  }

  object StringProperties {
    def `type` = pending
    def format = pending
    def pattern = pending
    def enum = pending
    def default = pending
  }

  object EmailProperties {
    def `type` = pending
    def pattern = pending
    def default = pending
  }

  object UUIDProperties {
    def `type` = pending
    def pattern = pending
  }

  object DateProperties {
    def `type` = pending
    def format = pending
  }

  object DateTimeProperties {
    def `type` = pending
    def format = pending
  }

  object ByteArrayProperties {
    def `type` = pending
    def format = pending
  }

  object IntegerProperties {
    def `type` = pending
    def format = pending
    def minimum = pending
    def maximum = pending
    def default = pending
  }

  object LongProperties {
    def `type` = pending
    def format = pending
    def minimum = pending
    def maximum = pending
    def default = pending
  }

  object BaseIntegerProperties {
    def `type` = pending
    def minimum = pending
    def maximum = pending
  }

  object DoubleProperties {
    def `type` = pending
    def format = pending
    def minimum = pending
    def maximum = pending
    def default = pending
  }

  object FloatProperties {
    def `type` = pending
    def format = pending
    def minimum = pending
    def maximum = pending
    def default = pending
  }

  object DecimalProperties {
    def `type` = pending
    def minimum = pending
    def maximum = pending
  }

  object ArrayProperties {
    def `type` = pending
    def items = pending
  }

  object MapProperties {
    def `type` = pending
    def additionalProperties = pending
  }

  object ObjectProperties {
    def `type` = pending
    def properties = pending
  }

  object RefProperties {
    def $ref = pending
  }

  // todo FileProperties

}
