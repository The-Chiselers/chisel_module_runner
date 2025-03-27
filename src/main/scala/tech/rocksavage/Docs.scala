package tech.rocksavage

import tech.rocksavage.args.Conf

import java.io.File

/** An object responsible for synthesizing the design based on the provided
  * configurations.
  */
object Docs {

    /** Synthesizes the design for the specified module and configurations.
      *
      * @param conf
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
