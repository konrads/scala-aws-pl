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
