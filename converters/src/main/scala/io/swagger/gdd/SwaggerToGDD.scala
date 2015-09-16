package io.swagger.gdd

import scala.collection.JavaConverters._

import io.swagger.gdd.models._
import io.swagger.gdd.models.factory.GDDModelFactory
import io.swagger.models._
import io.swagger.models.parameters.{AbstractSerializableParameter, BodyParameter, RefParameter}
import io.swagger.models.properties._

/**
 * Converts Swagger models to GDD equivalents.
 *
 * If you want to add customizations to your GDD output, subclass the GDD models and pass factory methods for them into
 * this class. Then you can either change your custom models after the fact, or you can subclass this class and override
 * the behavior, preferably by calling super and then making the changes you need afterward.
 *
 * @param modelFactory factory for creating GDD models. For custom implementations, subclass [[GDDModelFactory]] to
 *                     inject different models.
 */
class SwaggerToGDD(val modelFactory: GDDModelFactory = new GDDModelFactory) {

  // todo: header, form, cookie params are all things that Swagger supports but GDD does not. only path and query.

  /**
   * Create a new GoogleDiscoveryDocument from a Swagger instance. The Swagger will not be modified.
   * @param swagger a model of a swagger document
   * @return the Swagger converted into a GoogleDiscoveryDocument
   */
  def swaggerToGDD(swagger: Swagger): GoogleDiscoveryDocument = {
    val gdd = modelFactory.newGoogleDiscoveryDocument()

    // basics: basePath -> servicePath, schemes + host -> rootUrl
    gdd.setServicePath(swagger.getBasePath)
    val rootUrl = for {
      schemes <- Option(swagger.getSchemes)
      headScheme <- schemes.asScala.headOption
      host <- Option(swagger.getHost)
    } yield s"$headScheme://$host".toLowerCase
    rootUrl.foreach(gdd.setRootUrl)

    // info -> title, name, version, revision, id, description
    Option(swagger.getInfo).foreach { info =>
      gdd.setTitle(info.getTitle)
      // a naive but good enough assumption: lowercase the title and replace whitespace with hyphens
      Option(info.getTitle).map(_.toLowerCase.split("\\s").mkString("-")).foreach(gdd.setName)
      // split version into a major version and a minor version
      Option(info.getVersion).map(_.split('.').toList).foreach { lst =>
        lst.headOption.foreach { head =>
          gdd.setVersion(s"v$head")
          lst.tail.headOption.foreach(gdd.setRevision)
        }
      }
      gdd.setId(s"${gdd.getName}:${gdd.getVersion}")
      gdd.setDescription(info.getDescription)
    }

    // external docs -> documentationLink
    Option(swagger.getExternalDocs).map(_.getUrl).foreach(gdd.setDocumentationLink)

    // definitions -> schemas
    Option(swagger.getDefinitions).map(_.asScala.map { case (key, model) =>
      key -> schemaObjectToGDD(key, model)
    }.asJava).foreach(gdd.setSchemas)

    // paths -> methods, resources
    Option(swagger.getPaths).map(_.asScala).foreach { paths =>
      // group paths by base. does not include the leading /
      val pathsByBase = paths.foldLeft(Map.empty[String, Map[String, Path]]) { case (curr, (key, path)) =>
        val base = key.stripPrefix("/").split('/').toList.headOption.getOrElse("")
        curr + (base -> (curr.getOrElse(base, Map.empty[String, Path]) + (key -> path)))
      }
      gdd.setResources(pathsByBase.foldLeft(Map.empty[String, Resource]) {
        case (curr, ("", ps)) => // root level methods go under methods, not resources
          gdd.setMethods(pathObjectsToGDD(ps, gdd).getMethods)
          curr
        case (curr, (basePath, ps)) =>
          curr + (basePath.stripPrefix("/") -> pathObjectsToGDD(ps, gdd))
      }.asJava)
    }

    gdd
  }

  /**
   * Change the Schema Object (Model) into a GDD Schema. The Model will not be changed.
   * @param key the key that the Schema Object was stored under, will become the id of the GDD Schema
   * @param model the Schema Object that will be converted into a GDD Schema
   * @return the converted Schema
   */
  def schemaObjectToGDD(key: String, model: Model): Schema = {
    val schema = modelFactory.newSchema()
    schema.setId(key)
    schema.setDescription(model.getDescription)
    changeSchemaUsingModel(schema, model)
    schema
  }

  /**
   * Turn Path Objects into a GDD Resource. The Path Objects will not be changed.
   * @param paths Paths grouped by key (path value)
   * @return the Resource with all of its methods
   */
  def pathObjectsToGDD(paths: Map[String, Path], gdd: GoogleDiscoveryDocument): Resource = {
    val resource = modelFactory.newResource()
    resource.setMethods(paths.foldLeft(Map.empty[String, Method]) {
      case (curr, (pathValue, path)) => curr ++ pathObjectToGDD(pathValue, path, gdd)
    }.asJava)
    resource
  }

  /**
   * Convert the Operations of a Path Object to Methods keyed by id, to be added to a Resource. The Path Object will not
   * be changed.
   * @param pathValue the full path value (including path parameters)
   * @param path the Path object
   * @param gdd the original GoogleDiscoveryDocument, which may need to have its schemas changed based on the Operations
   *            of the Path Object
   * @return a map of Method id to Method
   */
  def pathObjectToGDD(pathValue: String, path: Path, gdd: GoogleDiscoveryDocument): Map[String, Method] = {
    val methods = Option(path.getGet).map(operationToGDD(_, pathValue, "GET", gdd)) ::
      Option(path.getPut).map(operationToGDD(_, pathValue, "PUT", gdd)) ::
      Option(path.getPost).map(operationToGDD(_, pathValue, "POST", gdd)) ::
      Option(path.getPatch).map(operationToGDD(_, pathValue, "PATCH", gdd)) ::
      Option(path.getDelete).map(operationToGDD(_, pathValue, "DELETE", gdd)) ::
      Option(path.getHead).map(operationToGDD(_, pathValue, "HEAD", gdd)) ::
      Option(path.getOptions).map(operationToGDD(_, pathValue, "OPTIONS", gdd)) :: List.empty[Option[Method]]
    methods.foldLeft(Map.empty[String, Method]) {
      case (curr, Some(m)) => curr + (m.getId -> m)
      case (curr, None) => curr
    }
  }

  /**
   * Convert an Operation to a GDD Method. The Operation will not be changed.
   * @param op the Operation on the Path
   * @param pathValue the full path value for the operation (including path parameters)
   * @param httpMethod the HTTP method for the operation
   * @param gdd the original GoogleDiscoveryDocument, which may have its schemas added to if an Operation's response is
   *            not a reference type
   * @return the converted Method
   */
  def operationToGDD(op: Operation, pathValue: String, httpMethod: String, gdd: GoogleDiscoveryDocument): Method = {
    val method = modelFactory.newMethod()
    method.setId(op.getOperationId)
    method.setDescription(op.getSummary)
    method.setHttpMethod(httpMethod)
    method.setPath(pathValue)
    Option(op.getResponses).map(_.asScala.toMap).flatMap(findMethodResponse).flatMap(r => Option(r.getSchema)).foreach {
      case property: RefProperty =>
        method.setResponse(modelFactory.newSchemaRef(property.getSimpleRef))
      case property =>
        // non-ref responses have to get added as schemas since GDD doesn't allow non-ref responses
        val prop = propertyToGDD(property)
        prop.setId(s"${method.getId}Response")
        prop.setRequired(null) // required seems awkward here
        gdd.getSchemas.put(prop.getId, prop)
        method.setResponse(modelFactory.newSchemaRef(prop.getId))
    }
    Option(op.getParameters).map(_.asScala.toList).foreach { parameters =>
      method.setParameters(parameters.foldLeft(Map.empty[String, Parameter]) {
        case (curr, param) if "body".equals(param.getIn) =>
          method.setRequest(modelFactory.newSchemaRef(parameterToGDD(param).get$ref)) // todo make sure this is in schemas
          curr
        case (curr, param) =>
          curr + (param.getName -> parameterToGDD(param))
      }.asJava)
    }
    method
  }

  /**
   * Find the response that best matches the default response. Prefers the smallest 2xx code.
   * @param responses an Operation's responses
   * @return the response with the lowest 2xx code, otherwise the default one
   */
  def findMethodResponse(responses: Map[String, Response]): Option[Response] = {
    implicit val ord = new Ordering[(String, Response)] {
      override def compare(x: (String, Response), y: (String, Response)) = x._1.compareTo(y._1)
    }
    responses.filter { case (code, _) => code.startsWith("2") || code.equals("default") }.toSeq.sorted.headOption.map(_._2)
  }

  /**
   * Turn a Swagger Parameter into a GDD Parameter. The (Swagger) Parameter will not be changed.
   * @param parameter the Parameter to transform
   * @return the converted Parameter
   */
  def parameterToGDD(parameter: io.swagger.models.parameters.Parameter): Parameter = {
    val param = modelFactory.newParameter()
    param.setId(parameter.getName)
    param.setDescription(parameter.getDescription)
    param.setRequired(parameter.getRequired)
    param.setPattern(parameter.getPattern)
    parameter match {
      case p: RefParameter =>
        param.set$ref(p.getSimpleRef)
      case p: AbstractSerializableParameter[_] =>
        param.setType(p.getType)
        param.setFormat(p.getFormat)
        param.setEnum(p.getEnum)
        param.setLocation(p.getIn) // GDD doesn't care about this for body params, but we only take the ref from it anyway
        Option(p.getMinimum).map(_.toString).foreach(param.setMinimum)
        Option(p.getMaximum).map(_.toString).foreach(param.setMaximum)
        Option(p.getItems).map(propertyToGDD).map { prop =>
          prop.setRequired(null)  // required seems awkward here
          prop
        }.foreach(param.setItems)
        Option(p.getCollectionFormat).foreach {
          case "multi" => param.setRepeated(true)
          case _ =>
        }
      case p: BodyParameter =>
        Option(p.getSchema).foreach(changeSchemaUsingModel(param, _))
    }
    param
  }

  /**
   * Turn a Property into a GDD Schema. The Property will not be changed.
   *
   * @param property the Property to convert
   * @return the converted Schema
   */
  def propertyToGDD(property: Property): Schema = {
    val schema = modelFactory.newSchema()
    schema.setId(property.getName)
    schema.setDescription(property.getDescription) // todo what about title?
    schema.setRequired(property.getRequired)
    property match {
      case prop: RefProperty =>
        schema.set$ref(prop.getSimpleRef)
      case prop: ArrayProperty =>
        schema.setType("array")
        Option(prop.getItems).map(propertyToGDD).map { p =>
          p.setRequired(null) // required seems awkward here
          p
        }.foreach(schema.setItems)
      case prop: UUIDProperty =>
        schema.setType("string")
        schema.setPattern(prop.getPattern)
      case prop: EmailProperty =>
        schema.setType("string")
        schema.setPattern(prop.getPattern)
        schema.setDefault(prop.getDefault)
      case prop: StringProperty =>
        schema.setType("string")
        Option(prop.getFormat).foreach {
          case "byte" => schema.setFormat("byte")
          case _ =>
        }
        schema.setPattern(prop.getPattern)
        schema.setEnum(prop.getEnum)
        schema.setDefault(prop.getDefault)
      case prop: AbstractNumericProperty =>
        schema.setType(prop.getType)
        schema.setFormat(prop.getFormat)
        Option(prop.getMinimum).map(_.toString).foreach(schema.setMinimum)
        Option(prop.getMaximum).map(_.toString).foreach(schema.setMaximum)
        prop match {
          case p: IntegerProperty => Option(p.getDefault).map(_.toString).foreach(schema.setDefault)
          case p: LongProperty => Option(p.getDefault).map(_.toString).foreach(schema.setDefault)
          case p: FloatProperty => Option(p.getDefault).map(_.toString).foreach(schema.setDefault)
          case p: DoubleProperty => Option(p.getDefault).map(_.toString).foreach(schema.setDefault)
          case _ =>
        }
      case prop: ObjectProperty =>
        schema.setType("object")
        // todo add additonalProperties when it's supported in swagger-models
      case prop =>
        schema.setType(prop.getType)
        schema.setFormat(prop.getFormat)
      // todo FileProperty - the problem is that GDD has no concept of formData
    }
    schema
  }

  /**
   * <i>Side effecting</i>. Changes the Schema/Parameter based on the type of the Model.
   * @param schema the Schema or Parameter to modify
   * @param model the Model to use for manipulating the schema
   */
  def changeSchemaUsingModel(schema: AbstractSchema, model: Model): Unit = model match {
    case model: RefModel =>
      schema.setType("object")
      schema.set$ref(model.getSimpleRef)
    case model: ArrayModel =>
      schema.setType("array")
      Option(model.getItems).map(propertyToGDD).foreach(schema.setItems)
    case model: ModelImpl =>
      schema.setType(model.getType)
      schema.setFormat(model.getFormat)
      schema.setDefault(model.getDefaultValue)
      Option(model.getProperties).map(_.asScala.map {
        case (propKey, prop) => propKey -> propertyToGDD(prop)
      }.toMap.asJava).foreach(schema.setProperties)
      Option(model.getAdditionalProperties).map(propertyToGDD).foreach(schema.setAdditionalProperties)
    case model: ComposedModel =>
      // todo
  }


}

object SwaggerToGDD {
  /**
   * Create a new GoogleDiscoveryDocument from a Swagger instance. The Swagger will not be modified.
   * @param swagger a model of a swagger document
   * @return the Swagger converted into a GoogleDiscoveryDocument
   */
  def swaggerToGDD(swagger: Swagger): GoogleDiscoveryDocument = {
    new SwaggerToGDD().swaggerToGDD(swagger)
  }
}
