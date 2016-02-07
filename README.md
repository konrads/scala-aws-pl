AWS playground in Scala
=======================

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
