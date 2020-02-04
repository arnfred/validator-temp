package validator

import org.specs2._
import specification.{Before, Scope}

import okhttp3._

class PlanSpec extends mutable.Specification with unfiltered.specs2.netty.Served {

  def setup = { _.handler(Plan) }

  val JSON = MediaType.get("application/json; charset=utf-8");

  val http = new OkHttpClient()

  "The validator" should {
    "when receiving a GET request for a schema" in {
      "return a 404 for non-existant schemas" in {
        val url = host.toString + "schema/blup"
        val request = new Request.Builder()
          .url(url)
          .header("Accept", "application/json")
          .build()
        val response = http.newCall(request).execute()
        response.code === 404
        response.close()
        ok
      }
      "return the schema when it exists" in {
        val url = host.toString + "schema/blup"
        val schema = "{}"
        val body = RequestBody.create(schema, JSON);
        val postRequest = new Request.Builder()
          .url(url)
          .header("Accept", "application/json")
          .post(body)
          .build()
        http.newCall(postRequest).execute()

        val getRequest = new Request.Builder()
          .url(url)
          .header("Accept", "application/json")
          .build()
        val response = http.newCall(getRequest).execute()
        response.code === 200
        response.body.string === schema
      }
    }
    "when receiving a POST request for a schema" in {
      "return a 400 for malformatted json" in {
        val url = host.toString + "schema/blup"
        val body = RequestBody.create("{ 'test': 'bl ", JSON);
        val request = new Request.Builder()
          .url(url)
          .header("Accept", "application/json")
          .post(body)
          .build()
        val response = http.newCall(request).execute()
        response.code === 400
        response.close()
        ok
      }
      "return a 200 and successful json response when a valid json schema is uploaded" in {
        val url = host.toString + "schema/blup"
        val body = RequestBody.create("{}", JSON);
        val request = new Request.Builder()
          .url(url)
          .header("Accept", "application/json")
          .post(body)
          .build()
        val response = http.newCall(request).execute()
        println(response)
        response.code === 200
        response.body.string === """{"action":"uploadSchema","id":"blup","status":"success"}"""
        response.close()
        ok
      }
    }
  }
}
