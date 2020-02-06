package validator

import org.specs2._
import specification.{Before, Scope}
import org.json4s._
import org.json4s.native.JsonMethods._

import okhttp3._

class PlanSpec extends mutable.Specification with unfiltered.specs2.netty.Served {

  def setup = { _.handler(Plan) }

  val JSON = MediaType.get("application/json; charset=utf-8");

  val http = new OkHttpClient()

  def post[T](path: String, json: String)(validation: okhttp3.Response => matcher.MatchResult[T]): matcher.MatchResult[T] = {
      val url = host.toString + path
      val body = RequestBody.create(json, JSON);
      val request = new Request.Builder()
        .url(url)
        .header("Accept", "application/json")
        .post(body)
        .build()
      val response = http.newCall(request).execute()
      val res = validation(response)
      response.close()
      res
  }

  def get[T](path: String)(validation: okhttp3.Response => matcher.MatchResult[T]): matcher.MatchResult[T] = {
      val url = host.toString + path
      val request = new Request.Builder()
        .url(url)
        .header("Accept", "application/json")
        .build()
      val response = http.newCall(request).execute()
      val res = validation(response)
      response.close()
      res
  }

  "The validator" should {
    "when receiving a GET request for a schema" in {

      "return a 404 for non-existant schemas" in {
        get("schema/blup") { response =>
          response.code === 404
        }
      }

      "return the schema when it exists" in {
        val schema = "{}"
        post("schema/blup", schema) { response => ok }
        get("schema/blup") { response =>
          response.code === 200
          response.body.string === schema
        }
      }
    }

    "when receiving a POST request for a schema" in {
      "return a 400 for malformatted json" in {
        post("schema/blup", "{ 'test': 'bl ") { response =>
          response.code === 400
        }
      }

      "return a 200 and successful json response when a valid json schema is uploaded" in {
        val json = "{}"
        post("schema/blup", json) { response =>
          val expected = """{"action":"uploadSchema","id":"blup","status":"success"}"""
          response.code === 200
          response.body.string === expected
        }
      }
    }

    "when receiving a validation request" in {
      "return a 404 not found error if schema doesn't exist" in {
        post("validate/blipblup", "{}") { response =>
          response.code === 404
        }
      }
      
      "return a 400 Bad Request error if json is malformed" in {
        post("schema/blup", "{}") { response => ok }
        post("validate/blup", "{ 'test': 'bl ") { response =>
          response.code === 400
        }
      }

      "return a happy validation if schema has no rules" in {
        post("schema/blup", "{}") { response => ok }
        post("validate/blup", "{}") { response =>
          val expected = """{"action":"validateDocument","id":"blup","status":"success"}"""
          response.code === 200
          response.body.string === expected
        }
      }

      "return an error if json violates a minimum rule" in {
        val schema = """{ "properties": { "n": { "minimum": 1 } } }"""
        val json = """{ "n": 0 }"""
        post("schema/minimum", schema) { response => ok }
        post("validate/minimum", json) { response =>
          val expected = """{"action":"validateDocument","id":"minimum","status":"error","message":"Key n must be more than 1. Currently 0"}"""
          response.code === 200
          response.body.string === expected
        }
      }

      "return an error if json violates a maximum rule" in {
        val schema = """{ "properties": { "n": { "maximum": 3 } } }"""
        val json = """{ "n": 4 }"""
        post("schema/maximum", schema) { response => ok }
        post("validate/maximum", json) { response =>
          val expected = """{"action":"validateDocument","id":"maximum","status":"error","message":"Key n must be less than 3. Currently 4"}"""
          response.code === 200
          response.body.string === expected
        }
      }

      "return an error if json violates an integer type rule" in {
        val schema = """{ "properties": { "n": { "type": "integer" } } }"""
        val json = """{ "n": "4" }"""
        post("schema/integer", schema) { response => ok }
        post("validate/integer", json) { response =>
          val expected = """{"action":"validateDocument","id":"integer","status":"error","message":"Key n must be an integer"}"""
          response.code === 200
          response.body.string === expected
        }
      }

      "return an error if json violates an object type rule" in {
        val schema = """{ "properties": { "n": { "type": "object" } } }"""
        val json = """{ "n": "4" }"""
        post("schema/object", schema) { response => ok }
        post("validate/object", json) { response =>
          val expected = """{"action":"validateDocument","id":"object","status":"error","message":"Key n must be an object"}"""
          response.code === 200
          response.body.string === expected
        }
      }

      "return an error if json violates a string type rule" in {
        val schema = """{ "properties": { "n": { "type": "string" } } }"""
        val json = """{ "n": 4 }"""
        post("schema/string", schema) { response => ok }
        post("validate/string", json) { response =>
          val expected = """{"action":"validateDocument","id":"string","status":"error","message":"Key n must be a string"}"""
          response.code === 200
          response.body.string === expected
        }
      }

      "return an error if json is missing a required key" in {
        val schema = """{ "required": ["n"] }"""
        val json = """{ "a": 4 }"""
        post("schema/required", schema) { response => ok }
        post("validate/required", json) { response =>
          val expected = """{"action":"validateDocument","id":"required","status":"error","message":"required key 'n' is missing"}"""
          response.code === 200
          response.body.string === expected
        }
      }

      "return multiple errors if there's multiple schema violations" in {
        val schema = """{ "required": ["n"], "properties": { "m": { "type": "string" } } }"""
        val json = """{ "m": 4 }"""
        post("schema/multiple-errors", schema) { response => ok }
        post("validate/multiple-errors", json) { response =>
          val expected = """{"action":"validateDocument","id":"multiple-errors","status":"error","message":"required key 'n' is missing\nKey m must be a string"}"""
          response.code === 200
          response.body.string === expected
        }
      }

      "return an error if there's an error for a child key" in {
        val schema = """{ "properties": {"o": { "properties": { "p": { "type": "integer" } } } } }"""
        val json = """{ "o": { "p": "some text" } }"""
        post("schema/child-key", schema) { response => ok }
        post("validate/child-key", json) { response =>
          val expected = """{"action":"validateDocument","id":"child-key","status":"error","message":"Key o.p must be an integer"}"""
          response.code === 200
          response.body.string === expected
        }
      }

      "return success despite a type error for a missing key" in {
        val schema = """{ "properties": { "n": { "type": "string" } } }"""
        val json = "{}"
        post("schema/missing-key", schema) { response => ok }
        post("validate/missing-key", json) { response =>
          val expected = """{"action":"validateDocument","id":"missing-key","status":"success"}"""
          response.code === 200
          response.body.string === expected
        }
      }
    }
  }
}
