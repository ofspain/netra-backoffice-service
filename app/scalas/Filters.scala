package scalas

import javax.inject.Inject
import play.api.http.DefaultHttpFilters
import play.filters.csrf.CSRFFilter
import play.filters.headers.SecurityHeadersFilter
import org.pac4j.play.filters.SecurityFilter
import security.SecurityConfig

class Filters @Inject() (
    securityFilter: SecurityFilter,
    csrfFilter: CSRFFilter,
  securityHeaders: SecurityHeadersFilter
) extends DefaultHttpFilters(securityFilter, csrfFilter, securityHeaders){
  // Add debug to verify filter is being used
  println("✅ Filters class initialized with custom SecurityFilter "+securityFilter.getClass.getCanonicalName)
}
