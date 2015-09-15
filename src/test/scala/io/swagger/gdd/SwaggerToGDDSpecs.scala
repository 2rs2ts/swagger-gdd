package io.swagger.gdd

import org.specs2.specification.core.SpecStructure
import org.specs2.{Specification, ScalaCheck}

class SwaggerToGDDSpecs extends Specification with ScalaCheck with TestHelpers {
  override def is: SpecStructure = s2"""

  SwaggerToGDD takes Swagger model objects and converts them to their GDD equivalents.

  SwaggerToGDD.convertProperty converts a Swagger Property into a GDD Schema.

  """
}
