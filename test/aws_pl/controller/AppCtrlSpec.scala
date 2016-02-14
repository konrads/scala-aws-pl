package aws_pl.controller

import org.specs2.mutable.Specification
import play.api.test.Helpers._

class AppCtrlSpec extends Specification {

  "AppCtrl" should {
    "authenticate known user" in new FakeAwsApp {
      getToken("user123", "pwd123") must beSome.which(_.length > 0)
    }
    "authenticate unknown user" in new FakeAwsApp {
      getToken("bogus_user", "bogus_password") must beNone
    }
    "get user" in new FakeAwsApp {
      val (statusCode, respContents) = get("/user", "user123", "pwd123")
      statusCode must equalTo(OK)
      respContents must equalTo("""{"uid":"user123","type":"trader"}""")
    }
    "get myspots from s3" in new FakeAwsApp {
      val (statusCode, respContents) = get("/myspots?currency=USD", "user123", "pwd123")
      statusCode must equalTo(OK)
      sanitize(respContents) must equalTo("""[{"ticker":"goog","timestamp":xxx,"currency":"USD","price":683.55},{"ticker":"blt","timestamp":xxx,"currency":"USD","price":1029.5725},{"ticker":"rio","timestamp":xxx,"currency":"USD","price":29.4}]""")
    }
    "get myspots from redis" in new FakeAwsApp {
      val (statusCode, respContents) = get("/myspots?currency=USD", "user456", "pwd456")
      statusCode must equalTo(OK)
      sanitize(respContents) must equalTo("""[{"ticker":"blt","timestamp":xxx,"currency":"USD","price":1029.5725},{"ticker":"rio","timestamp":xxx,"currency":"USD","price":29.4}]""")
    }
    "get spots" in new FakeAwsApp {
      val (statusCode, respContents) = get("/spots?tickers=rio,goog", "user123", "pwd123")
      statusCode must equalTo(OK)
      sanitize(respContents) must equalTo("""[{"ticker":"rio","timestamp":xxx,"currency":"AUD","price":42},{"ticker":"goog","timestamp":xxx,"currency":"USD","price":683.55}]""")
    }
    "get fx" in new FakeAwsApp {
      val (statusCode, respContents) = get("/fx?currency=AUD", "user123", "pwd123")
      statusCode must equalTo(OK)
      respContents must equalTo("""{"GBP":2.05,"USD":1.45}""")
    }
    "put-get fx" in new FakeAwsApp {
      val putStatusCode = put("/spot", "user456", "pwd456", """{"ticker":"aapl","currency":"USD","price":95.05}""")
      putStatusCode must equalTo(OK)
      val (getStatusCode, getRespContents) = get("/spots?tickers=aapl", "user456", "pwd456")
      getStatusCode must equalTo(OK)
      sanitize(getRespContents) must equalTo("""[{"ticker":"aapl","timestamp":xxx,"currency":"USD","price":95.05}]""")
    }
    // errors
    "get invalid path" in new FakeAwsApp {
      val (statusCode, _) = get("/bogus_path", "user123", "pwd123")
      statusCode must equalTo(NOT_FOUND)
    }
    "get myspots with bogus currency" in new FakeAwsApp {
      val (statusCode, respContents) = get("/myspots?currency=BOGUS_CURRENCT", "user456", "pwd456")
      statusCode must equalTo(FAILED_DEPENDENCY)
    }
  }
}
