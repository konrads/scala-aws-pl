AWS playground in Scala
=======================

Build status (master): [![Build Status](https://travis-ci.org/konrads/scala-aws-pl.svg?branch=master)](https://travis-ci.org/konrads/scala-aws-pl)

The purposes of this project:

* to embed a local set of AWS services for the benefit of DEV build and CI. This is achieved with [fake-aws](https://github.com/konrads/fake-aws) which provides a set of docker containers for local DynamoDB, S3 and ElastiCache (Redis). As per [.travis.yml](.travis.yml), the setup consists of:
  * install `fake-aws`
  * run `data-gen` to generate data from the templates [resources/aws-template](resources/aws-template) to `resources/aws-stage`
  * run `fake-aws` to setup and populate the docker containers from `resources/aws-stage` 
* to establish a set of best practices for development of a web service on [Play](https://github.com/playframework/playframework)
  * wrapping of [cats](https://github.com/typelevel/cats) library via [Cats.scala](app/aws_pl/util/Cats.scala)
    * `XorT[Future, Err, Result]` for Action results
    * `OptionT[T]` for repo/data store results
  * Err/Exception handling
  * dev/uat/prod deployment configuration
  * parameter validation performed by `Play`
  * logging
* to create pluggable components (✓ = implemented, ✗ = not yet)
  * authentication - via [AuthenticatedAction.scala](app/aws_pl/util/AuthenticatedAction.scala) ✓
  * authorization ✗
  * error handling - via [AuthenticatedAction.scala](app/aws_pl/util/AuthenticatedAction.scala) ✓
  * parameter validation  - via [ParamValidator.scala](app/aws_pl/validate/ParamValidator.scala) ✗
  * telemetry/metrics - via [AuthenticatedAction.scala](app/aws_pl/metrics/MetricsFilter.scala) (applied to all reqs) ✗
  * rate limiting - via [RateLimiter.scala](app/aws_pl/ratelimit/RateLimiter.scala) (applied to authorized reqs) ✗
  * pagination - via [AuthenticatedAction.scala](app/aws_pl/util/PaginationAction.scala) (applied to selected reqs) ✗

A sample web service based on [Play](https://github.com/playframework/playframework) and deployed on [fake-aws](https://github.com/konrads/fake-aws).
Exercises `fake-aws`'s `dynamodb`, `s3` and `redis`.

(Contrived) data storage requirements
-------------------------------------
* dynamoDB:
  * user data
  * spot data
* s3:
  * portfolio, ie. mappings between user and spot
* redis:
  * some user data
  * some portfolio data
  * some spot data

Setup fake-aws
--------------
```bash
# ensure fake-aws is installed
> data-gen dev resources/aws-template resources/aws-stage
> fake-aws refresh containers resources/aws-stage
> sbt test
```

To run
------
```bash
# ensure fake-aws and jq are installed/setup
> sbt run &
# GET token:
> export authtkn=$(curl localhost:9000/authtkn -u user456:pwd456 | jq --raw-output '.["Authentication-Token"]')
# GET user:
> curl -i -H "Authentication-Token: $authtkn" http://localhost:9000/user
# GET myspots:
> curl -i -H "Authentication-Token: $authtkn" http://localhost:9000/myspots\?currency\=USD
# GET spots:
> curl -i -H "Authentication-Token: $authtkn" http://localhost:9000/spots\?tickers\=goog,aapl
# PUT spot:
> curl -i -H "Authentication-Token: $authtkn" -H "Content-Type: application/json" -XPUT -d '{"ticker":"goog","currency":"USD","price":1000.01}' http://localhost:9000/spot
```

To test
-------
```
# ensure fake-aws is setup
> sbt test
```

To run in prod
--------------
```
> sbt assembly
# config file's "includes" pull in app.conf irrespective of config file location, eg. 
> cp conf/prod.conf ./prod.conf
# ensure /var/scala-aws-pl/config/private.conf contains valid value for aws.redis.host
> java -Dconfig.file=./prod.conf -jar target/scala-2.11/scala-aws-pl-assembly-1.0-SNAPSHOT.jar
```
