package tech.rocksavage

import tech.rocksavage.args.Conf

import java.io.File

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
        Synthesizer.synthesize(conf, defaultConfigs, build_folder)
        Sta.sta(conf, defaultConfigs, build_folder)

        val synth_files: Map[String, String] =
            new File(s"$build_folder/synth/").listFiles
                .filter(file => (new File(file + "/gates.txt").exists()))
                .map(file => file.getName -> (file + "/gates.txt"))
                .toMap

        val gate_count_map: Map[String, String] = synth_files.map { entry =>
            val file  = entry._2
            val name  = entry._1
            val gates = scala.io.Source.fromFile(file).getLines().mkString
            name -> gates
        }.toMap

        val sta_files: Map[String, String] =
            new File(s"$build_folder/sta/").listFiles
                .filter(file => (new File(file + "/slack.txt").exists()))
                .map(file => file.getName -> (file + "/slack.txt"))
                .toMap

        val wns_map: Map[String, String] = sta_files.map { entry =>
            val file = entry._2
            val name = entry._1
            val wns  = scala.io.Source.fromFile(file).getLines().mkString
            name -> wns
        }.toMap

        val joined_map: Map[String, (String, String)] = gate_count_map.map {
            case (name, gates) =>
                name -> (gates, wns_map(name))
        }

        val texTableEntries = joined_map
            .map { entry =>
                val name  = entry._1
                val gates = entry._2._1
                val wns   = entry._2._2
                s"\\texttt{$name} & $gates & $wns \\\\ \\hline"
            }
            .mkString("\n")

        val texTable = s"""
\\renewcommand*{\\arraystretch}{1.25}
\\begingroup
\\small
\\rowcolors{2}{gray!30}{gray!10}
\\arrayrulecolor{gray!80}
\\begin{longtable}{|p{0.35\\textwidth}|p{0.20\\textwidth}|p{0.20\\textwidth}|}
\\hline
\\rowcolor{gray}
\\textcolor{white}{\\textbf{Config Name}} & \\textcolor{white}{\\textbf{Gates}} & \\textcolor{white}{\\textbf{WNS}} \\\\ \\hline
\\endfirsthead

\\hline
\\rowcolor{gray}
\\textcolor{white}{\\textbf{Config Name}} & \\textcolor{white}{\\textbf{Gates}} & \\textcolor{white}{\\textbf{WNS}} \\\\ \\hline
\\endhead

\\hline
\\endfoot
$texTableEntries
\\end{longtable}
\\captionof{table}{Synth and STA Results}
\\label{table:synth_sta}
\\endgroup
"""

        val texTableClean = texTable.replaceAll("_", "-")
        writeFile(s"$build_folder/doc/synth_sta.tex", texTableClean)

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
        file.getParentFile.mkdirs()
        file.createNewFile()
        val bw = new java.io.BufferedWriter(new java.io.FileWriter(file))
        bw.write(content)
        bw.close()
    }
}
