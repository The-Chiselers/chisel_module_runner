package tech.rocksavage

import chisel3._
import java.io.File
import tech.rocksavage.args.Conf
import tech.rocksavage.synth.Synth.{
  genVerilogFromModuleName,
  synthesizeFromModuleName
}
import scala.sys.exit

object Main {
  def main(args_array: Array[String]): Unit = {
    val conf         = new Conf(args_array.toIndexedSeq)
    val build_folder = new File("out")

    conf.subcommand match {
      case Some(conf.verilog) => {
        val moduleClass =
          Class.forName(conf.verilog.module()).asSubclass(classOf[ChiselModule])
        val defaultConfigs =
          moduleClass.getDeclaredConstructor().newInstance().defaultConfigs

        defaultConfigs.foreach { case (name, params) =>
          println(s"Generating Verilog for configuration: $name")
          val verilogString =
            genVerilogFromModuleName(conf.verilog.module(), params)
          conf.verilog.mode() match {
            case "print" => println(verilogString)
            case "write" => {
              val filename = s"${conf.verilog.module()}_$name.sv"
              val f        = new File(filename)
              val bw = new java.io.BufferedWriter(new java.io.FileWriter(f))
              bw.write(verilogString)
              bw.close()
            }
          }
        }
      }
      case Some(conf.synth) => {
        val moduleClass =
          Class.forName(conf.synth.module()).asSubclass(classOf[ChiselModule])
        val defaultConfigs =
          moduleClass.getDeclaredConstructor().newInstance().defaultConfigs

        defaultConfigs.foreach { case (name, params) =>
          println(s"Synthesizing configuration: $name")
          val synthCommands = List(
            tech.rocksavage.synth.SynthCommand.Synth,
            tech.rocksavage.synth.SynthCommand.Flatten,
            tech.rocksavage.synth.SynthCommand.Dfflibmap,
            tech.rocksavage.synth.SynthCommand.Abc,
            tech.rocksavage.synth.SynthCommand.OptCleanPurge,
            tech.rocksavage.synth.SynthCommand.Write,
            tech.rocksavage.synth.SynthCommand.Stat
          )
          val synthConfig = new tech.rocksavage.synth.SynthConfig(
            conf.synth.techlib(),
            synthCommands
          )
          val synth =
            synthesizeFromModuleName(synthConfig, conf.synth.module(), params)
          val synth_folder = new File(s"$build_folder/synth/$name")
          synth_folder.mkdirs()
          val net_file =
            new File(s"$build_folder/synth/$name/${conf.synth.module()}_net.v")
          net_file.createNewFile()
          val net_bw =
            new java.io.BufferedWriter(new java.io.FileWriter(net_file))
          net_bw.write(synth.getSynthString)
          net_bw.close()
          val log_file = new File(s"$build_folder/synth/$name/log.txt")
          log_file.createNewFile()
          val log_bw =
            new java.io.BufferedWriter(new java.io.FileWriter(log_file))
          log_bw.write(synth.getStdout)
          log_bw.close()
          val gates_file = new File(s"$build_folder/synth/$name/gates.txt")
          gates_file.createNewFile()
          val gates_bw =
            new java.io.BufferedWriter(new java.io.FileWriter(gates_file))
          val gates_str: String = synth.getGates match {
            case Some(gates) => gates.toString
            case None        => "No gates found"
          }
          gates_bw.write(gates_str)
          gates_bw.close()
        }
      }
      case _ => println("No subcommand given")
    }
  }
}
