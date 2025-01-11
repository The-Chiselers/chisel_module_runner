package tech.rocksavage

import tech.rocksavage.args.Conf
import tech.rocksavage.synth.Synth.synthesizeFromModuleName
import tech.rocksavage.synth.{SynthCommand, SynthConfig}

import java.io.File

object Synthesizer {
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
      val synthConfig = new SynthConfig(synthConf.techlib(), synthCommands)
      val synth = synthesizeFromModuleName(synthConfig, synthConf.module(), params)

      val synth_folder = new File(s"$build_folder/synth/$name")
      synth_folder.mkdirs()

      writeFile(s"$build_folder/synth/$name/${synthConf.module()}_net.v", synth.getSynthString)
      writeFile(s"$build_folder/synth/$name/log.txt", synth.getStdout)
      writeFile(s"$build_folder/synth/$name/gates.txt", synth.getGates.map(_.toString).getOrElse("No gates found"))
    }
  }

  private def writeFile(path: String, content: String): Unit = {
    val file = new File(path)
    file.createNewFile()
    val bw = new java.io.BufferedWriter(new java.io.FileWriter(file))
    bw.write(content)
    bw.close()
  }
}