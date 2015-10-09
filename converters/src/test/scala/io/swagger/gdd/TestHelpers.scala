package io.swagger.gdd

import org.scalacheck.Gen
import org.specs2.ScalaCheck
import org.specs2.scalacheck.Parameters

/**
 * Helper methods and such for tests.
 */
trait TestHelpers {
  this: ScalaCheck =>

  /**
   * Limits collection size... without this, tests take eons.
   */
  override implicit lazy val defaultParameters = new Parameters(maxSize = 5)

  implicit class EasySuchThats[T](gen: Gen[T]) {
    def guarantee(f: T => Any): Gen[T] = gen.suchThat(t => Option(f(t)).isDefined)
  }

}
