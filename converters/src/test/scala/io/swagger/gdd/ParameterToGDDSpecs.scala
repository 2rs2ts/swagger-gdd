package io.swagger.gdd

import io.swagger.gdd.models.Parameter
import io.swagger.models.parameters
import io.swagger.models.parameters.AbstractSerializableParameter
import org.scalacheck.Gen
import org.scalacheck.Prop._
import org.specs2.specification.core.SpecStructure
import org.specs2.{ScalaCheck, Specification}

import SwaggerGenerators._

/**
 * Tests [[SwaggerToGDD.parameterToGDD)]].
 */
class ParameterToGDDSpecs extends Specification with ScalaCheck with TestHelpers {
  override def is: SpecStructure = s2"""
  SwaggerToGDD.parameterToGDD converts a Swagger Parameter to a GDD Parameter. It should populate the particular fields
  of the GDD Parameter based on the type of Swagger Parameter passed in.

  For all Parameter types:
    id should be set by the Parameter's name                                  ${AllParameters.id}
    description should be set by the Parameter's description                  ${AllParameters.description}
    required should be set by the Parameter's required                        ${AllParameters.required}
    pattern should be set by the Parameter's pattern                          ${AllParameters.pattern}
  For PathParameters, QueryParameters, HeaderParameters, CookieParameters, FormParameters:
    type should be set by the Parameter's type                                ${AbstractSerializableParameters.`type`}
    format should be set by the Parameter's format                            ${AbstractSerializableParameters.format}
    enum should be set by the Parameter's enum                                ${AbstractSerializableParameters.enum}
    location should be set to "path"                                          ${AbstractSerializableParameters.location}
    default should be set by the Parameter's default                          ${AbstractSerializableParameters.default}
    minimum should be set by the Parameter's minimum                          ${AbstractSerializableParameters.minimum}
    maximum should be set by the Parameter's maximum                          ${AbstractSerializableParameters.maximum}
    items should be set by the Parameter's items                              ${AbstractSerializableParameters.items}
    repeated should be set to true only if the Parameter's collectionFormat is "multi" ${AbstractSerializableParameters.repeated}
  For BodyParameters:
    If the Parameter's schema is a ModelImpl:
      type should be set by the schema's type                                 ${BodyParameters.modelImplType}
      format should be set by the schema's format                             ${BodyParameters.modelImplFormat}
      default should be set by the schema's default                           ${BodyParameters.modelImplDefault}
      properties should be set by the schema's properties                     ${BodyParameters.modelImplProperties}
      additionalProperties should be set by the schema's additionalProperties ${BodyParameters.modelImplAdditionalProperties}
    If the Parameter's schema is an ArrayModel:
      type should be "array"                                                  ${BodyParameters.arrayModelType}
      items should be set by the schema's items                               ${BodyParameters.arrayModelItems}
    If the Parameter's schema is a RefModel:
      $$ref should be set by the schema's $$ref (simple ref)                  ${BodyParameters.refModel$ref}
  For RefParameters:
    $$ref should be set by the Parameter's $$ref (simple ref)                 ${RefParameters.$ref}
  """

  def testSetBy[T, P <: parameters.Parameter](g: Gen[P])(t: Parameter => T)(p: P => T) = {
    forAll(g) { parameter =>
      val param = SwaggerToGDD.parameterToGDD(parameter)
      t(param) must beEqualTo(p(parameter))
    }
  }

  def testSetTo[T, P <: parameters.Parameter](g: Gen[P])(t: Parameter => T)(expected: T) = {
    forAll(g) { parameter =>
      val param = SwaggerToGDD.parameterToGDD(parameter)
      t(param) must beEqualTo(expected)
    }
  }

  object AllParameters {
    def id = pending
    def description = pending
    def required = pending
    def pattern = pending
  }

  trait AbstractSerializableParameters {
    def parameterGenerator = Gen.oneOf[AbstractSerializableParameter[_]](
      genPathParameter, genQueryParameter, genHeaderParameter, genCookieParameter, genFormParameter
    )

    def `type` = pending
    def format = pending
    def enum = pending
    def location = pending
    def default = pending
    def minimum = pending
    def maximum = pending
    def items = pending
    def repeated = pending
  }
  object AbstractSerializableParameters extends AbstractSerializableParameters

  object PathParameters extends AbstractSerializableParameters {
    override def parameterGenerator = genPathParameter
  }

  object QueryParameters extends AbstractSerializableParameters {
    override def parameterGenerator = genQueryParameter
  }

  object HeaderParameters extends AbstractSerializableParameters {
    override def parameterGenerator = genHeaderParameter
  }

  object CookieParameters extends AbstractSerializableParameters {
    override def parameterGenerator = genCookieParameter
  }

  object FormParameters extends AbstractSerializableParameters {
    override def parameterGenerator = genFormParameter
  }

  object BodyParameters {
    def modelImplType = pending
    def modelImplFormat = pending
    def modelImplDefault = pending
    def modelImplProperties = pending
    def modelImplAdditionalProperties = pending

    def arrayModelType = pending
    def arrayModelItems = pending

    def refModel$ref = pending

    // todo ComposedModels
  }

  object RefParameters {
    def $ref = pending
  }



}
