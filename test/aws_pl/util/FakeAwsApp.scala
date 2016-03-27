package aws_pl.util

import aws_pl.play.Components
import play.api.ApplicationLoader.Context
import play.api.test.WithApplicationLoader
import play.api.{ApplicationLoader, Configuration, Logger}

/**
  * Fake app, exposes components for testing.
  */
class FakeAwsApp(reconfig: Configuration => Configuration = x => x) extends WithComponents(new AppLoader(reconfig))

/**
  * Wrapper ApplicationLoader that exposes components.
  */
class WithComponents(appLoader: AppLoader) extends WithApplicationLoader(appLoader) {
  def components: Components = appLoader.components
}

/**
  * Reconfigures and loads app.
  */
class AppLoader(reconfig: Configuration => Configuration) extends ApplicationLoader {
  var components: Components = _
  def load(ctx: Context) = {
    val ctx2 = ctx.copy(initialConfiguration = reconfig(ctx.initialConfiguration))
    Logger.configure(ctx2.environment)
    components = new Components(ctx2)
    components.application
  }
}
