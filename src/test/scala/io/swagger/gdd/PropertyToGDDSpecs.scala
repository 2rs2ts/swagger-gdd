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
    id should be set by the Property's name                                     $id

  """

  def id = {
    forAll(genProperty()) { property =>
      val schema = SwaggerToGDD.propertyToGDD(property)
      schema.id must beEqualTo(property.getName)
    }
  }

}
