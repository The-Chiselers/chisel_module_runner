package tech.rocksavage.args

import org.rogach.scallop.{ScallopConf, Subcommand}

class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {
  // Define the verilog subcommand
  object verilog extends Subcommand("verilog") {
    val mode = opt[String](
      default = Some("print"),
      validate = List("print", "write").contains(_),
      descr = "Mode for Verilog generation: 'print' or 'write'"
    )
    val module = opt[String](
      required = true,
      descr =
        "Fully qualified name of the Chisel module to generate Verilog for"
    )
    val config = opt[String](
      default = None,
      descr = "JSON configuration for the module parameters"
    )
  }

  // Define the synth subcommand
  object synth extends Subcommand("synth") {
    val module = opt[String](
      required = true,
      descr = "Fully qualified name of the Chisel module to synthesize"
    )
    val techlib = opt[String](
      required = true,
      descr = "Technology library to use for synthesis"
    )
    val sta = opt[Boolean](
      default = Some(false),
      descr = "Enable static timing analysis"
    )
    val config = opt[String](
      default = None,
      descr = "JSON configuration for the module parameters"
    )
  }

  // Add the subcommands to the main configuration
  addSubcommand(verilog)
  addSubcommand(synth)

  // Verify the configuration
  verify()
}
