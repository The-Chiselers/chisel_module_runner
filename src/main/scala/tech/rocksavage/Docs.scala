package tech.rocksavage

import tech.rocksavage.args.Conf

import java.io.File
import scala.sys.exit

/** */
object Docs {

    /** @param conf
      *   The parsed command-line arguments.
      * @param defaultConfigs
      *   A map of configuration names to their corresponding parameters.
      * @param build_folder
      *   The directory where synthesis output will be stored.
      */
    def docs(
        conf: Conf,
        defaultConfigs: Map[String, Any],
        build_folder: File
    ): Unit = {
//        Synthesizer.synthesize(conf, defaultConfigs, build_folder)
//        Sta.sta(conf, defaultConfigs, build_folder)

        val synth_files: Map[String, File] =
            new File(s"$build_folder/synth/").listFiles
                .filter(_.toString.endsWith("gates.txt"))
                .map(file => file.toString.split("/").last -> file)
                .toMap

        println(synth_files)
        exit(0)

        val gate_count_map: Map[String, String] = synth_files.map { entry =>
            val file  = entry._2
            val name  = entry._1
            val gates = scala.io.Source.fromFile(file).toString()
            name -> gates
        }.toMap

        val sta_files: Map[String, File] =
            new File(s"$build_folder/sta/").listFiles
                .filter(_.toString.endsWith("slack.txt"))
                .map(file => file.toString.split("/").last -> file)
                .toMap

        val wns_map: Map[String, String] = sta_files.map { entry =>
            val file = entry._2
            val name = entry._1
            val wns  = scala.io.Source.fromFile(file).toString()
            name -> wns
        }.toMap

        val joined_map: Map[String, (String, String)] = gate_count_map.map {
            case (name, gates) =>
                name -> (gates, wns_map(name))
        }
        for ((name, (gates, wns)) <- joined_map) {
            val content = s"""|Module: $name
                              |Gates: $gates
                              |WNS: $wns
                              |""".stripMargin
            writeFile(s"$build_folder/docs/$name.txt", content)
        }

    }

    /** Writes the given content to a file at the specified path.
      *
      * @param path
      *   The path where the file will be created.
      * @param content
      *   The content to write to the file.
      */
    private def writeFile(path: String, content: String): Unit = {
        val file = new File(path)
        file.createNewFile()
        val bw = new java.io.BufferedWriter(new java.io.FileWriter(file))
        bw.write(content)
        bw.close()
    }
}
