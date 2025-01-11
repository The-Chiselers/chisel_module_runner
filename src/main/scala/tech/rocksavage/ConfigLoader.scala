package tech.rocksavage

import tech.rocksavage.args.Conf
import tech.rocksavage.traits.ModuleConfig
import scala.sys.exit

object ConfigLoader {
  def loadConfigs(conf: Conf): Map[String, Any] = {
    conf.subcommand match {
      case Some(conf.verilog) => loadConfig(conf.verilog.configClass.toOption)
      case Some(conf.synth)   => loadConfig(conf.synth.configClass.toOption)
      case _ => {
        println("No run option provided")
        exit(1)
      }
    }
  }

  private def loadConfig(configClassPath: Option[String]): Map[String, Any] = {
    configClassPath match {
      case Some(path) =>
        val configClass = Class.forName(path).asSubclass(classOf[ModuleConfig])
        val configInstance = configClass.getDeclaredConstructor().newInstance()
        configInstance.getDefaultConfigs
      case None => {
        println("Config class could not be found")
        exit(1)
      }
    }
  }
}