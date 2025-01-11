package tech.rocksavage

import tech.rocksavage.args.Conf
import tech.rocksavage.traits.ModuleConfig
import scala.sys.exit

/**
 * An object responsible for loading configuration classes based on the provided command-line arguments.
 */
object ConfigLoader {

  /**
   * Loads configurations based on the subcommand provided in the command-line arguments.
   *
   * @param conf The parsed command-line arguments.
   * @return A map of configuration names to their corresponding parameters.
   */
  def loadConfigs(conf: Conf): Map[String, Any] = {
    conf.subcommand match {
      case Some(conf.verilog) => loadConfig(conf.verilog.configClass.toOption) // Load config for Verilog generation
      case Some(conf.synth)   => loadConfig(conf.synth.configClass.toOption) // Load config for synthesis
      case _ => {
        println("No run option provided")
        exit(1) // Exit if no valid subcommand is provided
      }
    }
  }

  /**
   * Loads the configuration class dynamically using reflection.
   *
   * @param configClassPath The classpath of the configuration class.
   * @return A map of configuration names to their corresponding parameters.
   */
  private def loadConfig(configClassPath: Option[String]): Map[String, Any] = {
    configClassPath match {
      case Some(path) =>
        val configClass = Class.forName(path).asSubclass(classOf[ModuleConfig]) // Load the class
        val configInstance = configClass.getDeclaredConstructor().newInstance() // Create an instance
        configInstance.getDefaultConfigs // Retrieve the default configurations
      case None => {
        println("Config class could not be found")
        exit(1) // Exit if the configuration class is not found
      }
    }
  }
}