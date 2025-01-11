package tech.rocksavage.args
import org.rogach.scallop.{ScallopConf, Subcommand}
import org.scalatest.matchers.should.Matchers.convertToAnyShouldWrapper
class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  object verilog extends Subcommand("verilog") {
    val mode = opt[String](
      default = Some("print"),
      validate = List("print", "write").contains(_)
    )
    val module = opt[String](required = true)
    val configClass = opt[String](
      default = None,
      descr = "Classpath to the configuration class implementing ConfigTrait",
      required = true
    )
  }
  object synth extends Subcommand("synth") {
    val module  = opt[String](required = true)
    var techlib = opt[String](required = true)
    var sta     = opt[Boolean](default = Some(false))
    val configClass = opt[String](
      default = None,
      descr = "Classpath to the configuration class implementing ConfigTrait",
      required = true
    )
  }
  addSubcommand(verilog)
  addSubcommand(synth)
  verify()
}
