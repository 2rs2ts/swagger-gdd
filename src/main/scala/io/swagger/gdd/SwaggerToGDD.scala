package io.swagger.gdd

import scala.collection.JavaConverters._

import io.swagger.gdd.Method.SchemaRef
import io.swagger.models._
import io.swagger.models.parameters.{AbstractSerializableParameter, BodyParameter, RefParameter}
import io.swagger.models.properties._

object SwaggerToGDD {

  // todo: allow passing Operation => Operation to transform based on tags
  // todo: vendorExtensions
  // todo: header, form, cookie params are all things that Swagger supports but GDD does not. only path and query.

  def swaggerToGDD(swagger: Swagger): GoogleDiscoveryDocument = {
    val gdd = new GoogleDiscoveryDocument

    // basics: basePath -> servicePath, schemes + host -> rootUrl
    gdd.servicePath = swagger.getBasePath
    val rootUrlOpt = for {
      schemes <- Option(swagger.getSchemes)
      headScheme <- schemes.asScala.headOption
      host <- Option(swagger.getHost)
    } yield s"$headScheme://$host".toLowerCase
    gdd.rootUrl = rootUrlOpt.orNull

    // info -> title, name, version, revision, id, description
    Option(swagger.getInfo).foreach { info =>
      gdd.title = info.getTitle
      // a naive but good enough assumption: lowercase the title and replace whitespace with hyphens
      gdd.name = Option(info.getTitle).map(_.toLowerCase.split("\\s").mkString("-")).orNull
      // split version into a major version and a minor version
      Option(info.getVersion).map(_.split('.').toList).foreach { lst =>
        lst.headOption.foreach { head =>
          gdd.version = s"v$head"
          gdd.revision = lst.tail.headOption.orNull
        }
      }
      gdd.id = s"${gdd.name}:${gdd.version}"
      gdd.description = info.getDescription
    }

    // external docs -> documentationLink
    gdd.documentationLink = Option(swagger.getExternalDocs).map(_.getUrl).orNull

    // definitions -> schemas
    gdd.schemas = Option(swagger.getDefinitions).map(_.asScala.map { case (key, model) =>
      key -> schemaObjectToGDD(key, model)
    }.asJava).orNull

    // paths -> methods, resources
    Option(swagger.getPaths).map(_.asScala).foreach { paths =>
      // group paths by base. does not include the leading /
      val pathsByBase = paths.foldLeft(Map.empty[String, Map[String, Path]]) { case (curr, (key, path)) =>
        val base = key.stripPrefix("/").split('/').toList.headOption.getOrElse("")
        curr + (base -> (curr.getOrElse(base, Map.empty[String, Path]) + (key -> path)))
      }
      gdd.resources = pathsByBase.foldLeft(Map.empty[String, Resource]) {
        case (curr, ("", ps)) => // root level methods go under methods, not resources
          gdd.methods = pathObjectsToGDD(ps).methods
          curr
        case (curr, (basePath, ps)) =>
          curr + (basePath.stripPrefix("/") -> pathObjectsToGDD(ps))
      }.asJava
    }

    gdd
  }

  def schemaObjectToGDD(key: String, model: Model): Schema = {
    val schema = new Schema
    schema.id = key
    schema.description = model.getDescription
    changeSchemaUsingModel(schema, model)
    schema
  }

  /**
   *
   * @param paths Paths grouped by key (path value)
   * @return the Resource with all of its methods
   */
  def pathObjectsToGDD(paths: Map[String, Path]): Resource = {
    val resource = new Resource
    resource.methods = paths.foldLeft(Map.empty[String, Method]) {
      case (curr, (pathValue, path)) => curr ++ pathObjectToGDD(pathValue, path)
    }.asJava
    resource
  }

  /**
   * Add the operations of a Path object to a Resource as Methods keyed by id
   * @param pathValue the full path value (including path parameters)
   * @param path the Path object
   * @return
   */
  def pathObjectToGDD(pathValue: String, path: Path): Map[String, Method] = {
    val methods = Option(path.getGet).map(operationToGDD(_, pathValue, "GET")) ::
      Option(path.getPut).map(operationToGDD(_, pathValue, "PUT")) ::
      Option(path.getPost).map(operationToGDD(_, pathValue, "POST")) ::
      Option(path.getPatch).map(operationToGDD(_, pathValue, "PATCH")) ::
      Option(path.getDelete).map(operationToGDD(_, pathValue, "DELETE")) ::
      Option(path.getHead).map(operationToGDD(_, pathValue, "HEAD")) ::
      Option(path.getOptions).map(operationToGDD(_, pathValue, "OPTIONS")) :: List.empty[Option[Method]]
    methods.foldLeft(Map.empty[String, Method]) {
      case (curr, Some(m)) => curr + (m.id -> m)
      case (curr, None) => curr
    }
  }

  /**
   *
   * @param op the Operation on the Path
   * @param pathValue the full path value for the operation (including path parameters)
   * @param httpMethod the HTTP method for the operation
   * @return
   */
  def operationToGDD(op: Operation, pathValue: String, httpMethod: String): Method = {
    val method = new Method
    method.id = op.getOperationId
    method.description = op.getSummary
    method.httpMethod = httpMethod
    method.path = pathValue
    Option(op.getResponses).map(_.asScala.toMap).flatMap(findMethodResponse).map(_.getSchema).foreach {
      case property: RefProperty =>
        method.response = new SchemaRef(property.getSimpleRef)
      case property => // todo non-ref responses have to get added as schemas since GDD doesn't allow non-ref responses
    }
    Option(op.getParameters).map(_.asScala.toList).foreach { parameters =>
      method.parameters = parameters.foldLeft(Map.empty[String, Parameter]) {
        case (curr, param) if "body".equals(param.getIn) =>
          method.request = new SchemaRef(parameterToGDD(param).$ref) // todo make sure this is in schemas
          curr
        case (curr, param) =>
          curr + (param.getName -> parameterToGDD(param))
      }.asJava
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
   * Turn a Swagger parameter into a GDD Parameter.
   * @param parameter the parameter to transform
   * @return
   */
  def parameterToGDD(parameter: io.swagger.models.parameters.Parameter): Parameter = {
    val param = new Parameter
    param.id = parameter.getName
    param.description = parameter.getDescription
    param.required = parameter.getRequired
    param.pattern = parameter.getPattern
    parameter match {
      case p: RefParameter =>
        param.$ref = p.getSimpleRef
      case p: AbstractSerializableParameter[_] =>
        param.`type` = p.getType
        param.format = p.getFormat
        param._enum = p.getEnum
        param.location = p.getIn // GDD doesn't care about this for body params, but we only take the ref from it anyway
        param.minimum = Option(p.getMinimum).map(_.toString).orNull
        param.maximum = Option(p.getMaximum).map(_.toString).orNull
        param.items = Option(p.getItems).map(propertyToGDD).orNull
        Option(p.getCollectionFormat).foreach {
          case "multi" => param.repeated = true
          case _ =>
        }
      case p: BodyParameter =>
        Option(p.getSchema).foreach(changeSchemaUsingModel(param, _))
    }
    param
  }

  /**
   * Turn a Property into a Schema.
   *
   * Note: required is intentionally omitted since the field is marked with JsonIgnore in swagger-models.
   *
   * @param property the property to convert
   * @return
   */
  def propertyToGDD(property: Property): Schema = {
    val schema = new Schema
    schema.id = property.getName
    schema.description = property.getDescription // todo what about title?
    schema.required = property.getRequired
    property match {
      case prop: RefProperty =>
        schema.$ref = prop.getSimpleRef
      case prop: ArrayProperty =>
        schema.`type` = "array"
        schema.items = Option(prop.getItems).map(propertyToGDD).orNull
      case prop: UUIDProperty =>
        schema.`type` = "string"
        schema.pattern = prop.getPattern
      case prop: EmailProperty =>
        schema.`type` = "string"
        schema.pattern = prop.getPattern
        schema._default = prop.getDefault
      case prop: StringProperty =>
        schema.`type` = "string"
        Option(prop.getFormat).foreach {
          case "byte" => schema.format = "byte"
          case _ =>
        }
        schema.pattern = prop.getPattern
        schema._enum = prop.getEnum
        schema._default = prop.getDefault
      case prop: AbstractNumericProperty =>
        schema.`type` = prop.getType
        schema.format = prop.getFormat
        schema.minimum = Option(prop.getMinimum).map(_.toString).orNull
        schema.maximum = Option(prop.getMaximum).map(_.toString).orNull
        schema._default = prop match {
          case p: IntegerProperty => Option(p.getDefault).map(_.toString).orNull
          case p: LongProperty => Option(p.getDefault).map(_.toString).orNull
          case p: FloatProperty => Option(p.getDefault).map(_.toString).orNull
          case p: DoubleProperty => Option(p.getDefault).map(_.toString).orNull
          case p: DecimalProperty => null
        }
      case prop =>
        schema.`type` = prop.getType
        schema.format = prop.getFormat
      // todo FileProperty
    }
    schema
  }

  /**
   * <i>Side effecting</i>. Changes the schema/parameter based on the type of the model.
   * @param schema the Schema or Parameter to modify
   * @param model the Model to use for manipulating the schema
   */
  def changeSchemaUsingModel(schema: AbstractSchema, model: Model): Unit = model match {
    case model: RefModel =>
      schema.`type` = "object"
      schema.$ref = model.getSimpleRef
    case model: ArrayModel =>
      schema.`type` = "array"
      schema.items = Option(model.getItems).map(propertyToGDD).orNull
    case model: ModelImpl =>
      schema.`type` = model.getType
      schema.format = model.getFormat
      schema._default = model.getDefaultValue
      schema.properties = Option(model.getProperties).map(_.asScala.map {
        case (propKey, prop) => propKey -> propertyToGDD(prop)
      }.toMap.asJava).orNull
      schema.additionalProperties = Option(model.getAdditionalProperties).map(propertyToGDD).orNull
    case model: ComposedModel =>
      // todo
  }


}
