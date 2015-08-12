package io.swagger.gdd

import java.net.URL
import java.util.UUID

import scala.collection.JavaConverters._
import scala.reflect.ClassTag
import scala.util.Random

import io.swagger.models._
import io.swagger.models.auth._
import io.swagger.models.parameters._
import io.swagger.models.properties._
import org.scalacheck.Arbitrary._
import org.scalacheck.Gen
import org.scalacheck.Gen._

import com.paypal.cascade.common.tests.scalacheck._

/**
 * Scalacheck generators that create Swagger models.
 */
class SwaggerGenerators {

  // todo add xml generator and add it to properties

  /**
   * Generates a complete [[io.swagger.models.Swagger Swagger]]. Everything can be null, except for `swagger` which
   * defaults to 2.0 anyway, `info`, and `paths`.
   */
  def genSwagger: Gen[Swagger] = {
    for {
      info <- genInfo
      host <- arbitrary[Option[(String, Option[String])]]
      basePath <- arbitrary[Option[String]].map(_.map(path => s"/$path"))
      schemes <- option(listOf(oneOf(Scheme.values)).map(_.distinct.asJava))
      consumes <- option(listOf(genMIMEType).map(_.distinct.asJava))  // maybe derive these from the operations instead?
      produces <- option(listOf(genMIMEType).map(_.distinct.asJava))
      definitions <- option(mapOf(genSchema()))
      parameters <- option(mapOf(
        oneOf[parameters.Parameter](
          genQueryParameter, genBodyParameter(definitions), genHeaderParameter, genFormParameter, genCookieParameter
        ).map(p => p.getName -> p)))
      securityDefinitions <- option(mapOf(arbitrary[String].flatMap(k => genSecuritySchemeDefinition.map(k -> _))))
      securityRequirements <- option(someOf(securityDefinitions.map(_.values).getOrElse(Nil)).flatMap(l => sequence(l.map(genSecurityRequirement))))
      paths <- mapOf(genPath(definitions, parameters, securityDefinitions)).map(_.asJava)
      tags <- option(listOf(genTag).map(_.asJava))
      externalDocs <- option(genExternalDocs)
      // todo responses when swagger-models adds it
    } yield {
      val swagger = new Swagger
      swagger.setInfo(info)
      host.map {
        case (hostName, Some(port)) => s"$hostName:$port"
        case (hostName, None) => hostName
      }.foreach(swagger.setHost)
      basePath.foreach(swagger.setBasePath)
      schemes.foreach(swagger.setSchemes)
      consumes.foreach(swagger.setConsumes)
      produces.foreach(swagger.setProduces)
      definitions.map(_.asJava).foreach(swagger.setDefinitions)
      parameters.map(_.asJava).foreach(swagger.setParameters)
      securityDefinitions.map(_.asJava).foreach(swagger.setSecurityDefinitions)
      securityRequirements.foreach(swagger.setSecurity)
      swagger.setPaths(paths)
      tags.foreach(swagger.setTags)
      externalDocs.foreach(swagger.setExternalDocs)
      swagger
    }
  }

  /**
   * Generate the [[io.swagger.models.Info Info]] object of a [[io.swagger.models.Swagger Swagger]] document.
   * `title` and `version` will always be populated, while the other fields may be null.
   */
  def genInfo: Gen[Info] = {
    for {
      title <- arbitrary[String]
      version <- genVersion
      description <- arbitrary[Option[String]]
      termsOfService <- arbitrary[Option[String]]
      contact <- option(genContact)
      vendorExtensions <- mapOf(genVendorExtension)
    } yield {
      val info = new Info
      info.setTitle(title)
      info.setVersion(version)
      description.foreach(info.setDescription)
      termsOfService.foreach(info.setTermsOfService)
      contact.foreach(info.setContact)
      vendorExtensions.foreach { case (key, value) => info.setVendorExtension(key, value) }
      info
    }
  }

  /**
   * Generate the [[io.swagger.models.Contact Contact]] object of an [[io.swagger.models.Info Info]].
   * All fields can be null.
   */
  def genContact: Gen[Contact] = {
    for {
      name <- arbitrary[Option[String]]
      url <- option(genURL.map(_.toString))
      email <- option(genEmail)
    // todo vendorExtensions when swagger-models adds it
    } yield {
      val contact = new Contact
      name.foreach(contact.name)
      url.foreach(contact.url)
      email.foreach(contact.email)
      contact
    }
  }

  /**
   * Generate the [[io.swagger.models.License License] object of an [[io.swagger.models.Info Info]].
   * `name` will not be null, but `url` can be.
   */
  def genLicense: Gen[License] = {
    for {
      name <- arbitrary[String]
      url <- option(genURL.map(_.toString))
    // todo vendorExtensions when swagger-models adds it
    } yield {
      val license = new License
      license.name(name)
      url.foreach(license.url)
      license
    }
  }

  /**
   * Generate a path value and the [[io.swagger.models.Path Path]] for it.
   * @param globalDefinitions [[io.swagger.models.Model Model]]s defined in the [[io.swagger.models.Swagger Swagger]]
   *                         document which can be referred to
   * @param globalParameters [[io.swagger.models.parameters.Parameter Parameter]]s defined in the
   *                        [[io.swagger.models.Swagger Swagger]] document which can be referred to
   * @param securityDefinitions the Security Definitions Object from the [[io.swagger.models.Swagger Swagger]] document
   */
  // todo generate a Paths object with vendor extensions when swagger-models adds it
  def genPath(globalDefinitions: Option[Map[String, Model]] = None,
              globalParameters: Option[Map[String, parameters.Parameter]] = None,
              securityDefinitions: Option[Map[String, SecuritySchemeDefinition]] = None): Gen[(String, Path)] = {
    for {
      pathParams <- listOf(genPathParameter)
      staticPathParts <- listOf(genNonEmptyAlphaNumStr)
      get <- option(genOperation(pathParams, globalDefinitions, globalParameters, securityDefinitions))
      put <- option(genOperation(pathParams, globalDefinitions, globalParameters, securityDefinitions))
      post <- option(genOperation(pathParams, globalDefinitions, globalParameters, securityDefinitions))
      patch <- option(genOperation(pathParams, globalDefinitions, globalParameters, securityDefinitions))
      delete <- option(genOperation(pathParams, globalDefinitions, globalParameters, securityDefinitions))
      head <- option(genOperation(pathParams, globalDefinitions, globalParameters, securityDefinitions).map { op =>
          // head cannot return a request body, so remove produces and make responses have no schema
          op.setProduces(null)
          op.getResponses.asScala.foreach {
            case (_, response) => response.setSchema(null)
          }
          op
        })
      options <- option(genOperation(pathParams, globalDefinitions, globalParameters, securityDefinitions))
      // todo parameters: select only params that apply to all. what about letting the operations refer to these? maybe do this before. limit 1 body parameter.
      // if we generated a body parameter, then we couldn't have the head request. and operations could only override the body parameter.
      //parameters <- someOf(List(get, put, post, patch, delete, head, options).filter(_.isDefined).flatMap(_.get.getParameters.asScala.distinct.toList))
      vendorExtensions <- mapOf(genVendorExtension)
    } yield {
      val path = new Path
      val key = "/" + Random.shuffle(pathParams.map(p => s"{${p.getName}}") ::: staticPathParts).mkString("/")
      get.foreach(path.setGet)
      put.foreach(path.setPut)
      post.foreach(path.setPost)
      patch.foreach(path.setPatch)
      delete.foreach(path.setDelete)
      head.foreach(path.setHead)
      options.foreach(path.setOptions)
      vendorExtensions.foreach { case (k, value) => path.setVendorExtension(k, value) }
      key -> path
    }
  }

  /**
   * Generate an [[io.swagger.models.Operation Operation]]. `operationId` and `responses` will not be null,
   * but everything else can be. `parameters` will contain any kind of parameter type, but if it contains a
   * [[io.swagger.models.parameters.BodyParameter BodyParameter]] then it will not contain a
   * [[io.swagger.models.parameters.FormParameter FormParameter]] and vice versa. If it contains a
   * `BodyParameter` then it will only contain one such parameter. If it contains `FormParameter`s then `consumes`
   * will be a list containing either or both of "application/x-www-form-urlencoded" and "multipart/form-data".
   * @param pathParameters [[io.swagger.models.parameters.PathParameter PathParameter]]s that are used in the operation
   *                      as defined by the [[io.swagger.models.Path Path]] definition
   * @param globalDefinitions [[io.swagger.models.Model Model]]s defined in the [[io.swagger.models.Swagger Swagger]]
   *                         document which can be referred to
   * @param globalParameters [[io.swagger.models.parameters.Parameter Parameter]]s defined in the
   *                        [[io.swagger.models.Swagger Swagger]] document which can be referred to
   * @param securityDefinitions the Security Definitions Object from the [[io.swagger.models.Swagger Swagger]] document
   */
  // not sure whether there should be separate generators for each of the operation types or not
  def genOperation(pathParameters: List[parameters.Parameter] = Nil,
                   globalDefinitions: Option[Map[String, Model]] = None,
                   globalParameters: Option[Map[String, parameters.Parameter]] = None,
                   securityDefinitions: Option[Map[String, SecuritySchemeDefinition]] = None): Gen[Operation] = {
    for {
      operation <- (new Operation).withCommonFields(securityDefinitions)
      produces <- option(listOf(genMIMEType).map(_.distinct.asJava))
      parameters <- pathParameters match {
        case Nil => option(genOperationParameters(globalParameters, globalDefinitions))
        case _ => genOperationParameters(globalParameters, globalDefinitions).map(_ ::: pathParameters).map(Some(_))
      }
      consumes <- validConsumesForParameters(parameters, globalParameters).map(_.map(_.asJava))
      responses <- nonEmptyMap(genStatusCode.flatMap(code => genResponse(globalDefinitions).map(code -> _))).map(_.asJava)
    } yield {
      consumes.foreach(operation.setConsumes)
      produces.foreach(operation.setProduces)
      parameters.map(_.asJava).foreach(operation.setParameters)
      operation.setResponses(responses)
      operation
    }
  }

  /**
   * Generate a list of [[io.swagger.models.parameters.Parameter Parameter]]s. Will not generate
   * [[io.swagger.models.parameters.PathParameter PathParameter]]s because those must be defined as part of the
   * [[io.swagger.models.Path Path]] object keys.
   * @param globalParameters [[io.swagger.models.parameters.Parameter Parameter]]s defined in the
   *                        [[io.swagger.models.Swagger Swagger]] document which can be referred to
   * @param globalDefinitions [[io.swagger.models.Model Model]]s defined in the [[io.swagger.models.Swagger Swagger]]
   *                         document which can be referred to
   */
  // todo generate a Parameters object with vendor extensions when swagger-models adds it
  def genOperationParameters(globalParameters: Option[Map[String, parameters.Parameter]] = None,
                             globalDefinitions: Option[Map[String, Model]] = None): Gen[List[parameters.Parameter]] = {
    // todo still wondering if genRefParameter is prone to generation failure
    def genNonExclusiveParameters = globalParameters match {
      case Some(globalParams) if globalParams.nonEmpty => listOf(oneOf(
        genQueryParameter,
        genHeaderParameter,
        genCookieParameter,
        genRefParameter[QueryParameter](globalParams),
        genRefParameter[HeaderParameter](globalParams),
        genRefParameter[CookieParameter](globalParams)
      ))
      case _ => listOf(oneOf(
        genQueryParameter,
        genHeaderParameter,
        genCookieParameter
      ))
    }
    def genExclusiveParameters = globalParameters match {
      case Some(globalParams) if globalParams.nonEmpty =>
        oneOf(
          chooseNum(0, 1).flatMap(listOfN(_, oneOf(genBodyParameter(globalDefinitions), genRefParameter[BodyParameter](globalParams)))),
          listOf(oneOf(genFormParameter, genRefParameter[FormParameter](globalParams)))
        )
      case _ =>
        oneOf(
          chooseNum(0, 1).flatMap(listOfN(_, genBodyParameter(globalDefinitions))),
          listOf(genFormParameter)
        )
    }
    for {
      nonExclusive <- genNonExclusiveParameters
      exclusive <- genExclusiveParameters
    } yield nonExclusive ::: exclusive
  }

  /**
   * Generate a [[io.swagger.models.parameters.PathParameter PathParameter]].
   */
  def genPathParameter: Gen[PathParameter] = {
    for {
      parameter <- (new PathParameter).withCommonFields
      pattern <- arbitrary[Option[String]]
    } yield {
      parameter.setType("string")
      pattern.foreach(parameter.setPattern)
      parameter
    }
  }

  /**
   * Generate a [[io.swagger.models.parameters.QueryParameter QueryParameter]].
   */
  def genQueryParameter: Gen[QueryParameter] = {
    for {
      parameter <- (new QueryParameter).withCommonFields
      required <- arbitrary[Option[Boolean]]
      model <- genPrimitiveModelImpl
      pattern <- Option(model.getType) match {
        case Some("string") => arbitrary[Option[String]]
        case _ => const(None)
      }
    } yield {
      required.foreach(parameter.setRequired)
      parameter.setType(model.getType)
      parameter.setFormat(model.getFormat)
      pattern.foreach(parameter.setPattern)
      parameter
    }
  }

  /**
   * Generate a [[io.swagger.models.parameters.BodyParameter BodyParameter]].
   * @param globalDefinitions [[io.swagger.models.Model Model]]s defined in the [[io.swagger.models.Swagger Swagger]]
   *                         document which can be referred to
   */
  def genBodyParameter(globalDefinitions: Option[Map[String, Model]] = None): Gen[BodyParameter] = {
    for {
      parameter <- (new BodyParameter).withCommonFields
      required <- arbitrary[Option[Boolean]]
      (_, schema) <- genSchema(globalDefinitions)
    } yield {
      required.foreach(parameter.setRequired)
      parameter.setSchema(schema)
      parameter
    }
  }

  /**
   * Generate a [[io.swagger.models.parameters.HeaderParameter HeaderParameter]].
   */
  def genHeaderParameter: Gen[HeaderParameter] = {
    for {
      parameter <- (new HeaderParameter).withCommonFields
      required <- arbitrary[Option[Boolean]]
      model <- genPrimitiveModelImpl
      pattern <- Option(model.getType) match {
        case Some("string") => arbitrary[Option[String]]
        case _ => const(None)
      }
    } yield {
      required.foreach(parameter.setRequired)
      parameter.setType(model.getType)
      parameter.setFormat(model.getFormat)
      pattern.foreach(parameter.setPattern)
      parameter
    }
  }

  /**
   * Generate a [[io.swagger.models.parameters.FormParameter FormParameter]].
   */
  def genFormParameter: Gen[FormParameter] = {
    for {
      parameter <- (new FormParameter).withCommonFields
      required <- arbitrary[Option[Boolean]]
      model <- genPrimitiveModelImpl
      pattern <- Option(model.getType) match {
        case Some("string") => arbitrary[Option[String]]
        case _ => const(None)
      }
    } yield {
      required.foreach(parameter.setRequired)
      parameter.setType(model.getType)
      parameter.setFormat(model.getFormat)
      pattern.foreach(parameter.setPattern)
      parameter
    }
  }

  /**
   * Generate a [[io.swagger.models.parameters.CookieParameter CookieParameter]].
   */
  def genCookieParameter: Gen[CookieParameter] = {
    for {
      parameter <- (new CookieParameter).withCommonFields
      required <- arbitrary[Option[Boolean]]
      model <- genPrimitiveModelImpl
      pattern <- Option(model.getType) match {
        case Some("string") => arbitrary[Option[String]]
        case _ => const(None)
      }
    } yield {
      required.foreach(parameter.setRequired)
      parameter.setType(model.getType)
      parameter.setFormat(model.getFormat)
      pattern.foreach(parameter.setPattern)
      parameter
    }
  }

  /**
   * Generate a [[io.swagger.models.parameters.RefParameter RefParameter]] pointing to a particular type of Parameter.
   * @param globalParameters [[io.swagger.models.parameters.Parameter Parameter]]s defined in the
   *                        [[io.swagger.models.Swagger Swagger]] document which can be referred to. If this map does
   *                        not contain a Parameter of the type you specified this generator will be immediately
   *                        exhausted.
   */
  // todo this is pretty janky, and if it exhausts is that gonna fail the generation?
  def genRefParameter[T <: parameters.Parameter](globalParameters: Map[String, parameters.Parameter])(implicit ct: ClassTag[T]): Gen[RefParameter] = {
    for {
      ref <- oneOf(globalParameters.filter { case (_, p) => p.getClass == ct.runtimeClass }.keys.toSeq.map(k => s"#/parameters/$k"))
      p <- new RefParameter(ref).withCommonFields
    } yield p
  }

  /**
   * Generate a [[io.swagger.models.Response]].
   * @param globalDefinitions [[io.swagger.models.Model Model]]s defined in the [[io.swagger.models.Swagger Swagger]]
   *                         document which can be referred to
   */
  // todo generate a Responses object with vendor extensions when swagger-models adds it
  def genResponse(globalDefinitions: Option[Map[String, Model]] = None): Gen[Response] = {
    for {
      schema <- genProperty(globalDefinitions)
      description <- arbitrary[Option[String]]
      headers <- option(mapOf(zip(arbitrary[String], genProperty(globalDefinitions))))
      // todo vendorExtensions when swagger-models adds it
    } yield {
      val response = new Response
      response.setSchema(schema)
      description.foreach(response.setDescription)
      headers.map(_.asJava).foreach(response.setHeaders)
      response
    }
  }

  /**
   * Generate an [[io.swagger.models.AbstractModel AbstractModel]], which is either an object or array schema
   * definition, or a schema composed of other schemas. Typed as returning a `Model` for compilation reasons.
   */
  def genSchema(globalDefinitions: Option[Map[String, Model]] = None): Gen[(String, Model)] = {
    for {
      name <- arbitrary[String]
      model <- globalDefinitions match {
        case Some(globalDefs) if globalDefs.nonEmpty =>
          oneOf(genModelImpl(globalDefinitions), genArrayModel(globalDefinitions), genRefModel(globalDefs))
        case _ => oneOf(genModelImpl(globalDefinitions), genArrayModel(globalDefinitions))
      }
    } yield name -> model
  } // todo add genComposedModel when it's implemented

  /**
   * Generate a [[io.swagger.models.ModelImpl ModelImpl]].
   * @param globalDefinitions [[io.swagger.models.Model Model]]s defined in the [[io.swagger.models.Swagger Swagger]]
   *                         document which can be referred to
   */
  def genModelImpl(globalDefinitions: Option[Map[String, Model]] = None): Gen[ModelImpl] = {
    oneOf(genObjectModelImpl(globalDefinitions), genPrimitiveModelImpl)
  }

  /**
   * Generate a [[io.swagger.models.ModelImpl ModelImpl]] which is an object schema definition.
   * @param globalDefinitions [[io.swagger.models.Model Model]]s defined in the [[io.swagger.models.Swagger Swagger]]
   *                         document which can be referred to
   * @return a model which represents an object, which optionally has some `properties`.
   */
  def genObjectModelImpl(globalDefinitions: Option[Map[String, Model]] = None): Gen[ModelImpl] = {
    for {
      model <- (new ModelImpl).withCommonFields
      properties <- option(mapOf(genProperty(globalDefinitions).map(prop => prop.getName -> prop)))
      required <- option(someOf(properties.map(_.keys).getOrElse(Nil)))
      additionalProperties <- option(genProperty(globalDefinitions))
    } yield {
      model.setType(ModelImpl.OBJECT)
      properties.map(_.asJava).foreach(model.setProperties)
      required.map(_.asJava).foreach(model.setRequired)
      additionalProperties.foreach(model.setAdditionalProperties)
      model
    }
  }

  /**
   * Generate a [[io.swagger.models.ModelImpl ModelImpl]] which is a JSON primitive definition or a generic object.
   * @return a model which represents a JSON primitive or a generic object with some `additionalProperties`.
   */
  def genPrimitiveModelImpl: Gen[ModelImpl] = {
    for {
      model <- (new ModelImpl).withCommonFields
      property <- genProperty().suchThat(!_.isInstanceOf[ArrayProperty])
    } yield {
      model.setType(property.getType)
      model.setFormat(property.getFormat)
      property match {
        case mapProperty: MapProperty => model.setAdditionalProperties(mapProperty.getAdditionalProperties)
        case _ =>
      }
      model
    }
  }

  /**
   * Generate a [[io.swagger.models.ArrayModel ArrayModel]], which is an array schema definition.
   * @param globalDefinitions [[io.swagger.models.Model Model]]s defined in the [[io.swagger.models.Swagger Swagger]]
   *                         document which can be referred to
   * @return a model which represents an array.
   */
  def genArrayModel(globalDefinitions: Option[Map[String, Model]] = None): Gen[ArrayModel] = {
    for {
      model <- (new ArrayModel).withCommonFields
      items <- genProperty(globalDefinitions)
    } yield {
      model.setItems(items)
      model
    }
  }

  /**
   * Generate a [[io.swagger.models.ComposedModel ComposedModel]].
   * @throws NotImplementedError converter does not yet support ComposedModels.
   */
  def genComposedModel: Gen[(String, ComposedModel)] = ??? // todo implement this when converter supports it

  /**
   * Generate a [[io.swagger.models.RefModel RefModel]].
   * @param globalDefinitions [[io.swagger.models.Model Model]]s defined in the [[io.swagger.models.Swagger Swagger]]
   *                         document which can be referred to
   * @return a model which refers to a globally defined model.
   */
  def genRefModel(globalDefinitions: Map[String, Model]): Gen[RefModel] = {
    for {
      ref <- oneOf(globalDefinitions.keys.toSeq)
      description <- arbitrary[Option[String]]
      externalDocs <- option(genExternalDocs)
    } yield {
      val model = new RefModel(s"#/definitions/$ref")
      description.foreach(model.setDescription)
      externalDocs.foreach(model.setExternalDocs)
      model
    }
  }

  /**
   * Generate a [[io.swagger.models.properties.Property Property]].
   * @param globalDefinitions [[io.swagger.models.Model Model]]s defined in the [[io.swagger.models.Swagger Swagger]]
   *                         document which can be referred to
   */
  def genProperty(globalDefinitions: Option[Map[String, Model]] = None): Gen[Property] = {
    val gens = List[Gen[Property]](
      genBaseIntegerProperty, genIntegerProperty, genLongProperty,
      genDecimalProperty, genFloatProperty, genDoubleProperty,
      genBooleanProperty,
      genStringProperty, genEmailProperty,
      genDateProperty, genDateTimeProperty, genUUIDProperty,
      genArrayProperty(globalDefinitions), genMapProperty(globalDefinitions), genObjectProperty
      // todo file property
    )
    oneOf[Gen[Property]](
      globalDefinitions match {
        case Some(defs) => genRefProperty(defs) :: gens // RefProperties can only be generated if there are global definitions
        case None => gens
      }
    ).flatMap(identity)
  }

  /**
   * Generate a [[io.swagger.models.properties.BaseIntegerProperty BaseIntegerProperty]].
   */
  def genBaseIntegerProperty: Gen[BaseIntegerProperty] = {
    for {
      property <- (new BaseIntegerProperty).withCommonFields
      example <- option(arbitrary[Long].map(_.toString))
    } yield {
      example.foreach(property.setExample)
      property
    }
  }

  /**
   * Generate a [[io.swagger.models.properties.IntegerProperty IntegerProperty]].
   */
  def genIntegerProperty: Gen[IntegerProperty] = {
    for {
      property <- (new IntegerProperty).withCommonFields
      default <- arbitrary[Option[Int]]
      example <- option(arbitrary[Int].map(_.toString))
    } yield {
      default.foreach(property.setDefault(_))
      example.foreach(property.setExample)
      property
    }
  }

  /**
   * Generate a [[io.swagger.models.properties.LongProperty LongProperty]].
   */
  def genLongProperty: Gen[LongProperty] = {
    for {
      property <- (new LongProperty).withCommonFields
      default <- arbitrary[Option[Long]]
      example <- option(arbitrary[Long].map(_.toString))
    } yield {
      default.foreach(property.setDefault(_))
      example.foreach(property.setExample)
      property
    }
  }

  /**
   * Generate a [[io.swagger.models.properties.DecimalProperty DecimalProperty]].
   */
  def genDecimalProperty: Gen[DecimalProperty] = {
    for {
      property <- (new DecimalProperty).withCommonFields
      example <- option(arbitrary[Double].map(_.toString))
    } yield {
      example.foreach(property.setExample)
      property
    }
  }

  /**
   * Generate a [[io.swagger.models.properties.FloatProperty FloatProperty]].
   */
  def genFloatProperty: Gen[FloatProperty] = {
    for {
      property <- (new FloatProperty).withCommonFields
      default <- arbitrary[Option[Float]]
      example <- option(arbitrary[Float].map(_.toString))
    } yield {
      default.foreach(property.setDefault(_))
      example.foreach(property.setExample)
      property
    }
  }

  /**
   * Generate a [[io.swagger.models.properties.DoubleProperty DoubleProperty]].
   */
  def genDoubleProperty: Gen[DoubleProperty] = {
    for {
      property <- (new DoubleProperty).withCommonFields
      default <- arbitrary[Option[Double]]
      example <- option(arbitrary[Double].map(_.toString))
    } yield {
      default.foreach(property.setDefault(_))
      example.foreach(property.setExample)
      property
    }
  }

  /**
   * Generate a [[io.swagger.models.properties.BooleanProperty BooleanProperty]].
   */
  def genBooleanProperty: Gen[BooleanProperty] = {
    for {
      property <- (new BooleanProperty).withCommonFields
      default <- arbitrary[Option[Boolean]]
      example <- option(arbitrary[Boolean].map(_.toString))
    } yield {
      default.foreach(property.setDefault(_))
      example.foreach(property.setExample)
      property
    }
  }

  /**
   * Generate a [[io.swagger.models.properties.StringProperty StringProperty]]. All fields can be null.
   * If `minLength` and/or `maxLength` are not null, they will be positive integers. `default` will not be guaranteed
   * to match `pattern` if both are non-null and it will not be guaranteed to respect `minLength` or `maxLength`.
   * If `enum` and `default` are not null, then `default` will be one of the values in `enum`.
   */
  def genStringProperty: Gen[StringProperty] = {
    for {
      format <- option(oneOf(StringProperty.Format.values))
      property <- format.map(new StringProperty(_)).getOrElse(new StringProperty).withCommonFields
      (minLength, maxLength) <- genMinMax(1, Integer.MAX_VALUE).flatMap {
        case (min, max) => option(const(min)).flatMap(mn => option(const(max)).map(mn -> _))
      }
      enum <- option(nonEmptyListOf(arbitrary[String]))
      default <- option(sequence(enum.map(oneOf(_))).map(_.get(0))) // sequence only outputs arraylist...
      pattern <- arbitrary[Option[String]]
    } yield {
      minLength.map(_.toInt).foreach(i => property.setMinLength(i))
      maxLength.map(_.toInt).foreach(i => property.setMaxLength(i))
      enum.map(_.asJava).foreach(property.setEnum)
      default.foreach(property.setDefault)
      pattern.foreach(property.setPattern)
      property
    }
  }

  /**
   * Generate an [[io.swagger.models.properties.EmailProperty EmailProperty]].
   */
  def genEmailProperty: Gen[EmailProperty] = genStringProperty.map(new EmailProperty(_))

  /**
   * Generate a [[io.swagger.models.properties.DateProperty DateProperty]].
   */
  def genDateProperty: Gen[DateProperty] = (new DateProperty).withCommonFields

  /**
   * Generate a [[io.swagger.models.properties.DateTimeProperty DateTimeProperty]].
   */
  def genDateTimeProperty: Gen[DateTimeProperty] = (new DateTimeProperty).withCommonFields

  /**
   * Generate a [[io.swagger.models.properties.UUIDProperty UUIDProperty]]. All fields can be null.
   * If `minLength` and/or `maxLength` are not null, they will be positive integers. `default` will not be guaranteed
   * to match `pattern` if both are non-null and it will not be guaranteed to respect `minLength` or `maxLength`.
   */
  def genUUIDProperty: Gen[UUIDProperty] = {
    for {
      property <- (new UUIDProperty).withCommonFields
      (minLength, maxLength) <- genMinMax(1, Integer.MAX_VALUE).flatMap {
        case (min, max) => option(const(min)).flatMap(mn => option(const(max)).map(mn -> _))
      }
      default <- arbitrary[Option[String]]
      pattern <- arbitrary[Option[String]]
    } yield {
      minLength.map(_.toInt).foreach(i => property.setMinLength(i))
      maxLength.map(_.toInt).foreach(i => property.setMaxLength(i))
      default.foreach(property.setDefault)
      pattern.foreach(property.setPattern)
      property
    }
  }

  /**
   * Generate an [[io.swagger.models.properties.ArrayProperty ArrayProperty]]. `items` will not be null, but
   * `uniqueItems` may be.
   * @param globalDefinitions [[io.swagger.models.Model Model]]s defined in the [[io.swagger.models.Swagger Swagger]]
   *                         document which can be referred to
   */
  def genArrayProperty(globalDefinitions: Option[Map[String, Model]] = None): Gen[ArrayProperty] = {
    for {
      items <- genProperty(globalDefinitions)
      property <- new ArrayProperty(items).withCommonFields
      uniqueItems <- arbitrary[Option[Boolean]]
    } yield {
      uniqueItems.foreach(b => property.setUniqueItems(b.booleanValue))
      property
    }
  }

  /**
   * Generate a [[io.swagger.models.properties.MapProperty MapProperty]]. `additionalProperties` may be left null.
   * @param globalDefinitions [[io.swagger.models.Model Model]]s defined in the [[io.swagger.models.Swagger Swagger]]
   *                         document which can be referred to
   */
  def genMapProperty(globalDefinitions: Option[Map[String, Model]] = None): Gen[MapProperty] = {
    for {
      property <- (new MapProperty).withCommonFields
      additionalProperties <- option(genProperty(globalDefinitions))
    } yield {
      additionalProperties.foreach(property.setAdditionalProperties)
      property
    }
  }

  /**
   * Generate an [[io.swagger.models.properties.ObjectProperty ObjectProperty]].
   */
  def genObjectProperty: Gen[ObjectProperty] = (new ObjectProperty).withCommonFields

  /**
   * Generate a [[io.swagger.models.properties.RefProperty RefProperty]].
   * @param globalDefinitions [[io.swagger.models.Model Model]]s defined in the [[io.swagger.models.Swagger Swagger]]
   *                         document which can be referred to
   */
  def genRefProperty(globalDefinitions: Map[String, Model]): Gen[RefProperty] = {
    for {
      ref <- oneOf(globalDefinitions.keys.toSeq)
      property <- new RefProperty(s"#/definitions/$ref").withCommonFields
    } yield property
  }
  /**
   * Generate a [[io.swagger.models.properties.FileProperty FileProperty]].
   * @throws NotImplementedError because file type isn't supported by the converter yet
   */
  def genFileProperty: Gen[FileProperty] = ???  // todo implement this when the converter supports it.

  /**
   * Generate a [[io.swagger.models.Tag Tag]].
   */
  def genTag: Gen[Tag] = {
    for {
      name <- arbitrary[UUID].map(_.toString)
      description <- arbitrary[Option[String]]
      externalDocs <- option(genExternalDocs)
      vendorExtensions <- mapOf(genVendorExtension)
    } yield {
      val tag = new Tag
      tag.setName(name)
      description.foreach(tag.setDescription)
      externalDocs.foreach(tag.setExternalDocs)
      vendorExtensions.foreach { case (k, v) => tag.setVendorExtension(k, v) }
      tag
    }
  }

  /**
   * Generate a [[io.swagger.models.auth.SecuritySchemeDefinition]], which can be one of the following:
   * a [[io.swagger.models.auth.BasicAuthDefinition BasicAuthDefinition]],
   * an [[io.swagger.models.auth.ApiKeyAuthDefinition ApiKeyAuthDefinition]],
   * or an [[io.swagger.models.auth.OAuth2Definition OAuth2Definition]].
   */
  // todo these generators should have description added to them when swagger-models adds it
  def genSecuritySchemeDefinition: Gen[SecuritySchemeDefinition] = {
    oneOf(genBasicAuthDefinition, genApiKeyAuthDefinition, genOAuth2Definition)
  }

  /**
   * Generate a [[io.swagger.models.auth.BasicAuthDefinition]].
   */
  def genBasicAuthDefinition: Gen[BasicAuthDefinition] = (new BasicAuthDefinition).withCommonFields

  /**
   * Generate an [[io.swagger.models.auth.ApiKeyAuthDefinition]].
   */
  def genApiKeyAuthDefinition: Gen[ApiKeyAuthDefinition] = {
    for {
      name <- arbitrary[String]
      in <- oneOf(In.HEADER, In.QUERY)
      definition <- new ApiKeyAuthDefinition(name, in).withCommonFields
    } yield definition
  }

  /**
   * Generate an [[io.swagger.models.auth.OAuth2Definition]]. The required values change based on the value of `flow`,
   * but whatever is required will not be null.
   */
  def genOAuth2Definition: Gen[OAuth2Definition] = {
    oneOf(genOAuth2ImplicitFlowDefinition, genOAuth2PasswordFlowDefinition,
      genOAuth2ApplicationFlowDefinition, genOAuth2AccessCodeFlowDefinition)
  }

  /**
   * Generate an [[io.swagger.models.auth.OAuth2Definition]] with an "implicit" `flow`.
   */
  def genOAuth2ImplicitFlowDefinition: Gen[OAuth2Definition] = {
    for {
      definition <- (new OAuth2Definition).withCommonFields
      scopes <- arbitrary[Map[String, String]].map(_.asJava)
      authorizationUrl <- genURL.map(_.toString)
    } yield {
      definition.setFlow("implicit")
      definition.setAuthorizationUrl(authorizationUrl)
      definition.setScopes(scopes)
      definition
    }
  }

  /**
   * Generate an [[io.swagger.models.auth.OAuth2Definition]] with an "password" `flow`.
   */
  def genOAuth2PasswordFlowDefinition: Gen[OAuth2Definition] = {
    for {
      definition <- (new OAuth2Definition).withCommonFields
      scopes <- arbitrary[Map[String, String]].map(_.asJava)
      tokenUrl <- genURL.map(_.toString)
    } yield {
      definition.setFlow("password")
      definition.setTokenUrl(tokenUrl)
      definition.setScopes(scopes)
      definition
    }
  }

  /**
   * Generate an [[io.swagger.models.auth.OAuth2Definition]] with an "application" `flow`.
   */
  def genOAuth2ApplicationFlowDefinition: Gen[OAuth2Definition] = {
    for {
      definition <- (new OAuth2Definition).withCommonFields
      scopes <- arbitrary[Map[String, String]].map(_.asJava)
      tokenUrl <- genURL.map(_.toString)
    } yield {
      definition.setFlow("application")
      definition.setTokenUrl(tokenUrl)
      definition.setScopes(scopes)
      definition
    }
  }

  /**
   * Generate an [[io.swagger.models.auth.OAuth2Definition]] with an "accessCode" `flow`.
   */
  def genOAuth2AccessCodeFlowDefinition: Gen[OAuth2Definition] = {
    for {
      definition <- (new OAuth2Definition).withCommonFields
      scopes <- arbitrary[Map[String, String]].map(_.asJava)
      authorizationUrl <- genURL.map(_.toString)
      tokenUrl <- genURL.map(_.toString)
    } yield {
      definition.setFlow("accessCode")
      definition.setAuthorizationUrl(authorizationUrl)
      definition.setTokenUrl(tokenUrl)
      definition.setScopes(scopes)
      definition
    }
  }

  /**
   * Generate a [[io.swagger.models.SecurityRequirement SecurityRequirement]]. This generator may need to be removed
   * eventually since the model seems to be getting deprecated soon.
   * @param securityDefinition the security definition to take scopes from if it is an
   *                           [[io.swagger.models.auth.OAuth2Definition OAuth2Definition]].
   */
  def genSecurityRequirement(securityDefinition: SecuritySchemeDefinition): Gen[SecurityRequirement] = {
    for {
      scopes <- securityDefinition match {
        case o: OAuth2Definition => someOf(Option(o.getScopes).map(_.keySet.asScala).getOrElse(Nil))
        case _ => const(Nil)
      }
    } yield {
      val securityRequirement = new SecurityRequirement
      scopes.foreach(securityRequirement.addScope)
      securityRequirement
    }
  }

  /**
   * Generate the structural representation of the Security Requirement Object.
   * @param securityDefinitions the Security Definitions Object from the [[io.swagger.models.Swagger Swagger]] document
   * @return a map of security scheme names to a list of scope names required for execution (or empty if not oauth2.)
   */
  def genSecurityRequirement(securityDefinitions: Map[String, SecuritySchemeDefinition]): Gen[Map[String, List[String]]] = {
    someOf(securityDefinitions.mapValues {
      case o: OAuth2Definition => Option(o.getScopes).map(_.asScala.keys.toList).getOrElse(Nil)
      case _ => Nil
    }).map(_.toMap)
  }

  /**
   * Generate an [[io.swagger.models.ExternalDocs ExternalDocs]].
   */
  def genExternalDocs: Gen[ExternalDocs] = {
    for {
      url <- genURL.map(_.toString)
      description <- arbitrary[Option[String]]
      // todo vendorExtensions when swagger-models adds it
    } yield {
      val externalDocs = new ExternalDocs
      externalDocs.setUrl(url)
      description.foreach(externalDocs.setDescription)
      externalDocs
    }
  }

  /* =================
   * Helper generators
   * ================= */

  /**
   * Generate a vendor extension key/value pair. Value can be an Int, a Double, a Boolean, a List[Any],
   * a Map[String, Any], or null.
   */
  def genVendorExtension: Gen[(String, Any)] = {
    for {
      key <- genVendorExtensionKey
      value <- genJSONValue
    } yield key -> value
  }

  /**
   * Generate a vendor extension key. Characters are going to be alphanumeric and joined by '-'.
   */
  def genVendorExtensionKey: Gen[String] = {
    nonEmptyListOf(genNonEmptyAlphaNumStr).map(_.mkString("-")).map(s => s"x-$s").suchThat(_.matches("""x-\w+(?:-\w+)*"""))
  }

  /**
   * Generate a valid JSON value. It can be an Int, a Double, a Boolean, a List of other JSON values, a Map of String
   * to other JSON values, or null.
   */
  def genJSONValue: Gen[Any] = oneOf(
    arbitrary[Int], arbitrary[Double], arbitrary[Boolean], const(null),
    listOf(genJSONValue),
    mapOf(arbitrary[String].flatMap(key => genJSONValue.map(key -> _)))
  )

  /**
   * Generate a version string, which is delimited by periods (.), using only digits. Versions can contain other
   * characters in the real world, but this is fine for tests.
   */
  def genVersion: Gen[String] = {
    for {
      major <- arbitrary[Long]
      minor <- listOf(arbitrary[Long])
    } yield s"$major.${minor.mkString(".")}"
  }.suchThat(_.matches("""\d+(?:\.\d+)*"""))

  /**
   * Generate a URL. Not particularly adventurous in terms of the characters in the URL.
   */
  def genURL: Gen[URL] = {
    for {
      scheme <- oneOf("http", "https")
      host <- genNonEmptyAlphaNumStr
      port <- oneOf(arbitrary[Int].suchThat(_ != 0).map(math.abs), const(-1))
      file <- arbitrary[String]
    } yield new URL(scheme, host, port, file)
  }

  /**
   * Generate an email address. Not particularly adventurous in terms of allowed characters or TLDs.
   */
  def genEmail: Gen[String] = {
    for {
      address <- genNonEmptyAlphaNumStr
      domainName <- genNonEmptyAlphaNumStr
      domain <- oneOf("com", "org", "net") // good enough
    } yield s"$address@$domainName.$domain"
  }.suchThat(_.matches("""\w+@\w+\.(?:(?:com)|(?:org)|(?:net))"""))

  /**
   * Generate a MIME type. Includes only a very limited subset of possible media types, but to enumerate all of them
   * would take a long time, even when using Guava's `MediaType`.
   */
  def genMIMEType: Gen[String] = {
    val mimeTypes = List("application/json", "application/xml", "text/plain")
    oneOf(mimeTypes).suchThat(mimeTypes.contains)
  }

  /**
   * Generate one of a few common HTTP status codes. "default" is also included because of its use in the Responses
   * object.
   * @return one of the following: 200, 201, 204, 400, 403, 404, default
   */
  def genStatusCode: Gen[String] = oneOf("200", "201", "204", "400", "403", "404", "default")

  /**
   * Generate values for `minimum` and `maximum` to be used in some kinds of [[io.swagger.models.properties.Property Property]].
   * @param lowerBound the absolute minimum for an acceptable return value.
   * @param upperBound the absolute maximum for an acceptable return value. Must be greater than lowerbound.
   * @return `(min, max)`
   */
  def genMinMax(lowerBound: Double, upperBound: Double): Gen[(Double, Double)] = {
    for {
      min <- choose(lowerBound, upperBound)
      max <- choose(min, upperBound)
    } yield (min, max)
  }.suchThat { case (min, max) => max >= min }

  /**
   * Generate a `consumes` list that is valid given the following parameters.
   * @param generatedParameters an optional list of generated parameters. If `None` or empty, the result will be `None`
   *                            or empty.
   * @param globalParameters [[io.swagger.models.parameters.Parameter Parameter]]s defined in the
   *                        [[io.swagger.models.Swagger Swagger]] document which can be referred to
   * @return None or empty sequence if there were no generated parameters or if the generated parameters included
   *         neither a [[io.swagger.models.parameters.BodyParameter BodyParameter]] nor a
   *         [[io.swagger.models.parameters.FormParameter FormParameter]]; a list of either or both of
   *         "application/x-www-form-urlencoded" and "multipart/form-data" if the generated parameters included a
   *         [[io.swagger.models.parameters.FormParameter FormParameter]]; or an optional list of any of the MIME types
   *         defined in [[genMIMEType]].
   */
  private def validConsumesForParameters(generatedParameters: Option[List[parameters.Parameter]],
                                         globalParameters: Option[Map[String, parameters.Parameter]] = None): Gen[Option[Seq[String]]] = {
    generatedParameters match {
      case None => oneOf(Some(List.empty), None)
      case Some(Nil) => oneOf(Some(List.empty), None)
      case Some(parameters) =>
        if (parameters.exists(isParameterType[FormParameter](globalParameters))) {
          someOf("application/x-www-form-urlencoded", "multipart/form-data").map(Some(_)).suchThat(o => o.nonEmpty && o.forall(_.nonEmpty))
        } else if (parameters.exists(isParameterType[BodyParameter](globalParameters))) {
          option(listOf(genMIMEType).map(_.distinct))
        } else {
          oneOf(Some(List.empty), None)
        }
    }
  }

  /**
   * Checks to see if a parameter is effectively of a certain type.
   * @param globalParameters the optional list of global parameters
   * @param p the parameter to test
   * @tparam T the type of parameter to check for
   * @return true if the parameter is either an instance of `T` or is a
   *         [[io.swagger.models.parameters.RefParameter RefParameter]] pointing to an instance of `T`.
   */
  private def isParameterType[T <: parameters.Parameter](globalParameters: Option[Map[String, parameters.Parameter]] = None)
                                                        (p: parameters.Parameter)
                                                        (implicit ct: ClassTag[T]): Boolean = {
    p.getClass == ct.runtimeClass ||
      (p.isInstanceOf[RefParameter] &&
        globalParameters.nonEmpty &&
        globalParameters.forall(_.get(p.asInstanceOf[RefParameter].getSimpleRef).getClass == ct.runtimeClass))
  }

  /**
   * Exposes a generator that fills common fields in for Operations.
   * @param operation the Operation which will have its fields populated.
   */
  private implicit class OperationWithCommonFields(operation: => Operation) {
    /**
     * Generate common fields for the passed in Operation. `operationId` will never be null.
     * @param securityDefinitions the Security Definitions Object from the [[io.swagger.models.Swagger Swagger]] document
     * @return the Operation with tags, summary, description, externalDocs, operationId, schemes, deprecated, and
     *         vendorExtensions added
     */
    def withCommonFields(securityDefinitions: Option[Map[String, SecuritySchemeDefinition]] = None): Gen[Operation] = {
      for {
        tags <- arbitrary[Option[List[String]]].map(_.map(_.asJava))
        summary <- arbitrary[Option[String]]
        description <- arbitrary[Option[String]]
        externalDocs <- option(genExternalDocs)
        operationId <- arbitrary[UUID].map(_.toString)
        schemes <- option(listOf(oneOf(Scheme.HTTP, Scheme.HTTPS, Scheme.WS, Scheme.WSS)).map(_.distinct.asJava))
        deprecated <- arbitrary[Option[Boolean]].map(_.map(b => new java.lang.Boolean(b.booleanValue)))
        security <- securityDefinitions match {
          case Some(securityDefs) if securityDefs.nonEmpty =>
            option(listOf(genSecurityRequirement(securityDefs).map(_.mapValues(_.asJava)).map(_.asJava)).map(_.asJava))
          case None => const(None)
        }
        vendorExtensions <- mapOf(genVendorExtension)
      } yield {
        val op = operation
        tags.foreach(op.setTags)
        summary.foreach(op.setSummary)
        description.foreach(op.setDescription)
        externalDocs.foreach(op.setExternalDocs)
        op.setOperationId(operationId)
        schemes.foreach(op.setSchemes)
        deprecated.foreach(op.setDeprecated)
        security.foreach(op.setSecurity)
        vendorExtensions.foreach { case (key, value) => op.setVendorExtension(key, value) }
        op
      }
    }
  }

  /**
   * Exposes a generator that fills common fields in for Properties.
   * @param property the Property which will have its fields populated.
   * @tparam T the particular type of Property
   */
  private implicit class PropertyWithCommonFields[T <: Property](property: => T) {
    /**
     * Generate common fields for the passed in Property. `name` will never be null.
     * @return the Property with name, title, description, required, and readOnly added
     */
    def withCommonFields: Gen[T] = {
      for {
        name <- arbitrary[String]
        title <- arbitrary[Option[String]]
        description <- arbitrary[Option[String]]
        required <- arbitrary[Option[Boolean]]
        readOnly <- arbitrary[Option[Boolean]]
        // todo vendorExtensions when swagger-models adds it
      } yield {
        val prop = property
        prop.setName(name)
        title.foreach(prop.setTitle)
        description.foreach(prop.setDescription)
        required.foreach(prop.setRequired)
        readOnly.foreach(b => prop.setReadOnly(b.booleanValue))
        prop
      }
    }
  }

  /**
   * Exposes a generator that fills common fields in for AbstractParameters.
   * @param parameter the AbstractParameter which will have its fields populated.
   * @tparam T the particular type of AbstractParameter
   */
  private implicit class AbstractParameterWithCommonFields[T <: AbstractParameter](parameter: => T) {
    /**
     * Generate common fields for the passed in AbstractParameter. `name` will never be null.
     * @return the AbstractParameter with name, description, and vendorExtensions added
     */
    def withCommonFields: Gen[T] = {
      for {
        name <- arbitrary[String]
        description <- arbitrary[Option[String]]
        vendorExtensions <- mapOf(genVendorExtension)
      } yield {
        val param = parameter
        param.setName(name)
        description.foreach(param.setDescription)
        vendorExtensions.foreach { case (key, value) => param.setVendorExtension(key, value) }
        param
      }
    }
  }

  /**
   * Exposes a generator that fills common fields in for AbstractModels.
   * @param model the AbstractModel which will have its fields populated.
   * @tparam T the particular type of AbstractModel
   */
  private implicit class AbstractModelWithCommonFields[T <: AbstractModel](model: => T) {
    /**
     * Generate common fields for the passed in AbstractModel.
     * @return the AbstractModel with description, externalDocs, and vendorExtensions added
     */
    def withCommonFields: Gen[T] = {
      for {
        description <- arbitrary[Option[String]]
        externalDocs <- option(genExternalDocs)
        vendorExtensions <- mapOf(genVendorExtension)
      } yield {
        val m = model
        description.foreach(m.setDescription)
        externalDocs.foreach(m.setExternalDocs)
        vendorExtensions.foreach { case (key, value) => m.setVendorExtension(key, value) }
        m
      }
    }
  }

  /**
   * Exposes a generator that fills common fields in for AbstractSecuritySchemeDefinitions.
   * @param securitySchemeDefinition the AbstractSecuritySchemeDefinition which will have its fields populated.
   * @tparam T the particular type of AbstractSecuritySchemeDefinition
   */
  private implicit class AbstractSecuritySchemeDefinitionWithCommonFields[T <: AbstractSecuritySchemeDefinition](securitySchemeDefinition : => T) {
    /**
     * Generate common fields for the passed in AbstractSecuritySchemeDefinition.
     * @return the AbstractSecuritySchemeDefinition with vendorExtensions added
     */
    def withCommonFields: Gen[T] = {
      for {
        vendorExtensions <- mapOf(genVendorExtension)
      } yield {
        val ssd = securitySchemeDefinition
        vendorExtensions.foreach { case (key, value) => ssd.setVendorExtension(key, value) }
        ssd
      }
    }
  }

}
