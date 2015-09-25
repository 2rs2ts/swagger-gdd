package io.swagger.gdd

import scala.collection.JavaConverters._

import io.swagger.gdd.SwaggerGenerators._
import io.swagger.gdd.models.GoogleDiscoveryDocument
import io.swagger.models.parameters.{Parameter => SwaggerParameter}
import io.swagger.models.properties.RefProperty
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalacheck.Gen._
import org.scalacheck.Prop.{apply => _, _}
import org.specs2.mock.Mockito
import org.specs2.specification.core.SpecStructure
import org.specs2.{ScalaCheck, Specification}

/**
 * Tests [[io.swagger.gdd.SwaggerToGDD#operationToGDD SwaggerToGDD.operationToGDD]].
 */
class OperationToGDDSpecs extends Specification with ScalaCheck with Mockito with TestHelpers {
  override def is: SpecStructure = s2"""
  SwaggerToGDD.operationToGDD converts a Swagger Operation to a GDD Method.

  It should:
    Set the Method's id to the Operation's operationId                                              $id
    Set the Method's description to the Operation's summary                                         $description
    Set the Method's httpMethod to the key under which the Operation was defined                    $httpMethod
    Set the Method's path to the key under which the Path object holding the Operation was defined  $path
    Set the Method's parameters based on the Operation's parameters, sans body parameters           $parameters
    Set the Method's response based on the Operation's responses                                    $response
    If the chosen Response's schema is not a RefProperty, add it to the
    |  GoogleDiscoveryDocument's schemas                                                            $responseNotRef
    If the chosen Response does not have a schema, not add a response to the Method                 $noResponse
    Set the Method's request to the Operation's body parameter, if it has one                       $request
    If the chosen Parameter is not a RefParameter, add it to the
    |  GoogleDiscoveryDocument's schemas                                                            $requestNotRef
    If there is no body parameter, not add a request to the Method                                  $noRequest

  """

  def id = {
    forAll(genOperation(), arbitrary[String], arbitrary[String]) { (operation, pathValue, httpMethod) =>
      val method = new SwaggerToGDD().operationToGDD(operation, pathValue, httpMethod, mock[GoogleDiscoveryDocument])
      method.getId must beEqualTo(operation.getOperationId)
    }
  }
  def description = {
    forAll(genOperation(), arbitrary[String], arbitrary[String]) { (operation, pathValue, httpMethod) =>
      val method = new SwaggerToGDD().operationToGDD(operation, pathValue, httpMethod, mock[GoogleDiscoveryDocument])
      method.getDescription must beEqualTo(operation.getSummary)
    }
  }
  def httpMethod = {
    forAll(genOperation(), arbitrary[String], arbitrary[String]) { (operation, pathValue, httpMethod) =>
      val method = new SwaggerToGDD().operationToGDD(operation, pathValue, httpMethod, mock[GoogleDiscoveryDocument])
      method.getHttpMethod must beEqualTo(httpMethod)
    }
  }
  def path = {
    forAll(genOperation(), arbitrary[String], arbitrary[String]) { (operation, pathValue, httpMethod) =>
      val method = new SwaggerToGDD().operationToGDD(operation, pathValue, httpMethod, mock[GoogleDiscoveryDocument])
      method.getPath must beEqualTo(pathValue)
    }
  }
  def parameters = {
    forAll(genOperation().guarantee(_.getParameters), arbitrary[String], arbitrary[String]) { (operation, pathValue, httpMethod) =>
      val swaggerToGDD = new SwaggerToGDD()
      val method = swaggerToGDD.operationToGDD(operation, pathValue, httpMethod, mock[GoogleDiscoveryDocument])
      val expected = operation.getParameters.asScala.filter(p => "body" != p.getIn).map(swaggerToGDD.parameterToGDD).map(p => p.getId -> p).toMap
      method.getParameters.asScala must beEqualTo(expected)
    }
  }
  def response = {
    forAll(genOperation(), genResponse().guarantee(_.getSchema), arbitrary[String], arbitrary[String]) { (operation, response, pathValue, httpMethod) =>
      val responses = operation.getResponses.asScala + ("200" -> response)
      operation.setResponses(responses.asJava)
      operation.getResponses.put("200", response)
      val swaggerToGDD = new SwaggerToGDD()
      val method = swaggerToGDD.operationToGDD(operation, pathValue, httpMethod, mock[GoogleDiscoveryDocument])
      val responseRef = method.getResponse.get$ref()
      responseRef must beEqualTo(response.getSchema.getName) or (responseRef must beEqualTo(s"${method.getId}Response"))
    }
  }
  def responseNotRef = {
    forAll(genOperation(), genResponse().guarantee(_.getSchema).suchThat(!_.getSchema.isInstanceOf[RefProperty]),
        arbitrary[String], arbitrary[String]) { (operation, response, pathValue, httpMethod) =>
      val responses = operation.getResponses.asScala + ("200" -> response)
      operation.setResponses(responses.asJava)
      operation.setParameters(null) // avoid interaction with mock call
      val gddMock = mock[GoogleDiscoveryDocument]
      gddMock.getSchemas returns null
      val method = new SwaggerToGDD().operationToGDD(operation, pathValue, httpMethod, gddMock)
      val responseRef = method.getResponse.get$ref
      responseRef must beEqualTo(s"${method.getId}Response") and (there was one(gddMock).setSchemas(any))
    }
  }
  def noResponse = {
    forAll(genOperation(), genResponse(), arbitrary[String], arbitrary[String]) { (operation, response, pathValue, httpMethod) =>
      val responses = operation.getResponses.asScala.filterKeys(k => !(k.startsWith("2") || k == "default")) + ("400" -> response)
      operation.setResponses(responses.asJava)
      val method = new SwaggerToGDD().operationToGDD(operation, pathValue, httpMethod, mock[GoogleDiscoveryDocument])
      method.getResponse must beNull
    }
  }
  def request = {
    forAll(genOperation(), Gen.oneOf(genBodyParameter()(), mapOf(Gen.zip(arbitrary[String], genBodyParameter()())).flatMap(genRefParameter).map { p => p.setIn("body"); p }),
        arbitrary[String], arbitrary[String]) { (operation, parameter, pathValue, httpMethod) =>
      val parameters = Option(operation.getParameters).map(_.asScala.toList) match {
        case Some(params) => parameter :: params.filter("body" != _.getIn)
        case None => parameter :: List.empty[SwaggerParameter]
      }
      operation.setParameters(parameters.asJava)
      val swaggerToGDD = new SwaggerToGDD()
      val method = swaggerToGDD.operationToGDD(operation, pathValue, httpMethod, mock[GoogleDiscoveryDocument])
      val requestRef = method.getRequest.get$ref
      requestRef must beEqualTo(parameter.getName) or (requestRef must beEqualTo(s"${method.getId}${swaggerToGDD.parameterToGDD(parameter).getId}Request"))
    }
  }
  def requestNotRef = {
    forAll(genOperation(), genBodyParameter()(), arbitrary[String], arbitrary[String]) { (operation, parameter, pathValue, httpMethod) =>
      val parameters = Option(operation.getParameters).map(_.asScala.toList) match {
        case Some(params) => parameter :: params.filter("body" != _.getIn)
        case None => parameter :: List.empty[SwaggerParameter]
      }
      operation.setParameters(parameters.asJava)
      operation.setResponses(null) // avoid interaction with mock call
      val gddMock = mock[GoogleDiscoveryDocument]
      gddMock.getSchemas returns null
      val swaggerToGDD = new SwaggerToGDD()
      val method = swaggerToGDD.operationToGDD(operation, pathValue, httpMethod, gddMock)
      val requestRef = method.getRequest.get$ref
      requestRef must beEqualTo(s"${method.getId}${swaggerToGDD.parameterToGDD(parameter).getId}Request") and (there was one(gddMock).setSchemas(any))
    }
  }
  def noRequest = {
    forAll(genOperation(), arbitrary[String], arbitrary[String]) { (operation, pathValue, httpMethod) =>
      val parameters = Option(operation.getParameters).map(_.asScala.toList.filter(p => "body" != p.getIn)).getOrElse(List.empty[SwaggerParameter])
      operation.setParameters(parameters.asJava)
      val method = new SwaggerToGDD().operationToGDD(operation, pathValue, httpMethod, mock[GoogleDiscoveryDocument])
      method.getRequest must beNull
    }
  }

}
