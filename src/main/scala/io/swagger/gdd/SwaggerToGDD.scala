package io.swagger.gdd

import scala.collection.JavaConverters._

import io.swagger.gdd.Method.SchemaRef
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
 * @param gddFactory factory method for making a GoogleDiscoveryDocument, useful for injecting your own implementation
 * @param resourceFactory factory method for making a Resource, useful for injecting your own implementation
 * @param methodFactory factory method for making a Method, useful for injecting your own implementation
 * @param schemaFactory factory method for making a Schema, useful for injecting your own implementation
 * @param parameterFactory factory method for making a Parameter, useful for injecting your own implementation
 */
class SwaggerToGDD(
                    val gddFactory: () => GoogleDiscoveryDocument = () => new GoogleDiscoveryDocument,
                    val resourceFactory: () => Resource = () => new Resource,
                    val methodFactory: () => Method = () => new Method,
                    val schemaFactory: () => Schema = () => new Schema,
                    val parameterFactory: () => Parameter = () => new Parameter) {

  // todo: header, form, cookie params are all things that Swagger supports but GDD does not. only path and query.

  /**
   * Create a new GoogleDiscoveryDocument from a Swagger instance. The Swagger will not be modified.
   * @param swagger a model of a swagger document
   * @return the Swagger converted into a GoogleDiscoveryDocument
   */
  def swaggerToGDD(swagger: Swagger): GoogleDiscoveryDocument = {
    val gdd = gddFactory()

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
          gdd.methods = pathObjectsToGDD(ps, gdd).methods
          curr
        case (curr, (basePath, ps)) =>
          curr + (basePath.stripPrefix("/") -> pathObjectsToGDD(ps, gdd))
      }.asJava
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
    val schema = schemaFactory()
    schema.id = key
    schema.description = model.getDescription
    changeSchemaUsingModel(schema, model)
    schema
  }

  /**
   * Turn Path Objects into a GDD Resource. The Path Objects will not be changed.
   * @param paths Paths grouped by key (path value)
   * @return the Resource with all of its methods
   */
  def pathObjectsToGDD(paths: Map[String, Path], gdd: GoogleDiscoveryDocument): Resource = {
    val resource = resourceFactory()
    resource.methods = paths.foldLeft(Map.empty[String, Method]) {
      case (curr, (pathValue, path)) => curr ++ pathObjectToGDD(pathValue, path, gdd)
    }.asJava
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
      case (curr, Some(m)) => curr + (m.id -> m)
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
    val method = methodFactory()
    method.id = op.getOperationId
    method.description = op.getSummary
    method.httpMethod = httpMethod
    method.path = pathValue
    Option(op.getResponses).map(_.asScala.toMap).flatMap(findMethodResponse).flatMap(r => Option(r.getSchema)).foreach {
      case property: RefProperty =>
        method.response = new SchemaRef(property.getSimpleRef)
      case property =>
        // non-ref responses have to get added as schemas since GDD doesn't allow non-ref responses
        val prop = propertyToGDD(property)
        prop.id = s"${method.id}Response"
        prop.required = null // required seems awkward here
        gdd.schemas.put(prop.id, prop)
        method.response = new SchemaRef(prop.id)
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
   * Turn a Swagger Parameter into a GDD Parameter. The (Swagger) Parameter will not be changed.
   * @param parameter the Parameter to transform
   * @return the converted Parameter
   */
  def parameterToGDD(parameter: io.swagger.models.parameters.Parameter): Parameter = {
    val param = parameterFactory()
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
        param.items = Option(p.getItems).map(propertyToGDD).map { prop =>
          prop.required = null  // required seems awkward here
          prop
        }.orNull
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
   * Turn a Swagger [[Property]] into a GDD [[Schema]]. The `Property` will not be changed.
   *
   * If any of the `Property`'s fields are `null`, the resulting `Schema` will not have its related fields populated.
   * All language below speaks as though those fields will not be `null`.
   *
   * The resulting Schema will have its `id`, `description`, and `required` fields populated regardless of the subclass
   * of `Property`. Besides [[RefProperty]], everything else will result in the `type` field being populated as well.
   * `RefProperty` will result in the `$ref` field being populated.
   *
   * For [[StringProperty]], [[EmailProperty]], and [[UUIDProperty]], the `pattern` field will be populated.
   * `StringProperty` and `EmailProperty` will result in the `_default` field being populated. `StringProperty` will
   * also result in the `_enum` field being populated, and if the `Property`'s `format` is `"byte"`, then so will the
   * `Schema`'s `format` be.
   *
   * [[DateProperty]], [[DateTimeProperty]], and [[ByteArrayProperty]] will be treated as a `StringProperty` with
   * `format` equal to `"date"`, `"date-time"`, and `"byte"` respectively, but none will result in the `pattern` field
   * being populated.
   *
   * For any [[AbstractNumericProperty]], the `format`, `minimum`, and `maximum` fields will all be populated.
   * [[IntegerProperty]], [[LongProperty]], [[DoubleProperty]], and [[FloatProperty]] will result in the `_default`
   * field being populated.
   *
   * For [[ArrayProperty]], the `items` field will be populated.
   *
   * For [[MapProperty]], the `additionalProperties` field will be populated.
   *
   * For [[ObjectProperty]], the `properties` field will be populated.
   *
   * @todo Implement something sane for [[FileProperty]].
   *
   * <table>
   *  <tr><th>`Schema` field</th><th>`Property` field which determines it</th></tr>
   *  <tr><td>`id`</td><td>`name`</td></tr>
   *  <tr><td>`description`</td><td>`description`</td></tr>
   *  <tr><td>`required`</td><td>`required`</td></tr>
   *  <tr><td>`type`</td><td>`type` (or manually set)</td></tr>
   *  <tr><td>`format`</td><td>`format` (or manually set)</td></tr>
   *  <tr><td>`pattern`</td><td>`pattern`</td></tr>
   *  <tr><td>`_default`</td><td>`default`</td></tr>
   *  <tr><td>`_enum`</td><td>`enum`</td></tr>
   *  <tr><td>`minimum`</td><td>`minimum`</td></tr>
   *  <tr><td>`maximum`</td><td>`maximum`</td></tr>
   *  <tr><td>`items`</td><td>`items`</td></tr>
   *  <tr><td>`properties`</td><td>`properties`</td></tr>
   *  <tr><td>`additionalProperties`</td><td>`additionalProperties`</td></tr>
   *  <tr><td>`$ref`</td><td>`$ref` (simple ref)</td></tr>
   * </table>
   *
   * @param property the Property to convert
   * @return the converted Schema
   */
  def propertyToGDD(property: Property): Schema = {
    val schema = schemaFactory()
    schema.id = property.getName
    schema.description = property.getDescription // todo what about title?
    schema.required = property.getRequired
    property match {
      case prop: RefProperty =>
        schema.$ref = prop.getSimpleRef
      case prop: BooleanProperty =>
        schema.`type` = "boolean"
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
      case prop: DateProperty =>
        schema.`type` = "string"
        schema.format = "date"
      case prop: DateTimeProperty =>
        schema.`type` = "string"
        schema.format = "date-time"
      case prop: ByteArrayProperty =>
        schema.`type` = "string"
        schema.format = "byte"
      case prop: LongProperty =>
        schema.`type` = "string"
        schema.format = "int64"
        schema.minimum = Option(prop.getMinimum).map(_.toString).orNull
        schema.maximum = Option(prop.getMaximum).map(_.toString).orNull
        schema._default = Option(prop.getDefault).map(_.toString).orNull
      case prop: AbstractNumericProperty =>
        schema.`type` = prop.getType
        schema.format = prop.getFormat
        schema.minimum = Option(prop.getMinimum).map(_.toString).orNull
        schema.maximum = Option(prop.getMaximum).map(_.toString).orNull
        schema._default = prop match {
          case p: IntegerProperty => Option(p.getDefault).map(_.toString).orNull
          case p: FloatProperty => Option(p.getDefault).map(_.toString).orNull
          case p: DoubleProperty => Option(p.getDefault).map(_.toString).orNull
          case _ => null
        }
      case prop: ArrayProperty =>
        schema.`type` = "array"
        schema.items = Option(prop.getItems).map(propertyToGDD).map { p =>
          p.required = null // required seems awkward here
          p
        }.orNull
      case prop: MapProperty =>
        schema.`type` = "object"
        schema.additionalProperties = Option(prop.getAdditionalProperties).map(propertyToGDD).orNull
      case prop: ObjectProperty =>
        schema.`type` = "object"
        schema.properties = Option(prop.getProperties).map(_.asScala.mapValues(propertyToGDD)).getOrElse(Map.empty).asJava
      case prop: FileProperty =>
        // todo figure out a sane way to treat these since GDD has no concept of formData.
        schema.`type` = prop.getType
        schema.format = prop.getFormat
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

object SwaggerToGDD {

  /**
   * Create a new GoogleDiscoveryDocument from a Swagger instance. The Swagger will not be modified.
   * @param swagger a model of a swagger document
   * @return the Swagger converted into a GoogleDiscoveryDocument
   */
  def swaggerToGDD(swagger: Swagger): GoogleDiscoveryDocument = {
    new SwaggerToGDD().swaggerToGDD(swagger)
  }

  def propertyToGDD(property: Property): Schema = {
    new SwaggerToGDD().propertyToGDD(property)
  }
}
