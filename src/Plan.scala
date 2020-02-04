package validator

import unfiltered.request._
import unfiltered.response._
import unfiltered.netty._
import scala.concurrent._
import ExecutionContext.Implicits.global
import org.json4s._
import org.json4s.native.Serialization
import org.json4s.native.Serialization.{write}
import org.json4s.native.JsonMethods._
import scala.util.{Try, Success, Failure}

case class Response(action: String, id: String, status: String, message: Option[String] = None)

@io.netty.channel.ChannelHandler.Sharable
object Plan extends async.Plan with ServerErrorResponse {

  implicit val formats = Serialization.formats(NoTypeHints)

  def intent = {

    case req @ POST(Path(Seg("schema" :: schemaID :: Nil))) & Accepts.Json(_) => {
      Try(parse(Body.string(req))) match {
        case Success(json) => {
          val res = Response("uploadSchema", schemaID, "success")
          req.respond(JsonContent ~> ResponseString(write(res)))
        }
        case Failure(e) => {
          val res = Response("uploadSchema", schemaID, "error", Some(e.getMessage))
          req.respond(BadRequest ~> ResponseString(write(res)))
        }
      }
    }

    case req @ GET(Path(Seg("schema" :: schemaID :: Nil))) & Accepts.Json(_) => {
      val res = Response("uploadSchema", schemaID, "success")
      req.respond(JsonContent ~> ResponseString(write(res)))
    }

    case req @ POST(Path(Seg("validate" :: schemaID :: Nil))) & Accepts.Json(_) => {
      val res = Response("uploadSchema", schemaID, "success")
      req.respond(JsonContent ~> ResponseString(write(res)))
    }
  }

}

