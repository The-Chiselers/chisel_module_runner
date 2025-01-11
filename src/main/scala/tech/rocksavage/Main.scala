package tech.rocksavage

import chisel3._
import java.io.File
import tech.rocksavage.args.Conf
import tech.rocksavage.traits.ModuleConfig
import tech.rocksavage.synth.Synth.{genVerilogFromModuleName, synthesizeFromModuleName}
import scala.sys.exit

object Main {
  def main(args_array: Array[String]): Unit = {
    val conf = new Conf(args_array.toIndexedSeq)
    val defaultConfigs = ConfigLoader.loadConfigs(conf)
    val build_folder = new File("out")

    conf.subcommand match {
      case Some(conf.verilog) => VerilogGenerator.generate(conf, defaultConfigs)
      case Some(conf.synth)   => Synthesizer.synthesize(conf, defaultConfigs, build_folder)
      case _                  => println("No subcommand given")
    }
  }
}