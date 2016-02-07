AWS playground in Scala
=======================

Created from `play-scala` activator template:
```bash
> activator new scala-aws-pl play-scala
> cd scala-aws-pl
```

This project is a sample web service based on [Play](https://github.com/playframework/playframework) and deployed on [fake-aws](https://github.com/konrads/fake-aws).

(Contrived) data storage requirements
-------------------------------------
* dynamoDB:
  * user
  * spot
* s3:
  * portfolio - user to spot mappings
* redis:
  * redis.get(uid) => user
  * redis.smembers(uid:portfolioId) => Seq(ticker)
  * redis.get(ticker) => last spot

Setup `fake-aws`
----------------
```bash
# ensure fake-aws is installed
> data-gen dev resources/aws-template resources/aws-stage
> fake-aws refresh containers resources/aws-stage
> sbt test
```

To run
------
```bash
# ensure fake-aws is setup
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

Testing
-------
```
# ensure fake-aws is setup
> sbt test
```
