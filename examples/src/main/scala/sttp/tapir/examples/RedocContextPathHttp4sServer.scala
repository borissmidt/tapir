package sttp.tapir.examples

import cats.effect._
import cats.syntax.all._
import org.http4s.HttpRoutes
import org.http4s.server.Router
import org.http4s.blaze.server.BlazeServerBuilder
import sttp.tapir._
import sttp.tapir.examples.MultipleEndpointsDocumentationHttp4sServer.booksListingRoutes
import sttp.tapir.redoc.RedocUIOptions
import sttp.tapir.redoc.bundle.RedocInterpreter
import sttp.tapir.server.http4s.Http4sServerInterpreter

import scala.concurrent.ExecutionContext

object RedocContextPathHttp4sServer extends IOApp {
  val contextPath = List("api", "v1")
  val docPathPrefix: List[String] = "redoc" :: Nil

  val helloWorld: PublicEndpoint[String, Unit, String, Any] =
    endpoint.get.in("hello").in(query[String]("name")).out(stringBody)

  // converting an endpoint to a route (providing server-side logic); extension method comes from imported packages
  // adding redoc endpoints
  val routes: HttpRoutes[IO] =
    Http4sServerInterpreter[IO]().toRoutes(
      helloWorld.serverLogic(name => IO(s"Hello, $name!".asRight[Unit])) ::
        RedocInterpreter(redocUIOptions = RedocUIOptions.default.contextPath(contextPath).pathPrefix(docPathPrefix))
          .fromEndpoints[IO](List(helloWorld), "The tapir library", "1.0.0")
    )

  implicit val ec: ExecutionContext = scala.concurrent.ExecutionContext.Implicits.global

  override def run(args: List[String]): IO[ExitCode] = {
    // starting the server
    BlazeServerBuilder[IO]
      .withExecutionContext(ec)
      .bindHttp(8080, "localhost")
      .withHttpApp(Router(s"/${contextPath.mkString("/")}" -> routes).orNotFound)
      .resource
      .use { _ => IO.println(s"go to: http://127.0.0.1:8080/${(contextPath ++ docPathPrefix).mkString("/")}") *> IO.never }
      .as(ExitCode.Success)
  }
}
