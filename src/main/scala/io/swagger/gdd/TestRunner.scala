package io.swagger.gdd

import com.fasterxml.jackson.annotation.JsonInclude.Include
import com.fasterxml.jackson.core.util.DefaultPrettyPrinter
import com.fasterxml.jackson.databind.{SerializationFeature, DeserializationFeature, ObjectMapper}
import io.swagger.parser.SwaggerParser

object TestRunner extends App {
  val swagger = new SwaggerParser().read("http://petstore.swagger.io/v2/swagger.json")
  val gdd = SwaggerToGDD.swaggerToGDD(swagger)
  val mapper = new ObjectMapper()
  mapper.setSerializationInclusion(Include.NON_NULL)
  mapper.configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
  mapper.configure(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS, false)
  mapper.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
  val writer = mapper.writer(new DefaultPrettyPrinter())
  println(writer.writeValueAsString(gdd))
}
