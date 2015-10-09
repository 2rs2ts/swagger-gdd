package io.swagger.gdd

import io.swagger.gdd.SwaggerGenerators._
import io.swagger.gdd.models.factory.GDDModelFactory
import io.swagger.gdd.models.{GoogleDiscoveryDocument, Method}
import io.swagger.models.{Operation, Path}
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalacheck.Prop.{apply => _, _}
import org.specs2.mock.Mockito
import org.specs2.specification.core.SpecStructure
import org.specs2.{ScalaCheck, Specification}

/**
 * Tests [[io.swagger.gdd.SwaggerToGDD#pathObjectToGDD SwaggerToGDD.pathObjectTogDD]] and its counterpart
 * [[io.swagger.gdd.SwaggerToGDD#pathObjectsToGDD pathObjectsToGDD]].
 */
class PathObjectToGDDSpecs extends Specification with ScalaCheck with Mockito with TestHelpers {
  override def is: SpecStructure = s2"""
  SwaggerToGDD.pathObjectToGDD converts the Swagger Path object to its GDD counterpart, a map of Method ids to Methods.

  It should:
    Convert the Path's "get" Operation to a Method with httpMethod of "GET"           ${PathObjectToGDD.get}
    Convert the Path's "put" Operation to a Mentod with httpMethod of "PUT"           ${PathObjectToGDD.put}
    Convert the Path's "post" Operation to a Method with httpMethod of "POST"         ${PathObjectToGDD.post}
    Convert the Path's "patch" Operation to a Method with httpMethod of "PATCH"       ${PathObjectToGDD.patch}
    Convert the Path's "delete" Operation to a Method with httpMethod of "DELETE"     ${PathObjectToGDD.delete}
    Convert the Path's "head" Operation to a Method with httpMethod of "HEAD"         ${PathObjectToGDD.head}
    Convert the Path's "options" Operation to a Method with httpMethod of "OPTIONS"   ${PathObjectToGDD.options}
    Key by Method id                                                                  ${PathObjectToGDD.keyById}

  SwaggerToGDD.pathObjectsToGDD converts the Swagger Paths object to its GDD counterpart, a Resource.

  It should:
    Have a Method for each Operation in each Path, for each Path                      ${PathObjectsToGDD.convertAllOperations}
    Key the Methods in the methods field by Method id                                 ${PathObjectsToGDD.keyByMethodId}
  """

  def genPathWithAtLeastOneOperation = {
    genPath().suchThat { case (_, path) =>
      List(path.getGet, path.getPut, path.getPost, path.getPatch,
        path.getDelete, path.getHead, path.getOptions).flatMap(Option(_)).nonEmpty
    }
  }

  object PathObjectToGDD {
    def testCallAndCorrectHttpMethod(o: Path => Operation, httpMethod: String) = {
      forAll(genPath().suchThat { case (_, p) => Option(o(p)).isDefined }) { case (pathValue, path) =>
        val swaggerToGDD = spy(new SwaggerToGDD())
        val mockGDD = mock[GoogleDiscoveryDocument]
        swaggerToGDD.pathObjectToGDD(pathValue, path, mockGDD)
        there was one(swaggerToGDD).operationToGDD(o(path), pathValue, httpMethod, mockGDD)
      }
    }
    def get = testCallAndCorrectHttpMethod(_.getGet, "GET")
    def put = testCallAndCorrectHttpMethod(_.getPut, "PUT")
    def post = testCallAndCorrectHttpMethod(_.getPost, "POST")
    def patch = testCallAndCorrectHttpMethod(_.getPatch, "PATCH")
    def delete = testCallAndCorrectHttpMethod(_.getDelete, "DELETE")
    def head = testCallAndCorrectHttpMethod(_.getHead, "HEAD")
    def options = testCallAndCorrectHttpMethod(_.getOptions, "OPTIONS")

    def keyById = {
      forAll(genPathWithAtLeastOneOperation, arbitrary[String]) { case ((pathValue, path), id) =>
        val mockMethod = mock[Method]
        mockMethod.getId returns id
        val swaggerToGDD = new SwaggerToGDD(new GDDModelFactory() {
          override def newMethod() = mockMethod
        })
        val mockGDD = mock[GoogleDiscoveryDocument]
        val methods = swaggerToGDD.pathObjectToGDD(pathValue, path, mockGDD)
        methods.get(id) must beSome(mockMethod)
      }
    }
  }

  object PathObjectsToGDD {
    def convertAllOperations = {
      forAll(Gen.mapOf(genPath())) { paths =>
        // make all ids unique, otherwise it's hard to test this; suchThat will just get exhausted otherwise
        var operationCount = 0
        paths.foreach { case (_, path) =>
          List(path.getGet, path.getPut, path.getPost, path.getPatch,
            path.getDelete, path.getHead, path.getOptions).flatMap(Option(_)).foreach { op =>
            operationCount += 1
            op.setOperationId(s"${op.getOperationId}$operationCount")
          }
        }
        val mockGDD = mock[GoogleDiscoveryDocument]
        val resource = new SwaggerToGDD().pathObjectsToGDD(paths, mockGDD)
        resource.getMethods.size must beEqualTo(operationCount)
      }
    }
    def keyByMethodId = {
      forAll(Gen.nonEmptyMap(genPathWithAtLeastOneOperation), arbitrary[String]) { (paths, id) =>
        val mockMethod = mock[Method]
        mockMethod.getId returns id
        val swaggerToGDD = new SwaggerToGDD(new GDDModelFactory() {
          override def newMethod() = mockMethod
        })
        val mockGDD = mock[GoogleDiscoveryDocument]
        val resource = swaggerToGDD.pathObjectsToGDD(paths, mockGDD)
        resource.getMethods.get(id) must beEqualTo(mockMethod)
      }
    }
  }

}
