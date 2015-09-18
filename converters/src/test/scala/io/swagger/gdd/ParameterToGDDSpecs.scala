package io.swagger.gdd

import scala.collection.JavaConverters._

import io.swagger.gdd.SwaggerGenerators._
import io.swagger.gdd.models.Parameter
import io.swagger.models.{Model, parameters}
import org.scalacheck.Gen
import org.scalacheck.Prop._
import org.specs2.specification.core.SpecStructure
import org.specs2.{ScalaCheck, Specification}

/**
 * Tests [[SwaggerToGDD.parameterToGDD)]].
 */
class ParameterToGDDSpecs extends Specification with ScalaCheck with TestHelpers {
  override def is: SpecStructure = s2"""
  SwaggerToGDD.parameterToGDD converts a Swagger Parameter to a GDD Parameter. It should populate the particular fields
  of the GDD Parameter based on the type of Swagger Parameter passed in.

  For all Parameter types except RefParameters:
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
    nothing else should be set                                                ${RefParameters.nothingElse}
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
    def testAllParameters = testSetBy(Gen.oneOf[parameters.Parameter](
      genQueryParameter, genBodyParameter()(), genHeaderParameter, genFormParameter, genCookieParameter
    )) _
    def id = testAllParameters(_.getId)(_.getName)
    def description = testAllParameters(_.getDescription)(_.getDescription)
    def required = testAllParameters(_.getRequired)(_.getRequired)
    def pattern = testAllParameters(_.getPattern)(_.getPattern)
  }

  object AbstractSerializableParameters {
    def `type` = testSetBy(genAbstractSerializableParameter)(_.getType)(_.getType)
    def format = testSetBy(genAbstractSerializableParameter)(_.getFormat)(_.getFormat)
    def enum = testSetBy(genAbstractSerializableParameter)(_.getEnum)(_.getEnum)
    def location = testSetBy(genAbstractSerializableParameter)(_.getLocation)(_.getIn)
    def default = testSetBy(genAbstractSerializableParameter)(_.getDefault)(_.getDefaultValue)
    def minimum = testSetBy(genAbstractSerializableParameter)(_.getMinimum)(p => Option(p.getMinimum).map(_.toString).orNull)
    def maximum = testSetBy(genAbstractSerializableParameter)(_.getMinimum)(p => Option(p.getMaximum).map(_.toString).orNull)
    def items = testSetBy(genAbstractSerializableParameter)(_.getItems)(p => Option(p.getItems).map(SwaggerToGDD.propertyToGDD).orNull)
    def repeated = {
      forAll(genAbstractSerializableParameter) { parameter =>
        Option(parameter.getCollectionFormat) match {
          case Some("multi") => (SwaggerToGDD.parameterToGDD(parameter).getRepeated: Boolean) must beTrue
          case _ => Option(SwaggerToGDD.parameterToGDD(parameter).getRepeated) match {
            case Some(b) => (b: Boolean) must beFalse
            case None => ok
          }
        }
      }
    }
  }

  object BodyParameters {
    def testSetByWithModel[T, M <: Model](g: Gen[M])(t: Parameter => T)(m: M => T) = {
      testSetBy(genBodyParameter()(_ => g))(t)(p => m(p.getSchema.asInstanceOf[M]))
    }
    def modelImplType = testSetByWithModel(genModelImpl())(_.getType)(_.getType)
    def modelImplFormat = testSetByWithModel(genModelImpl())(_.getFormat)(_.getFormat)
    def modelImplDefault = testSetByWithModel(genModelImpl())(_.getDefault)(_.getDefaultValue)
    def modelImplProperties = testSetByWithModel(genModelImpl())(_.getProperties)(m => Option(m.getProperties).map(_.asScala.mapValues(SwaggerToGDD.propertyToGDD).asJava).orNull)
    def modelImplAdditionalProperties = testSetByWithModel(genModelImpl())(_.getAdditionalProperties)(m => Option(m.getAdditionalProperties).map(SwaggerToGDD.propertyToGDD).orNull)

    def arrayModelType = testSetTo(genBodyParameter()(_ => genArrayModel()))(_.getType)("array")
    def arrayModelItems = testSetByWithModel(genArrayModel())(_.getItems)(m => Option(m.getItems).map(SwaggerToGDD.propertyToGDD).orNull)

    def refModel$ref = testSetByWithModel(Gen.mapOf(genSchema()).flatMap(genRefModel))(_.get$ref)(_.getSimpleRef)

    // todo ComposedModels
  }

  object RefParameters {
    def genRefParam = Gen.nonEmptyMap[String, parameters.Parameter](genAbstractSerializableParameter.map(p => p.getName -> p)).flatMap(genRefParameter[parameters.Parameter])
    def $ref = testSetBy(genRefParam)(_.get$ref)(_.getSimpleRef)
    def nothingElse = {
      forAll(genRefParam) { parameter =>
        val param = SwaggerToGDD.parameterToGDD(parameter)
        (param.getId must beNull) and (param.getDescription must beNull) and (param.getRequired must beNull) and
          (param.getPattern must beNull)
      }
    }
  }



}
