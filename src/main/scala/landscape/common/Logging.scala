package landscape.common

import org.slf4j.LoggerFactory

/**
 * author mikwie
 *
 */
trait Logging {

  protected val logger = LoggerFactory.getLogger(getClass)

}
