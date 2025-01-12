package tech.rocksavage.args

import org.rogach.scallop.{ScallopConf, Subcommand}

/**
 * A class that defines and parses command-line arguments using Scallop.
 * It supports two subcommands: `verilog` and `synth`, each with their own options.
 *
 * @param arguments The command-line arguments passed to the application.
 */
class Conf(arguments: Seq[String]) extends ScallopConf(arguments) {

  /**
   * Subcommand for generating Verilog.
   */
  object verilog extends Subcommand("verilog") {
    val mode = opt[String](
      default = Some("print"),
      validate = List("print", "write").contains(_),
      descr = "Mode of operation: 'print' to print Verilog to console, 'write' to write to a file."
    )
    val module = opt[String](
      required = true,
      descr = "The name of the module to generate Verilog for."
    )
    val configClass = opt[String](
      default = None,
      descr = "Classpath to the configuration class implementing ModuleConfig.",
      required = true
    )
  }

  /**
   * Subcommand for synthesizing the design.
   */
  object synth extends Subcommand("synth") {
    val module = opt[String](
      required = true,
      descr = "The name of the module to synthesize."
    )
    var techlib = opt[String](
      required = true,
      descr = "The technology library to use for synthesis."
    )
    var sta = opt[Boolean](
      default = Some(false),
      descr = "Enable static timing analysis (STA)."
    )
    val configClass = opt[String](
      default = None,
      descr = "Classpath to the configuration class implementing ModuleConfig.",
      required = true
    )
  }

  /**
   * Subcommand for running static timing analysis (STA).
   */
  object sta extends Subcommand("sta") {
    val module = opt[String](
      required = true,
      descr = "The name of the module to synthesize."
    )
    val configClass = opt[String](
      default = None,
      descr = "Classpath to the configuration class implementing ModuleConfig.",
      required = true
    )
    var techlib = opt[String](
      required = true,
      descr = "The technology library to use for synthesis."
    )
    val clockPeriod = opt[Double](
      required = true,
      descr = "The target clock period for the design."
    )
  }

  addSubcommand(verilog) // Register the verilog subcommand
  addSubcommand(synth)   // Register the synth subcommand
  addSubcommand(sta)     // Register the sta subcommand
  verify() // Validate the parsed arguments
}