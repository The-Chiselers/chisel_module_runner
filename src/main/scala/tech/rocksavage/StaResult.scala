package tech.rocksavage

/**
 * The `StaResult` class represents the result of a static timing analysis run, including the estimated slack.
 *
 * @param slack The estimated slack in the synthesized design.
 */
class StaResult(slack: Float) {

  /**
   * Returns the estimated slack in the synthesized design.
   *
   * @return The slack.
   */
  def getSlack: Float = slack

}