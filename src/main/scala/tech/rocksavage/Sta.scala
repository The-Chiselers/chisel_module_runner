package tech.rocksavage

import chisel3._
import tech.rocksavage.args.Conf
import tech.rocksavage.synth.Synth.synthesizeFromModuleName
import tech.rocksavage.synth.{SynthCommand, SynthConfig}
import java.io.{File, PrintWriter}
import java.lang.reflect.Field
import sys.process._

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
      val sdcContent = generateSdc(conf, build_folder, name, params)

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
   * @param configName     The name of the configuration.
   * @param params         The parameters to instantiate the module.
   */
  def generateSdc(conf: Conf, build_folder: File, configName: String, params: Any): String = {
    val staConf = conf.sta
    val name = staConf.module().split('.').last
    println(s"Generating SDC for configuration: $name")
    // Load the module dynamically
    val clazz = Class.forName(staConf.module()).asSubclass(classOf[RawModule])
    val constructors = clazz.getConstructors
    var moduleInstance: Option[RawModule] = None
    for (c <- constructors) {
      try {
        // Unpack the Seq into individual arguments
        val unpackedParams = params match {
          case seq: Seq[_] => seq.asInstanceOf[Seq[Object]] // Explicitly cast to Seq[Object]
          case _ => Seq(params).asInstanceOf[Seq[Object]] // Wrap single param in a Seq
        }
        moduleInstance = Some(c.newInstance(unpackedParams: _*).asInstanceOf[RawModule])
      } catch {
        case e: java.lang.IllegalArgumentException =>
          println(s"Constructor $c failed: $e")
      }
    }
    moduleInstance match {
      case Some(module) =>
        // Extract IO signals
        val ioField = module.getClass.getDeclaredField("io")
        ioField.setAccessible(true)
        val ioSignals = ioField.get(module).asInstanceOf[Bundle].elements.toList
        // Generate SDC content
        generateSdcContent(ioSignals, staConf.clockPeriod()) // Generate SDC content
      case None => throw new RuntimeException(s"Failed to instantiate module ${staConf.module()} with params $params")
    }
  }

  /** Generates the SDC content based on the IO signals and clock period.
   *
   * @param ioSignals   A list of IO signals with their names and types.
   * @param clockPeriod The clock period in nanoseconds.
   * @return The generated SDC content as a string.
   */
  private def generateSdcContent(ioSignals: List[(String, Data)], clockPeriod: Double): String = {
    val clockSignal = ioSignals.find(_._1 == "clock").getOrElse(throw new RuntimeException("Clock signal not found"))
    val sdcBuilder = new StringBuilder
    // Create clock constraint
    sdcBuilder.append(s"create_clock -period $clockPeriod -waveform {0 ${clockPeriod / 2}} ${clockSignal._1}\n")
    // Add input/output delays
    ioSignals.foreach { case (name, data) =>
      if (name != "clock") {
        // Use reflection to access the private `direction` field
        val directionField: Field = data.getClass.getDeclaredField("direction")
        directionField.setAccessible(true)
        val direction = directionField.get(data).asInstanceOf[ActualDirection]
        direction match {
          case ActualDirection.Input =>
            sdcBuilder.append(s"set_input_delay -clock ${clockSignal._1} 1.0 {$name}\n")
          case ActualDirection.Output =>
            sdcBuilder.append(s"set_output_delay -clock ${clockSignal._1} 1.0 {$name}\n")
          case _ => // Ignore other directions (e.g., Bidirectional)
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