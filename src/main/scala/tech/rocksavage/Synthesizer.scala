package tech.rocksavage

import tech.rocksavage.args.Conf
import tech.rocksavage.synth.Synth.synthesizeFromModuleName
import tech.rocksavage.synth.{SynthCommand, SynthConfig}
import java.io.File

/**
 * An object responsible for synthesizing the design based on the provided configurations.
 */
object Synthesizer {

  /**
   * Synthesizes the design for the specified module and configurations.
   *
   * @param conf The parsed command-line arguments.
   * @param defaultConfigs A map of configuration names to their corresponding parameters.
   * @param build_folder The directory where synthesis output will be stored.
   */
  def synthesize(conf: Conf, defaultConfigs: Map[String, Any], build_folder: File): Unit = {
    val synthConf = conf.synth
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
      val synthConfig = new SynthConfig(synthConf.techlib(), synthCommands) // Create synthesis configuration
      val synth = synthesizeFromModuleName(synthConfig, synthConf.module(), params) // Perform synthesis

      val synth_folder = new File(s"$build_folder/synth/$name") // Define the output directory
      synth_folder.mkdirs()

      // Write synthesis results to files
      writeFile(s"$build_folder/synth/$name/${synthConf.module()}_net.v", synth.getSynthString)
      writeFile(s"$build_folder/synth/$name/log.txt", synth.getStdout)
      writeFile(s"$build_folder/synth/$name/gates.txt", synth.getGates.map(_.toString).getOrElse("No gates found"))
    }
  }

  /**
   * Writes the given content to a file at the specified path.
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