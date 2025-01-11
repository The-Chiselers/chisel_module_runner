package tech.rocksavage

import tech.rocksavage.args.Conf
import tech.rocksavage.synth.Synth.genVerilogFromModuleName
import java.io.File

/**
 * An object responsible for generating Verilog code based on the provided configurations.
 */
object VerilogGenerator {

  /**
   * Generates Verilog code for the specified module and configurations.
   *
   * @param conf The parsed command-line arguments.
   * @param defaultConfigs A map of configuration names to their corresponding parameters.
   */
  def generate(conf: Conf, defaultConfigs: Map[String, Any]): Unit = {
    val verilogConf = conf.verilog
    defaultConfigs.foreach { case (name, params) =>
      println(s"Generating Verilog for configuration: $name")
      val verilogString = genVerilogFromModuleName(verilogConf.module(), params) // Generate Verilog

      verilogConf.mode() match {
        case "print" => println(verilogString) // Print Verilog to console
        case "write" =>
          val filename = s"${verilogConf.module()}_$name.sv" // Define the output filename
          val f = new File(filename)
          val bw = new java.io.BufferedWriter(new java.io.FileWriter(f))
          bw.write(verilogString) // Write Verilog to file
          bw.close()
      }
    }
  }
}