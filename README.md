Created with:
```bash
> activator new scala-aws-pl play-scala
> cd scala-aws-pl
```

(Contrived) data storage:
- dynamoDB:
  - user
  - spot
- s3:
  - portfolio - user to spot mappings
- redis:
  - redis.get(uid) => user
  - redis.smembers(uid:portfolioId) => Seq(ticker)
  - redis.get(ticker) => last spot

Curl usage:
- GET token:
  export authtkn=$(curl localhost:9000/token -u user456:pwd456 | jq --raw-output '.["Authentication-Token"]')
- GET user:
  curl -i -H "Authentication-Token: $authtkn" http://localhost:9000/user
- GET myspots:
  curl -i -H "Authentication-Token: $authtkn" http://localhost:9000/myspots\?currency\=USD
- GET spots:
  curl -i -H "Authentication-Token: $authtkn" http://localhost:9000/spots\?tickers\=goog,aapl
- PUT spot:
  curl -i -H "Authentication-Token: $authtkn" -H "Content-Type: application/json" -XPUT -d '{"ticker":"goog","timestamp":12121212,"currency":"USD","price":1000.01}' http://localhost:9000/spot
