# swagger-gdd
Interoperation between [Swagger](http://swagger.io/)
and [Google Discovery Documents](https://developers.google.com/discovery/v1/reference/apis?hl=en)

Contains [logic-less models](models/src/main/java/io/swagger/gdd) of the GDD spec and
[conversion utilities](converters/src/main/scala/io/swagger/gdd).

## Testing

Modify [`TestRunner.scala`](converters/src/main/scala/io/swagger/gdd/TestRunner.scala) to point to your
`swagger.json` file and run it.
