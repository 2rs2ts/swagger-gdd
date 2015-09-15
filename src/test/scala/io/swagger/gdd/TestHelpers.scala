package io.swagger.gdd

import org.scalacheck.Gen

/**
 * Helper methods for tests.
 */
trait TestHelpers {

  implicit class EasySuchThats[T](gen: Gen[T]) {
    def guarantee(f: T => Any): Gen[T] = gen.suchThat(t => Option(f(t)).isDefined)
  }

}
