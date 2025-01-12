package tech.rocksavage

import chisel3._
import chisel3.stage.{ChiselCircuitAnnotation, DesignAnnotation}
import circt.stage.{CIRCTTarget, CIRCTTargetAnnotation, ChiselStage}
import tech.rocksavage.args.Conf
import tech.rocksavage.synth.Synth.synthesizeFromModuleName
import tech.rocksavage.synth.{SynthCommand, SynthConfig}

import java.io.{File, PrintWriter}
import java.lang.reflect.Field
import sys.process._
//import chisel3.stage.ChiselStage
import chisel3.stage.ChiselGeneratorAnnotation
import firrtl.AnnotationSeq

/** An object responsible for synthesizing the design based on the provided configurations. */
object Sta {

  /** Uses Timing Analysis on the design for the specified module and configurations.
   *
   * @param conf The parsed command-line arguments.
   * @param defaultConfigs A map of configuration names to their corresponding parameters.
   * @param build_folder The directory where synthesis output will be stored.
   */
  def sta(conf: Conf, defaultConfigs: Map[String, Any], build_folder: File): Unit = {
    val staConf = conf.sta
    defaultConfigs.foreach { case (name, params) =>
      println(s"Synthesizing configuration: $name")
      val synthCommands = List(
        SynthCommand.Synth,
        SynthCommand.Flatten,
        SynthCommand.Dfflibmap,
        SynthCommand.Abc,
        SynthCommand.OptCleanPurge,
        SynthCommand.Write,
        SynthCommand.Stat
      )
      val synthConfig = new SynthConfig(staConf.techlib(), synthCommands) // Create synthesis configuration
      val synth = synthesizeFromModuleName(synthConfig, staConf.module(), params) // Perform synthesis
      val sdcContent = generateSdc(conf, build_folder, synth.getSynthString, name, params)

      // Write SDC to file
      val sdcFile = new File(s"$build_folder/sdc/$name/${staConf.module()}.sdc")
      sdcFile.getParentFile.mkdirs()
      val pw = new PrintWriter(sdcFile)
      pw.write(sdcContent)
      pw.close()

      // Generate TCL file for STA
      val tclContent = generateTcl(staConf.module(), build_folder, name)
      val tclFile = new File(s"$build_folder/sta/$name/sta.tcl")
      tclFile.getParentFile.mkdirs()
      val tclWriter = new PrintWriter(tclFile)
      tclWriter.write(tclContent)
      tclWriter.close()

      // Perform STA
      val staResult = performSta(staConf.module(), build_folder, name)

      // Write STA results to files
      val sta_folder = new File(s"$build_folder/sta/$name")
      sta_folder.mkdirs()
      writeFile(s"$sta_folder/slack.txt", staResult.getSlack.toString)
    }
  }

  /** Generates an SDC file for the specified module and configurations.
   *
   * @param conf           The parsed command-line arguments.
   * @param build_folder   The directory where SDC files will be stored.
   * @param netlist        The Verilog netlist content.
   * @param configName     The name of the configuration.
   * @param params         The parameters to instantiate the module.
   */
  def generateSdc(conf: Conf, build_folder: File, netlist: String, configName: String, params: Any): String = {
    val staConf = conf.sta
    val name = staConf.module().split('.').last
    println(s"Generating SDC for configuration: $name")

    // Parse IO signals from the Verilog netlist
    val ioSignals = parseIoSignalsFromVerilog(netlist)

    // Generate SDC content
    generateSdcContent(ioSignals, staConf.clockPeriod())
  }

  private def parseIoSignalsFromVerilog(verilogContent: String): List[(String, String)] = {
    // Regex to match input/output declarations in Verilog
    val ioPattern = """(input|output)\s+(?:\[.*?\])?\s*(\w+)""".r

    // Extract IO signals
    val ioSignals = ioPattern.findAllMatchIn(verilogContent).map { m =>
      val direction = m.group(1) // "input" or "output"
      val signalName = m.group(2) // Signal name
      (signalName, direction)
    }.toList

    ioSignals
  }

  /** Generates the SDC content based on the IO signals and clock period.
   *
   * @param ioSignals   A list of IO signals with their names and types.
   * @param clockPeriod The clock period in nanoseconds.
   * @return The generated SDC content as a string.
   */
  private def generateSdcContent(ioSignals: List[(String, String)], clockPeriod: Double): String = {
    val clockSignal = ioSignals.find(_._1 == "clock").getOrElse(throw new RuntimeException("Clock signal not found"))
    val sdcBuilder = new StringBuilder

    // Create clock constraint
    sdcBuilder.append(s"create_clock -period $clockPeriod -waveform {0 ${clockPeriod / 2}} ${clockSignal._1}\n")

    // Add input/output delays
    ioSignals.foreach { case (name, direction) =>
      if (name != "clock") {
        direction match {
          case "input" =>
            sdcBuilder.append(s"set_input_delay -clock ${clockSignal._1} 1.0 {$name}\n")
          case "output" =>
            sdcBuilder.append(s"set_output_delay -clock ${clockSignal._1} 1.0 {$name}\n")
          case _ => // Ignore other directions (e.g., inout)
        }
      }
    }

    sdcBuilder.toString()
  }

  /** Generates the TCL file content for STA.
   *
   * @param moduleName The name of the module.
   * @param build_folder The directory where the TCL file will be stored.
   * @param configName The name of the configuration.
   * @return The generated TCL content as a string.
   */
  private def generateTcl(moduleName: String, build_folder: File, configName: String): String = {
    val top = moduleName.split('.').last
    s"""
       |set top $top
       |set projectRoot ${build_folder.getAbsolutePath}
       |set buildRoot ${build_folder.getAbsolutePath}
       |read_liberty $build_folder/synth/stdcells.lib
       |read_verilog $build_folder/synth/$configName/${top}_net.v
       |link_design $top
       |source $build_folder/sta/$configName/${top}.sdc
       |check_setup
       |report_checks
       |""".stripMargin
  }

  /** Performs STA using the generated TCL and SDC files.
   *
   * @param moduleName The name of the module.
   * @param build_folder The directory where the STA files are stored.
   * @param configName The name of the configuration.
   * @return The STA result.
   */
  private def performSta(moduleName: String, build_folder: File, configName: String): StaResult = {
    val top = moduleName.split('.').last
    val staFolder = s"$build_folder/sta/$configName"
    val timingRpt = s"$staFolder/timing.rpt"

    // Run STA using the TCL file
    val staCommand = s"sta -no_init -no_splash -exit $staFolder/sta.tcl"
    val exitCode = staCommand.!

    if (exitCode != 0) {
      throw new RuntimeException(s"STA process failed with exit code $exitCode")
    }

    // Parse the timing report to extract slack
    val slack = scala.io.Source.fromFile(timingRpt).getLines()
      .find(_.contains("slack"))
      .map(_.split("\\s+").last.toFloat)
      .getOrElse(0.0f)

    new StaResult(slack)
  }

  /** Writes the given content to a file at the specified path.
   *
   * @param path The path where the file will be created.
   * @param content The content to write to the file.
   */
  private def writeFile(path: String, content: String): Unit = {
    val file = new File(path)
    file.createNewFile()
    val bw = new java.io.BufferedWriter(new java.io.FileWriter(file))
    bw.write(content)
    bw.close()
  }
}