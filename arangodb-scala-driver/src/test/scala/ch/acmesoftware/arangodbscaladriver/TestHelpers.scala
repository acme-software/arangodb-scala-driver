package ch.acmesoftware.arangodbscaladriver

import java.util.concurrent.CompletableFuture

import scala.compat.java8.FutureConverters._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Future

object TestHelpers {

  def completableFutureOfJava(boolean: Boolean): CompletableFuture[java.lang.Boolean] =
    Future.successful(boolean).map(Boolean.box).toJava.toCompletableFuture
}
