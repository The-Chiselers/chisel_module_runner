package tech.rocksavage

import tech.rocksavage.args.Conf
import tech.rocksavage.synth.Synth.genVerilogFromModuleName

import java.io.File

object VerilogGenerator {
  def generate(conf: Conf, defaultConfigs: Map[String, Any]): Unit = {
    val verilogConf = conf.verilog
    defaultConfigs.foreach { case (name, params) =>
      println(s"Generating Verilog for configuration: $name")
      val verilogString = genVerilogFromModuleName(verilogConf.module(), params)
      verilogConf.mode() match {
        case "print" => println(verilogString)
        case "write" =>
          val filename = s"${verilogConf.module()}_$name.sv"
          val f = new File(filename)
          val bw = new java.io.BufferedWriter(new java.io.FileWriter(f))
          bw.write(verilogString)
          bw.close()
      }
    }
  }
}