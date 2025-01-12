package tech.rocksavage

import chisel3._
import java.io.File
import tech.rocksavage.args.Conf
import tech.rocksavage.traits.ModuleConfig
import tech.rocksavage.synth.Synth.{genVerilogFromModuleName, synthesizeFromModuleName}
import scala.sys.exit

/**
 * The main entry point for the application.
 * This object handles command-line arguments and delegates tasks to either the VerilogGenerator or Synthesizer.
 */
object Main {

  /**
   * The main method that processes command-line arguments and executes the appropriate subcommand.
   *
   * @param args_array Command-line arguments passed to the application.
   */
  def main(args_array: Array[String]): Unit = {
    val conf = new Conf(args_array.toIndexedSeq) // Parse command-line arguments
    val defaultConfigs = ConfigLoader.loadConfigs(conf) // Load configurations based on the subcommand
    val build_folder = new File("out") // Define the output directory

    conf.subcommand match {
      case Some(conf.verilog) => VerilogGenerator.generate(conf, defaultConfigs) // Generate Verilog
      case Some(conf.synth)   => Synthesizer.synthesize(conf, defaultConfigs, build_folder) // Synthesize the design
      case Some(conf.sta)     => Sta.sta(conf, defaultConfigs, build_folder) // Perform Timing Analysis
      case _                  => println("No subcommand given") // Handle invalid or missing subcommand
    }
  }
}