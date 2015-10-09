package io.swagger.gdd

import io.swagger.gdd.SwaggerGenerators._
import org.scalacheck.Prop.{apply => _, _}
import org.specs2.specification.core.SpecStructure
import org.specs2.{ScalaCheck, Specification}

/**
 * Tests [[io.swagger.gdd.SwaggerToGDD#findMethodResponse SwaggerToGDD.findMethodResponse]].
 */
class FindMethodResponseSpecs extends Specification with ScalaCheck {
  override def is: SpecStructure = s2"""
  SwaggerToGDD.findMethodResponse selects the Response from an Operation which is most suited to be the "response"
  field of a GDD Method.

  It should:
    Choose a Response with a 2xx status code if there is one                      $choose2xx
    Choose the Response with the lowest 2xx status code                           $lowest2xx
    Choose a Response with the 'default' status if there is no 2xx status code    $default
    Return None if there is no 2xx status code nor the 'default' status.          $noCandidate
  """

  def choose2xx = {
    forAll(genResponse(), genResponse()) { (successResponse, failureResponse) =>
      val responses = Map("200" -> successResponse, "400" -> failureResponse)
      val swaggerToGDD = new SwaggerToGDD()
      swaggerToGDD.findMethodResponse(responses) must beSome(successResponse)
    }
  }
  def lowest2xx = {
    forAll(genResponse(), genResponse()) { (okResponse, createdResponse) =>
      val responses = Map("200" -> okResponse, "201" -> createdResponse)
      val swaggerToGDD = new SwaggerToGDD()
      swaggerToGDD.findMethodResponse(responses) must beSome(okResponse)
    }
  }
  def default = {
    forAll(genResponse(), genResponse()) { (defaultResponse, failureResponse) =>
      val responses = Map("default" -> defaultResponse, "400" -> failureResponse)
      val swaggerToGDD = new SwaggerToGDD()
      swaggerToGDD.findMethodResponse(responses) must beSome(defaultResponse)
    }
  }
  def noCandidate = {
    forAll(genResponse()) { failureResponse =>
      val responses = Map("400" -> failureResponse)
      val swaggerToGDD = new SwaggerToGDD()
      swaggerToGDD.findMethodResponse(responses) must beNone
    }
  }

}
