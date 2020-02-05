# Run & Test

To run the validator, make sure you have [SBT](https://www.scala-sbt.org/) installed, then run `sbt test` to run acceptance tests and `sbt run` to run the validation server on port 7777.

Once running, you can test the server using curl:

```sh
cat test-schema.json
cat config.json

curl -H "Accept: application/json" -d @test-schema.json localhost:7777/schema/blah -v
curl -H "Accept: application/json" http://localhost:7777/validate/blah -d @config.json -v
```

Remember to specify the header (`-H "Accept: application/json"`). Otherwise the server will return a 404.

# Known Shortcomings

I have limited time available to code this up so I'm publishing this repo with a couple of known shortcomings:

* There's no validation of the schema at all. If it's malformed, the server will fall flat on it's head. It's a great tragedy that the schema format can't be encoded by the schema format.
* The schema doesn't cover `booleans`, `floats` and anything else not specifically mentioned in the [doc](https://gist.github.com/goodits/20818f6ded767bca465a7c674187223e). It should be pretty easy to see how they can be added.
* Schemas are stored in local memory. Resetting the service will erase them. It's trivial (from a complexity point of view) to add in some persistence, but it adds a level of testing complexity I'm not willing to deal with.
* As if that wasn't bad enough, I'm using a normal map to store schema's when I should be using a concurrent one.
