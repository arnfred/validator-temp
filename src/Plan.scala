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
import scala.collection.mutable
import scala.util.{Try, Success, Failure}

case class Response(action: String, id: String, status: String, message: Option[String] = None)

@io.netty.channel.ChannelHandler.Sharable
object Plan extends async.Plan with ServerErrorResponse {

  implicit val formats = Serialization.formats(NoTypeHints)

  val schemas: mutable.Map[String, String] = mutable.Map.empty
  val rules: mutable.Map[String, Rules] = mutable.Map.empty

  def intent = {

    // The response format for this endpoint isn't specified in the docs
    case req @ GET(Path(Seg("schema" :: schemaID :: Nil))) & Accepts.Json(_) => {
      schemas.get(schemaID) match {
        case Some(s) => req.respond(JsonContent ~> ResponseString(s))
        case None => req.respond(NotFound)
      }
    }

    case req @ POST(Path(Seg("schema" :: schemaID :: Nil))) & Accepts.Json(_) => {
      val body = Body.string(req)
      Try(parse(body)) match {

        case Success(json) => {
          // Any sensible design would persist this somewhere else like Redis, Postgresql or DynamoDB
          schemas += (schemaID -> body)
          val schemaRules = Rules.parse(json)
          rules += (schemaID -> schemaRules)

          val res = Response("uploadSchema", schemaID, "success")
          req.respond(JsonContent ~> ResponseString(write(res)))
        }

        case Failure(e) => {
          val res = Response("uploadSchema", schemaID, "error", Some(e.getMessage))
          req.respond(BadRequest ~> ResponseString(write(res)))
        }
      }
    }

    case req @ POST(Path(Seg("validate" :: schemaID :: Nil))) & Accepts.Json(_) => {
      rules.get(schemaID) match {
        case None => req.respond(NotFound)
        case Some(r) => {
          val body = Body.string(req)
          Try(parse(body)) match {
            case Success(json) => {
              val errors: Seq[String] = r.validate(json).collect { case Some(e) => e }
              if (errors.size == 0) {
                val res = Response("validateDocument", schemaID, "success")
                req.respond(JsonContent ~> ResponseString(write(res)))
              } else {
                val res = Response("validateDocument", schemaID, "error", Some(errors.mkString("\n")))
                req.respond(JsonContent ~> ResponseString(write(res)))
              }
            }
            case Failure(e) => {
              val res = Response("validateDocument", schemaID, "error", Some(e.getMessage))
              req.respond(BadRequest ~> ResponseString(write(res)))
            }
          }
        }
      }
    }
  }
}

