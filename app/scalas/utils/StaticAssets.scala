package scalas.utils

import controllers.routes
import play.mvc.Controller

object StaticAssets extends Controller {
  def getUrl(file: String): String = {
    routes.Assets.versioned(file).toString
  }
}
