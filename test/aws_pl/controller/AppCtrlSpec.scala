package aws_pl.controller

import aws_pl.util.{ValidateHelper, FakeAwsApp}
import play.api.test.Helpers._

/**
  * Tests AppCtrl by:
  * - getting value and validating inline
  * - delegating single validation to helper
  * - delegating bulk validations to helper
  */
class AppCtrlSpec extends ValidateHelper {
  "AppCtrl" should {
    "authenticate known user" in new FakeAwsApp {
      getToken("user123", "pwd123") must beSome.which(_.length > 0)
    }

    "authenticate unknown user" in new FakeAwsApp {
      getToken("bogus_user", "bogus_password") must beNone
    }

    "get" in {
      br append validateGets(
        "user"                        -> ("/user", "user123", "pwd123", OK, Some("""{"type":"trader","uid":"user123"}""")),
        "my spots"                    -> ("/myspots?currency=USD", "user123", "pwd123", OK, Some("""[{"ticker":"goog","timestamp":xxx,"currency":"USD","price":683.55},{"ticker":"blt","timestamp":xxx,"currency":"USD","price":1029.5725},{"ticker":"rio","timestamp":xxx,"currency":"USD","price":29.4}]""")),
        "spots"                       -> ("/fx?currency=AUD", "user123", "pwd123", OK, Some("""{"GBP":2.05,"USD":1.45}""")),
        "invalid path"                -> ("/bogus_path", "user123", "pwd123", NOT_FOUND, None),
        "myspots with bogus currency" -> ("/myspots?currency=BOGUS_CURRENCT", "user456", "pwd456", FAILED_DEPENDENCY, None)
      )
    }

    "put-get fx" in new FakeAwsApp {
      validatePut("/spot", "user456", "pwd456", """{"ticker":"aapl","currency":"USD","price":95.05}""", OK)
      validateGet("/spots?tickers=aapl", "user456", "pwd456", OK, Some("""[{"ticker":"aapl","timestamp":xxx,"currency":"USD","price":95.05}]"""))
    }
  }
}
