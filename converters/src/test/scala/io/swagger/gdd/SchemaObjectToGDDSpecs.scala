package io.swagger.gdd

import scala.collection.JavaConverters._

import io.swagger.gdd.SwaggerGenerators._
import io.swagger.gdd.models.Schema
import io.swagger.models.Model
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalacheck.Prop.{apply => _, _}
import org.specs2.specification.core.SpecStructure
import org.specs2.{ScalaCheck, Specification}

/**
 * Tests [[io.swagger.gdd.SwaggerToGDD#schemaObjectToGDD SwaggerToGDD.schemaObjectToGDD]].
 */
class SchemaObjectToGDDSpecs extends Specification with ScalaCheck with TestHelpers {
  override def is: SpecStructure = s2"""
  SwaggerToGDD.schemaObjectToGDD converts the Swagger Schema Object (Model) to a GDD Schema.

  For all models, it should:
    Set the Schema's id to the key under which the Model is defined       ${AllModels.id}
    Set the Schema's description to the Model's description               ${AllModels.description}
  For ModelImpls, it should:
    Set the Schema's type to the Model's type                             ${ModelImpls.`type`}
    Set the Schema's format to the Model's format                         ${ModelImpls.format}
    Set the Schema's default to the Model's default (as a String)         ${ModelImpls.default}
    Set the Schema's properties to the Model's properties, converted      ${ModelImpls.properties}
    Set the Schema's additionalProperties to the Model's additionalProperties, converted ${ModelImpls.additionalProperties}
  For ArrayModels, it should:
    Set the Schema's type to "array"                                      ${ArrayModels.`type`}
    Set the Schema's items to the Model's items, converted                ${ArrayModels.items}
  For RefModels, it should:
    Set the Schema's $$ref to the Model's $$ref (simpleRef)               ${RefModels.$ref}
  """

  def testSetBy[T, M <: Model](g: Gen[M])(t: Schema => T)(m: M => T) = {
    forAll(arbitrary[String], g) { (key, model) =>
      val schema = new SwaggerToGDD().schemaObjectToGDD(key, model)
      t(schema) must beEqualTo(m(model))
    }
  }

  def testSetTo[T, M <: Model](g: Gen[M])(t: Schema => T)(expected: T) = {
    forAll(arbitrary[String], g) { (key, model) =>
      val schema = new SwaggerToGDD().schemaObjectToGDD(key, model)
      t(schema) must beEqualTo(expected)
    }
  }

  def genAModel[M <: Model](modelGenerator: Option[Map[String, Model]] => Gen[M]) = for {
    globalDefinitions <- Gen.mapOf(Gen.zip(arbitrary[String], genModel()))
    model <- modelGenerator(Some(globalDefinitions))
  } yield model

  object AllModels {
    def id = forAll(arbitrary[String], genAModel(genModel)) { (key, model) =>
      val schema = new SwaggerToGDD().schemaObjectToGDD(key, model)
      schema.getId must beEqualTo(key)
    }
    def description = testSetBy(genAModel(genModel))(_.getDescription)(_.getDescription)
  }

  object ModelImpls {
    def `type` = testSetBy(genAModel(genModelImpl))(_.getType)(_.getType)
    def format = testSetBy(genAModel(genModelImpl))(_.getFormat)(_.getFormat)
    def default = testSetBy(genAModel(genModelImpl))(_.getDefault)(_.getDefaultValue)
    def properties = testSetBy(genAModel(genObjectModelImpl).guarantee(_.getProperties))(_.getProperties) { model =>
      model.getProperties.asScala.mapValues(new SwaggerToGDD().propertyToGDD).asJava
    }
    def additionalProperties = testSetBy(genAModel(genObjectModelImpl).guarantee(_.getAdditionalProperties))(_.getAdditionalProperties) { model =>
      new SwaggerToGDD().propertyToGDD(model.getAdditionalProperties)
    }
  }

  object ArrayModels {
    def `type` = testSetTo(genAModel(genArrayModel))(_.getType)("array")
    def items = testSetBy(genAModel(genArrayModel))(_.getItems) { model =>
      new SwaggerToGDD().propertyToGDD(model.getItems)
    }
  }

  object RefModels {
    def $ref = {
      forAll(arbitrary[String], Gen.mapOf(Gen.zip(arbitrary[String], genModel())).flatMap(genRefModel)) { (key, model) =>
        val swaggerToGDD = new SwaggerToGDD()
        val schema = swaggerToGDD.schemaObjectToGDD(key, model)
        schema.get$ref must beEqualTo(model.getSimpleRef)
      }
    }
  }
}
